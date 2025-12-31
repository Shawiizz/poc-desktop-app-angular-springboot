package sample.app.desktop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sample.app.desktop.config.AppConfigService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConfigController {

    private final AppConfigService appConfigService;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of(
            "name", appConfigService.getName(),
            "id", appConfigService.getId(),
            "version", appConfigService.getVersion(),
            "description", appConfigService.getDescription()
        );
    }
}
