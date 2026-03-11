package no.nav.dokarkivpleie.config;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import no.nav.dokarkivpleie.slack.SlackProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(value = {
		DokarkivpleieProperties.class,
		NaisProperties.class,
		SlackProperties.class
})
@EnableScheduling
@EnableResilientMethods
public class CoreConfig {

	@Bean
	MethodsClient slackClient(SlackProperties slackProperties) {
		return Slack.getInstance().methods(slackProperties.token());
	}
}