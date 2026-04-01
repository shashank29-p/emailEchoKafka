package com.kafka.emailecho.mail.integration.config;

import com.kafka.emailecho.mail.integration.util.EmailEventListener;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailConfiguration {

  @Value("${email.port}")
  private String emailPort;

  @Value("${mail.imaps.username}")
  private String emailUsername;

  @Value("${mail.imaps.password}")
  private String emailPassword;

  @Bean
  public Session mailSession() {
    Properties props = new Properties();
    props.setProperty("mail.store.protocol", "imaps");
    props.setProperty("mail.imaps.host", "imap.gmail.com");
    props.setProperty("mail.imaps.port", emailPort);
    props.setProperty("mail.imaps.ssl", "true");
    Session session = Session.getInstance(props, new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(emailUsername, emailPassword);
      }
    });
    session.setDebug(true);
    return session;
  }

  @Bean
  public EmailEventListener emailListener() {
    return new EmailEventListener(mailSession());
  }

}
