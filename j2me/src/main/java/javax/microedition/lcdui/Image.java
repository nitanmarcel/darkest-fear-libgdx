package javax.microedition.lcdui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Image {

    private final boolean mutable;
    private int width;
    private int height;

    private Pixmap pixmap;
    private boolean packed;
    private Texture texture;
    private boolean dirty;
    private Texture upscaled;
    private Texture shape;
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
        String path = name.startsWith("/") ? name.substring(1) : name;
        byte[] data;
        try (InputStream in = darkestfear.menu.GameJar.loader().getResourceAsStream(path)) {
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

    boolean packed() {
        return packed;
    }

    void repack(Pixmap sheet, Texture detailed, Texture surface) {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        if (upscaled != null) {
            upscaled.dispose();
            shape.dispose();
        }
        if (packed) {
            pixmap.dispose();
        }
        pixmap = sheet;
        width = sheet.getWidth();
        height = sheet.getHeight();
        upscaled = detailed;
        shape = surface;
        upscaleTried = detailed != null;
        packed = detailed != null;
    }

    void markDirty() {
        dirty = true;
    }

    Texture upscaledTexture() {
        buildSheets();
        return upscaled;
    }

    /** How each sprite's surface faces the light (see SpriteUpscaler.shape), or null if the sheet holds no sprites. */
    Texture shapeTexture() {
        buildSheets();
        return shape;
    }

    private void buildSheets() {
        if (upscaleTried) {
            return;
        }
        upscaleTried = true;
        int[] frames = darkestfear.menu.LightBridge.spriteFrames(this);
        if (frames.length == 0) {
            return;
        }
        GlThread.call(() -> {
            Pixmap up = SpriteUpscaler.upscale(pixmap, frames);
            Pixmap surface = SpriteUpscaler.shape(up, frames);
            upscaled = new Texture(up);
            upscaled.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            shape = new Texture(surface);
            shape.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            up.dispose();
            surface.dispose();
            return null;
        });
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
