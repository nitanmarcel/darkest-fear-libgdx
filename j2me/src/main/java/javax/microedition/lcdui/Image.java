package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.io.IOException;

public class Image {

    private final boolean mutable;
    private final int width;
    private final int height;

    private final Pixmap pixmap;
    private Texture texture;
    private boolean dirty;

    private Image(boolean mutable, int width, int height, Pixmap pixmap) {
        this.mutable = mutable;
        this.width = width;
        this.height = height;
        this.pixmap = pixmap;
    }

    public static Image createImage(int width, int height) {
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fill();
        return new Image(true, width, height, pm);
    }

    public static Image createImage(byte[] data, int offset, int length) {
        Pixmap pm = new Pixmap(data, offset, length);
        return new Image(false, pm.getWidth(), pm.getHeight(), pm);
    }

    public static Image createImage(String name) throws IOException {
        String path = name.startsWith("/") ? name.substring(1) : name;
        Pixmap pm = new Pixmap(Gdx.files.internal(path));
        return new Image(false, pm.getWidth(), pm.getHeight(), pm);
    }

    public Graphics getGraphics() {
        if (!mutable) {
            throw new IllegalStateException("getGraphics on immutable image");
        }
        return new Graphics(this);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    Pixmap pixmap() {
        return pixmap;
    }

    void markDirty() {
        dirty = true;
    }

    Texture texture() {
        if (texture == null) {
            texture = GlThread.call(() -> {
                Texture t = new Texture(pixmap);
                t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                return t;
            });
            dirty = false;
        } else if (dirty) {
            GlThread.call(() -> {
                texture.draw(pixmap, 0, 0);
                return null;
            });
            dirty = false;
        }
        return texture;
    }
}
