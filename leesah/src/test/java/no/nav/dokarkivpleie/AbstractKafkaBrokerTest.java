package no.nav.dokarkivpleie;

import no.nav.dokarkivpleie.config.CoreConfig;
import no.nav.dokarkivpleie.config.KafkaConfig;
import no.nav.dokarkivpleie.config.RepositoryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.wiremock.spring.EnableWireMock;

@Transactional
@EnableWireMock
@ActiveProfiles("itest")
@SpringBootTest(classes = {
		ApplicationTestConfig.class,
		RepositoryConfig.class,
		CoreConfig.class,
		KafkaConfig.class
})
@EmbeddedKafka(topics = "pdl.leesah-v1")
public abstract class AbstractKafkaBrokerTest {

	protected void reinitTransaction() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
		TestTransaction.start();
	}

}