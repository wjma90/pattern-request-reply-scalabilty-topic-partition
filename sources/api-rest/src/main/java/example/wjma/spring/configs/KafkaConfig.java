package example.wjma.spring.configs;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

import example.wjma.spring.model.EventDemo;

@Configuration
public class KafkaConfig {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Value("${spring.kafka.consumer.reply-topic}")
    private String _kafkaConsumerReplyTopic;

    @Value("${spring.kafka.consumer.partitions_descriptor_filepath:EMPTY}")
    private String _kafkaConsumerAssignPartitions;

    @Bean
    public ProducerFactory<String, EventDemo> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public ConsumerFactory<String, EventDemo> consumerFactory() throws IOException {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public ReplyingKafkaTemplate<String, EventDemo, EventDemo> replyingTemplate(
            ProducerFactory<String, EventDemo> pf,
            ConcurrentMessageListenerContainer<String, EventDemo> repliesContainer) {

        return new ReplyingKafkaTemplate<>(pf, repliesContainer);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, EventDemo> repliesContainer(
            ConcurrentKafkaListenerContainerFactory<String, EventDemo> containerFactory) throws IOException {

        ConcurrentMessageListenerContainer<String, EventDemo> repliesContainer = containerFactory.createContainer(_kafkaConsumerReplyTopic);
        containerFactory.setConsumerFactory(consumerFactory());
        return repliesContainer;
    }

}