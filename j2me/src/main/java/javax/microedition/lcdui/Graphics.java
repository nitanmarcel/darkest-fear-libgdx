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

    public static final int CLASSIC_WIDTH = 240;
    public static final int CLASSIC_HEIGHT = 320;
    public static int VIRTUAL_WIDTH = CLASSIC_WIDTH;
    public static int VIRTUAL_HEIGHT = CLASSIC_HEIGHT;
    public static final int RENDER_SCALE = 4;
    public static int FBO_W = VIRTUAL_WIDTH * RENDER_SCALE;
    public static int FBO_H = VIRTUAL_HEIGHT * RENDER_SCALE;

    public static final int DIMMED = 0x100;
    public static final int HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8, TOP = 16, BOTTOM = 32, BASELINE = 64;

    static SpriteBatch batch;
    static Texture white;
    private static Viewport viewport;

    static FrameBuffer gameFbo;
    static OrthographicCamera gameCam;
    private static boolean gameTargetActive;

    static int playAreaH = VIRTUAL_HEIGHT;
    private static boolean flatFrame;
    static boolean textMuted;

    static ShaderProgram sceneLightShader;
    static ShaderProgram objectLightShader;
    private static ShaderProgram fogShader;
    private static float fogTime;
    private static float fogAmount;
    static boolean fogWanted;
    private static final com.badlogic.gdx.math.Matrix4 fogProjection = new com.badlogic.gdx.math.Matrix4();

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
        viewport = new FitViewport(CLASSIC_WIDTH, CLASSIC_HEIGHT, cam);
        cam.setToOrtho(true, CLASSIC_WIDTH, CLASSIC_HEIGHT);
        resized(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        gameCam = new OrthographicCamera();
        createGameTarget();

        ShaderProgram.pedantic = false;
        LightEngine.lightShader = loadShader("post", "light");
        LightEngine.kawaseDownShader = loadShader("post", "kawase_down");
        LightEngine.kawaseUpShader = loadShader("post", "kawase_up");
        sceneLightShader = loadShader("scenelight", "scenelight");
        objectLightShader = loadShader("objectlight", "objectlight");
        fogShader = loadShader("post", "fog");
        lightingEnabled = loadFlag(LIGHT_STORE, true);
        cuesEnabled = loadFlag(CUES_STORE, true);
        upscaleEnabled = loadFlag(UPSCALE_STORE, true);
        expandedView = loadFlag(VIEW_STORE, true);
        boolean phone = Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android;
        zoom = loadFlag(ZOOM_CLOSER_STORE, phone) ? 2 : loadFlag(ZOOM_STORE, true) ? 1 : 0;
    }

    private static void createGameTarget() {
        gameFbo = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_W, FBO_H, false);
        gameFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        gameCam.setToOrtho(true, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        gameFbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        gameFbo.end();
    }

    public static void resizeScene(int width, int height) {
        if (width == VIRTUAL_WIDTH && height == VIRTUAL_HEIGHT) {
            return;
        }
        VIRTUAL_WIDTH = width;
        VIRTUAL_HEIGHT = height;
        FBO_W = width * RENDER_SCALE;
        FBO_H = height * RENDER_SCALE;
        gameFbo.dispose();
        createGameTarget();
        SceneCompositor.dispose();
        LightEngine.resize();
    }

    private static final String VIEW_STORE = "df_view";
    private static boolean expandedView = true;
    private static final String ZOOM_STORE = "df_zoom", ZOOM_CLOSER_STORE = "df_zoom_closer";
    private static int zoom = 1;

    public static int zoom() {
        return zoom;
    }

    public static void setZoom(int steps) {
        zoom = steps;
        saveFlag(ZOOM_STORE, steps >= 1);
        saveFlag(ZOOM_CLOSER_STORE, steps >= 2);
    }
    static boolean levelShown;
    private static float sceneX, sceneY, sceneW, sceneH;

    private static float sceneUnit() {
        float unit = wideUnit();
        if (!expandedView) {
            return unit;
        }
        float density = Gdx.graphics.getBackBufferWidth() / (float) Gdx.graphics.getWidth();
        return (Math.max(1f, (float) Math.floor(unit * density)) + zoom) / density;
    }

    public static int sceneRoomWidth() {
        return (int) (Gdx.graphics.getWidth() / sceneUnit());
    }

    public static int sceneRoomHeight() {
        return (int) (Gdx.graphics.getHeight() / sceneUnit());
    }

    public static int sceneWidth() {
        return Math.round(sceneW);
    }

    public static int sceneHeight() {
        return Math.round(sceneH);
    }

    public static boolean isExpandedView() {
        return expandedView;
    }

    public static void setExpandedView(boolean enabled) {
        expandedView = enabled;
        saveFlag(VIEW_STORE, enabled);
    }

    public static int sceneX() {
        return Math.round(sceneX);
    }

    public static int sceneY() {
        return Math.round(sceneY);
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
        if (viewport == null) return;
        viewport.update(width, height, false);
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            viewport.setScreenY(Math.max(0, height - viewport.getScreenHeight() - Gdx.graphics.getSafeInsetTop()));
        }
    }

    public static int gameBottom() {
        return Gdx.graphics.getHeight() - viewport.getScreenY();
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

    private static final int WIDE_MIN = 400;
    private static final float REGION_INSET = 0.02f;

    public static int wideInsetLeft() {
        return (int) Math.ceil(Gdx.graphics.getSafeInsetLeft() / wideUnit());
    }

    public static int wideInsetRight() {
        return (int) Math.ceil(Gdx.graphics.getSafeInsetRight() / wideUnit());
    }
    private static final com.badlogic.gdx.math.Matrix4 wideProjection = new com.badlogic.gdx.math.Matrix4();
    private static final com.badlogic.gdx.math.Matrix4 wideTransform = new com.badlogic.gdx.math.Matrix4();
    private static boolean wideActive;
    private static float wideX, wideY;

    private static float wideUnit() {
        return viewport.getScreenHeight() / (float) CLASSIC_HEIGHT;
    }

    public static int wideWidth() {
        return (int) (Gdx.graphics.getWidth() / wideUnit());
    }

    public static boolean wide() {
        return wideWidth() >= WIDE_MIN;
    }

    public static void beginWide() {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float unit = wideUnit();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        HdpiUtils.glViewport(0, 0, w, h);
        batch.setProjectionMatrix(wideProjection.setToOrtho(0, w / unit, h / unit, 0, 0, 1));
        batch.setShader(null);
        wideActive = true;
        batch.begin();
        wideOrigin(0, 0);
    }

    public static void wideOrigin(float x, float y) {
        wideX = x;
        wideY = y;
        batch.setTransformMatrix(wideTransform.setToTranslation(x, y, 0));
    }

    public static void endWide() {
        batch.end();
        batch.setTransformMatrix(wideTransform.idt());
        wideActive = false;
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    public static com.badlogic.gdx.math.Vector2 toWide(int screenX, int screenY) {
        float unit = wideUnit();
        return new com.badlogic.gdx.math.Vector2(screenX / unit, screenY / unit);
    }

    public static void dim(int x, int y, int width, int height, float amount) {
        batch.setColor(0f, 0f, 0f, amount);
        batch.draw(white, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    public static int sceneScrollX() {
        return Math.round(LightEngine.scrollX());
    }

    public static int sceneScrollY() {
        return Math.round(LightEngine.scrollY());
    }

    public static void drawGame(int srcX, int srcY, int width, int height, int x, int y) {
        Texture tex = gameFbo.getColorBufferTexture();
        int s = RENDER_SCALE;
        batch.setColor(Color.WHITE);
        batch.draw(tex, x, y, width, height, srcX * s, tex.getHeight() - (srcY + height) * s, width * s, height * s,
                false, false);
    }

    public void drawRegionScaled(Image src, int xSrc, int ySrc, int width, int height, int x, int y, int w, int h) {
        batch.setColor(Color.WHITE);
        batch.draw(src.texture(), x, y, w, h, xSrc, ySrc, width, height, false, true);
        batch.setColor(color);
    }

    public static void drawHud(Texture texture, float centerX, float centerY, float width, float height, int rgb) {
        drawHud(texture, centerX, centerY, width, height, rgb, 1f);
    }

    public static void drawHud(Texture texture, float centerX, float centerY, float width, float height, int rgb,
                               float alpha) {
        batch.setColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, alpha);
        batch.draw(texture, centerX - width / 2, centerY - height / 2, width, height, 0, 0, texture.getWidth(),
                texture.getHeight(), false, true);
    }

    public static void dispose() {
        if (batch != null) batch.dispose();
        if (white != null) white.dispose();
        if (gameFbo != null) gameFbo.dispose();
        if (sceneLightShader != null) sceneLightShader.dispose();
        if (fogShader != null) fogShader.dispose();
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
        flatFrame = darkestfear.menu.GameBridge.inCutscene();
        gameTargetActive = true;
        SceneCompositor.begin();
    }

    void end() {
        if (target != null) return;
        gameTargetActive = false;
        SceneCompositor.end();
        SceneCompositor.composite(gameFbo);
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


        Texture tex = gameFbo.getColorBufferTexture();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        boolean cropped = levelShown && wide();
        batch.setShader(null);
        if (cropped) {
            int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
            float unit = wideUnit(), zoom = sceneUnit();
            int play = Math.min(playAreaH, VIRTUAL_HEIGHT);
            sceneW = VIRTUAL_WIDTH * zoom / unit;
            sceneH = play * zoom / unit;
            sceneX = (w / unit - sceneW) / 2f;
            sceneY = (h / unit - sceneH) / 2f;
            HdpiUtils.glViewport(0, 0, w, h);
            batch.setProjectionMatrix(wideProjection.setToOrtho(0, w / unit, h / unit, 0, 0, 1));
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(tex, sceneX, sceneY, sceneW, sceneH, 0, tex.getHeight() - play * RENDER_SCALE,
                    tex.getWidth(), play * RENDER_SCALE, false, false);
            batch.end();
            drawFog(tex, sceneX * unit, sceneY * unit, sceneW * unit, sceneH * unit, zoom, true);
        } else {
            viewport.apply();
            batch.setProjectionMatrix(viewport.getCamera().combined);
            batch.begin();
            batch.setColor(Color.WHITE);
            batch.draw(tex, 0, 0, CLASSIC_WIDTH, CLASSIC_HEIGHT, 0, 0, tex.getWidth(), tex.getHeight(), false, false);
            batch.end();
            float unit = viewport.getScreenWidth() / (float) CLASSIC_WIDTH;
            float strip = (CLASSIC_HEIGHT - Math.min(playAreaH, CLASSIC_HEIGHT)) * unit;
            drawFog(tex, viewport.getScreenX(), Gdx.graphics.getHeight() - viewport.getScreenY() - viewport.getScreenHeight(),
                    viewport.getScreenWidth(), viewport.getScreenHeight() - strip, unit, false);
        }
    }

    private static final float FOG_FADE_SECONDS = 0.6f;
    private static final float FOG_SIDE_MIN = 40f;
    private static final float EDGE_LIGHT_REACH = 30f;
    private static final float[] edgeSides = new float[4];

    static void setEdgeUniforms(ShaderProgram shader) {
        shader.setUniformf("u_edge", edgeSides[0], edgeSides[1], edgeSides[2], edgeSides[3]);
        shader.setUniformf("u_edgeArea", VIRTUAL_WIDTH, Math.min(playAreaH, VIRTUAL_HEIGHT), EDGE_LIGHT_REACH);
    }

    private static void drawFog(Texture tex, float x, float top, float width, float height, float unit, boolean bottom) {
        int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float dt = Gdx.graphics.getDeltaTime();
        float room = FOG_SIDE_MIN * unit;
        edgeSides[0] = x >= room ? 1f : 0f;
        edgeSides[1] = w - x - width >= room ? 1f : 0f;
        edgeSides[2] = top >= room ? 1f : 0f;
        edgeSides[3] = bottom && h - top - height >= room ? 1f : 0f;
        fogAmount = Math.max(0f, Math.min(1f, fogAmount + (fogWanted ? dt : -dt) / FOG_FADE_SECONDS));
        if (fogShader == null || fogAmount <= 0f || (width >= w && height >= h)) {
            return;
        }
        fogTime += dt;
        float scale = Gdx.graphics.getBackBufferWidth() / (float) w;
        HdpiUtils.glViewport(0, 0, w, h);
        batch.setProjectionMatrix(fogProjection.setToOrtho2D(0, 0, w, h));
        batch.setShader(fogShader);
        batch.begin();
        batch.setColor(Color.WHITE);
        fogShader.setUniformf("u_scene", x * scale, (h - top - height) * scale, width * scale, height * scale);
        fogShader.setUniformf("u_sides", edgeSides[0], edgeSides[1], edgeSides[2], edgeSides[3]);
        fogShader.setUniformf("u_unit", unit * scale);
        fogShader.setUniformf("u_time", fogTime);
        fogShader.setUniformf("u_amount", fogAmount);
        batch.draw(tex, 0, 0, w, h);
        batch.end();
        batch.setShader(null);
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
            SceneCompositor.setBand(SceneCompositor.FX);
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
            SceneCompositor.setBand(SceneCompositor.FX);
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
            if (flatFrame || clipY >= playAreaH) {
                SceneCompositor.setBand(SceneCompositor.FX);
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

    public void drawRegion(Image src, int xSrc, int ySrc, int width, int height, int transform,
                           int xDest, int yDest, int anchor) {
        int x = anchorX(xDest, width, anchor), y = anchorY(yDest, height, anchor);
        if (target != null) {
            pixmap.setBlending(Pixmap.Blending.SourceOver);
            pixmap.drawPixmap(src.pixmap(), x, y, xSrc, ySrc, width, height);
            target.markDirty();
            return;
        }
        SceneCompositor.setBand(SceneCompositor.FX);
        float shade = transform == DIMMED ? 0.28f : 1f;
        batch.setColor(shade, shade, shade, 1f);
        if (wideActive) {
            Texture tex = src.texture();
            float iw = 1f / tex.getWidth(), ih = 1f / tex.getHeight();
            batch.draw(tex, x, y, width, height, (xSrc + REGION_INSET) * iw, (ySrc + REGION_INSET) * ih,
                    (xSrc + width - REGION_INSET) * iw, (ySrc + height - REGION_INSET) * ih);
        } else {
            batch.draw(src.texture(), x, y, width, height, xSrc, ySrc, width, height, false, true);
        }
        batch.setColor(color);
    }

    public void drawString(String str, int x, int y, int anchor) {
        if (textMuted || target != null || font == null || str == null || str.isEmpty()) return;
        synchronized (Font.TEXT_LOCK) {
            layout.setText(font, str);
            x = anchorX(x, (int) layout.width, anchor);
            if ((anchor & BOTTOM) != 0)       y -= (int) layout.height;
            else if ((anchor & VCENTER) != 0) y -= (int) layout.height / 2;
            font.setColor(color);
            SceneCompositor.setBand(SceneCompositor.FX);
            font.draw(batch, str, x, y);
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

        if (wideActive) {
            float unit = wideUnit();
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            HdpiUtils.glScissor(Math.round((x + wideX) * unit),
                    Gdx.graphics.getHeight() - Math.round((y + wideY + height) * unit),
                    Math.round(width * unit), Math.round(height * unit));
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
