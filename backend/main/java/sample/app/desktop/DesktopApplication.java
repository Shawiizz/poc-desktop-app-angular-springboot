package sample.app.desktop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Desktop Application - Headless Backend Server
 * 
 * This application serves as the backend API and static file server.
 * The UI is handled by the native Tauri launcher.
 */
@Slf4j
@SpringBootApplication
public class DesktopApplication {

	private static final String APP_ID = "desktop-app";

	private final ServletWebServerApplicationContext webServerAppCtx;

	public DesktopApplication(ServletWebServerApplicationContext webServerAppCtx) {
		this.webServerAppCtx = webServerAppCtx;
	}

	public static void main(String[] args) {
		SpringApplication.run(DesktopApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		int port = webServerAppCtx.getWebServer().getPort();
		log.info("Backend server started on http://localhost:{}", port);
		
		// Write port to file for Tauri launcher
		writePortFile(port);
		
		log.info("Waiting for Tauri launcher to connect...");
	}

	/**
	 * Write the server port to a file in the app data directory.
	 * This allows the Tauri launcher to discover which port the backend is using.
	 */
	private void writePortFile(int port) {
		try {
			Path appDataDir = getAppDataDir();
			Files.createDirectories(appDataDir);
			Path portFile = appDataDir.resolve("backend.port");
			Files.writeString(portFile, String.valueOf(port));
			log.info("Port file written to: {}", portFile);
		} catch (IOException e) {
			log.error("Failed to write port file", e);
		}
	}

	private Path getAppDataDir() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return Path.of(System.getenv("APPDATA"), APP_ID);
		} else if (os.contains("mac")) {
			return Path.of(System.getProperty("user.home"), "Library", "Application Support", APP_ID);
		} else {
			return Path.of(System.getProperty("user.home"), ".local", "share", APP_ID);
		}
	}
}
