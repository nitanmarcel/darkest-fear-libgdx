package javax.microedition.lcdui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Image {

    private final boolean mutable;
    private final int width;
    private final int height;

    private final Pixmap pixmap;
    private Texture texture;
    private boolean dirty;
    private Texture upscaled;
    private boolean upscaleTried;

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
        String path = name.startsWith("/") ? name : "/" + name;
        byte[] data;
        try (InputStream in = Image.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("resource not found: " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            data = out.toByteArray();
        }
        Pixmap pm = new Pixmap(data, 0, data.length);
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

    boolean isMutable() {
        return mutable;
    }

    void markDirty() {
        dirty = true;
    }

    Texture upscaledTexture() {
        if (!upscaleTried) {
            upscaleTried = true;
            int[] frames = darkestfear.menu.LightBridge.spriteFrames(this);
            if (frames.length > 0) {
                upscaled = GlThread.call(() -> {
                    Pixmap pm = SpriteUpscaler.upscale(pixmap, frames);
                    Texture t = new Texture(pm);
                    t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    pm.dispose();
                    return t;
                });
            }
        }
        return upscaled;
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
