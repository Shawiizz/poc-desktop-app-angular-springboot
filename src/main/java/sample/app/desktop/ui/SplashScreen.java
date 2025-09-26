package sample.app.desktop.ui;

import me.friwi.jcefmaven.EnumProgress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

import static sample.app.desktop.util.FileUtil.loadFontFromResource;

@Component
public class SplashScreen {
    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.splash.subtitle}")
    private String splashSubtitle;

    @Value("${app.splash.text.init}")
    private String splashTextInit;

    @Value("${app.splash.text.locating}")
    private String splashTextLocating;

    @Value("${app.splash.text.downloading}")
    private String splashTextDownloading;

    @Value("${app.splash.text.extracting}")
    private String splashTextExtracting;

    @Value("${app.splash.text.installed}")
    private String splashTextInstalled;

    @Value("${app.splash.text.ready}")
    private String splashTextReady;

    @Value("${app.splash.window.width:600}")
    private int splashWindowWidth;

    @Value("${app.splash.window.height:350}")
    private int splashWindowHeight;

    private JWindow splashWindow;
    private JLabel splashMessageLabel;
    private int splashProgressValue = 0;

    public void showSplash() {
        if (splashWindow != null && splashWindow.isShowing()) return;
        splashWindow = new JWindow();

        Font interRegular = null;
        Font interBold = null;
        try {
            interRegular = loadFontFromResource("fonts/Inter-Regular.ttf", 14f);
            interBold = loadFontFromResource("fonts/Inter-Bold.ttf", 24f);
        } catch (Exception ignored) {
        }

        final Image backgroundImage = Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("images/splash.jpg"));

        JPanel root = getJPanel(backgroundImage);

        JLabel title = new JLabel(appName, SwingConstants.LEFT);
        title.setForeground(new Color(230, 240, 255));
        title.setFont(interBold != null ? interBold.deriveFont(Font.BOLD, 32f) : new Font("Segoe UI", Font.BOLD, 32));

        JLabel subtitle = new JLabel(splashSubtitle, SwingConstants.LEFT);
        subtitle.setForeground(new Color(160, 185, 210));
        subtitle.setFont(interRegular != null ? interRegular.deriveFont(Font.PLAIN, 18f) : new Font("Segoe UI", Font.PLAIN, 18));

        JPanel topBox = new JPanel();
        topBox.setOpaque(false);
        topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
        topBox.setBorder(BorderFactory.createEmptyBorder(56, 72, 0, 72));
        topBox.add(title);
        topBox.add(Box.createVerticalStrut(6));
        topBox.add(subtitle);

        JPanel center = new JPanel();
        center.setOpaque(false);

        JPanel infoContainer = new JPanel(new BorderLayout());
        infoContainer.setOpaque(false);

        splashMessageLabel = new JLabel("Initialisation...", SwingConstants.LEFT);
        splashMessageLabel.setForeground(Color.WHITE);
        splashMessageLabel.setFont(interRegular != null ? interRegular.deriveFont(Font.PLAIN, 12f) : new Font("Segoe UI", Font.PLAIN, 12));

        JPanel infoLine = new JPanel(new BorderLayout());
        infoLine.setOpaque(false);
        infoLine.setBorder(BorderFactory.createEmptyBorder(0, 16, 6, 16));
        infoLine.add(splashMessageLabel, BorderLayout.WEST);
        infoContainer.add(infoLine, BorderLayout.CENTER);

        JPanel barPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int w = getWidth();
                int barHeight = getHeight();
                int filled = (int) (w * (splashProgressValue / 100.0));
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRect(0, 0, w, barHeight);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, filled, barHeight);
                g2.dispose();
            }
        };
        barPanel.setPreferredSize(new Dimension(10, 4));
        barPanel.setOpaque(false);

        /* ####### BOTTOM CONTAINER ####### */
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(infoContainer, BorderLayout.CENTER);
        bottom.add(barPanel, BorderLayout.SOUTH);

        root.add(topBox, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        splashWindow.setContentPane(root);
        splashWindow.setSize(splashWindowWidth, splashWindowHeight);
        Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
        splashWindow.setLocation(scr.width / 2 - splashWindowWidth / 2, scr.height / 2 - splashWindowHeight / 2);
        splashWindow.setAlwaysOnTop(true);
        splashWindow.setBackground(new Color(0, 0, 0, 255));
        splashWindow.setVisible(true);
    }

    private JPanel getJPanel(Image backgroundImage) {
        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth();
                int h = getHeight();
                if (backgroundImage != null) {
                    g2.drawImage(backgroundImage, 0, 0, w, h, this);
                } else {
                    GradientPaint gp = new GradientPaint(0, 0, new Color(30, 36, 48), 0, h, new Color(45, 58, 74));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, w, h);
                }
                g2.setColor(new Color(0, 0, 0, 90));
                g2.fillRect(0, 0, w, h);
                g2.dispose();
            }
        };
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        root.setOpaque(false);
        return root;
    }

    public void updateProgress(EnumProgress state, int percent) {
        if (splashWindow == null) {
            showSplash();
        }
        if (splashMessageLabel == null) {
            return;
        }
        switch (state) {
            case LOCATING -> {
                splashMessageLabel.setText(splashTextLocating);
            }
            case DOWNLOADING -> {
                splashMessageLabel.setText(splashTextDownloading);
                splashProgressValue = percent;
                splashWindow.repaint();
            }
            case EXTRACTING -> {
                splashMessageLabel.setText(splashTextExtracting);
            }
            case INSTALL, INITIALIZING -> {
                splashMessageLabel.setText(splashTextInstalled);
            }
            case INITIALIZED -> {
                splashMessageLabel.setText(splashTextReady);
                splashProgressValue = 100;
                splashWindow.repaint();
            }
            default -> splashMessageLabel.setText(String.valueOf(state));
        }
    }

    public void hideSplash() {
        if (splashWindow != null) {
            splashWindow.setVisible(false);
            splashWindow.dispose();
            splashWindow = null;
        }
    }
}
