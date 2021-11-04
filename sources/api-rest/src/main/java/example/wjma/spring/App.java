package example.wjma.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	public static byte[] intToBytesBigEndian(final int data) {
		return new byte[] {(byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff),
			(byte) ((data >> 8) & 0xff), (byte) ((data >> 0) & 0xff),};
	  }
}
