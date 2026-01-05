package sample.app.desktop.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.jbosslog.JBossLog;
import sample.app.desktop.config.AppConfigService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@JBossLog
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApiController {

    @Inject
    AppConfigService appConfigService;

    @POST
    @Path("/hello")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello(String message) {
        log.infof("Received message: %s", message);
        return "Hello from Quarkus!";
    }

    @GET
    @Path("/logs/path")
    public Map<String, String> getLogsPath() {
        return Map.of("path", appConfigService.getLogsPath());
    }

    @GET
    @Path("/logs/recent")
    public Map<String, Object> getRecentLogs(@QueryParam("lines") @DefaultValue("100") int lines) {
        try {
            java.nio.file.Path logFile = Paths.get(appConfigService.getLogsPath(), "app.log");
            if (!Files.exists(logFile)) {
                return Map.of("lines", List.of(), "exists", false);
            }
            
            List<String> allLines = Files.readAllLines(logFile);
            int start = Math.max(0, allLines.size() - lines);
            
            return Map.of("lines", allLines.subList(start, allLines.size()), "exists", true, "total", allLines.size());
        } catch (IOException e) {
            log.error("Failed to read logs", e);
            return Map.of("error", e.getMessage(), "exists", false);
        }
    }
}