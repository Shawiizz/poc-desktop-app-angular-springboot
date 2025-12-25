package sample.app.desktop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import sample.app.desktop.config.AppConfigService;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApiController {

    private final AppConfigService appConfigService;

    @PostMapping("/hello")
    public String sayHello(@RequestBody(required = false) String message) {
        log.info("Received message: {}", message);
        return "Hello from Spring Boot!";
    }

    @GetMapping("/logs/path")
    public Map<String, String> getLogsPath() {
        return Map.of("path", appConfigService.getLogsPath());
    }

    @GetMapping("/logs/recent")
    public Map<String, Object> getRecentLogs(@RequestParam(defaultValue = "100") int lines) {
        try {
            Path logFile = Paths.get(appConfigService.getLogsPath(), "app.log");
            if (!Files.exists(logFile)) {
                return Map.of("lines", List.of(), "exists", false);
            }
            
            List<String> allLines = Files.readAllLines(logFile);
            int start = Math.max(0, allLines.size() - lines);
            List<String> recentLines = allLines.subList(start, allLines.size());
            
            return Map.of("lines", recentLines, "exists", true, "total", allLines.size());
        } catch (IOException e) {
            log.error("Failed to read logs", e);
            return Map.of("error", e.getMessage(), "exists", false);
        }
    }
}