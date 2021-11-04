package example.wjma.spring.controller;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import example.wjma.spring.App;
import example.wjma.spring.model.EventDemo;
import example.wjma.spring.model.Person;

@RestController
@RequestMapping("api/v1/person")
public class ApiController {

	private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);

	@Value("${spring.kafka.producer.request-topic}")
    private String _kafkaConsumerDbMsDemoRequestTopic;

	@Value("${spring.kafka.producer.error-topic}")
    private String _kafkaConsumerDbMsDemoErrorTopic;

	@Value("${spring.kafka.consumer.reply-topic}")
    private String _kafkaConsumerDbMsDemoReplyTopic;

	@Autowired
	ReplyingKafkaTemplate<String, EventDemo,EventDemo> replyKafkaTemplate;

	@Autowired
	KafkaTemplate<String, EventDemo> kafkaTemplate;

	@GetMapping(value="", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAll(
		Pageable pageable,
		@RequestHeader("requestID") String requestID){
		String rqID = requestID == null ? UUID.randomUUID().toString() : requestID;


		int partitionAssignedIndex 	= new Random().nextInt(replyKafkaTemplate.getAssignedReplyTopicPartitions().toArray().length);
		int partitionAssigned		= ((TopicPartition) replyKafkaTemplate.getAssignedReplyTopicPartitions().toArray()[partitionAssignedIndex]).partition();

		HttpHeaders headers = new HttpHeaders();
		headers.add("requestID", rqID);
		headers.add("partition", partitionAssigned + "");
		headers.add("hostname", System.getenv("POD_NAME"));

		try {

			if(requestID == null){
				LOG.info("Generando requestID :: {}", rqID);
			}

			EventDemo event = new EventDemo();
			event.setOperation("GET_ALL_PERSONS");
			event.setRequestID(rqID);
			
			ProducerRecord<String, EventDemo> record = new ProducerRecord<String, EventDemo>(_kafkaConsumerDbMsDemoRequestTopic, partitionAssigned, null, event);
			record.headers()
				.add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, _kafkaConsumerDbMsDemoReplyTopic.getBytes()))
				.add(new RecordHeader(KafkaHeaders.REPLY_PARTITION, App.intToBytesBigEndian(partitionAssigned) ));

			RequestReplyFuture<String, EventDemo, EventDemo> sendAndReceive = replyKafkaTemplate.sendAndReceive(record);

			SendResult<String, EventDemo> sendResult = sendAndReceive.getSendFuture().get(5, TimeUnit.SECONDS);

			sendResult.getProducerRecord().headers().forEach(
				header -> {
					LOG.info("[{}] header-key={}, header-value={},",rqID, header.key(), header.key().equalsIgnoreCase("kafka_replyPartition") ? ByteBuffer.wrap(header.value()).getInt() : new String(header.value(), StandardCharsets.UTF_8));
				});
			LOG.info("[{}] partition-{}, offset-{}", rqID, sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset() );

			ConsumerRecord<String, EventDemo> consumerRecord = sendAndReceive.get(5, TimeUnit.SECONDS);

			EventDemo result = consumerRecord.value();

			if(!result.isStatus()){
				LOG.info("error sent topic-{}, message-{}", _kafkaConsumerDbMsDemoErrorTopic, result);
				kafkaTemplate.send(_kafkaConsumerDbMsDemoErrorTopic, null, consumerRecord.value()).wait(1000L);
			}

			return new ResponseEntity<List<Person>>(result.getResponse() == null ? new ArrayList<Person>() : result.getResponse(), headers, result.isStatus() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
		}catch(Exception e) {
			LOG.error("Error: ",e);
			return new ResponseEntity<String>(e.getMessage(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	
	@GetMapping(value="/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getById(
		@PathVariable("id") final int id,
		@RequestHeader("requestID") String requestID){
			
		String rqID = requestID == null ? UUID.randomUUID().toString() : requestID;

		int partitionAssignedIndex 	= new Random().nextInt(replyKafkaTemplate.getAssignedReplyTopicPartitions().toArray().length);
		int partitionAssigned		= ((TopicPartition) replyKafkaTemplate.getAssignedReplyTopicPartitions().toArray()[partitionAssignedIndex]).partition();

		HttpHeaders headers = new HttpHeaders();
		headers.add("requestID", rqID);
		headers.add("partition", partitionAssigned + "");
		headers.add("hostname", System.getenv("POD_NAME"));

		try {

			if(requestID == null){
				LOG.info("Generando requestID :: {}", rqID);
			}

			EventDemo event = new EventDemo();
			event.setOperation("GET_PERSON");
			event.setRequestID(rqID);

			Person input = new Person();
			input.setId(id);
			List<Person> data = new ArrayList<Person>();
			data.add(input);
			event.setData(data);
			
			ProducerRecord<String, EventDemo> record = new ProducerRecord<String, EventDemo>(_kafkaConsumerDbMsDemoRequestTopic, partitionAssigned, null, event);
			record.headers()
				.add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, _kafkaConsumerDbMsDemoReplyTopic.getBytes()))
				.add(new RecordHeader(KafkaHeaders.REPLY_PARTITION, App.intToBytesBigEndian(partitionAssigned)));

			RequestReplyFuture<String, EventDemo, EventDemo> sendAndReceive = replyKafkaTemplate.sendAndReceive(record);

			SendResult<String, EventDemo> sendResult = sendAndReceive.getSendFuture().get(5, TimeUnit.SECONDS);

			LOG.info("operation type={}", event.getOperation());
			sendResult.getProducerRecord().headers().forEach(
				header -> {
					LOG.info("[{}] header-key={}, header-value={},",rqID, header.key(), header.key().equalsIgnoreCase("kafka_replyPartition") ? ByteBuffer.wrap(header.value()).getInt() : new String(header.value(), StandardCharsets.UTF_8));
				});
			LOG.info("[{}] partition-{}, offset-{}", rqID, sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset() );

			ConsumerRecord<String, EventDemo> consumerRecord = sendAndReceive.get(5, TimeUnit.SECONDS);

			EventDemo result = consumerRecord.value();

			if(!result.isStatus()){
				LOG.info("error sent topic-{}, message-{}", _kafkaConsumerDbMsDemoErrorTopic, result);
				kafkaTemplate.send(_kafkaConsumerDbMsDemoErrorTopic, result).get(2, TimeUnit.SECONDS);
				return new ResponseEntity<String>(!result.getMessage().isEmpty() ? result.getMessage() : "" , headers,  HttpStatus.INTERNAL_SERVER_ERROR);
			}

			return new ResponseEntity<List<Person>>(result.getResponse(),headers, HttpStatus.OK);
		}catch(Exception e) {
			LOG.error("Error: ",e);
			return new ResponseEntity<String>(e.getMessage(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
