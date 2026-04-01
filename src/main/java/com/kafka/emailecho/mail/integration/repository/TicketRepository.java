package com.kafka.emailecho.mail.integration.repository;

import com.kafka.emailecho.mail.integration.model.TicketResource;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TicketRepository extends MongoRepository<TicketResource,String> {

}
