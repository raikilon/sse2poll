package ch.sse2poll.api.demo;

import ch.sse2poll.core.framework.config.Sse2PollAutoConfiguration;
import ch.sse2poll.core.framework.web.PolledGetAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({Sse2PollAutoConfiguration.class, PolledGetAspect.class})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
