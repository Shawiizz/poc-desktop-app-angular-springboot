package sample.app.desktop.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.Getter;
import lombok.extern.jbosslog.JBossLog;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service to access application configuration from app.config.json
 */
@JBossLog
@ApplicationScoped
public class AppConfigService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    @Getter private String name = "Desktop App";
    @Getter private String id = "desktop-app";
    @Getter private String version = "1.0.0";
    @Getter private String description = "";

    void onStart(@Observes StartupEvent ev) {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("app-config.json")) {
            if (is == null) return;
            
            JsonNode config = MAPPER.readTree(is);
            if (config.has("name")) name = config.get("name").asText();
            if (config.has("id")) id = config.get("id").asText();
            if (config.has("version")) version = config.get("version").asText();
            if (config.has("description")) description = config.get("description").asText();
            
            log.infof("Loaded app config: %s v%s", name, version);
        } catch (IOException e) {
            log.warnf("Could not load app-config.json, using defaults: %s", e.getMessage());
        }
    }

    public String getAppDataPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        
        if (os.contains("win")) {
            String appData = System.getenv("LOCALAPPDATA");
            return (appData != null ? appData : home + "/AppData/Local") + "/" + id;
        } else if (os.contains("mac")) {
            return home + "/Library/Application Support/" + id;
        }
        return home + "/.local/share/" + id;
    }

    public String getLogsPath() {
        return getAppDataPath() + "/logs";
    }
}
