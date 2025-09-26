package sample.app.desktop;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import sample.app.desktop.ui.ChromiumWindow;
import sample.app.desktop.util.SingleInstanceGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class DesktopApplication {
	private final ChromiumWindow chromiumWindow;
	private final ServletWebServerApplicationContext webServerAppCtx;
    private final Environment environment;
    private final SingleInstanceGuard singleInstanceGuard;

	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(DesktopApplication.class, args);
	}

	@Bean
	public ApplicationRunner singleInstanceRunner() {
		return args -> singleInstanceGuard.acquire();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		// Don't launch the desktop UI when running with the 'dev' profile
		boolean devActive = false;
		for (String profile : environment.getActiveProfiles()) {
			if ("dev".equalsIgnoreCase(profile)) {
				devActive = true;
				break;
			}
		}
		if (!devActive) {
			launchDesktopApp();
		} else {
			log.info("Dev profile active -> skipping desktop UI launch (Chromium window).");
		}
	}
	
	private void launchDesktopApp() {
		try {
			String remote = environment.getProperty("frontend.remote-url", "").trim();
			String targetUrl;
			if (!remote.isEmpty()) {
				// Use remote frontend URL
				log.info("Loading remote frontend: {}", remote);
				targetUrl = remote;
			} else {
				int port = webServerAppCtx.getWebServer().getPort();
				targetUrl = "http://localhost:" + port;
				log.info("Loading packaged frontend on {}", targetUrl);
			}
			chromiumWindow.createAndShowWindow(targetUrl);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}
