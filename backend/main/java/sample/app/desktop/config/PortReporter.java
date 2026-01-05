package sample.app.desktop.config;

import io.quarkus.vertx.http.HttpServerStart;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.jbosslog.JBossLog;

/**
 * Reports the actual HTTP port to stdout for Tauri to capture.
 */
@JBossLog
@ApplicationScoped
public class PortReporter {

    void onHttpServerStart(@Observes HttpServerStart event) {
        int port = event.options().getPort();
        // Output port for Tauri to capture
        System.out.println("BACKEND_PORT:" + port);
        System.out.flush();
        log.infof("Backend ready on http://localhost:%d", port);
    }
}
