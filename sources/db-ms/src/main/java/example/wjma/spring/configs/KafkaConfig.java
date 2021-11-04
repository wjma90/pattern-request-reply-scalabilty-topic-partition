package example.wjma.spring.configs;

// import org.apache.kafka.common.TopicPartition;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
// import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
// import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import example.wjma.spring.model.EventDemo;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, EventDemo> producerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public ConsumerFactory<String, EventDemo> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
    }

    @Bean
    public KafkaTemplate<String, EventDemo> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, EventDemo>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventDemo> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setReplyTemplate(kafkaTemplate());
        // factory.setMessageConverter(simpleMapperConverter());
        factory.setErrorHandler(new SeekToCurrentErrorHandler(new FixedBackOff(0L, 0L))); // to deadletter topic, 0 retries | or without deadletter this can to controle kafkalistener
        return factory;
    }

    // @Bean
    // public DeadLetterPublishingRecoverer recoverer(KafkaTemplate<String, EventDemo> template) {
    //     DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
    //     (r, e) -> {
    //         LOG.debug("[DeadLetterPublishingRecoverer] e -> " + e.getClass());
    //         return new TopicPartition(r.topic() + ".dlt", r.partition()); // else send to error topic
    //     });
    //     return recoverer;
    // }

    // @Bean // not required if Jackson is on the classpath
    // public MessagingMessageConverter simpleMapperConverter() {
    //     MessagingMessageConverter messagingMessageConverter = new MessagingMessageConverter();
    //     messagingMessageConverter.setRawRecordHeader(true); // add kafka_data header into records
    //     return messagingMessageConverter;
    // }
}