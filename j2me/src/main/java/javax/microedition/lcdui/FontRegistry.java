package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import java.util.ArrayList;
import java.util.List;

public final class FontRegistry {

    private static int sizeSmall  = 14;
    private static int sizeMedium = 18;
    private static int sizeLarge  = 22;

    private static final List<BitmapFont> CREATED = new ArrayList<>();

    private FontRegistry() {}

    public static void configureSizes(int small, int medium, int large) {
        sizeSmall = small;
        sizeMedium = medium;
        sizeLarge = large;
    }

    static BitmapFont resolve(final int face, final int style, final int size) {
        return GlThread.call(() -> create(face, style, size));
    }

    private static BitmapFont create(int face, int style, int size) {
        boolean mono   = face == Font.FACE_MONOSPACE;
        boolean bold   = (style & Font.STYLE_BOLD) != 0;
        boolean italic = (style & Font.STYLE_ITALIC) != 0;

        String base = mono ? "RobotoMono" : "Roboto";
        String variant =
            (bold && italic) ? "-BoldItalic" :
            bold             ? "-Bold" :
            italic           ? "-Italic" :
                               "-Regular";

        String path = "fonts/" + base + variant + ".ttf";

        FreeTypeFontGenerator gen =
            new FreeTypeFontGenerator(Gdx.files.internal(path));
        try {
            FreeTypeFontParameter p = new FreeTypeFontParameter();
            p.size = pixelHeightFor(size) * Graphics.RENDER_SCALE;
            p.flip = true;
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;

            BitmapFont bf = gen.generateFont(p);
            bf.getData().setScale(1f / Graphics.RENDER_SCALE);
            bf.setUseIntegerPositions(false);
            CREATED.add(bf);
            return bf;
        } finally {
            gen.dispose();
        }
    }

    private static int pixelHeightFor(int j2meSize) {
        switch (j2meSize) {
            case Font.SIZE_SMALL: return sizeSmall;
            case Font.SIZE_LARGE: return sizeLarge;
            case Font.SIZE_MEDIUM:
            default:              return sizeMedium;
        }
    }

    public static void disposeAll() {
        for (BitmapFont bf : CREATED) bf.dispose();
        CREATED.clear();
        Font.clearCache();
    }
}
