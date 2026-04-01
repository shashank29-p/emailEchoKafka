package com.kafka.emailecho.mail.integration.util;

import com.kafka.emailecho.kafka.producer.KafkaProducerService;
import com.kafka.emailecho.mail.integration.model.EmailResponse;
import com.kafka.emailecho.mail.integration.model.EmailResponse.FileDetails;
import com.kafka.emailecho.mail.integration.repository.EmailListenerRepository;
import com.kafka.emailecho.mail.integration.service.EmailListenerServiceImpl;
import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class EmailEventListener extends MessageCountAdapter {

  private Session session;

  @Value("${mail.imaps.username}")
  private String username;

  @Value("${mail.imaps.password}")
  private String password;

  @Autowired
  private EmailListenerRepository listenerRepository;

  @Autowired
  private KafkaProducerService kafkaProducerService;

  public EmailEventListener(Session session) {
    this.session = session;
  }

  public void listenForIncomingEmails() {
    try {
      Store store = session.getStore("imaps");
      store.connect(username, password);
      IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
      inbox.open(Folder.READ_WRITE);
      Thread keepAliveThread = new Thread(new EmailListenerServiceImpl(inbox),
          "IdleConnectionKeepAlive");
      keepAliveThread.start();
      inbox.addMessageCountListener(new MessageCountAdapter() {
        @Override
        public void messagesAdded(MessageCountEvent event) {
          Message[] messages = event.getMessages();
          for (Message message : messages) {
            try {
              log.info("New email received: " + message.getSubject());
              handleIncomingMessage(message);
            } catch (Exception exception) {
              handleException(exception);
            }
          }
        }
      });
      while (!Thread.interrupted()) {
        idleInbox(inbox);
      }
      if (keepAliveThread.isAlive()) {
        keepAliveThread.interrupt();
      }
    } catch (Exception exception) {
      handleException(exception);
    }
  }

  private void handleIncomingMessage(Message message) {
    try {
      EmailResponse emailResponse = createEmailResponse(message);
      kafkaProducerService.sendMessage(emailResponse);
      listenerRepository.save(emailResponse);
    } catch (Exception e) {
      handleException(e);
    }
  }

  private EmailResponse createEmailResponse(Message message) {
    try {
      EmailResponse emailResponse = new EmailResponse();
      emailResponse.setEmailResponseId(UUID.randomUUID().toString());
      emailResponse.setSubject(message.getSubject());
      emailResponse.setFrom(extractEmailAddress(Arrays.toString(message.getFrom())));
      emailResponse.setRecipients(Arrays.toString(message.getAllRecipients()));
      processAttachments(message, emailResponse);
      String bodyContent = getTextFromMessage(message);
      emailResponse.setBody(bodyContent.trim());
      return emailResponse;
    } catch (Exception e) {
      handleException(e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private void processAttachments(Message message, EmailResponse emailResponse) {
    try {
      if (message.isMimeType("multipart/mixed")) {
        Multipart multipart = (Multipart) message.getContent();
        for (int i = 0; i < multipart.getCount(); i++) {
          BodyPart bodyPart = multipart.getBodyPart(i);
          if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
            handleAttachment(bodyPart, emailResponse);
          }
        }
      }
    } catch (IOException | MessagingException e) {
      handleException(e);
    }
  }

  private void handleAttachment(BodyPart bodyPart, EmailResponse emailResponse)
      throws MessagingException, IOException {
    String fileName = bodyPart.getFileName();
    InputStream attachmentInputStream = bodyPart.getInputStream();
    byte[] attachmentBytes = readBytesFromInputStream(attachmentInputStream);
    String base64Content = Base64.getEncoder().encodeToString(attachmentBytes);
    log.info(fileName + " File name from the mail");
    emailResponse.setAttachments(FileDetails.builder().fileName(fileName)
        .fileType(bodyPart.getContentType())
        .content(base64Content).build());
  }

  private byte[] readBytesFromInputStream(InputStream inputStream) throws IOException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      return outputStream.toByteArray();
    }
  }

  private String getTextFromMessage(Message message) {
    try {
      if (message.isMimeType("text/plain")) {
        return message.getContent().toString();
      }
      if (message.isMimeType("multipart/*")) {
        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
        return getTextFromMimeMultipart(mimeMultipart);
      }
    } catch (Exception exception) {
      handleException(exception);
    }
    return "";
  }

  private String getTextFromMimeMultipart(MimeMultipart mimeMultipart)
      throws MessagingException, IOException {
    StringBuilder result = new StringBuilder();
    boolean foundPlainText = false;
    for (int i = 0; i < mimeMultipart.getCount(); i++) {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      if (bodyPart.isMimeType("text/plain") && !foundPlainText) {
        result.append("\n").append(bodyPart.getContent());
        foundPlainText = true;
      } else if (!foundPlainText) {
        result.append(parseBodyPart(bodyPart));
      }
    }
    return result.toString();
  }

  private String parseBodyPart(BodyPart bodyPart) throws MessagingException, IOException {
    if (bodyPart.isMimeType("text/html")) {
      return "\n" + Jsoup.parse(bodyPart.getContent().toString()).text();
    }
    if (bodyPart.getContent() instanceof MimeMultipart) {
      return getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
    }
    return "";
  }

  private void idleInbox(IMAPFolder inbox) {
    try {
      log.info("Starting IDLE");
      inbox.idle();
    } catch (MessagingException e) {
      handleException(e);
    }
  }

  private void handleException(Exception exception) {
    log.error(exception.getMessage(), exception);
  }


  private String extractEmailAddress(String str) {
    Pattern pattern = Pattern.compile("\\b[\\w\\.-]+@[\\w\\.-]+\\.\\w{2,4}\\b");
    Matcher matcher = pattern.matcher(str);
    if (matcher.find()) {
      return matcher.group();
    }
    return null;
  }

}
