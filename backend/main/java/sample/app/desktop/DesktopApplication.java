package sample.app.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import sample.app.desktop.config.AppConfigService;

import java.io.IOException;
import java.io.InputStream;

/**
 * Desktop Application - Headless Backend Server
 * 
 * This application serves as the backend API.
 * The UI is handled by the native Tauri launcher.
 */
@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class DesktopApplication {

	private final ServletWebServerApplicationContext webServerAppCtx;
	private final AppConfigService appConfigService;

	public static void main(String[] args) {
		String appId = loadAppIdEarly();
		System.setProperty("APP_ID", appId);
		
		SpringApplication.run(DesktopApplication.class, args);
	}

	private static String loadAppIdEarly() {
		try {
			ClassPathResource resource = new ClassPathResource("app-config.json");
			try (InputStream is = resource.getInputStream()) {
				JsonNode config = new ObjectMapper().readTree(is);
				if (config.has("id")) {
					return config.get("id").asText();
				}
			}
		} catch (IOException e) {
			// Ignore, use default
		}
		return "desktop-app";
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		int port = webServerAppCtx.getWebServer().getPort();
		log.info("{} v{} started on http://localhost:{}", 
			appConfigService.getName(), 
			appConfigService.getVersion(), 
			port);
		
		System.out.println("BACKEND_PORT:" + port);
		System.out.flush();
		
		log.info("Logs directory: {}", appConfigService.getLogsPath());
	}
}
