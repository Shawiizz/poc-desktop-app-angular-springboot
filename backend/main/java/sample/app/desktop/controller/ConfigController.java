package sample.app.desktop.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import sample.app.desktop.config.AppConfigService;

import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigController {

    @Inject
    AppConfigService appConfigService;

    @GET
    @Path("/config")
    public Map<String, String> getConfig() {
        return Map.of(
            "name", appConfigService.getName(),
            "id", appConfigService.getId(),
            "version", appConfigService.getVersion(),
            "description", appConfigService.getDescription()
        );
    }
}
