package sample.app.desktop.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service to access application configuration from app.config.json
 */
@Slf4j
@Service
public class AppConfigService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Getter
    private String name = "Desktop App";
    
    @Getter
    private String id = "desktop-app";
    
    @Getter
    private String version = "1.0.0";
    
    @Getter
    private String description = "";

    @PostConstruct
    public void init() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            ClassPathResource resource = new ClassPathResource("app-config.json");
            try (InputStream is = resource.getInputStream()) {
                JsonNode config = objectMapper.readTree(is);
                
                if (config.has("name")) {
                    this.name = config.get("name").asText();
                }
                if (config.has("id")) {
                    this.id = config.get("id").asText();
                }
                if (config.has("version")) {
                    this.version = config.get("version").asText();
                }
                if (config.has("description")) {
                    this.description = config.get("description").asText();
                }
                
                log.info("Loaded app config: {} v{}", name, version);
            }
        } catch (IOException e) {
            log.warn("Could not load app-config.json, using defaults: {}", e.getMessage());
        }
    }

    /**
     * Get the application data directory path (for logs, cache, etc.)
     * Uses the app ID to create a unique folder per OS
     */
    public String getAppDataPath() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home") + "/AppData/Local";
            }
            return appData + "/" + id;
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + "/Library/Application Support/" + id;
        } else {
            // Linux and others
            return System.getProperty("user.home") + "/.local/share/" + id;
        }
    }

    /**
     * Get the logs directory path
     */
    public String getLogsPath() {
        return getAppDataPath() + "/logs";
    }
}
