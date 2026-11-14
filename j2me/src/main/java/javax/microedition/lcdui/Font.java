package javax.microedition.lcdui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

import java.util.HashMap;
import java.util.Map;

public final class Font {

    public static final int FACE_SYSTEM       = 0;
    public static final int FACE_MONOSPACE    = 32;
    public static final int FACE_PROPORTIONAL = 64;

    public static final int STYLE_PLAIN      = 0;
    public static final int STYLE_BOLD       = 1;
    public static final int STYLE_ITALIC     = 2;
    public static final int STYLE_UNDERLINED = 4;

    public static final int SIZE_SMALL  = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE  = 16;

    private final BitmapFont bitmapFont;
    private final int face;
    private final int style;
    private final int size;

    private final GlyphLayout layout = new GlyphLayout();

    private static final Map<Integer, Font> CACHE = new HashMap<>();

    private Font(int face, int style, int size, BitmapFont bitmapFont) {
        this.face = face;
        this.style = style;
        this.size = size;
        this.bitmapFont = bitmapFont;
    }

    public static Font getFont(int face, int style, int size) {
        int key = (face << 16) | (style << 8) | size;
        Font cached = CACHE.get(key);
        if (cached != null) return cached;

        BitmapFont bf = FontRegistry.resolve(face, style, size);
        Font f = new Font(face, style, size, bf);
        CACHE.put(key, f);
        return f;
    }

    static void clearCache() {
        CACHE.clear();
    }

    public int getHeight() {
        return Math.round(bitmapFont.getLineHeight());
    }

    public int stringWidth(String str) {
        if (str == null || str.isEmpty()) return 0;
        layout.setText(bitmapFont, str);
        return Math.round(layout.width);
    }

    public int charWidth(char ch) {
        return stringWidth(String.valueOf(ch));
    }

    public int substringWidth(String str, int offset, int len) {
        return stringWidth(str.substring(offset, offset + len));
    }

    public int getFace()  { return face; }
    public int getStyle() { return style; }
    public int getSize()  { return size; }

    public boolean isBold()       { return (style & STYLE_BOLD) != 0; }
    public boolean isItalic()     { return (style & STYLE_ITALIC) != 0; }
    public boolean isUnderlined() { return (style & STYLE_UNDERLINED) != 0; }
    public boolean isPlain()      { return style == STYLE_PLAIN; }

    public BitmapFont getBitmapFont() { return bitmapFont; }
}
