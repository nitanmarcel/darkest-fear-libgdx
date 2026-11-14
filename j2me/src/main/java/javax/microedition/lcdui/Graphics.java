package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Graphics {

    public static final int VIRTUAL_WIDTH = 240;
    public static final int VIRTUAL_HEIGHT = 320;

    static final int HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8, TOP = 16, BOTTOM = 32, BASELINE = 64;

    private static SpriteBatch batch;
    private static Texture white;
    private static Viewport viewport;

    private final FrameBuffer target;
    private final int w, h;
    private final OrthographicCamera camera;
    private final Color color = new Color(0f, 0f, 0f, 1f);
    private final GlyphLayout layout = new GlyphLayout();

    private BitmapFont font;
    private int clipX, clipY, clipW, clipH;

    public Graphics(int width, int height) {
        this.target = null;
        this.w = width;
        this.h = height;
        this.camera = null;
        setClip(0, 0, width, height);
    }

    Graphics(FrameBuffer fb) {
        this.target = fb;
        this.w = fb.getWidth();
        this.h = fb.getHeight();
        OrthographicCamera cam = new OrthographicCamera();
        cam.setToOrtho(true, this.w, this.h);
        this.camera = cam;
        setClip(0, 0, this.w, this.h);
    }

    public static void init() {
        batch = new SpriteBatch();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();

        OrthographicCamera cam = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, cam);
        cam.setToOrtho(true, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), false);
    }

    public static void resized(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, false);
        }
    }

    public static void dispose() {
        if (batch != null) batch.dispose();
        if (white != null) white.dispose();
    }

    void begin() {
        if (target != null) {
            target.begin();
            batch.setProjectionMatrix(camera.combined);
        } else {
            viewport.apply();
            batch.setProjectionMatrix(viewport.getCamera().combined);
        }
        batch.begin();
    }

    void end() {
        batch.end();
        if (target != null) {
            target.end();
        }
    }

    public void setColor(int rgb) {
        color.set(((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, 1f);
        batch.setColor(color);
    }

    public void setFont(Font f) {
        this.font = f.getBitmapFont();
    }

    public void fillRect(int x, int y, int width, int height) {
        batch.draw(white, x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height) {
        fillRect(x, y, width, 1);
        fillRect(x, y + height, width, 1);
        fillRect(x, y, 1, height);
        fillRect(x + width, y, 1, height + 1);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        batch.draw(white, x1, y1, 0, 0, len, 1, 1, 1, ang, 0, 0, 1, 1, false, false);
    }

    public void drawImage(Image img, int x, int y, int anchor) {
        int iw = img.getWidth(), ih = img.getHeight();
        x = anchorX(x, iw, anchor);
        y = anchorY(y, ih, anchor);
        if (img.frameBuffer() != null) {
            batch.draw(img.frameBuffer().getColorBufferTexture(), x, y, iw, ih, 0, 0, iw, ih, false, false);
        } else {
            batch.draw(img.texture(), x, y, iw, ih, 0, 0, iw, ih, false, true);
        }
    }

    public void drawString(String str, int x, int y, int anchor) {
        layout.setText(font, str);
        x = anchorX(x, (int) layout.width, anchor);
        if ((anchor & BOTTOM) != 0)       y -= (int) layout.height;
        else if ((anchor & VCENTER) != 0) y -= (int) layout.height / 2;
        font.setColor(color);
        font.draw(batch, str, x, y);
    }

    public void setClip(int x, int y, int width, int height) {
        clipX = x; clipY = y; clipW = width; clipH = height;
    }

    public int getClipX()      { return clipX; }
    public int getClipY()      { return clipY; }
    public int getClipWidth()  { return clipW; }
    public int getClipHeight() { return clipH; }

    private static int anchorX(int x, int w, int a) {
        if ((a & RIGHT) != 0)   return x - w;
        if ((a & HCENTER) != 0) return x - w / 2;
        return x;
    }

    private static int anchorY(int y, int h, int a) {
        if ((a & BOTTOM) != 0)  return y - h;
        if ((a & VCENTER) != 0) return y - h / 2;
        return y;
    }
}
