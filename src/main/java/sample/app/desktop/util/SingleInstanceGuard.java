package sample.app.desktop.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Ensures only one instance of the application runs (when enabled).
 * Uses a file lock in the user data directory.
 */
@Slf4j
@Component
public class SingleInstanceGuard implements AutoCloseable {

    @Value("${app.single-instance.enabled:true}")
    private boolean enabled;

    @Value("${app.data.directory.name}")
    private String appDataDirectoryName;

    private FileLock lock;
    private FileChannel channel;
    private File lockFile;

    public void acquire() {
        if (!enabled) {
            log.debug("Single instance guard disabled.");
            return;
        }
        try {
            File baseDir = new File(System.getenv("APPDATA"), appDataDirectoryName);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                log.warn("Could not create app data directory: {}", baseDir);
            }
            lockFile = new File(baseDir, "instance.lock");
            channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                // Another instance holds the lock
                log.error("Another instance is already running (lock file: {}). Exiting.", lockFile.getAbsolutePath());
                System.exit(2);
            } else {
                String info = ("PID=" + getPid() + " START=" + Instant.now());
                channel.truncate(0);
                channel.write(StandardCharsets.UTF_8.encode(info));
                log.info("Acquired single-instance lock: {}", lockFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to acquire single-instance lock: {}", e.getMessage(), e);
        }
    }

    private String getPid() {
        try {
            String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            int at = jvmName.indexOf('@');
            if (at > 0) {
                return jvmName.substring(0, at);
            }
            return jvmName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public void close() {
        if (lock != null) {
            try {
                lock.release();
            } catch (Exception ignored) {
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ignored) {
            }
        }
        if (lockFile != null) {
            // keep the file for debugging; could delete if desired
        }
    }
}
