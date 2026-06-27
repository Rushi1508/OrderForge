package com.orderforge.payment;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;
import java.util.HashMap;
import java.util.Map;
@Configuration
public class KafkaConsumerConfig {
    private final JsonMapper jsonMapper;
    private final MeterRegistry meterRegistry;
    public KafkaConsumerConfig(JsonMapper jsonMapper, MeterRegistry meterRegistry) {
        this.jsonMapper = jsonMapper;
        this.meterRegistry = meterRegistry;
    }
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JacksonJsonDeserializer<Object> valueDeserializer =
                new JacksonJsonDeserializer<>(jsonMapper);
        valueDeserializer.addTrustedPackages("com.orderforge.events");
        valueDeserializer.setUseTypeHeaders(true);
        DefaultKafkaConsumerFactory<String, Object> factory =
                new DefaultKafkaConsumerFactory<>(
                        config,
                        new StringDeserializer(),
                        valueDeserializer);
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return factory;
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}