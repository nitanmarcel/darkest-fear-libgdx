package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import java.io.IOException;

public class Image {

    private final Texture texture;
    private final FrameBuffer buffer;
    private final int width;
    private final int height;

    private Image(Texture texture, FrameBuffer buffer, int width, int height) {
        this.texture = texture;
        this.buffer = buffer;
        this.width = width;
        this.height = height;
    }

    public static Image createImage(int width, int height) {
        FrameBuffer fb = GlThread.call(() ->
            new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false));
        return new Image(null, fb, width, height);
    }

    public static Image createImage(byte[] data, int offset, int length) {
        Pixmap pm = new Pixmap(data, offset, length);
        Texture tex = GlThread.call(() -> {
            Texture t = new Texture(pm);
            t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            return t;
        });
        int w = pm.getWidth();
        int h = pm.getHeight();
        pm.dispose();
        return new Image(tex, null, w, h);
    }

    public static Image createImage(String name) throws IOException {
        String path = name.startsWith("/") ? name.substring(1) : name;
        Texture tex = GlThread.call(() -> {
            Texture t = new Texture(Gdx.files.internal(path));
            t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            return t;
        });
        return new Image(tex, null, tex.getWidth(), tex.getHeight());
    }

    public Graphics getGraphics() {
        if (buffer == null) {
            throw new IllegalStateException("immutable image");
        }
        return new Graphics(buffer);
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    Texture texture()        { return texture; }
    FrameBuffer frameBuffer() { return buffer; }
}
