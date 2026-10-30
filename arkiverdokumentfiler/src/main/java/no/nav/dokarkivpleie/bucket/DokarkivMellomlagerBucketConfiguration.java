package no.nav.dokarkivpleie.bucket;

import com.google.cloud.storage.HttpStorageOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokarkivpleie.config.DokarkivpleieProperties;
import no.nav.dokarkivpleie.config.GoogleCloudProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Configuration
@Profile("nais")
public class DokarkivMellomlagerBucketConfiguration {

	@Bean
	@Lazy
	public DokarkivmellomlagerBucketStorage dokarkivMellomlagerBucketStorage(
			GoogleCloudProperties googleCloudProperties,
			DokarkivpleieProperties dokarkivpleieProperties
	) {
		Storage storage = createStorage(googleCloudProperties.getProject());
		return new DokarkivmellomlagerBucketStorage(storage, dokarkivpleieProperties.getArkiverDokumentfiler().getBucket());
	}

	Storage createStorage(String projectId) {
		return StorageOptions.newBuilder()
				.setProjectId(projectId)
				.setTransportOptions(HttpStorageOptions.defaults().getDefaultTransportOptions().toBuilder()
						.setConnectTimeout((int) SECONDS.toMillis(5))
						.setReadTimeout((int) SECONDS.toMillis(180))
						.build())
				.build().getService();
	}
}
