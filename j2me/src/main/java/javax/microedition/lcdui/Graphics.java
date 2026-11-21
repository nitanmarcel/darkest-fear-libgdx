package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

public class Graphics {

    public static final int VIRTUAL_WIDTH = 240;
    public static final int VIRTUAL_HEIGHT = 320;
    public static final int RENDER_SCALE = 4;
    public static final int FBO_W = VIRTUAL_WIDTH * RENDER_SCALE;
    public static final int FBO_H = VIRTUAL_HEIGHT * RENDER_SCALE;

    public static final int HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8, TOP = 16, BOTTOM = 32, BASELINE = 64;

    static SpriteBatch batch;
    static Texture white;
    private static Viewport viewport;

    static FrameBuffer gameFbo;
    static OrthographicCamera gameCam;
    private static boolean gameTargetActive;

    static int playAreaH = VIRTUAL_HEIGHT;

    static ShaderProgram sceneLightShader;
    static ShaderProgram objectLightShader;

    static final int TILE = 16;
    private static final String LIGHT_STORE = "df_light";
    private static final String CUES_STORE = "df_cues";
    private static final String UPSCALE_STORE = "df_upscale";
    static boolean lightingEnabled = true;
    static boolean cuesEnabled = true;
    static boolean upscaleEnabled = true;

    static boolean sceneClassifyDebug;

    public static void setSceneClassifyDebug(boolean on) {
        sceneClassifyDebug = on;
    }

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

        gameFbo = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_W, FBO_H, false);
        gameFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        gameCam = new OrthographicCamera();
        gameCam.setToOrtho(true, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        gameFbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gameFbo.end();

        ShaderProgram.pedantic = false;
        LightEngine.lightShader = loadShader("post", "light");
        LightEngine.kawaseDownShader = loadShader("post", "kawase_down");
        LightEngine.kawaseUpShader = loadShader("post", "kawase_up");
        sceneLightShader = loadShader("scenelight", "scenelight");
        objectLightShader = loadShader("objectlight", "objectlight");
        lightingEnabled = loadFlag(LIGHT_STORE, true);
        cuesEnabled = loadFlag(CUES_STORE, true);
        upscaleEnabled = loadFlag(UPSCALE_STORE, true);
    }

    static ShaderProgram loadShader(String vert, String frag) {
        ShaderProgram shader = new ShaderProgram(
                Gdx.files.internal("shaders/" + vert + ".vert"),
                Gdx.files.internal("shaders/" + frag + ".frag"));
        if (!shader.isCompiled()) {
            Gdx.app.error("Graphics", "shader " + frag + " failed: " + shader.getLog());
            shader.dispose();
            return null;
        }
        return shader;
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
        if (gameFbo != null) gameFbo.dispose();
        if (sceneLightShader != null) sceneLightShader.dispose();
        if (objectLightShader != null) objectLightShader.dispose();
        SceneCompositor.dispose();
        LightEngine.dispose();
    }

    void begin() {
        if (target != null) return;
        int ph = darkestfear.menu.LightBridge.playHeight();
        if (ph > 0) {
            playAreaH = ph;
        }
        darkestfear.menu.LightBridge.normalizeIdlePlayerColumn();
        gameTargetActive = true;
        SceneCompositor.begin();
    }

    void end() {
        if (target != null) return;
        gameTargetActive = false;
        SceneCompositor.end();
        SceneCompositor.composite(gameFbo);
    }

    static boolean hudHidden() {
        return darkestfear.menu.MenuOverlay.active() || darkestfear.menu.GameBridge.inCutscene();
    }

    private static int overlayBand(int y) {
        return y >= playAreaH && !hudHidden() ? SceneCompositor.HUD : SceneCompositor.FX;
    }

    public static void beginOverlay() {
        if (gameFbo == null) return;
        gameFbo.begin();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        gameTargetActive = true;
        batch.setProjectionMatrix(gameCam.combined);
        batch.begin();
    }

    public static void endOverlay() {
        if (gameFbo == null) return;
        batch.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        gameTargetActive = false;
        gameFbo.end();
    }

    public static boolean isLightingEnabled() {
        return lightingEnabled;
    }

    public static void setLightingEnabled(boolean enabled) {
        lightingEnabled = enabled;
        saveFlag(LIGHT_STORE, enabled);
        if (!enabled) {
            darkestfear.menu.LightBridge.restoreFloor();
        }
    }

    public static boolean isCuesEnabled() {
        return cuesEnabled;
    }

    public static void setCuesEnabled(boolean enabled) {
        cuesEnabled = enabled;
        saveFlag(CUES_STORE, enabled);
    }

    public static boolean isUpscaleEnabled() {
        return upscaleEnabled;
    }

    public static void setUpscaleEnabled(boolean enabled) {
        upscaleEnabled = enabled;
        saveFlag(UPSCALE_STORE, enabled);
    }

    private static boolean loadFlag(String store, boolean def) {
        try {
            javax.microedition.rms.RecordStore rs =
                    javax.microedition.rms.RecordStore.openRecordStore(store, false);
            byte[] rec = rs.getRecord(1);
            rs.closeRecordStore();
            if (rec != null && rec.length > 0) {
                return rec[0] != 0;
            }
        } catch (Throwable t) {
        }
        return def;
    }

    private static void saveFlag(String store, boolean value) {
        try {
            javax.microedition.rms.RecordStore rs =
                    javax.microedition.rms.RecordStore.openRecordStore(store, true);
            rs.setRecord(1, new byte[]{(byte) (value ? 1 : 0)}, 0, 1);
            rs.closeRecordStore();
        } catch (Throwable t) {
        }
    }

    public static void prepareLightmap() {
        if (!lightingEnabled) {
            darkestfear.menu.LightBridge.rebakeRestoredFloor();
            return;
        }
        if (gameFbo == null) {
            return;
        }
        darkestfear.menu.LightBridge.LightState s = darkestfear.menu.LightBridge.read();
        if (s == null) {
            return;
        }
        darkestfear.menu.LightBridge.flattenFloor(s.playerRow, s.playerCol);
        LightEngine.render(s);
    }

    static Texture upscaledSheet(Image img) {
        return upscaleEnabled ? img.upscaledTexture() : null;
    }

    public static void present() {
        if (gameFbo == null) return;

        boolean menu = hudHidden();

        Texture tex = gameFbo.getColorBufferTexture();

        viewport.apply();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.setShader(null);
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(tex, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT, 0, 0, tex.getWidth(), tex.getHeight(), false, false);
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
            SceneCompositor.setBand(overlayBand(y));
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
            SceneCompositor.setBand(overlayBand(Math.min(y1, y2)));
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
            if (darkestfear.menu.LightBridge.isTileObjectSheet(img)) return;
            if (!clipTo(x, y, iw, ih)) return;
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            pixmap.drawPixmap(img.pixmap(), cx, cy, cx - x, cy - y, cw, ch);
            target.markDirty();
        } else if (img.isMutable()) {
            if (SceneCompositor.levelPainted()) {
                setClip(clipX, clipY, clipW, clipH);
            }
            SceneCompositor.setBand(SceneCompositor.FLOOR);
            batch.draw(img.texture(), x, y, iw, ih, 0, 0, iw, ih, false, true);
        } else {
            if (clipY >= playAreaH) {
                SceneCompositor.setBand(overlayBand(clipY));
            } else {
                SceneCompositor.routeSprite(img, x, y, clipX - x, clipY - y, clipW, clipH);
            }
            SceneCompositor.useShape(img);
            Texture upscaled = upscaledSheet(img);
            if (upscaled != null) {
                batch.draw(upscaled, x, y, iw, ih, 0, 0, upscaled.getWidth(), upscaled.getHeight(), false, true);
            } else {
                batch.draw(img.texture(), x, y, iw, ih, 0, 0, iw, ih, false, true);
            }
            SceneCompositor.flushCues();
            batch.setColor(Color.WHITE);
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
            SceneCompositor.setBand(overlayBand(y));
            font.draw(batch, str, x, y);
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    public void setClip(int x, int y, int width, int height) {
        clipX = x; clipY = y; clipW = width; clipH = height;
        if (target != null) return;
        if (batch == null || !batch.isDrawing()) return;

        batch.flush();

        if (gameTargetActive) {
            int s = RENDER_SCALE;
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(x * s, FBO_H - (y + height) * s, width * s, height * s);
            return;
        }

        float worldW = viewport.getWorldWidth();
        float worldH = viewport.getWorldHeight();
        float sx = viewport.getScreenWidth()  / worldW;
        float sy = viewport.getScreenHeight() / worldH;

        int gx = viewport.getScreenX() + Math.round(x * sx);
        int gy = viewport.getScreenY() + Math.round((worldH - (y + height)) * sy);
        int gw = Math.round(width  * sx);
        int gh = Math.round(height * sy);

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glScissor(gx, gy, gw, gh);
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
