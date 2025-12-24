package sample.app.desktop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;

/**
 * Desktop Application - Headless Backend Server
 * 
 * This application serves as the backend API and static file server.
 * The UI is handled by the native Bun WebView launcher.
 */
@Slf4j
@SpringBootApplication
public class DesktopApplication {

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
		log.info("Waiting for Bun WebView launcher to connect...");
	}
}
