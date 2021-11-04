package example.wjma.spring.services;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import example.wjma.spring.dao.PersonDAO;
import example.wjma.spring.model.EventDemo;
import example.wjma.spring.model.Person;

@Service
public class PreferenceServiceImp {

    private static final Logger LOG = LoggerFactory.getLogger(PreferenceServiceImp.class);

    @Autowired
	private PersonDAO personDao;

    @KafkaListener(topics = "${spring.kafka.consumer.request-topic}", errorHandler = "myTopicErrorHandler")
    @SendTo
    public Message<?> listen(
        @Payload EventDemo message, 
        @Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTopic,
        @Header(KafkaHeaders.REPLY_PARTITION) byte[] replyPartition,
        @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationID,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
        @Header(KafkaHeaders.OFFSET) int offset) {

        LOG.info("received message=[{}] from partition-{}, offset-{}, reply_topic-{}, reply_partition-{}, correlationId-{}", 
            message, partition, offset, String.valueOf(replyTopic), ByteBuffer.wrap(replyPartition).getInt(), String.valueOf(correlationID));

        message.setStatus(false);

        if(message != null && !message.getOperation().isEmpty()){
            List<Person> persons = null;
            switch(message.getOperation().toUpperCase()){
                case "GET_ALL_PERSONS": 
                    persons = this.personDao.findAll();
                    break;
                case "GET_PERSON": 
                    persons = List.of(this.personDao.getById(message.getData().get(0).getId()));
                    break;
                default:
                    message.setMessage("INVALID_OPERATION");
                    throw new IllegalArgumentException("INCORRECT DATA");
            }
            message.setResponse(persons);
            message.setStatus(persons != null && !persons.isEmpty());
            message.setMessage( (persons != null && !persons.isEmpty()) ? "" : "EMPTY");
        } else {
            throw new IllegalArgumentException("EMPTY OPERATION");
        }

        return MessageBuilder.withPayload(message)
            .setHeader(KafkaHeaders.TOPIC, replyTopic)
            .setHeader(KafkaHeaders.PARTITION_ID, ByteBuffer.wrap(replyPartition).getInt())
            .setHeader(KafkaHeaders.CORRELATION_ID, correlationID)
            .build();
    }

    @Bean
    public KafkaListenerErrorHandler myTopicErrorHandler() {
        return (msg, ex) -> {
            LOG.error("business exception message=[{}] from partition-{} with offset-{} | error=[{}]", msg, msg.getHeaders().get(KafkaHeaders.RECEIVED_PARTITION_ID), msg.getHeaders().get(KafkaHeaders.OFFSET),ex);
            return msg; // return null -> disabled @sendTo
        };
    }
}
