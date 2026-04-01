package com.kafka.emailecho.mail.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "email.response")
public class EmailResponse {

  private String from;

  private String recipients;
  private String subject;

  private String body;

  private FileDetails attachments;

  private String emailResponseId;


  @Data
  @AllArgsConstructor
  @Builder
  @NoArgsConstructor
  public static class FileDetails{
    private String fileName;

    private String fileType;

    private String content;
  }

}
