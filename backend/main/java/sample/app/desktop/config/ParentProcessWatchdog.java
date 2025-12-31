package sample.app.desktop.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Watchdog that monitors the parent Tauri process.
 * If the parent process dies (crash, kill, etc.), this component
 * triggers a graceful shutdown of the Spring Boot application.
 * 
 * This ensures no orphan backend processes remain running.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParentProcessWatchdog {

    private static final String PARENT_PID_ENV = "TAURI_PARENT_PID";
    private static final long CHECK_INTERVAL_MS = 1000; // 1 second

    private final ApplicationContext applicationContext;
    private ScheduledExecutorService scheduler;
    private Long parentPid;

    @PostConstruct
    public void init() {
        String pidStr = System.getenv(PARENT_PID_ENV);
        
        if (pidStr == null || pidStr.isBlank()) {
            log.info("No parent PID provided ({}), watchdog disabled (standalone mode)", PARENT_PID_ENV);
            return;
        }

        try {
            parentPid = Long.parseLong(pidStr.trim());
            log.info("Parent process watchdog started, monitoring PID: {}", parentPid);
            
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "parent-watchdog");
                t.setDaemon(true);
                return t;
            });
            
            scheduler.scheduleAtFixedRate(
                this::checkParentProcess,
                CHECK_INTERVAL_MS,
                CHECK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
        } catch (NumberFormatException e) {
            log.warn("Invalid parent PID '{}', watchdog disabled", pidStr);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private void checkParentProcess() {
        if (parentPid == null) return;
        
        try {
            if (!isProcessRunning(parentPid)) {
                log.info("Parent process {} is no longer running, initiating shutdown...", parentPid);
                triggerShutdown();
            }
        } catch (Exception e) {
            log.error("Error checking parent process", e);
        }
    }

    private boolean isProcessRunning(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private void triggerShutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure log is flushed
            } catch (InterruptedException ignored) {}
            
            log.info("Goodbye!");
            SpringApplication.exit(applicationContext, () -> 0);
        }, "shutdown-thread").start();
    }
}
