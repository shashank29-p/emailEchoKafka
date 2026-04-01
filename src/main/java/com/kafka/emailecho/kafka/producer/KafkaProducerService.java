package com.kafka.emailecho.kafka.producer;

import com.kafka.emailecho.mail.integration.model.EmailResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

  private static final String TOPIC = "topic1";
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void sendMessage(EmailResponse emailResponse) {
    try {
      EmailResponse mailResponseForPartition = new EmailResponse();
      mailResponseForPartition.setFrom(emailResponse.getFrom());
      mailResponseForPartition.setRecipients(emailResponse.getRecipients());
      mailResponseForPartition.setSubject(emailResponse.getSubject());
      mailResponseForPartition.setBody(emailResponse.getBody());
      mailResponseForPartition.setEmailResponseId(emailResponse.getEmailResponseId());
      kafkaTemplate.send(TOPIC, 0, "", mailResponseForPartition);
      kafkaTemplate.send(TOPIC, 1, "", emailResponse.getAttachments());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
