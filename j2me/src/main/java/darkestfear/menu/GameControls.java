package darkestfear.menu;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public final class GameControls {

    private static final int MAX_POINTERS = 10;
    private static final int NONE = 0, LEVEL = 1, CUTSCENE = 2;
    private static final float DEAD_ZONE = 0.035f;
    private static final float REACH = 0.11f;
    private static final float STICKY = 1.1f;
    private static final float TRAVEL = 0.08f;
    private static final long SKIP_TAP_GUARD_MS = 400;

    private static final boolean[] down = new boolean[MAX_POINTERS];
    private static int mode;
    private static int stickPointer = -1;
    private static int actionPointer = -1;
    private static int direction = -1;
    private static float originX, originY, lastX, lastY, tiltX, tiltY;
    private static long skippedAt;
    private static Texture padArt, nubArt, buttonArt, handArt, skipArt;
    private static final float SIDE_X = 0.25f, SKIP_W = 0.32f;
    private static final float MAX_UNIT_DP = 420;

    private static float unit() {
        return Math.min(Gdx.graphics.getWidth(), MAX_UNIT_DP * Gdx.graphics.getDensity());
    }

    private static float rightX() {
        return Gdx.graphics.getWidth() - unit() * SIDE_X;
    }

    private static final int COLOR_IDLE = 0x6A6A6A;

    private static Texture art(String name) {
        Texture texture = new Texture(Gdx.files.internal("controls/" + name + ".png"));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private GameControls() {
    }

    public static boolean active() {
        return touchDevice() && (GameBridge.inLevel() || GameBridge.inCutscene());
    }

    static boolean justSkipped() {
        return System.currentTimeMillis() - skippedAt < SKIP_TAP_GUARD_MS;
    }

    private static boolean touchDevice() {
        Application.ApplicationType type = Gdx.app.getType();
        return type == Application.ApplicationType.Android || type == Application.ApplicationType.iOS;
    }

    private static float controlsTop() {
        float h = Gdx.graphics.getHeight();
        float bottom = Graphics.gameBottom();
        return h - bottom >= unit() * 0.4f ? bottom : h * 0.6f;
    }

    private static float controlsY() {
        float h = Gdx.graphics.getHeight();
        return Math.max((controlsTop() + h) / 2, h - unit() * 0.45f);
    }

    public static void poll() {
        int now = GameBridge.inLevel() ? LEVEL : GameBridge.inCutscene() ? CUTSCENE : NONE;
        if (now != mode) {
            reset();
            mode = now;
        }
        float w = Gdx.graphics.getWidth();
        for (int p = 0; p < MAX_POINTERS; p++) {
            boolean touched = Gdx.input.isTouched(p);
            boolean began = touched && !down[p];
            down[p] = touched;
            if (!began) {
                continue;
            }
            float x = Gdx.input.getX(p), y = Gdx.input.getY(p);
            if (mode == LEVEL && x < w / 2 && stickPointer < 0) {
                stickPointer = p;
                originX = x;
                originY = y;
                lastX = x;
                lastY = y;
                tiltX = 0;
                tiltY = 0;
            } else if (mode == LEVEL && x >= w / 2 && y >= controlsTop() && actionPointer < 0) {
                actionPointer = p;
                GameInput.press(GameInput.PRIMARY);
            } else if (mode == CUTSCENE && onSkip(x, y)) {
                skippedAt = System.currentTimeMillis();
                GameInput.skipCutscene();
            }
        }
        if (stickPointer >= 0) {
            if (Gdx.input.isTouched(stickPointer)) {
                steer(Gdx.input.getX(stickPointer), Gdx.input.getY(stickPointer));
            } else {
                stickPointer = -1;
                setDirection(-1);
            }
        }
        if (actionPointer >= 0 && !Gdx.input.isTouched(actionPointer)) {
            actionPointer = -1;
            GameInput.release(GameInput.PRIMARY);
        }
    }

    private static void steer(float x, float y) {
        tiltX += x - lastX;
        tiltY += y - lastY;
        lastX = x;
        lastY = y;
        float length = (float) Math.sqrt(tiltX * tiltX + tiltY * tiltY);
        float travel = unit() * TRAVEL;
        if (length > travel) {
            tiltX *= travel / length;
            tiltY *= travel / length;
        }
        float dx = tiltX, dy = tiltY;
        float ax = Math.abs(dx), ay = Math.abs(dy);
        if (Math.max(ax, ay) < unit() * DEAD_ZONE * (direction >= 0 ? 0.6f : 1f)) {
            setDirection(-1);
            return;
        }
        boolean wasHorizontal = direction == GameInput.LEFT || direction == GameInput.RIGHT;
        boolean horizontal = direction < 0 ? ax > ay : wasHorizontal ? ax * STICKY > ay : ax > ay * STICKY;
        setDirection(horizontal ? (dx < 0 ? GameInput.LEFT : GameInput.RIGHT) : (dy < 0 ? GameInput.UP : GameInput.DOWN));
    }

    private static void setDirection(int d) {
        if (d == direction) {
            return;
        }
        if (direction >= 0) {
            GameInput.release(direction);
        }
        if (d >= 0) {
            GameInput.press(d);
        }
        direction = d;
    }

    public static void reset() {
        stickPointer = -1;
        setDirection(-1);
        if (actionPointer >= 0) {
            actionPointer = -1;
            GameInput.release(GameInput.PRIMARY);
        }
    }

    private static boolean onSkip(float x, float y) {
        float u = unit();
        return Math.abs(x - rightX()) < u * SKIP_W / 2 && Math.abs(y - controlsY()) < u * SKIP_W / 4;
    }

    public static void renderHud(Graphics g) {
        float u = unit();
        Font font = MenuTheme.bold();
        BitmapFont bf = font.getBitmapFont();
        float oldX = bf.getData().scaleX;
        float oldY = bf.getData().scaleY;
        float scale = (u * 0.04f) / (bf.getCapHeight() * Graphics.RENDER_SCALE);

        Graphics.beginHud();
        try {
            g.setFont(font);
            if (mode == LEVEL) {
                if (padArt == null) {
                    padArt = art("stick_pad");
                    nubArt = art("stick_nub");
                    buttonArt = art("button");
                    handArt = art("icon_hand");
                }
                float reach = u * REACH;
                float cx = stickPointer >= 0 ? originX : u * SIDE_X;
                float cy = stickPointer >= 0 ? originY : controlsY();
                float kx = cx + (direction == GameInput.LEFT ? -reach : direction == GameInput.RIGHT ? reach : 0) * 0.6f;
                float ky = cy + (direction == GameInput.UP ? -reach : direction == GameInput.DOWN ? reach : 0) * 0.6f;
                Graphics.drawHud(padArt, cx, cy, reach * 2.8f, reach * 2.8f, COLOR_IDLE);
                Graphics.drawHud(nubArt, kx, ky, reach * 1.3f, reach * 1.3f,
                        direction >= 0 ? MenuTheme.COLOR_ACCENT : COLOR_IDLE);

                int action = actionPointer >= 0 ? MenuTheme.COLOR_ACCENT : COLOR_IDLE;
                Graphics.drawHud(buttonArt, rightX(), controlsY(), u * 0.26f, u * 0.26f, action);
                Graphics.drawHud(handArt, rightX(), controlsY(), u * 0.13f, u * 0.13f, action);
            } else if (mode == CUTSCENE) {
                if (skipArt == null) {
                    skipArt = art("button_wide");
                }
                Graphics.drawHud(skipArt, rightX(), controlsY(), u * SKIP_W, u * SKIP_W / 2, COLOR_IDLE);
                bf.getData().setScale(scale);
                g.setColor(COLOR_IDLE);
                g.drawString("Skip", Math.round(rightX()), Math.round(controlsY()), Graphics.HCENTER | Graphics.VCENTER);
            }
            bf.getData().setScale(scale * 0.6f);
            g.setColor(MenuTheme.COLOR_TEXT);
            g.drawString(Gdx.graphics.getFramesPerSecond() + " FPS", (int) (u * 0.04f), (int) (u * 0.09f),
                    Graphics.LEFT | Graphics.VCENTER);
        } finally {
            bf.getData().setScale(oldX, oldY);
            Graphics.endHud();
        }
    }
}
