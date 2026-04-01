package com.kafka.emailecho.mail.integration.repository;

import com.kafka.emailecho.mail.integration.model.EmailResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailListenerRepository extends MongoRepository<EmailResponse, String> {

}
