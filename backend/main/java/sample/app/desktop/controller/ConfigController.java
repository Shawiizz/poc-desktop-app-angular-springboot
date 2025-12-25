package sample.app.desktop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api")
public class ConfigController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode cachedConfig;

    @GetMapping("/config")
    public ResponseEntity<JsonNode> getConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfig();
        }
        return ResponseEntity.ok(cachedConfig);
    }

    private JsonNode loadConfig() {
        try {
            ClassPathResource resource = new ClassPathResource("app-config.json");
            try (InputStream is = resource.getInputStream()) {
                return objectMapper.readTree(is);
            }
        } catch (IOException e) {
            return objectMapper.createObjectNode()
                    .put("name", "Desktop App")
                    .put("id", "desktop-app")
                    .put("version", "unknown");
        }
    }
}
