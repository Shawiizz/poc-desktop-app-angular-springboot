package sample.app.desktop.config;

import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.vertx.core.http.HttpServerOptions;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;

/**
 * Configures the HTTP port from BACKEND_PORT environment variable.
 * If not set, uses a random available port.
 */
@JBossLog
@ApplicationScoped
public class DynamicPortCustomizer implements HttpServerOptionsCustomizer {

    private static final String BACKEND_PORT_ENV = "BACKEND_PORT";

    @Override
    public void customizeHttpServer(HttpServerOptions options) {
        String portEnv = System.getenv(BACKEND_PORT_ENV);
        
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                int port = Integer.parseInt(portEnv.trim());
                options.setPort(port);
                log.infof("Using port from %s: %d", BACKEND_PORT_ENV, port);
                return;
            } catch (NumberFormatException e) {
                log.warnf("Invalid %s value: %s", BACKEND_PORT_ENV, portEnv);
            }
        }
        
        // Fallback: let Quarkus use the configured port (0 = random)
        log.info("No BACKEND_PORT set, using default configuration");
    }

    @Override
    public void customizeHttpsServer(HttpServerOptions options) {
        // Not used
    }
}
