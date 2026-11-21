package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

import darkestfear.menu.LightBridge;

final class LightEngine {

    private LightEngine() {
    }

    private static int W = Graphics.VIRTUAL_WIDTH, H = Graphics.VIRTUAL_HEIGHT;
    private static int LW = W, LH = H;

    static void resize() {
        FrameBuffer[] fbs = {rawFbo, lightFbo, bounceFbo, mid, half, quarter, halfUp};
        for (FrameBuffer fb : fbs) {
            if (fb != null) fb.dispose();
        }
        rawFbo = lightFbo = bounceFbo = mid = half = quarter = halfUp = null;
        W = LW = Graphics.VIRTUAL_WIDTH;
        H = LH = Graphics.VIRTUAL_HEIGHT;
    }
    private static final int TILE = Graphics.TILE;
    private static final int SDF_CELL = TILE;
    private static final float SDF_RANGE = 32f;
    private static final float SOFT_RANGE = 8f;
    private static final float HARDNESS = 8f;
    private static final int FURN_CELL = 4;
    private static final float BOUNCE = 0.2f;

    private static final float SLOPE_NARROW = 3f, SLOPE_WIDE = 2f;

    private static final int LANTERN = 0, FLASHLIGHT = 1, MIRROR = 2, AMBIENT = 3, WINDOW = 4, FIREPLACE = 5;

    private static final float[][] AUTHORED = {
            {1.00f, 0.89f, 0.77f}, {1.00f, 0.93f, 0.81f}, null, {1f, 1f, 1f}, {0.94f, 0.96f, 1.00f}, {1.00f, 0.86f, 0.69f}};
    private static final float[][] PUSHED = {
            {1.00f, 0.74f, 0.42f}, {1.00f, 0.82f, 0.52f}, null, {1.00f, 0.96f, 0.90f}, {0.62f, 0.76f, 1.00f}, {1.00f, 0.64f, 0.32f}};
    private static final int WOOD_RAMP_TOP = 99;

    static ShaderProgram lightShader, kawaseDownShader, kawaseUpShader;

    private static FrameBuffer rawFbo, lightFbo, bounceFbo, mid, half, quarter, halfUp, sdfView, gridView;
    private static Texture sdfTex, furnTex;
    private static Pixmap sdfPix, furnPix;
    private static final Matrix4 proj = new Matrix4();

    private static int mapCols, mapRows;
    private static long occluderHash;
    private static float scrollX, scrollY;
    private static float shiftX, shiftY;
    private static final float[] bounceGain = {BOUNCE, BOUNCE, BOUNCE};
    private static int tintRampTop = -1;
    private static final float[] levelCurve = {0f, 1 / 7f, 2 / 7f, 3 / 7f, 4 / 7f, 5 / 7f, 6 / 7f, 1f};

    private static float time;
    private static float[] fade;
    private static byte[][] fadeTable;
    private static int carried = -1;
    private static float aimX, aimY;
    private static final float[] tmpColor = new float[3];

    static void dispose() {
        FrameBuffer[] fbs = {rawFbo, lightFbo, bounceFbo, mid, half, quarter, halfUp, sdfView, gridView};
        for (FrameBuffer fb : fbs) {
            if (fb != null) fb.dispose();
        }
        if (sdfTex != null) sdfTex.dispose();
        if (furnTex != null) furnTex.dispose();
        if (sdfPix != null) sdfPix.dispose();
        if (furnPix != null) furnPix.dispose();
        if (lightShader != null) lightShader.dispose();
        if (kawaseDownShader != null) kawaseDownShader.dispose();
        if (kawaseUpShader != null) kawaseUpShader.dispose();
    }

    static FrameBuffer debugFbo(int which) {
        switch (which) {
            case 0: return sdfView;
            case 1: return lightFbo;
            case 2: return bounceFbo;
            case 3: return gridView;
            default: return null;
        }
    }

    static Texture lightmap() {
        return lightFbo != null && sdfTex != null ? lightFbo.getColorBufferTexture() : null;
    }

    static Texture bounce() {
        return bounceFbo.getColorBufferTexture();
    }

    static Texture furniture() {
        return furnTex;
    }

    static float scrollX() {
        return scrollX;
    }

    static float scrollY() {
        return scrollY;
    }

    static float shiftX() {
        return shiftX;
    }

    static float shiftY() {
        return shiftY;
    }

    static int mapCols() {
        return mapCols;
    }

    static int mapRows() {
        return mapRows;
    }

    static float[] bounceGain() {
        return bounceGain;
    }

    static float[] levelCurve() {
        return levelCurve;
    }

    private static FrameBuffer make(int w, int h, Texture.TextureFilter filter) {
        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fb.getColorBufferTexture().setFilter(filter, filter);
        return fb;
    }

    static void render(LightBridge.LightState s) {
        if (s == null || s.grid == null
                || lightShader == null || kawaseDownShader == null || kawaseUpShader == null) {
            return;
        }
        if (lightFbo == null) {
            rawFbo = make(LW, LH, Texture.TextureFilter.Linear);
            lightFbo = make(LW, LH, Texture.TextureFilter.Linear);
            bounceFbo = make(W / 2, H / 2, Texture.TextureFilter.Linear);
            mid = make(W / 2, H / 2, Texture.TextureFilter.Linear);
            half = make(W / 4, H / 4, Texture.TextureFilter.Linear);
            quarter = make(W / 8, H / 8, Texture.TextureFilter.Linear);
            halfUp = make(W / 4, H / 4, Texture.TextureFilter.Linear);
        }
        if (s.playH > 0) {
            Graphics.playAreaH = s.playH;
        }
        time += Math.min(0.05f, Gdx.graphics.getDeltaTime());
        scrollX = TILE / 2f - (W / 2f - s.camX);
        scrollY = TILE / 2f - (s.playH / 2f - s.camY);
        float texel = W / (float) LW;
        shiftX = scrollX - (float) Math.floor(scrollX / texel) * texel;
        shiftY = scrollY - (float) Math.floor(scrollY / texel) * texel;

        ensureMap(s);
        updateDistanceField(s);
        readFloorRamp();
        drawLights(s);
        blur(rawFbo, lightFbo, kawaseUpShader);
        blur(lightFbo, mid, kawaseDownShader);
        blur(mid, half, kawaseDownShader);
        blur(half, quarter, kawaseDownShader);
        blur(quarter, halfUp, kawaseUpShader);
        blur(halfUp, bounceFbo, kawaseUpShader);
        blit(sdfTex, sdfView);
        blit(furnTex, gridView);
    }

    private static void ensureMap(LightBridge.LightState s) {
        if (sdfTex != null && mapCols == s.cols && mapRows == s.rows) {
            return;
        }
        mapCols = s.cols;
        mapRows = s.rows;
        occluderHash = 0;
        if (sdfTex != null) {
            sdfTex.dispose();
            furnTex.dispose();
            sdfPix.dispose();
            furnPix.dispose();
            sdfView.dispose();
            gridView.dispose();
        }
        sdfPix = new Pixmap(mapCols * SDF_CELL, mapRows * SDF_CELL, Pixmap.Format.RGBA8888);
        furnPix = new Pixmap(mapCols * FURN_CELL, mapRows * FURN_CELL, Pixmap.Format.RGBA8888);
        sdfPix.setBlending(Pixmap.Blending.None);
        furnPix.setBlending(Pixmap.Blending.None);
        sdfTex = new Texture(sdfPix);
        furnTex = new Texture(furnPix);
        sdfTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        furnTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        sdfView = make(sdfPix.getWidth(), sdfPix.getHeight(), Texture.TextureFilter.Nearest);
        gridView = make(mapCols * FURN_CELL, mapRows * FURN_CELL, Texture.TextureFilter.Nearest);
    }

    private static void updateDistanceField(LightBridge.LightState s) {
        long hash = 1;
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                hash = hash * 31 + Math.min(0, s.grid[r][c]);
            }
        }
        if (hash == occluderHash) {
            return;
        }
        occluderHash = hash;
        int w = mapCols * SDF_CELL, h = mapRows * SDF_CELL;
        float[] d = new float[w * h], soft = new float[w * h];
        java.util.Arrays.fill(d, 1e9f);
        java.util.Arrays.fill(soft, 1e9f);
        for (int r = 0; r < mapRows; r++) {
            for (int c = 0; c < mapCols; c++) {
                int code = s.grid[r][c];
                if (code <= -20 && code >= -38) {
                    for (int j = 0; j < SDF_CELL; j++) {
                        for (int i = 0; i < SDF_CELL; i++) {
                            soft[(r * SDF_CELL + j) * w + c * SDF_CELL + i] = 0f;
                        }
                    }
                }
                if (code > -1 || code < -19) {
                    continue;
                }
                float x0 = 0, x1 = TILE, y1 = TILE;
                short[] a = code <= -7 ? LightBridge.spriteAttr(LightBridge.tileSpriteId(code)) : null;
                if (a != null) {
                    if (c > 0 && s.grid[r][c - 1] >= 0) x0 = Math.max(x0, TILE / 2f - a[4]);
                    if (c + 1 < mapCols && s.grid[r][c + 1] >= 0) x1 = Math.min(x1, TILE / 2f + a[2] - a[4]);
                    if (r + 1 < mapRows && s.grid[r + 1][c] >= 0) y1 = Math.min(y1, TILE / 2f + a[3] - a[5]);
                }
                float px = TILE / (float) SDF_CELL;
                for (int j = 0; j < SDF_CELL; j++) {
                    for (int i = 0; i < SDF_CELL; i++) {
                        float cx = (i + 0.5f) * px, cy = (j + 0.5f) * px;
                        if (cx >= x0 && cx < x1 && cy < y1) {
                            d[(r * SDF_CELL + j) * w + c * SDF_CELL + i] = 0f;
                        }
                    }
                }
            }
        }
        distanceTransform(d, w, h);
        distanceTransform(soft, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float v = Math.min(1f, (float) Math.sqrt(d[y * w + x]) / SDF_RANGE) * 255f;
                int hi = (int) v;
                int lo = (int) ((v - hi) * 255f);
                int sf = Math.round(Math.min(1f, (float) Math.sqrt(soft[y * w + x]) / SOFT_RANGE) * 255f);
                sdfPix.drawPixel(x, y, hi << 24 | lo << 16 | sf << 8 | 0xFF);
            }
        }
        sdfTex.draw(sdfPix, 0, 0);
        for (int y = 0; y < mapRows * FURN_CELL; y++) {
            for (int x = 0; x < mapCols * FURN_CELL; x++) {
                int code = s.grid[y / FURN_CELL][x / FURN_CELL];
                furnPix.drawPixel(x, y, code <= -20 && code >= -38 ? 0xFF0000FF : 0xFF);
            }
        }
        furnTex.draw(furnPix, 0, 0);
    }

    private static void distanceTransform(float[] d, int w, int h) {
        int n = Math.max(w, h);
        float[] f = new float[n], out = new float[n], z = new float[n + 1];
        int[] v = new int[n];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) f[x] = d[y * w + x];
            transform1d(f, out, v, z, w);
            for (int x = 0; x < w; x++) d[y * w + x] = out[x];
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) f[y] = d[y * w + x];
            transform1d(f, out, v, z, h);
            for (int y = 0; y < h; y++) d[y * w + x] = out[y];
        }
    }

    private static void transform1d(float[] f, float[] out, int[] v, float[] z, int n) {
        int k = 0;
        v[0] = 0;
        z[0] = Float.NEGATIVE_INFINITY;
        z[1] = Float.POSITIVE_INFINITY;
        for (int q = 1; q < n; q++) {
            float sq;
            while (true) {
                int p = v[k];
                sq = ((f[q] + q * q) - (f[p] + p * p)) / (2f * (q - p));
                if (sq > z[k]) break;
                k--;
            }
            k++;
            v[k] = q;
            z[k] = sq;
            z[k + 1] = Float.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < n; q++) {
            while (z[k + 1] < q) k++;
            int p = v[k];
            out[q] = (q - p) * (q - p) + f[p];
        }
    }

    private static void readFloorRamp() {
        int[] ramp = LightBridge.floorSpriteTable();
        if (ramp == null || ramp.length != levelCurve.length || ramp[ramp.length - 1] == tintRampTop) {
            return;
        }
        float[][] mean = new float[ramp.length][3];
        for (int lv = 1; lv < ramp.length; lv++) {
            short[] a = LightBridge.spriteAttr(ramp[lv]);
            Object img = LightBridge.spriteImage(ramp[lv]);
            if (a == null || !(img instanceof Image)) {
                return;
            }
            Pixmap pm = ((Image) img).pixmap();
            for (int y = a[1]; y < a[1] + a[3]; y++) {
                for (int x = a[0]; x < a[0] + a[2]; x++) {
                    int p = pm.getPixel(x, y);
                    mean[lv][0] += p >>> 24 & 255;
                    mean[lv][1] += p >>> 16 & 255;
                    mean[lv][2] += p >>> 8 & 255;
                }
            }
        }
        tintRampTop = ramp[ramp.length - 1];
        float[] top = mean[ramp.length - 1];
        float topLuma = Math.max(1f, luma(top));
        for (int lv = 1; lv < ramp.length; lv++) {
            levelCurve[lv] = luma(mean[lv]) / topLuma;
        }
        float max = Math.max(1f, Math.max(top[0], Math.max(top[1], top[2])));
        bounceGain[0] = BOUNCE * top[0] / max;
        bounceGain[1] = BOUNCE * top[1] / max;
        bounceGain[2] = BOUNCE * top[2] / max;
        Gdx.app.log("LightEngine", "level curve " + java.util.Arrays.toString(levelCurve));
    }

    private static float luma(float[] c) {
        return 0.299f * c[0] + 0.587f * c[1] + 0.114f * c[2];
    }

    private static float noise(float x) {
        int i = (int) Math.floor(x);
        float f = x - i;
        f = f * f * (3f - 2f * f);
        return hash(i) * (1f - f) + hash(i + 1) * f;
    }

    private static float hash(int i) {
        i = (i << 13) ^ i;
        return ((i * (i * i * 15731 + 789221) + 1376312589) & 0x7fffffff) / 2147483647f;
    }

    private static float flicker(int type, int i) {
        float phase = i * 17.31f;
        switch (type) {
            case FIREPLACE:
                return 0.6f + 0.4f * (0.65f * noise(time * 10f + phase) + 0.35f * noise(time * 1.5f + phase));
            case LANTERN:
                return 0.93f + 0.14f * noise(time * 3f + phase);
            case WINDOW:
                return 0.95f + 0.10f * noise(time / 15f + phase);
            default:
                return 1f;
        }
    }

    private static int mirrorParent(LightBridge.LightState s, int m) {
        byte[][] e = s.entities;
        if (s.footprints == null) {
            return -1;
        }
        for (int i = 0; i < s.entCount && i < s.footprints.length; i++) {
            if (i == m || e[4][i] == 0 || s.footprints[i] == null) {
                continue;
            }
            int lr = e[0][m] - e[0][i] + e[2][i] - 1, lc = e[1][m] - e[1][i] + e[2][i] - 1;
            byte[][] fp = s.footprints[i];
            if (lr >= 0 && lr < fp.length && lc >= 0 && lc < fp[lr].length && fp[lr][lc] > 0) {
                return i;
            }
        }
        return -1;
    }

    private static float emission(LightBridge.LightState s, int i, boolean wood, int depth) {
        int type = s.entities[5][i];
        if (type == MIRROR) {
            int parent = depth < 4 ? mirrorParent(s, i) : -1;
            if (parent >= 0) {
                return 1f + (emission(s, parent, wood, depth + 1) - 1f) * 0.5f;
            }
            type = AMBIENT;
        }
        float[] c = type >= 0 && type < AUTHORED.length
                ? (wood && type != WINDOW ? AUTHORED : PUSHED)[type] : AUTHORED[AMBIENT];
        System.arraycopy(c, 0, tmpColor, 0, 3);
        return flicker(type, i);
    }

    private static void drawLights(LightBridge.LightState s) {
        byte[][] e = s.entities;
        rawFbo.begin();
        Gdx.gl.glViewport(0, 0, LW, LH);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (e == null || e.length < 6 || e[0] == null) {
            rawFbo.end();
            return;
        }
        if (fadeTable != e || fade == null || fade.length != s.entCount) {
            fadeTable = e;
            fade = new float[s.entCount];
            for (int i = 0; i < s.entCount; i++) {
                fade[i] = e[4][i] != 0 ? 1f : 0f;
            }
            carried = -1;
        }
        float dt = Math.min(0.05f, Gdx.graphics.getDeltaTime());
        int[] ramp = LightBridge.floorSpriteTable();
        boolean wood = ramp != null && ramp.length > 0 && ramp[ramp.length - 1] == WOOD_RAMP_TOP;
        int held = LightBridge.carriedLight();
        int[] player = LightBridge.actor(0);
        float ox = W / 2f - s.camX + shiftX, oy = s.playH / 2f - s.camY + shiftY;

        proj.setToOrtho2D(0, 0, W, H);
        Graphics.batch.setProjectionMatrix(proj);
        Graphics.batch.setShader(lightShader);
        Graphics.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        Graphics.batch.setColor(Color.WHITE);
        Graphics.batch.begin();
        lightShader.setUniformf("u_scroll", scrollX - shiftX, scrollY - shiftY);
        lightShader.setUniformf("u_k", HARDNESS);
        lightShader.setUniformf("u_sdfSize", mapCols * SDF_CELL, mapRows * SDF_CELL);
        for (int i = 0; i < s.entCount; i++) {
            int type = e[5][i], mode = e[3][i];
            float target = e[4][i] != 0 ? 1f : 0f;
            float tau = type == MIRROR ? 0.1f : type == WINDOW ? 0.2f : 0f;
            fade[i] = tau > 0f ? fade[i] + (target - fade[i]) * (1f - (float) Math.exp(-dt / tau)) : target;
            if (fade[i] < 0.01f) {
                continue;
            }
            float dx = mode == 1 ? 1f : mode == 3 ? -1f : 0f;
            float dy = mode == 2 ? 1f : mode == 4 ? -1f : 0f;
            if (type == LANTERN || type == AMBIENT) {
                dx = 0f;
                dy = 0f;
            }
            float lx = e[1][i] * TILE + ox, ly = e[0][i] * TILE + oy;
            if (i == held && player != null) {
                int facing = player[2];
                lx = player[0] + ox;
                ly = player[1] + oy;
                if (type == LANTERN) {
                    lx += facing == 1 ? 2.5f : facing == 2 ? -0.5f : facing == 3 ? -4.5f : 0f;
                    ly += facing == 1 || facing == 3 ? -1.9f : facing == 2 ? -2.9f : 0f;
                } else if (type == FLASHLIGHT) {
                    lx += facing == 1 ? 6.7f : facing == 2 ? -1.5f : facing == 3 ? -8.7f : 0f;
                    ly += facing == 1 || facing == 3 ? -3.5f : facing == 2 ? 0.1f : 0f;
                }
                if (carried != i) {
                    carried = i;
                    aimX = dx;
                    aimY = dy;
                }
                float k = 1f - (float) Math.exp(-dt / 0.08f);
                aimX += (dx - aimX) * k;
                aimY += (dy - aimY) * k;
                float len = (float) Math.sqrt(aimX * aimX + aimY * aimY);
                if (len > 0.05f) {
                    dx = aimX / len;
                    dy = aimY / len;
                }
            } else if (type == LANTERN) {
                lx += 0.5f;
                ly -= 0.9f;
            } else if (type == FLASHLIGHT) {
                lx += mode == 1 ? 6.7f : mode == 3 ? -2.7f : 0.5f;
                ly += mode == 2 ? 2.1f : mode == 4 ? -4.2f : 0.5f;
            } else if (type == FIREPLACE) {
                lx += mode == 1 ? -2f : mode == 3 ? 2f : 0f;
                ly -= 4f;
            }
            float rad = Math.max(1, e[2][i]) * TILE;
            if (lx < -rad || lx > W + rad || ly < -rad || ly > H + rad) {
                continue;
            }
            float flick = emission(s, i, wood, 0);
            lightShader.setUniformf("u_lightPx", lx, ly);
            lightShader.setUniformf("u_radiusPx", rad);
            lightShader.setUniformf("u_tile", TILE);
            lightShader.setUniformf("u_color", tmpColor[0], tmpColor[1], tmpColor[2]);
            lightShader.setUniformf("u_dir", dx, dy);
            lightShader.setUniformf("u_cone", type == MIRROR ? SLOPE_NARROW : SLOPE_WIDE);
            lightShader.setUniformf("u_intensity", 0.5f * flick * fade[i]);
            Graphics.batch.draw(sdfTex, lx - rad, ly - rad, rad * 2f, rad * 2f);
            Graphics.batch.flush();
        }
        if (held < 0) {
            carried = -1;
        }
        Graphics.batch.end();
        Graphics.batch.setShader(null);
        Graphics.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        rawFbo.end();
    }

    private static void blur(FrameBuffer src, FrameBuffer dst, ShaderProgram shader) {
        dst.begin();
        Gdx.gl.glViewport(0, 0, dst.getWidth(), dst.getHeight());
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        proj.setToOrtho2D(0, 0, 1, 1);
        Graphics.batch.setProjectionMatrix(proj);
        Graphics.batch.setShader(shader);
        Graphics.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
        Graphics.batch.begin();
        shader.setUniformf("u_half", 0.5f / src.getWidth(), 0.5f / src.getHeight());
        Graphics.batch.draw(src.getColorBufferTexture(), 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f);
        Graphics.batch.end();
        Graphics.batch.setShader(null);
        Graphics.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        dst.end();
    }

    private static void blit(Texture src, FrameBuffer dst) {
        dst.begin();
        Gdx.gl.glViewport(0, 0, dst.getWidth(), dst.getHeight());
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        proj.setToOrtho2D(0, 0, 1, 1);
        Graphics.batch.setProjectionMatrix(proj);
        Graphics.batch.setShader(null);
        Graphics.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ZERO);
        Graphics.batch.begin();
        Graphics.batch.draw(src, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 0f);
        Graphics.batch.end();
        Graphics.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        dst.end();
    }
}
