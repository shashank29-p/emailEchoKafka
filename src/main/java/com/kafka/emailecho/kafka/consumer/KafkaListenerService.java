package com.kafka.emailecho.kafka.consumer;

import com.kafka.emailecho.mail.integration.model.EmailResponse;
import com.kafka.emailecho.mail.integration.model.EmailResponse.FileDetails;
import com.kafka.emailecho.mail.integration.service.MailSenderService;
import java.io.FileOutputStream;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaListenerService {

  private static final String TOPIC = "topic1";
  @Autowired
  private MailSenderService mailSenderService;

  @KafkaListener(topics = TOPIC, topicPartitions = {
      @TopicPartition(topic = TOPIC, partitionOffsets =
          {@PartitionOffset(partition = "0", initialOffset = "0")})})
  public void listenKafkaIncomingMessages(EmailResponse data,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    try {
      log.info("Listener 1 - Received message : " + data + " from partition " + partition);
      mailSenderService.sendMailTemplate(data.getFrom(), data.getEmailResponseId());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @KafkaListener(topics = TOPIC, topicPartitions = {
      @TopicPartition(topic = TOPIC, partitionOffsets =
          {@PartitionOffset(partition = "1", initialOffset = "0")})})
  public void listenKafkaIncomingAttachment(FileDetails data,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
    log.info("Listener 2 - Received message : " + data + " from partition " + partition);
    String bytes = data.getContent();
    byte[] decodedBytes = Base64.getDecoder().decode(bytes);
    try (FileOutputStream fos = new FileOutputStream(data.getFileName())) {
      fos.write(decodedBytes);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @DltHandler
  public void handleError(EmailResponse response
      , @Header(KafkaHeaders.RECEIVED_PARTITION) int partition){
    mailSenderService.sendMailTemplate(response.getFrom(), response.getEmailResponseId());
    log.info("Error occurred for partition "+ partition);
  }
}
