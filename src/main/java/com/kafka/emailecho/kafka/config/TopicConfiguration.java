package com.kafka.emailecho.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicConfiguration {

  @Bean
  public NewTopic createTopic() {
    return TopicBuilder.name("topic1")
        .partitions(5)
        .replicas(1)
        .config(TopicConfig.RETENTION_MS_CONFIG,"3600000")
        .build();
  }

}
