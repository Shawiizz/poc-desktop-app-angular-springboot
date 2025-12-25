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
import java.nio.file.Files;
import java.nio.file.Path;

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
		
		// Write port to file for Tauri launcher
		writePortFile(port);
		
		log.info("Logs directory: {}", appConfigService.getLogsPath());
	}

	/**
	 * Write the server port to a file in the app data directory.
	 * This allows the Tauri launcher to discover which port the backend is using.
	 */
	private void writePortFile(int port) {
		try {
			Path appDataDir = Path.of(appConfigService.getAppDataPath());
			Files.createDirectories(appDataDir);
			Path portFile = appDataDir.resolve("backend.port");
			Files.writeString(portFile, String.valueOf(port));
			log.info("Port file written to: {}", portFile);
		} catch (IOException e) {
			log.error("Failed to write port file", e);
		}
	}
}
