package sample.app.desktop.util;

import java.awt.*;

public class FileUtil {
    public static Font loadFontFromResource(String path, float size) {
        try (java.io.InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return null;
            Font f = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(f);
            return f;
        } catch (Exception e) {
            return null;
        }
    }
}
