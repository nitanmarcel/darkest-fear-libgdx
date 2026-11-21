package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import darkestfear.menu.LightBridge;

final class SceneCompositor {

    private SceneCompositor() {
    }

    static final int FLOOR = 0, DECAL = 1, OBJECTS = 2, ACTORS = 3, OVERHANG = 4, FX = 5, HUD = 6;
    private static final int BAND_COUNT = 7;

    private static final boolean[] PERSIST = {false, false, false, false, false, false, true};
    private static final boolean[] FLOOR_LIT = {true, true, false, false, false, false, false};
    private static final boolean[] PER_SPRITE = {false, false, true, true, true, false, false};

    private static final int CLS_WALL = 1, CLS_PROP = 2, CLS_ACTOR = 3, CLS_LOW = 4, CLS_PLAYER = 5;

    private static final float DANGER_FADE = 6f;
    private static final float DANGER_BPM_MIN = 70f, DANGER_BPM_MAX = 130f;
    private static final float DANGER_BODY = 0.35f, DANGER_PULSE = 0.45f, PERSON_BODY = 0.1f;
    private static final float DANGER_VIGNETTE_MIN = 0.30f, DANGER_VIGNETTE_MAX = 0.65f;
    private static final int VIGNETTE_SIZE = 64;

    private static final float SHAPE_LIFT = 0.6f, SHAPE_EDGE = 0.5f, SHAPE_EDGE_WIDTH = 2f;

    private static final int CLS_CUE = 6;
    private static final int PATIENT_UNSAVED = 60, CHILD_UNSAVED = 79;
    private static final float CUE_R = 0.46f, CUE_G = 0.39f, CUE_B = 0.70f;

    private static final int EMBER = 0, HANDLE = 1;
    private static final int[] CUES = {
        92, 3, 6, 2, 2, EMBER,
        91, 0, 2, 1, 3, EMBER,
        50, 3, 4, 2, 2, EMBER,
        57, 10, 4, 2, 2, EMBER,
        48, 8, 14, 1, 4, EMBER,
        54, 8, 10, 1, 5, EMBER,
        1, 16, 13, 3, 1, HANDLE,
        42, 9, 12, 1, 2, HANDLE,
        13, 6, 15, 2, 1, HANDLE,
        13, 11, 15, 2, 1, HANDLE,
    };
    private static final float EMBER_PERIOD = 2.4f, EMBER_MIN = 0.25f, CUE_LIT = 0.25f, HANDLE_LEVEL = 0.6f;
    private static final float EMBER_RANGE = 7f, HANDLE_RANGE = 2.5f;
    private static final int MAX_PENDING = 4;
    private static final float WHITE = Color.WHITE.toFloatBits();

    private static final float STRUCT_FLOOR = 0.07f;
    private static final float AMB_R = 0.040f, AMB_G = 0.035f, AMB_B = 0.065f;

    private static final int[] EAST_THEN_SOUTH = {0, 1, 1, 0};
    private static final int[] SOUTH_THEN_NORTH = {1, 0, -1, 0};
    private static final int[] EAST_WEST = {0, 1, 0, -1};
    private static final int[] EAST_WEST_NORTH = {0, 1, 0, -1, -1, 0};

    private static final FrameBuffer[] bands = new FrameBuffer[BAND_COUNT];
    private static FrameBuffer classifyFbo;
    private static int current = FLOOR;
    private static boolean active;

    private static LightBridge.LightState frame;
    private static int[] tileSid;
    private static float[] capColor;
    private static int selR, selC;

    private static float dangerAmount, dangerProgress, dangerPhase, dangerPulse;
    private static int playerX = Integer.MIN_VALUE, playerY;
    private static Texture vignette;
    private static float cueTime;
    private static float dimBody;
    private static Texture boundShape, flatShape;
    private static int cuesPending;
    private static final int[] cueRect = new int[MAX_PENDING * 4];
    private static float cueBits;

    static FrameBuffer band(int i) {
        return i >= 0 && i < BAND_COUNT ? bands[i] : null;
    }

    private static void ensure() {
        if (bands[0] != null) {
            return;
        }
        for (int i = 0; i < BAND_COUNT; i++) {
            FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, Graphics.FBO_W, Graphics.FBO_H, false);
            fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            fb.begin();
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            fb.end();
            bands[i] = fb;
        }
    }

    static void begin() {
        ensure();
        for (int i = 0; i < BAND_COUNT; i++) {
            if (PERSIST[i]) {
                continue;
            }
            bands[i].begin();
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            bands[i].end();
        }
        frame = null;
        current = FLOOR;
        active = true;
        bands[FLOOR].begin();
        Gdx.gl.glViewport(0, 0, Graphics.FBO_W, Graphics.FBO_H);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Graphics.batch.setProjectionMatrix(Graphics.gameCam.combined);
        Graphics.batch.setShader(null);
        Graphics.batch.begin();
    }

    static void setBand(int b) {
        if (!active || b == current || Graphics.batch == null || !Graphics.batch.isDrawing()) {
            return;
        }
        Graphics.batch.end();
        bands[current].end();
        current = b;
        bands[b].begin();
        Gdx.gl.glViewport(0, 0, Graphics.FBO_W, Graphics.FBO_H);
        Graphics.batch.setProjectionMatrix(Graphics.gameCam.combined);
        cuesPending = 0;
        Graphics.batch.setShader(PER_SPRITE[b] ? Graphics.objectLightShader : null);
        Graphics.batch.begin();
        if (PER_SPRITE[b] && Graphics.objectLightShader != null) {
            configureObjectShader(false);
        }
    }

    static void end() {
        if (!active) {
            return;
        }
        Graphics.batch.end();
        Graphics.batch.setShader(null);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        bands[current].end();
        active = false;
    }

    private static boolean lit() {
        return Graphics.lightingEnabled && LightEngine.lightmap() != null;
    }

    private static void configureObjectShader(boolean debug) {
        ShaderProgram sh = Graphics.objectLightShader;
        boolean lit = lit();
        float[] cap = capColor();
        float[] bounce = LightEngine.bounceGain();
        sh.setUniformi("u_light", 1);
        sh.setUniformi("u_bounce", 2);
        sh.setUniformf("u_lightShift", LightEngine.shiftX(), LightEngine.shiftY());
        sh.setUniformf("u_screen", Graphics.VIRTUAL_WIDTH, Graphics.VIRTUAL_HEIGHT);
        sh.setUniformf("u_lit", lit ? 1f : 0f);
        sh.setUniformf("u_ambient", AMB_R, AMB_G, AMB_B);
        sh.setUniformf("u_bounceGain", bounce[0], bounce[1], bounce[2]);
        sh.setUniformf("u_capColor", cap[0], cap[1], cap[2]);
        sh.setUniformf("u_structFloor", STRUCT_FLOOR);
        sh.setUniform1fv("u_curveF", LightEngine.levelCurve(), 0, 8);
        sh.setUniformf("u_actorPixel", LightTuning.actorPixel);
        sh.setUniformf("u_wallSoft", LightTuning.wallSoft);
        sh.setUniformf("u_debug", debug ? 1f : 0f);
        sh.setUniformf("u_cueColor", CUE_R, CUE_G, CUE_B);
        sh.setUniformi("u_shape", 3);
        sh.setUniformf("u_shapeLight", SHAPE_LIFT, SHAPE_EDGE, SHAPE_EDGE_WIDTH / SpriteUpscaler.SHAPE_RANGE);
        boundShape = null;
        dimBody = Graphics.cuesEnabled ? PERSON_BODY : 0f;
        sh.setUniformf("u_dangerBody", dimBody);
        if (lit) {
            LightEngine.lightmap().bind(1);
            LightEngine.bounce().bind(2);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        }
    }

    private static float[] capColor() {
        if (capColor == null) {
            int sid = LightBridge.tileSpriteId(-40);
            short[] a = LightBridge.spriteAttr(sid);
            Object img = LightBridge.spriteImage(sid);
            if (a == null || !(img instanceof Image)) {
                return new float[] {-1f, -1f, -1f};
            }
            int p = ((Image) img).pixmap().getPixel(a[0], a[1]);
            capColor = new float[] {(p >>> 24 & 255) / 255f, (p >>> 16 & 255) / 255f, (p >>> 8 & 255) / 255f};
        }
        return capColor;
    }

    static boolean levelPainted() {
        if (frame != null || !active) {
            return false;
        }
        LightBridge.LightState s = LightBridge.read();
        if (s == null || s.grid == null) {
            return false;
        }
        frame = s;
        updateDanger(s);
        setBand(OBJECTS);
        Graphics.batch.flush();
        int sc = Graphics.RENDER_SCALE;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, Graphics.FBO_H - Graphics.playAreaH * sc, Graphics.FBO_W, Graphics.playAreaH * sc);
        drawObjects(s);
        Graphics.batch.flush();
        drawUnlitPeople(s);
        return true;
    }

    // The game skips people standing on unlit tiles, though they still block and talk. Draw them
    // ourselves so they get the same dim body as the player.
    private static void drawUnlitPeople(LightBridge.LightState s) {
        byte[][] t = s.interactables;
        if (!Graphics.cuesEnabled || !lit() || Graphics.objectLightShader == null
                || t == null || t.length < 4 || t[0] == null) {
            return;
        }
        int n = Math.min(LightBridge.interactableCount(), t[0].length);
        int ox = Graphics.VIRTUAL_WIDTH / 2 - s.camX;
        int oy = Graphics.playAreaH / 2 - s.camY;
        int sc = Graphics.RENDER_SCALE;
        boolean drawn = false;
        for (int i = 0; i < n; i++) {
            int row = t[0][i], col = t[1][i], variant = t[3][i];
            if (row < 0 || row >= s.rows || col < 0 || col >= s.cols || s.grid[row][col] != 0) {
                continue;
            }
            int sid = variant == 18 ? PATIENT_UNSAVED : variant == 19 ? CHILD_UNSAVED : LightBridge.npcSpriteId(variant);
            short[] a = LightBridge.spriteAttr(sid);
            Object img = LightBridge.spriteImage(sid);
            if (a == null || !(img instanceof Image)) {
                continue;
            }
            int cx = col * Graphics.TILE + ox, cy = row * Graphics.TILE + oy;
            int x = cx - a[4], y = cy - a[5];
            if (x + a[2] <= 0 || x >= Graphics.VIRTUAL_WIDTH || y + a[3] <= 0 || y >= Graphics.playAreaH) {
                continue;
            }
            int bottom = Graphics.playAreaH;
            if (row + 1 < s.rows && s.grid[row + 1][col] < 0) {
                short[] below = LightBridge.spriteAttr(tileSid[-s.grid[row + 1][col]]);
                if (below != null) {
                    bottom = Math.min(bottom, (row + 1) * Graphics.TILE + oy - below[5]);
                }
            }
            if (bottom <= 0) {
                continue;
            }
            if (!drawn) {
                setBand(ACTORS);
                drawn = true;
            }
            Graphics.batch.flush();
            Gdx.gl.glScissor(0, Graphics.FBO_H - bottom * sc, Graphics.FBO_W, bottom * sc);
            Graphics.batch.setPackedColor(pack(cx, cy, CLS_PLAYER, FLAT));
            useShape((Image) img);
            Texture upscaled = Graphics.upscaledSheet((Image) img);
            int up = SpriteUpscaler.SCALE;
            if (upscaled != null) {
                Graphics.batch.draw(upscaled, x, y, a[2], a[3], a[0] * up, a[1] * up, a[2] * up, a[3] * up, false, true);
            } else {
                Graphics.batch.draw(((Image) img).texture(), x, y, a[2], a[3], a[0], a[1], a[2], a[3], false, true);
            }
        }
        if (drawn) {
            Graphics.batch.flush();
            Graphics.batch.setPackedColor(WHITE);
            Gdx.gl.glScissor(0, Graphics.FBO_H - Graphics.playAreaH * sc, Graphics.FBO_W, Graphics.playAreaH * sc);
        }
    }

    private static void drawObjects(LightBridge.LightState s) {
        if (tileSid == null) {
            tileSid = new int[129];
            for (int code = -128; code < 0; code++) {
                tileSid[-code] = LightBridge.tileSpriteId(code);
            }
        }
        boolean perSprite = Graphics.objectLightShader != null;
        int ox = Graphics.VIRTUAL_WIDTH / 2 - s.camX;
        int oy = Graphics.playAreaH / 2 - s.camY;
        for (int r = 0; r < s.rows; r++) {
            for (int c = 0; c < s.cols; c++) {
                int code = s.grid[r][c];
                if (code >= 0) {
                    continue;
                }
                int sid = tileSid[-code];
                short[] a = LightBridge.spriteAttr(sid);
                Object img = LightBridge.spriteImage(sid);
                if (a == null || !(img instanceof Image)) {
                    continue;
                }
                int x = c * Graphics.TILE + ox - a[4];
                int y = r * Graphics.TILE + oy - a[5];
                if (x + a[2] <= 0 || x >= Graphics.VIRTUAL_WIDTH || y + a[3] <= 0 || y >= Graphics.playAreaH) {
                    continue;
                }
                if (perSprite && isSideDoor(code)) {
                    int py = r * Graphics.TILE + oy;
                    int west = a[4];
                    Graphics.batch.setPackedColor(pack((c - 1) * Graphics.TILE + ox, py, CLS_PROP, ALONG_Y));
                    drawTile((Image) img, x, y, west, a[3], a[0], a[1], west, a[3]);
                    Graphics.batch.setPackedColor(pack((c + 1) * Graphics.TILE + ox, py, CLS_PROP, ALONG_Y));
                    drawTile((Image) img, x + west, y, a[2] - west, a[3],
                            a[0] + west, a[1], a[2] - west, a[3]);
                    cue(s, sid, x, y, r, c);
                    flushCues();
                    continue;
                }
                if (perSprite && isWallPiece(code) && r + 1 < s.rows && isSideDoor(s.grid[r + 1][c])) {
                    int west = a[4];
                    Graphics.batch.setPackedColor(pack((c - 1) * Graphics.TILE + ox, (r + 1) * Graphics.TILE + oy,
                            CLS_WALL, FLAT));
                    drawTile((Image) img, x, y, west, a[3], a[0], a[1], west, a[3]);
                    Graphics.batch.setPackedColor(tileBits(s, r, c));
                    drawTile((Image) img, x + west, y, a[2] - west, a[3],
                            a[0] + west, a[1], a[2] - west, a[3]);
                    continue;
                }
                short[] door = perSprite && isWallPiece(code) && c + 1 < s.cols && isFrontDoor(s.grid[r][c + 1])
                        ? LightBridge.spriteAttr(tileSid[-s.grid[r][c + 1]]) : null;
                int tipX = door == null ? 0 : a[4] + Graphics.TILE - door[4], tipH = door == null ? 0 : a[5] - door[5];
                if (door != null && tipX > 0 && tipX < a[2] && tipH > 0 && tipH < a[3]) {
                    Graphics.batch.setPackedColor(pack((c + 1) * Graphics.TILE + ox, (r - 1) * Graphics.TILE + oy,
                            CLS_WALL, FLAT));
                    drawTile((Image) img, x + tipX, y, a[2] - tipX, tipH,
                            a[0] + tipX, a[1], a[2] - tipX, tipH);
                    Graphics.batch.setPackedColor(tileBits(s, r, c));
                    drawTile((Image) img, x, y, tipX, a[3], a[0], a[1], tipX, a[3]);
                    drawTile((Image) img, x + tipX, y + tipH, a[2] - tipX, a[3] - tipH,
                            a[0] + tipX, a[1] + tipH, a[2] - tipX, a[3] - tipH);
                    continue;
                }
                Graphics.batch.setPackedColor(perSprite ? tileBits(s, r, c) : WHITE);
                drawTile((Image) img, x, y, a[2], a[3], a[0], a[1], a[2], a[3]);
                if (perSprite) {
                    cue(s, sid, x, y, r, c);
                    flushCues();
                }
            }
        }
        Graphics.batch.setPackedColor(WHITE);
    }

    private static void drawTile(Image img, int x, int y, int w, int h, int sx, int sy, int sw, int sh) {
        useShape(img);
        Texture upscaled = Graphics.upscaledSheet(img);
        if (upscaled != null) {
            int up = SpriteUpscaler.SCALE;
            Graphics.batch.draw(upscaled, x, y, w, h, sx * up, sy * up, sw * up, sh * up, false, true);
        } else {
            Graphics.batch.draw(img.texture(), x, y, w, h, sx, sy, sw, sh, false, true);
        }
    }

    static void routeSprite(Image img, int drawX, int drawY, int srcX, int srcY, int w, int h) {
        LightBridge.LightState s = frame;
        int id = s == null ? -1 : LightBridge.spriteAt(img, srcX, srcY, w, h);
        short[] a = id < 0 ? null : LightBridge.spriteAttr(id);
        if (a == null) {
            setBand(ACTORS);
            return;
        }
        if (isDecal(id)) {
            setBand(DECAL);
            return;
        }
        boolean perSprite = Graphics.objectLightShader != null;
        int px = drawX + a[0] + a[4];
        int py = drawY + a[1] + a[5];
        int ox = Graphics.VIRTUAL_WIDTH / 2 - s.camX;
        int oy = Graphics.playAreaH / 2 - s.camY;
        int col = Math.floorDiv(px - ox + Graphics.TILE / 2, Graphics.TILE);
        int row = Math.floorDiv(py - oy + Graphics.TILE / 2, Graphics.TILE);
        boolean inGrid = row >= 0 && row < s.rows && col >= 0 && col < s.cols;
        if (LightBridge.isTileSprite(id)) {
            setBand(OVERHANG);
            if (perSprite && inGrid && isSideDoor(s.grid[row][col])) {
                Graphics.batch.setColor(0f, 0f, 0f, 0f);
            } else if (perSprite && inGrid && s.grid[row][col] < 0) {
                Graphics.batch.setPackedColor(tileBits(s, row, col));
                cue(s, id, drawX + a[0], drawY + a[1], row, col);
            }
            return;
        }
        setBand(ACTORS);
        if (perSprite && inGrid) {
            cue(s, id, drawX + a[0], drawY + a[1], row, col);
        }
        if (perSprite) {
            int cls = CLS_ACTOR;
            int cx = drawX + srcX + w / 2, cy = drawY + srcY + h / 2;
            boolean player = Math.abs(cx - playerX) <= 8 && cy >= playerY - 18 && cy <= playerY + 6;
            if (player || (inGrid && personAt(s, row, col))) {
                cls = CLS_PLAYER;
                float warned = (1f - dangerProgress) * (1f - DANGER_PULSE * (1f - dangerPulse));
                float body = !Graphics.cuesEnabled ? 0f
                        : player ? DANGER_BODY * (1f + dangerAmount * (warned - 1f)) : PERSON_BODY;
                if (body != dimBody) {
                    Graphics.batch.flush();
                    Graphics.objectLightShader.setUniformf("u_dangerBody", body);
                    dimBody = body;
                }
            }
            Graphics.batch.setPackedColor(pack(px, py, cls, FLAT));
        }
    }

    private static void cue(LightBridge.LightState s, int sid, int x, int y, int row, int col) {
        if (!Graphics.cuesEnabled || !lit() || s.playerRow < 0) {
            return;
        }
        float alpha = -1f;
        for (int i = 0; i < CUES.length && cuesPending < MAX_PENDING; i += 6) {
            if (CUES[i] != sid) {
                continue;
            }
            if (alpha < 0f) {
                alpha = cueAlpha(s, row, col, CUES[i + 5]);
                if (alpha <= 0f) {
                    return;
                }
                cueBits = Color.toFloatBits(0, 0, CLS_CUE * 32, (int) (alpha * 254f));
            }
            int k = cuesPending++ * 4;
            cueRect[k] = x + CUES[i + 1];
            cueRect[k + 1] = y + CUES[i + 2];
            cueRect[k + 2] = CUES[i + 3];
            cueRect[k + 3] = CUES[i + 4];
        }
    }

    private static float cueAlpha(LightBridge.LightState s, int row, int col, int kind) {
        int code = s.grid[row][col];
        float range = kind == EMBER ? EMBER_RANGE : HANDLE_RANGE;
        int dr = row - s.playerRow, dc = col - s.playerCol;
        if (dr * dr + dc * dc > range * range || !sees(s, row, col)) {
            return 0f;
        }
        int level = code;
        if (level < 0) {
            level = 0;
            for (int k = 0; k < EAST_WEST_NORTH.length + 2; k += 2) {
                int nr = row + (k < 6 ? EAST_WEST_NORTH[k] : 1), nc = col + (k < 6 ? EAST_WEST_NORTH[k + 1] : 0);
                if (open(s, nr, nc)) {
                    level = Math.max(level, s.grid[nr][nc]);
                }
            }
        }
        float strength = HANDLE_LEVEL;
        if (kind == EMBER) {
            float phase = cueTime / EMBER_PERIOD + ((row * 31 + col * 17) & 15) / 16f;
            strength = EMBER_MIN + (1f - EMBER_MIN) * (0.5f + 0.5f * (float) Math.sin(phase * 2.0 * Math.PI));
        }
        return strength * (1f - (1f - CUE_LIT) * Math.min(level, 7) / 7f);
    }

    static void flushCues() {
        if (cuesPending == 0) {
            return;
        }
        Graphics.batch.setPackedColor(cueBits);
        for (int k = 0; k < cuesPending * 4; k += 4) {
            Graphics.batch.draw(Graphics.white, cueRect[k], cueRect[k + 1], cueRect[k + 2], cueRect[k + 3]);
        }
        cuesPending = 0;
    }

    private static boolean sees(LightBridge.LightState s, int row, int col) {
        int r = s.playerRow, c = s.playerCol;
        int dr = Math.abs(row - r), dc = Math.abs(col - c);
        int sr = r < row ? 1 : -1, sc = c < col ? 1 : -1;
        int err = dc - dr;
        while (r != row || c != col) {
            int e2 = 2 * err;
            if (e2 > -dr) {
                err -= dr;
                c += sc;
            }
            if (e2 < dc) {
                err += dc;
                r += sr;
            }
            if ((r != row || c != col) && s.grid[r][c] <= -1 && s.grid[r][c] >= -19) {
                return false;
            }
        }
        return true;
    }

    /** Puts the sheet's shape map on texture unit 3 for the sprites about to be drawn from it. */
    static void useShape(Image img) {
        if (!active || !PER_SPRITE[current] || Graphics.objectLightShader == null) {
            return;
        }
        Texture shape = lit() ? img.shapeTexture() : null;
        if (shape == null) {
            if (flatShape == null) {
                Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                pm.setBlending(Pixmap.Blending.None);
                pm.drawPixel(0, 0, 0x8080ff00);
                flatShape = new Texture(pm);
                pm.dispose();
            }
            shape = flatShape;
        }
        if (shape != boundShape) {
            Graphics.batch.flush();
            shape.bind(3);
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            boundShape = shape;
        }
    }

    private static boolean personAt(LightBridge.LightState s, int row, int col) {
        byte[][] t = s.interactables;
        if (t == null || t.length < 4 || t[0] == null) {
            return false;
        }
        for (int i = Math.min(LightBridge.interactableCount(), t[0].length) - 1; i >= 0; i--) {
            if (t[0][i] == row && t[1][i] == col) {
                return true;
            }
        }
        return false;
    }

    private static void updateDanger(LightBridge.LightState s) {
        float dt = Gdx.graphics.getDeltaTime();
        cueTime = (cueTime + dt) % (EMBER_PERIOD * 16f);
        boolean on = Graphics.cuesEnabled && lit();
        float progress = on ? LightBridge.darknessProgress() : 0f;
        if (progress > 0f) {
            dangerProgress = progress;
        }
        float step = DANGER_FADE * dt;
        dangerAmount = progress > 0f ? Math.min(1f, dangerAmount + step) : Math.max(0f, dangerAmount - step);
        float bpm = DANGER_BPM_MIN + (DANGER_BPM_MAX - DANGER_BPM_MIN) * dangerProgress;
        dangerPhase = (dangerPhase + dt * bpm / 60f) % 1f;
        dangerPulse = 0.5f + 0.5f * (float) Math.sin(dangerPhase * 2.0 * Math.PI);
        int[] p = on ? LightBridge.actor(0) : null;
        if (p == null) {
            playerX = Integer.MIN_VALUE;
        } else {
            playerX = p[0] + Graphics.VIRTUAL_WIDTH / 2 - s.camX;
            playerY = p[1] + Graphics.playAreaH / 2 - s.camY;
        }
    }

    private static void drawVignette() {
        if (vignette == null) {
            Pixmap pm = new Pixmap(VIGNETTE_SIZE, VIGNETTE_SIZE, Pixmap.Format.RGBA8888);
            pm.setBlending(Pixmap.Blending.None);
            for (int y = 0; y < VIGNETTE_SIZE; y++) {
                for (int x = 0; x < VIGNETTE_SIZE; x++) {
                    double dx = Math.abs((x + 0.5) / VIGNETTE_SIZE * 2 - 1), dy = Math.abs((y + 0.5) / VIGNETTE_SIZE * 2 - 1);
                    double d = Math.cbrt(dx * dx * dx + dy * dy * dy);
                    double t = Math.max(0, Math.min(1, (d - 0.45) / 0.55));
                    pm.drawPixel(x, y, (int) Math.round(t * t * (3 - 2 * t) * 255));
                }
            }
            vignette = new Texture(pm);
            vignette.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            pm.dispose();
        }
        float strength = DANGER_VIGNETTE_MIN + (DANGER_VIGNETTE_MAX - DANGER_VIGNETTE_MIN) * dangerProgress;
        Graphics.batch.setShader(null);
        Graphics.batch.begin();
        Graphics.batch.setColor(1f, 1f, 1f, dangerAmount * strength * (0.8f + 0.2f * dangerPulse));
        Graphics.batch.draw(vignette, 0, 0, Graphics.VIRTUAL_WIDTH, Graphics.playAreaH);
        Graphics.batch.setColor(Color.WHITE);
        Graphics.batch.end();
    }

    private static boolean isDecal(int id) {
        return id == 45 || id == 30 || id == 82 || id == 83 || id == 86 || id == 98;
    }

    private static boolean isFrontDoor(int code) {
        return code == -12 || code == -14 || code == -16 || code == -100;
    }

    private static boolean isSideDoor(int code) {
        return code == -13 || code == -15 || code == -17 || code == -101;
    }

    private static boolean isWallPiece(int code) {
        return (code <= -1 && code >= -6) || code == -40;
    }

    private static boolean open(LightBridge.LightState s, int r, int c) {
        return r >= 0 && r < s.rows && c >= 0 && c < s.cols && s.grid[r][c] >= 0;
    }

    private static boolean firstOpen(LightBridge.LightState s, int r, int c, int[] dirs) {
        for (int i = 0; i < dirs.length; i += 2) {
            if (open(s, r + dirs[i], c + dirs[i + 1])) {
                selR = r + dirs[i];
                selC = c + dirs[i + 1];
                return true;
            }
        }
        return false;
    }

    private static boolean brightestOpen(LightBridge.LightState s, int r, int c, int[] dirs) {
        int best = -1;
        for (int i = 0; i < dirs.length; i += 2) {
            int nr = r + dirs[i], nc = c + dirs[i + 1];
            if (open(s, nr, nc) && s.grid[nr][nc] > best) {
                best = s.grid[nr][nc];
                selR = nr;
                selC = nc;
            }
        }
        return best >= 0;
    }

    private static float tileBits(LightBridge.LightState s, int r, int c) {
        int code = s.grid[r][c];
        int cls = CLS_PROP;
        boolean found;
        if (isWallPiece(code)) {
            cls = CLS_WALL;
            if (code == -2 || code == -3 || code == -5) {
                if (r + 1 < s.rows && s.grid[r + 1][c] < -19 && s.grid[r + 1][c] != -40) {
                    return pack(c * Graphics.TILE + Graphics.VIRTUAL_WIDTH / 2 - s.camX,
                            (r + 1) * Graphics.TILE + Graphics.playAreaH / 2 - s.camY, cls, ALONG_X);
                }
                int south = r + 1 < s.rows ? s.grid[r + 1][c] : 0;
                found = (open(s, r + 1, c) && pick(r + 1, c))
                        || (((south <= -7 && south >= -11) || south == -19) && pick(r + 1, c))
                        || (open(s, r, c + 1) && pick(r, c + 1));
            } else {
                found = code != -40 && (firstOpen(s, r, c, EAST_THEN_SOUTH)
                        || (open(s, r + 1, c + 1) && pick(r + 1, c + 1)));
            }
        } else if (code == -103 || code == -105) {
            cls = CLS_WALL;
            found = firstOpen(s, r, c, SOUTH_THEN_NORTH);
        } else if (code == -104 || code == -106) {
            cls = CLS_WALL;
            found = brightestOpen(s, r, c, EAST_WEST);
        } else if (code == -12 || code == -14 || code == -16 || code == -100) {
            found = firstOpen(s, r, c, SOUTH_THEN_NORTH);
        } else if (code == -13 || code == -15 || code == -17 || code == -101) {
            found = brightestOpen(s, r, c, EAST_WEST);
        } else if (code >= -19) {
            found = open(s, r + 1, c) ? pick(r + 1, c) : brightestOpen(s, r, c, EAST_WEST_NORTH);
        } else {
            return pack(c * Graphics.TILE + Graphics.VIRTUAL_WIDTH / 2 - s.camX,
                    r * Graphics.TILE + Graphics.playAreaH / 2 - s.camY, CLS_LOW, ALONG_BOTH);
        }
        int tr = found ? selR : r, tc = found ? selC : c;
        int along = tr != r && tc == c ? ALONG_X : tc != c && tr == r ? ALONG_Y : FLAT;
        return pack(tc * Graphics.TILE + Graphics.VIRTUAL_WIDTH / 2 - s.camX,
                tr * Graphics.TILE + Graphics.playAreaH / 2 - s.camY, cls, along);
    }

    private static boolean pick(int r, int c) {
        selR = r;
        selC = c;
        return true;
    }

    private static final int ALONG_BOTH = 0, ALONG_X = 1, FLAT = 2, ALONG_Y = 3;

    private static float pack(int sx, int sy, int cls, int along) {
        int qx = Math.max(0, Math.min(Graphics.VIRTUAL_WIDTH - 1, sx));
        int qy = Math.max(0, Math.min(Graphics.VIRTUAL_HEIGHT - 1, sy));
        return Color.toFloatBits(qx, qy & 255, cls * 32 + (qy >> 8) * 4 + along, 255);
    }

    static void composite(FrameBuffer target) {
        if (bands[0] == null || target == null) {
            return;
        }
        compositeInto(target, false);
        if (Graphics.sceneClassifyDebug) {
            if (classifyFbo == null) {
                classifyFbo = new FrameBuffer(Pixmap.Format.RGBA8888, Graphics.FBO_W, Graphics.FBO_H, false);
                classifyFbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            }
            compositeInto(classifyFbo, true);
        }
    }

    static FrameBuffer classify() {
        return classifyFbo;
    }

    private static void compositeInto(FrameBuffer dst, boolean debug) {
        ShaderProgram floorShader = Graphics.sceneLightShader;
        boolean canLight = lit() && floorShader != null;
        dst.begin();
        Gdx.gl.glViewport(0, 0, Graphics.FBO_W, Graphics.FBO_H);
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Graphics.batch.setProjectionMatrix(Graphics.gameCam.combined);
        Graphics.batch.setColor(Color.WHITE);
        Graphics.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < BAND_COUNT; i++) {
            if (i == HUD && Graphics.hudHidden()) {
                continue;
            }
            if (i == HUD && !debug && dangerAmount > 0f && frame != null) {
                drawVignette();
            }
            if (debug && i == OBJECTS && frame != null && Graphics.objectLightShader != null) {
                Graphics.batch.setShader(Graphics.objectLightShader);
                Graphics.batch.begin();
                configureObjectShader(true);
                drawObjects(frame);
                Graphics.batch.end();
                continue;
            }
            boolean lit = canLight && FLOOR_LIT[i];
            Graphics.batch.setShader(lit ? floorShader : null);
            Graphics.batch.begin();
            if (lit) {
                float[] bounce = LightEngine.bounceGain();
                floorShader.setUniformi("u_light", 1);
                floorShader.setUniformi("u_bounce", 2);
                floorShader.setUniformi("u_furniture", 3);
                floorShader.setUniformf("u_view", Graphics.FBO_H, Graphics.RENDER_SCALE);
                floorShader.setUniformf("u_screen", Graphics.VIRTUAL_WIDTH, Graphics.VIRTUAL_HEIGHT);
                floorShader.setUniformf("u_scroll", LightEngine.scrollX(), LightEngine.scrollY());
                floorShader.setUniformf("u_lightShift", LightEngine.shiftX(), LightEngine.shiftY());
                floorShader.setUniformf("u_mapTiles", LightEngine.mapCols(), LightEngine.mapRows());
                floorShader.setUniformf("u_ambient", AMB_R, AMB_G, AMB_B);
                floorShader.setUniformf("u_bounceGain", bounce[0], bounce[1], bounce[2]);
                floorShader.setUniform1fv("u_curveF", LightEngine.levelCurve(), 0, 8);
                floorShader.setUniformf("u_debug", debug ? 1f : 0f);
                LightEngine.lightmap().bind(1);
                LightEngine.bounce().bind(2);
                LightEngine.furniture().bind(3);
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
            }
            Texture t = bands[i].getColorBufferTexture();
            Graphics.batch.draw(t, 0, 0, Graphics.VIRTUAL_WIDTH, Graphics.VIRTUAL_HEIGHT,
                    0, 0, t.getWidth(), t.getHeight(), false, false);
            Graphics.batch.end();
        }
        Graphics.batch.setShader(null);
        dst.end();
    }

    static void dispose() {
        for (int i = 0; i < BAND_COUNT; i++) {
            if (bands[i] != null) {
                bands[i].dispose();
                bands[i] = null;
            }
        }
        if (vignette != null) {
            vignette.dispose();
            vignette = null;
        }
        if (flatShape != null) {
            flatShape.dispose();
            flatShape = null;
        }
        if (classifyFbo != null) {
            classifyFbo.dispose();
            classifyFbo = null;
        }
    }
}
