package sample.app.desktop;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;
import sample.app.desktop.config.AppConfigService;

/**
 * Desktop Application - Headless Backend Server
 */
@JBossLog
@QuarkusMain
public class DesktopApplication implements QuarkusApplication {

    @Inject
    AppConfigService appConfigService;

    public static void main(String[] args) {
        Quarkus.run(DesktopApplication.class, args);
    }

    @Override
    public int run(String... args) {
        log.infof("%s v%s started", appConfigService.getName(), appConfigService.getVersion());
        Quarkus.waitForExit();
        return 0;
    }
}
