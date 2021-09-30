package org.example.simple;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.example.simple.dto.SimpleDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.messaging.handler.annotation.Payload;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableKafka
@EnableKafkaStreams
@Slf4j
public class SampleSimpleApplication implements CommandLineRunner {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleSimpleApplication.class, args);
	}

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Override
	public void run(String... args) throws Exception {
		for (String arg : args) {
			TimeUnit.SECONDS.sleep(2);
			kafkaTemplate.send("text-topic", arg);
		}
	}

	@KafkaListener(topics = "text-topic")
	public void listenText(@Payload String text) {
		log.info("############ Received text data from kafka: {}. ##########", text);
	}

	@Bean
	public RecordMessageConverter jsonBodyMessageConverter() {
		return new StringJsonMessageConverter();
	}

	@Bean
	public KafkaAdmin.NewTopics multipartTopic(){
		return new KafkaAdmin.NewTopics(
				TopicBuilder.name("text-topic").build(),
				TopicBuilder.name("dto-topic").build()
		);
	}


	@KafkaListener(topics = "dto-topic")
	public void listenObject(@Payload SimpleDto dtoData) {
		log.info("############ Received object data from kafka: {}. ##########", dtoData);
	}

	@Bean
	public KStream<String, String> transformToDto(StreamsBuilder builder) {
		KStream<String, String> stream = builder.stream("text-topic", Consumed.with(Serdes.String(), Serdes.String()));
		stream
				.mapValues(text -> new SimpleDto(text,1,LocalDate.now()))
				.to("dto-topic", Produced.with(Serdes.String(), new JsonSerde()));
		return stream;
	}

}
