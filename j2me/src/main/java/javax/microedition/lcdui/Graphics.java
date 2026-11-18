package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

public class Graphics {

    public static final int VIRTUAL_WIDTH = 240;
    public static final int VIRTUAL_HEIGHT = 320;

    public static final int HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8, TOP = 16, BOTTOM = 32, BASELINE = 64;

    private static SpriteBatch batch;
    private static Texture white;
    private static Viewport viewport;

    private static FrameBuffer sceneFbo;
    private static OrthographicCamera sceneCam;

    private final Image target;
    private final Pixmap pixmap;
    private final int w, h;

    private final Color color = new Color(0f, 0f, 0f, 1f);
    private int colorRGBA = 0x000000ff;
    private final GlyphLayout layout = new GlyphLayout();

    private BitmapFont font;
    private int clipX, clipY, clipW, clipH;
    private int cx, cy, cw, ch;

    public Graphics(int width, int height) {
        this.target = null;
        this.pixmap = null;
        this.w = width;
        this.h = height;
        setClip(0, 0, width, height);
    }

    Graphics(Image img) {
        this.target = img;
        this.pixmap = img.pixmap();
        this.w = img.getWidth();
        this.h = img.getHeight();
        setClip(0, 0, this.w, this.h);
    }

    public static void init() {
        GlThread.init();
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

        sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, false);
        sceneFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sceneCam = new OrthographicCamera();
        sceneCam.setToOrtho(true, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        sceneCam.update();
        sceneFbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        sceneFbo.end();
    }

    public static void resized(int width, int height) {
        if (viewport != null) viewport.update(width, height, false);
    }

    private static OrthographicCamera hudCam;

    public static void beginHud() {
        if (hudCam == null) hudCam = new OrthographicCamera();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        hudCam.setToOrtho(true, w, h);
        HdpiUtils.glViewport(0, 0, w, h);
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
    }

    public static void endHud() {
        batch.end();
    }

    public static void dispose() {
        if (batch != null) batch.dispose();
        if (white != null) white.dispose();
        if (sceneFbo != null) sceneFbo.dispose();
    }

    void begin() {
        if (target != null) return;
        sceneFbo.begin();
        batch.setProjectionMatrix(sceneCam.combined);
        batch.begin();
    }

    void end() {
        if (target != null) return;
        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        sceneFbo.end();
    }

    public static void present() {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        Texture t = sceneFbo.getColorBufferTexture();
        batch.draw(t, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 0, 0, t.getWidth(), t.getHeight(), false, false);
        batch.end();
    }

    public void setColor(int rgb) {
        int r = (rgb >> 16) & 0xff, g = (rgb >> 8) & 0xff, b = rgb & 0xff;
        color.set(r / 255f, g / 255f, b / 255f, 1f);
        colorRGBA = (r << 24) | (g << 16) | (b << 8) | 0xff;
        if (target == null) batch.setColor(color);
    }

    public void setFont(Font f) {
        this.font = f.getBitmapFont();
    }

    public void fillRect(int x, int y, int width, int height) {
        if (target != null) {
            if (!clipTo(x, y, width, height)) return;
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(colorRGBA);
            pixmap.fillRectangle(cx, cy, cw, ch);
            target.markDirty();
        } else {
            batch.draw(white, x, y, width, height);
        }
    }

    public void drawRect(int x, int y, int width, int height) {
        fillRect(x, y, width, 1);
        fillRect(x, y + height, width, 1);
        fillRect(x, y, 1, height);
        fillRect(x + width, y, 1, height + 1);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        if (target != null) {
            pixmap.setBlending(Pixmap.Blending.None);
            pixmap.setColor(colorRGBA);
            pixmap.drawLine(x1, y1, x2, y2);
            target.markDirty();
        } else {
            float dx = x2 - x1, dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
            batch.draw(white, x1, y1, 0, 0, len, 1, 1, 1, ang, 0, 0, 1, 1, false, false);
        }
    }

    public void drawImage(Image img, int x, int y, int anchor) {
        int iw = img.getWidth(), ih = img.getHeight();
        x = anchorX(x, iw, anchor);
        y = anchorY(y, ih, anchor);
        if (target != null) {
            if (!clipTo(x, y, iw, ih)) return;
            int srcX = cx - x, srcY = cy - y;
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            pixmap.drawPixmap(img.pixmap(), cx, cy, srcX, srcY, cw, ch);
            target.markDirty();
        } else {
            batch.draw(img.texture(), x, y, iw, ih, 0, 0, iw, ih, false, true);
        }
    }

    public void drawString(String str, int x, int y, int anchor) {
        if (target != null || font == null || str == null || str.isEmpty()) return;
        try {
            layout.setText(font, str);
            x = anchorX(x, (int) layout.width, anchor);
            if ((anchor & BOTTOM) != 0)       y -= (int) layout.height;
            else if ((anchor & VCENTER) != 0) y -= (int) layout.height / 2;
            font.setColor(color);
            font.draw(batch, str, x, y);
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    public void setClip(int x, int y, int width, int height) {
        clipX = x; clipY = y; clipW = width; clipH = height;
        if (target != null) return;
        if (batch == null || !batch.isDrawing()) return;

        batch.flush();

        int gx = x;
        int gy = VIRTUAL_HEIGHT - (y + height);
        int gw = width;
        int gh = height;
        if (gx < 0) { gw += gx; gx = 0; }
        if (gy < 0) { gh += gy; gy = 0; }
        if (gx + gw > VIRTUAL_WIDTH)  gw = VIRTUAL_WIDTH - gx;
        if (gy + gh > VIRTUAL_HEIGHT) gh = VIRTUAL_HEIGHT - gy;
        if (gw < 0) gw = 0;
        if (gh < 0) gh = 0;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(gx, gy, gw, gh);
    }

    public static com.badlogic.gdx.math.Vector2 toVirtual(int screenX, int screenY) {
        com.badlogic.gdx.math.Vector2 v = new com.badlogic.gdx.math.Vector2(screenX, screenY);
        if (viewport != null) {
            viewport.unproject(v);
        }
        return v;
    }

    public static float screenToWorldScaleY() {
        if (viewport == null || viewport.getScreenHeight() == 0) {
            return 1f;
        }
        return viewport.getWorldHeight() / viewport.getScreenHeight();
    }

    public int getClipX()      { return clipX; }
    public int getClipY()      { return clipY; }
    public int getClipWidth()  { return clipW; }
    public int getClipHeight() { return clipH; }

    private boolean clipTo(int x, int y, int width, int height) {
        int x0 = Math.max(x, clipX), y0 = Math.max(y, clipY);
        int x1 = Math.min(x + width, clipX + clipW), y1 = Math.min(y + height, clipY + clipH);
        cx = x0; cy = y0; cw = x1 - x0; ch = y1 - y0;
        return cw > 0 && ch > 0;
    }

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
