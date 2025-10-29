package com.Dolmeng_E.user.common.config;

import com.Dolmeng_E.user.common.dto.UserSignupFailEventDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServer;
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String offset;

    @Bean
    public ConsumerFactory<String, UserSignupFailEventDto> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);

        // 핵심 설정
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.Dolmeng_E.user.common.dto.UserSignupFailEventDto");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserSignupFailEventDto> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserSignupFailEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // 예외 발생 시 다음 레코드로 진행 (무한루프 방지)
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler((rec, ex) ->
                        System.err.println("[Kafka][User] 레코드 역직렬화 실패 → skip: " + ex.getMessage()),
                        new FixedBackOff(0L, 0L));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> notificationConsumerFactory(){
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> notificationKafkaListener(){
        ConcurrentKafkaListenerContainerFactory<String, String> listener
                = new ConcurrentKafkaListenerContainerFactory<>();
        listener.setConsumerFactory(notificationConsumerFactory());
        // 수동커밋을 위한 설정. 메시지를 배치단위로 커밋하는게 아닌, 레코드 단위로 커밋
        listener.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return listener;
    }
}

