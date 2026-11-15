package darkestfear.menu;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public final class GameControls {

    private static final int MAX_POINTERS = 10;
    private static final int CODE_SKIP_ALL = Integer.MIN_VALUE;
    private static final int SKIP_ALL_LIMIT = 64;

    private static final int COLOR_FILL = 0x1A1A1A;
    private static final int COLOR_FILL_DOWN = 0x7A1E1E;
    private static final int COLOR_BORDER = 0x707070;
    private static final int COLOR_LABEL = 0xE0E0E0;

    private static final class Button {
        final int code;
        final String label;
        float x;
        float y;
        float w;
        float h;
        boolean pressed;

        Button(int code, String label) {
            this.code = code;
            this.label = label;
        }

        void set(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(float px, float py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private static final Button UP = new Button(Keys.UP, "^");
    private static final Button DOWN = new Button(Keys.DOWN, "v");
    private static final Button LEFT = new Button(Keys.LEFT, "<");
    private static final Button RIGHT = new Button(Keys.RIGHT, ">");
    private static final Button A = new Button(-5, "A");
    private static final Button MENU = new Button(-6, "MENU");
    private static final Button NEXT = new Button(-5, "NEXT");
    private static final Button SKIP = new Button(CODE_SKIP_ALL, "SKIP");
    private static final Button OK = new Button(-5, "OK");

    private static final Button[] LEVEL = {UP, DOWN, LEFT, RIGHT, A, MENU};
    private static final Button[] CUTSCENE = {NEXT, SKIP};
    private static final Button[] POPUP = {UP, DOWN, OK};
    private static final Button[] DIALOG = {OK};

    private static Button[] current;

    private GameControls() {
    }

    public static boolean active() {
        return touchDevice() && set() != null;
    }

    private static Button[] set() {
        if (GameBridge.inLevel()) {
            return LEVEL;
        }
        if (GameBridge.inCutscene()) {
            return CUTSCENE;
        }
        if (GameBridge.inPopup()) {
            return POPUP;
        }
        if (GameBridge.inDialog()) {
            return DIALOG;
        }
        return null;
    }

    private static boolean touchDevice() {
        Application.ApplicationType type = Gdx.app.getType();
        return type == Application.ApplicationType.Android || type == Application.ApplicationType.iOS;
    }

    private static void layout(Button[] s) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float m = w * 0.04f;
        if (s == LEVEL) {
            float bd = w * 0.14f;
            float ox = m;
            float oy = h - m - 3 * bd;
            UP.set(ox + bd, oy, bd, bd);
            LEFT.set(ox, oy + bd, bd, bd);
            RIGHT.set(ox + 2 * bd, oy + bd, bd, bd);
            DOWN.set(ox + bd, oy + 2 * bd, bd, bd);
            float ba = w * 0.20f;
            A.set(w - m - ba, h - m - ba, ba, ba);
            float mw = w * 0.22f;
            float mh = w * 0.10f;
            MENU.set(w - m - mw, m, mw, mh);
        } else if (s == CUTSCENE) {
            float bw = w * 0.24f;
            float bh = w * 0.12f;
            SKIP.set(w - m - bw, h - m - bh, bw, bh);
            NEXT.set(w - m - bw - m - bw, h - m - bh, bw, bh);
        } else {
            float ow = w * 0.30f;
            float oh = w * 0.13f;
            OK.set((w - ow) / 2, h - m - oh, ow, oh);
            if (s == POPUP) {
                float bd = w * 0.14f;
                UP.set(w - m - bd, h - m - oh - m - 2 * bd, bd, bd);
                DOWN.set(w - m - bd, h - m - oh - m - bd, bd, bd);
            }
        }
    }

    public static void poll() {
        Button[] s = set();
        if (s != current) {
            reset();
            current = s;
        }
        if (s == null) {
            return;
        }
        layout(s);
        boolean[] now = new boolean[s.length];
        for (int p = 0; p < MAX_POINTERS; p++) {
            if (!Gdx.input.isTouched(p)) {
                continue;
            }
            float px = Gdx.input.getX(p);
            float py = Gdx.input.getY(p);
            for (int b = 0; b < s.length; b++) {
                if (s[b].contains(px, py)) {
                    now[b] = true;
                }
            }
        }
        for (int b = 0; b < s.length; b++) {
            Button btn = s[b];
            if (btn.code == CODE_SKIP_ALL) {
                if (now[b] && !btn.pressed) {
                    skipCutscene();
                }
            } else if (now[b] && !btn.pressed) {
                Canvas.dispatchKeyPressed(btn.code);
            } else if (!now[b] && btn.pressed) {
                Canvas.dispatchKeyReleased(btn.code);
            }
            btn.pressed = now[b];
        }
    }

    private static void skipCutscene() {
        for (int i = 0; i < SKIP_ALL_LIMIT && GameBridge.pendingCutscene(); i++) {
            Canvas.dispatchKeyPressed(-5);
            Canvas.dispatchKeyReleased(-5);
        }
    }

    public static void reset() {
        if (current == null) {
            return;
        }
        for (Button b : current) {
            if (b.pressed) {
                if (b.code != CODE_SKIP_ALL) {
                    Canvas.dispatchKeyReleased(b.code);
                }
                b.pressed = false;
            }
        }
    }

    public static void renderHud(Graphics g) {
        Button[] s = current;
        if (s == null) {
            return;
        }
        layout(s);

        Font font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
        BitmapFont bf = font.getBitmapFont();
        float oldX = bf.getData().scaleX;
        float oldY = bf.getData().scaleY;
        float scale = Math.max(1f, (Gdx.graphics.getWidth() * 0.05f) / bf.getCapHeight());

        Graphics.beginHud();
        try {
            g.setFont(font);
            bf.getData().setScale(scale);
            for (Button b : s) {
                g.setColor(b.pressed ? COLOR_FILL_DOWN : COLOR_FILL);
                g.fillRect((int) b.x, (int) b.y, (int) b.w, (int) b.h);
                g.setColor(COLOR_BORDER);
                g.drawRect((int) b.x, (int) b.y, (int) b.w, (int) b.h);
                g.setColor(COLOR_LABEL);
                g.drawString(b.label, (int) (b.x + b.w / 2), (int) (b.y + b.h / 2),
                        Graphics.HCENTER | Graphics.VCENTER);
            }
        } finally {
            bf.getData().setScale(oldX, oldY);
            Graphics.endHud();
        }
    }
}
