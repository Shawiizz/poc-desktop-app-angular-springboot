package sample.app.desktop.config;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.jbosslog.JBossLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Watchdog that monitors the parent Tauri process.
 * If the parent process dies, triggers graceful shutdown.
 */
@JBossLog
@ApplicationScoped
public class ParentProcessWatchdog {

    private static final String PARENT_PID_ENV = "TAURI_PARENT_PID";
    private static final long CHECK_INTERVAL_MS = 1000;

    private ScheduledExecutorService scheduler;
    private Long parentPid;

    void onStart(@Observes StartupEvent ev) {
        String pidStr = System.getenv(PARENT_PID_ENV);
        
        if (pidStr == null || pidStr.isBlank()) {
            log.infof("No parent PID provided (%s), watchdog disabled", PARENT_PID_ENV);
            return;
        }

        try {
            parentPid = Long.parseLong(pidStr.trim());
            log.infof("Watchdog monitoring PID: %d", parentPid);
            
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "watchdog");
                t.setDaemon(true);
                return t;
            });
            
            scheduler.scheduleAtFixedRate(this::checkParent, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } catch (NumberFormatException e) {
            log.warnf("Invalid parent PID '%s'", pidStr);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void checkParent() {
        if (parentPid != null && !ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false)) {
            log.info("Parent process gone, shutting down...");
            Quarkus.asyncExit(0);
        }
    }
}
