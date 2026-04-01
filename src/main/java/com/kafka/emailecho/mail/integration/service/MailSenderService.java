package com.kafka.emailecho.mail.integration.service;


import com.kafka.emailecho.mail.integration.model.TicketResource;
import com.kafka.emailecho.mail.integration.model.TicketResource.Priority;
import com.kafka.emailecho.mail.integration.repository.TicketRepository;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class MailSenderService {

  @Value("${mail.imaps.username}")
  private String username;

  @Value("${mail.imaps.password}")
  private String password;

  @Autowired
  private TicketRepository ticketRepository;

  public Session configureProperties() {
    String host = "smtp.gmail.com";
    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", "587");
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");
    Authenticator authenticator = new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    };
    Session session = Session.getInstance(props, authenticator);
    return session;
  }

  public void sendMailTemplate(String to, String emailResponseId) {
    try {
      Session session = configureProperties();
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      message.setRecipients(Message.RecipientType.TO,
          InternetAddress.parse(to));
      message.setSubject("Email Received!!!");
      Resource resource = new ClassPathResource("templates/email-template.txt");
      String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
      String ticketId = assignTicketId(emailResponseId);
      template = template.replace("{{Ticket-id}}", ticketId);
      message.setText(template);
      Transport.send(message);
      log.info("Email Message Sent Successfully");
    } catch (MessagingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String assignTicketId(String emailResponseId) {
    String ticketId = UUID.randomUUID().toString();
    ticketRepository.save(TicketResource.builder().ticketId(ticketId)
        .ticketPriority(Priority.Low).emailResponseId(emailResponseId).build());
    return ticketId;
  }
}
