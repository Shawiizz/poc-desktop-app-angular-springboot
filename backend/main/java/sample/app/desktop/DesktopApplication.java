package sample.app.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class DesktopApplication {

	private final ServletWebServerApplicationContext webServerAppCtx;
	private final AppConfigService appConfigService;

	public DesktopApplication(ServletWebServerApplicationContext webServerAppCtx, AppConfigService appConfigService) {
		this.webServerAppCtx = webServerAppCtx;
		this.appConfigService = appConfigService;
	}

	public static void main(String[] args) {
		// Load app ID early for logging configuration
		String appId = loadAppIdEarly();
		System.setProperty("APP_ID", appId);
		
		SpringApplication.run(DesktopApplication.class, args);
	}

	/**
	 * Load app ID before Spring context is ready (for logback)
	 */
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
		
		// Output port to stdout for Tauri launcher to capture
		// Format: BACKEND_PORT:12345
		System.out.println("BACKEND_PORT:" + port);
		System.out.flush();
		
		log.info("Logs directory: {}", appConfigService.getLogsPath());
	}
}
