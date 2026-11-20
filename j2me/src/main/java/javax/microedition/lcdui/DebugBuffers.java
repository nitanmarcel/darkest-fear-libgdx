package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public final class DebugBuffers {

    private DebugBuffers() {
    }

    private static final String[] NAMES = {
            "Distance Field", "Light", "Bounce", "Grid", "Scene",
            "Band: Floor", "Band: Decal", "Band: Objects", "Band: Actors", "Band: Overhang", "Band: FX",
            "Lighting Classify"
    };

    public static int classifyIndex() {
        return NAMES.length - 1;
    }

    public static String[] names() {
        return NAMES.clone();
    }

    public static int handle(int which) {
        Texture t = texture(which);
        return t != null ? t.getTextureObjectHandle() : 0;
    }

    public static int width(int which) {
        Texture t = texture(which);
        return t != null ? t.getWidth() : 0;
    }

    public static int height(int which) {
        Texture t = texture(which);
        return t != null ? t.getHeight() : 0;
    }

    private static Texture texture(int which) {
        FrameBuffer fb = frameBuffer(which);
        return fb != null ? fb.getColorBufferTexture() : null;
    }

    private static FrameBuffer frameBuffer(int which) {
        switch (which) {
            case 4: return Graphics.gameFbo;
            case 5: return SceneCompositor.band(SceneCompositor.FLOOR);
            case 6: return SceneCompositor.band(SceneCompositor.DECAL);
            case 7: return SceneCompositor.band(SceneCompositor.OBJECTS);
            case 8: return SceneCompositor.band(SceneCompositor.ACTORS);
            case 9: return SceneCompositor.band(SceneCompositor.OVERHANG);
            case 10: return SceneCompositor.band(SceneCompositor.FX);
            case 11: return SceneCompositor.classify();
            default: return LightEngine.debugFbo(which);
        }
    }

    public static String export(int which, String dir) {
        FrameBuffer fb = frameBuffer(which);
        if (fb == null) {
            return null;
        }
        int w = fb.getWidth();
        int h = fb.getHeight();
        try {
            fb.begin();
            Pixmap raw = Pixmap.createFromFrameBuffer(0, 0, w, h);
            fb.end();
            Pixmap flip = new Pixmap(w, h, Pixmap.Format.RGBA8888);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    flip.drawPixel(x, h - 1 - y, raw.getPixel(x, y));
                }
            }
            String name = NAMES[which].toLowerCase().replace(' ', '_').replace('-', '_')
                    + "_" + System.currentTimeMillis() + ".png";
            com.badlogic.gdx.files.FileHandle out = Gdx.files.absolute(dir).child(name);
            out.parent().mkdirs();
            PixmapIO.writePNG(out, flip);
            raw.dispose();
            flip.dispose();
            return out.path();
        } catch (Throwable t) {
            Gdx.app.log("DEBUGBUFFERS", "export failed: " + t);
            return null;
        }
    }
}
