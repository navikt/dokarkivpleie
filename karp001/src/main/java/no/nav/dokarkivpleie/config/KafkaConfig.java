package no.nav.dokarkivpleie.config;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.DokarkivpleieFunctionalException;
import no.nav.person.pdl.leesah.Personhendelse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.ExponentialBackOff;

import static java.time.Duration.ofSeconds;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

	@Value("${kafka.retry.initial-interval-ms:1000}")
	private long initialIntervalMs;

	@Value("${kafka.retry.backoff-multiplier:1.5}")
	private double backoffMultiplier;

	@Bean
	ConcurrentKafkaListenerContainerFactory<String, Personhendelse> kafkaListenerContainerFactory(ConsumerFactory<String, Personhendelse> kafkaConsumerFactory) {

		ConcurrentKafkaListenerContainerFactory<String, Personhendelse> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(kafkaConsumerFactory);
		factory.getContainerProperties().setAuthExceptionRetryInterval(ofSeconds(10L));

		var errorHandler = getDefaultErrorHandler();
		errorHandler.addNotRetryableExceptions(DokarkivpleieFunctionalException.class);
		errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
			if (isRetryableException(ex)) {
				log.warn("Teknisk feil i prosessering av melding. Forsøk nr. {} for record med key={}, offset={}",
						deliveryAttempt, record.key(), record.offset(), ex);
			}
		});
		factory.setCommonErrorHandler(errorHandler);

		return factory;
	}

	private @NonNull DefaultErrorHandler getDefaultErrorHandler() {
		var backOff = new ExponentialBackOff(initialIntervalMs, backoffMultiplier);

		// Recovery handler - called when all retries exhausted OR for non-retryable exceptions
		ConsumerRecordRecoverer recoverer = (record, ex) -> {
			//OBS! MessageConversionException vil logge hele den innkommende kafka-recorden hvis man printer stacktrace (persondata)
			if (isMessageConversionException(ex)) {
				log.error("Kafkamelding feilet permanent pga deserialiseringsfeil og hoppes over. Topic={}, key={}, offset={}, partition={}, message{}, cause={}",
						record.topic(), record.key(), record.offset(), record.partition(), ex.getMessage(), ex.getCause().toString());
			} else {
				log.error("Kafkamelding feilet permanent og hoppes over. Topic={}, key={}, offset={}, partition={}, exception={}",
						record.topic(), record.key(), record.offset(), record.partition(), ex.getMessage(), ex);
			}
		};

		return new DefaultErrorHandler(recoverer, backOff);
	}

	private boolean isRetryableException(Exception ex) {
		return !isDokarkivpleieFunctionalException(ex) && !isMessageConversionException(ex);
	}

	private boolean isDokarkivpleieFunctionalException(Exception ex) {
		return ex.getCause() instanceof DokarkivpleieFunctionalException;
	}

	private boolean isMessageConversionException(Exception ex) {
		return ex.getCause() instanceof MessageConversionException;
	}

}