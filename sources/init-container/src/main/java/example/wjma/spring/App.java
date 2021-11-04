package example.wjma.spring;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaAdmin;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.ClientBuilder;

@SpringBootApplication
public class App implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	@Value(value = "${DEPLOYMENT_NAME}")
	private String 	DEPLOYMENT_NAME = "";

	private int 	DEPLOYMENT_REPLICAS = 0;

	@Value(value = "${POD_NAMESPACE:default}")
	private String	POD_NAMESPACE;

	@Value(value = "${TOPICS:testing}")
	private String TOPICS;

	private int	   TOPIC_PARTITIONS_COUNT = 0;

	@Value(value = "${TOPIC_REPLICATION_FACTOR:1}")
	private int	   TOPIC_REPLICATION_FACTOR = 1;

	private int	   TOPIC_PARTITIONS_DEFAULT = 1;

	@Value(value = "${kafka_brokers:localhost:9092}")
    private String bootstrapAddress;


	@Autowired
	private KafkaAdmin kafkaAdmin;

	private AdminClient kafkaAdminClient = null;

	private AppsV1Api appsV1Api = null;


	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
		LOG.info("STOPPED  : Spring boot application stopped");
	}

	@Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

	@Override
    public void run(ApplicationArguments args) throws Exception {
        LOG.info("bootstrapAddress: {}, deploymentName: {}", bootstrapAddress, this.DEPLOYMENT_NAME);

		if(bootstrapAddress.split(",").length > 2){
			this.TOPIC_REPLICATION_FACTOR = 2;
		}

		ApiClient apiClient = ClientBuilder.defaultClient();
		appsV1Api = new AppsV1Api(apiClient);

		V1Deployment deployment = appsV1Api.readNamespacedDeployment(this.DEPLOYMENT_NAME, this.POD_NAMESPACE, null, null, null);
		this.DEPLOYMENT_REPLICAS	= deployment.getSpec().getReplicas();
		LOG.info("deployment: {} -> replicas: {}", deployment.getMetadata().getName(), this.DEPLOYMENT_REPLICAS);
		
		kafkaAdminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());

		for(String topic : Arrays.asList(TOPICS.split(","))){
			try{
				LOG.info("1.- Verify TOPIC");
				this.TOPIC_PARTITIONS_COUNT = getPartitionsOfTopic(topic);
			}catch(ExecutionException e){
				if(e.getMessage().contains("org.apache.kafka.common.errors.UnknownTopicOrPartitionException")){
					LOG.error("1.1.- {} not exits", topic);
					createTopicDefault(topic);
				}			
			}
	
			execute(topic);
		}
    }

	private void execute(String topic) throws InterruptedException, ExecutionException, ApiException, FileNotFoundException, UnsupportedEncodingException {
		this.TOPIC_PARTITIONS_COUNT = getPartitionsOfTopic(topic);
		LOG.info("2.- Get Partitions count = {}", this.TOPIC_PARTITIONS_COUNT);
		if(this.DEPLOYMENT_REPLICAS < this.TOPIC_PARTITIONS_COUNT){
			LOG.info("3.2.- RT < PARTITIONS_COUNT");
		}else if(this.DEPLOYMENT_REPLICAS > this.TOPIC_PARTITIONS_COUNT){
				LOG.info("3.3.- RT > PARTITIONS_COUNT");
				alertTopicWithPartitions(topic, this.DEPLOYMENT_REPLICAS);
		}else {
			LOG.info("3.2.- RT = PARTITIONS_COUNT");
		}
	}

	private void alertTopicWithPartitions(String topic, int partitionsTotal) throws InterruptedException, ExecutionException{
		NewPartitions newPartitionRequest = NewPartitions.increaseTo(partitionsTotal);
		this.kafkaAdminClient.createPartitions(Collections.singletonMap(topic, newPartitionRequest)).all().get();
	}

	private int getPartitionsOfTopic(String topic) throws InterruptedException, ExecutionException{
		DescribeTopicsResult dtr = kafkaAdminClient.describeTopics(new ArrayList<String>(Arrays.asList(topic)) );
		if(dtr.values().get(topic).get() != null){
			LOG.info("preview :: " + dtr.values().get(topic).get().name() + " | " + dtr.values().get(topic).get().partitions().size());
			return dtr.values().get(topic).get().partitions().size();
		}
		throw new ExecutionException("UNEXPECTED MESSAGE", new IllegalArgumentException());
	}

	private void createTopicDefault(String topic) throws InterruptedException, ExecutionException{
		LOG.info("1.2.- Create topic {} because not exits", topic);
		NewTopic topico = new NewTopic(topic, this.TOPIC_PARTITIONS_DEFAULT, Short.valueOf(this.TOPIC_REPLICATION_FACTOR+""));
		CreateTopicsResult ctr = kafkaAdminClient.createTopics(new ArrayList<NewTopic>(Arrays.asList(topico)));
		ctr.all().isDone();
	}
}