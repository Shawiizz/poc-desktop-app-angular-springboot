package sample.app.desktop.ui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChromiumWindow {

    private JFrame frame;
    private CefApp cefApp;
    private CefClient client;
    private CefBrowser browser;
    private JPanel browserPanel;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.data.directory.name}")
    private String appDataDirectoryName;

    private final SplashScreen splashScreen;

    public void createAndShowWindow(String url) {
        SwingUtilities.invokeLater(() -> {
            try {
                System.setProperty("java.awt.headless", "false");
                createMainWindow();
                initChromiumAsync(url);
            } catch (Exception e) {
                log.error("Cannot create Chromium window: {}", e.getMessage(), e);
            }
        });
    }

    private void createMainWindow() {
        frame = new JFrame(appName);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        try {
            frame.setIconImage(createAppIcon());
        } catch (Exception ignored) {
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
                System.exit(0);
            }
        });

        frame.setLayout(new BorderLayout());
        browserPanel = new JPanel(new BorderLayout());
        frame.add(browserPanel, BorderLayout.CENTER);
    }


    private void initChromiumAsync(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Init Chromium with JCEF...");
                try {
                    SwingUtilities.invokeAndWait(splashScreen::showSplash);
                } catch (Exception splashEx) {
                    log.warn("Cannot show splash screen: {}", splashEx.getMessage());
                }

                CefAppBuilder builder = new CefAppBuilder();

                java.io.File installDir = new java.io.File(System.getenv("APPDATA"), appDataDirectoryName + "/jcef");
                if (!installDir.exists()) {
                    installDir.mkdirs();
                }
                builder.setInstallDir(installDir);
                builder.setProgressHandler((state, percent) -> {
                    int p = Math.round(percent);
                    SwingUtilities.invokeLater(() -> splashScreen.updateProgress(state, p));
                });

                CefSettings cefSettings = builder.getCefSettings();
                cefSettings.windowless_rendering_enabled = false; // rendu standard
                cefSettings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE;

                builder.addJcefArgs(
                        "--disable-gpu-sandbox",
                        "--disable-logging",
                        "--log-severity=disable",
                        "--no-default-browser-check",
                        "--disable-component-update",
                        "--disable-features=MetricsReporting",
                        "--disable-crash-reporter"
                );

                cefApp = builder.build();
                client = cefApp.createClient();

                browser = client.createBrowser(url, false, false);

                SwingUtilities.invokeLater(() -> {
                    splashScreen.hideSplash();
                    browserPanel.add(browser.getUIComponent(), BorderLayout.CENTER);
                    browserPanel.revalidate();
                    browserPanel.repaint();
                    if (!frame.isVisible()) {
                        frame.setVisible(true);
                        log.info("Chromium window shown.");
                    }
                });

            } catch (Exception e) {
                log.error("Error initializing Chromium: {}", e.getMessage(), e);
                SwingUtilities.invokeLater(splashScreen::hideSplash);
            }
        });
    }

    private Image createAppIcon() {
        // Try to load favicon from resources first
        try {
            java.net.URL url = getClass().getResource("/images/favicon.png");
            if (url != null) {
                Image img = ImageIO.read(url);
                if (img != null) {
                    int target = 32;
                    int w = img.getWidth(null);
                    int h = img.getHeight(null);
                    if (w > target || h > target) {
                        // scale down smoothly
                        return img.getScaledInstance(target, target, Image.SCALE_SMOOTH);
                    }
                    return img;
                }
            }
        } catch (Exception e) {
            log.warn("Cannot load /images/favicon.png, falling back to generated icon: {}", e.getMessage());
        }

        // Fallback: generate an icon with the first letter of the app name
        int size = 32;
        java.awt.image.BufferedImage icon = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gradient = new GradientPaint(0, 0, new Color(0, 150, 136), size, size, new Color(0, 188, 212));
        g2.setPaint(gradient);
        g2.fillRoundRect(2, 2, size - 4, size - 4, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        String letter = (appName != null && !appName.isEmpty()) ? appName.substring(0, 1).toUpperCase() : "A";
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth(letter)) / 2;
        int y = (size + fm.getAscent()) / 2 - 2;
        g2.drawString(letter, x, y);
        g2.dispose();
        return icon;
    }

    private void cleanup() {
        try {
            if (browser != null) {
                browser.close(true);
            }
            if (cefApp != null) {
                cefApp.dispose();
            }
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }
}