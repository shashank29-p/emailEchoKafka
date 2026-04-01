package com.kafka.emailecho.mail.integration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ticket")
public class TicketResource {


  @Id
  private String ticketId;

  private Priority ticketPriority;

  private String emailResponseId;

  public enum Priority{
    Low, Medium, High
  }
}
