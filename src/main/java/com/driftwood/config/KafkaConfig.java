package com.driftwood.config;

import com.driftwood.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic stepDispatchTopic() {
        return TopicBuilder.name(Topics.STEP_DISPATCH).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic stepResultTopic() {
        return TopicBuilder.name(Topics.STEP_RESULT).partitions(1).replicas(1).build();
    }
}
