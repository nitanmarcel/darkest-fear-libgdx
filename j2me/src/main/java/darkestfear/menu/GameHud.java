package darkestfear.menu;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public final class GameHud {

    private static final int HEART = 76, KEY = 77, PATIENT = 93;
    private static final int MAX_HEARTS = 8;
    private static final int HEART_STEP = 11;
    private static final int ITEM_BOX = 19;
    private static final int LINE_STEP = 14;

    private static final int W = MenuTheme.W;
    private static final int H = MenuTheme.H;

    private GameHud() {
    }

    private static final int PAUSE_KEY = -6;
    private static final int PAUSE_REACH = 10;
    private static int pauseX, pauseY;

    private static final int ABILITIES_TEXT = 22;
    private static final int HINT_DELAY = 30;
    private static Object hintedLevel;
    private static int hintFrames;

    public static void renderLevel(Graphics g) {
        if (GameBridge.inLevel()) {
            render(g, true);
            startHint();
        } else {
            hintFrames = 0;
            if (GameBridge.inCutscene()) {
                renderCaption(g);
            }
        }
    }

    private static void startHint() {
        Object level = LightBridge.currentLevel();
        if (LightBridge.levelIndex() != 0 || level == hintedLevel) {
            hintFrames = 0;
            return;
        }
        if (++hintFrames < HINT_DELAY) {
            return;
        }
        hintedLevel = level;
        hintFrames = 0;
        String abilities = GameBridge.text(ABILITIES_TEXT).trim();
        abilities = abilities.substring(abilities.lastIndexOf('\n') + 1).trim();
        String controls = GameInput.gamepad ? "Move: stick or D-pad. Interact: A. Pause: Start."
                : MenuOverlay.isDesktop() ? "Move: " + GameInput.keyName(GameInput.UP) + " " + GameInput.keyName(GameInput.LEFT)
                        + " " + GameInput.keyName(GameInput.DOWN) + " " + GameInput.keyName(GameInput.RIGHT) + ". Interact: "
                        + GameInput.keyName(GameInput.PRIMARY) + ". Pause: Esc."
                : Graphics.wide() ? "Drag on the left to move. Tap on the right to pick up or put down a light."
                : null;
        LightBridge.showMessage(controls == null ? abilities : controls + "\n\n" + abilities);
    }

    private static void renderCaption(Graphics g) {
        int top = GameBridge.captionTop();
        if (top <= 0 || top >= H) {
            return;
        }
        g.setClip(0, 0, W, H);
        g.setColor(0);
        g.fillRect(0, top, W, H - top);
        String caption = GameBridge.caption();
        if (caption == null || caption.trim().isEmpty()) {
            return;
        }
        g.setColor(MenuTheme.COLOR_BAR);
        g.fillRect(MenuTheme.MARGIN, top, W - 2 * MenuTheme.MARGIN, 1);
        Font bold = MenuTheme.bold();
        Font plain = MenuTheme.plain();
        int y = top + 8;
        int cut = caption.indexOf('\n');
        if (cut > 0 && caption.substring(0, cut).trim().endsWith(":")) {
            String speaker = caption.substring(0, cut).trim();
            g.setFont(bold);
            g.setColor(MenuTheme.COLOR_SECTION);
            g.drawString(speaker.substring(0, speaker.length() - 1).trim().toUpperCase(), W / 2, y,
                    Graphics.TOP | Graphics.HCENTER);
            y += LINE_STEP + 1;
            caption = caption.substring(cut + 1);
        }
        g.setFont(plain);
        g.setColor(MenuTheme.COLOR_VALUE);
        for (String line : MenuOverlay.wrapText(plain, caption.replace('\n', ' ').trim(), W - 2 * MenuTheme.MARGIN)) {
            g.drawString(line, W / 2, y, Graphics.TOP | Graphics.HCENTER);
            y += LINE_STEP;
        }
    }

    public static void renderBox(Graphics g) {
        render(g, false);
        if (!corners) {
            renderPopup(g, LightBridge.playHeight() - 8);
        }
    }

    static boolean onPause(float screenX, float screenY) {
        com.badlogic.gdx.math.Vector2 p = corners ? Graphics.toWide((int) screenX, (int) screenY)
                : Graphics.toVirtual((int) screenX, (int) screenY);
        return p.x >= pauseX - PAUSE_REACH && p.y >= pauseY - PAUSE_REACH
                && p.y < (corners ? pauseY + ITEM_BOX + PAUSE_REACH : H);
    }

    public static boolean tap(int screenX, int screenY) {
        if (GameControls.justSkipped()) {
            return true;
        }
        if (GameBridge.inPopup() || GameBridge.inDialog() || GameBridge.inCutscene()) {
            GameInput.press(GameInput.PRIMARY);
            GameInput.release(GameInput.PRIMARY);
            return true;
        }
        if (!GameBridge.inLevel()) {
            return false;
        }
        if (onPause(screenX, screenY)) {
            javax.microedition.lcdui.Canvas.dispatchKeyPressed(PAUSE_KEY);
            return true;
        }
        return false;
    }

    private static final int CORNER = 14;
    private static final int BACK_PAD = 4;
    private static final float BACK_DIM = 0.55f;
    private static boolean corners;

    private static void render(Graphics g, boolean pause) {
        int[] v = LightBridge.hud();
        int top = LightBridge.playHeight();
        corners = Graphics.wide();
        if (corners) {
            g.setClip(0, 0, Graphics.VIRTUAL_WIDTH, Graphics.VIRTUAL_HEIGHT);
            g.setColor(0);
            g.fillRect(0, top, Graphics.VIRTUAL_WIDTH, Graphics.VIRTUAL_HEIGHT - top);
            return;
        }
        if (v == null || top <= 0 || top >= H) {
            return;
        }
        g.setClip(0, 0, W, H);
        g.setColor(MenuTheme.COLOR_BG);
        g.fillRect(0, top, W, H - top);
        g.setColor(MenuTheme.COLOR_BAR);
        g.fillRect(0, top, W, 1);
        stats(g, v, 12, top + 5, W - 12);
        int ruleY = top + 25;
        g.setColor(MenuTheme.COLOR_RULE);
        g.fillRect(MenuTheme.MARGIN, ruleY, W - 2 * MenuTheme.MARGIN, 1);
        item(g, v, 10, ruleY + 4);
        pauseX = W - 10 - ITEM_BOX;
        pauseY = ruleY + 4;
        if (pause) {
            pauseButton(g);
        }
    }

    public static void renderWide(Graphics g, boolean box) {
        int[] v = LightBridge.hud();
        if (v == null) {
            return;
        }
        int wide = Graphics.wideWidth();
        g.setClip(0, 0, wide, H);
        int left = CORNER + Graphics.wideInsetLeft(), right = wide - CORNER - Graphics.wideInsetRight();
        pauseX = right - ITEM_BOX;
        pauseY = CORNER - 2;
        int statsRight = box ? right : pauseX - 10;
        int itemY = GameControls.touchDevice() ? CORNER + 24 : H - CORNER - ITEM_BOX;
        Font bold = MenuTheme.bold();
        int hearts = v[1] <= MAX_HEARTS ? v[1] * HEART_STEP : 13 + bold.stringWidth(v[0] + "/" + v[1]);
        int counts = bold.stringWidth(v[3] + "/" + v[4]) + 16 + 10 + bold.stringWidth(String.valueOf(v[2])) + 18;
        Graphics.dim(left - BACK_PAD, CORNER - BACK_PAD, hearts + 2 * BACK_PAD, 16 + 2 * BACK_PAD, BACK_DIM);
        Graphics.dim(statsRight - counts - BACK_PAD, CORNER - BACK_PAD, counts + 2 * BACK_PAD, 16 + 2 * BACK_PAD, BACK_DIM);
        if (v[5] >= 0) {
            Graphics.dim(left + ITEM_BOX, itemY, 8 + MenuTheme.plain().stringWidth(GameBridge.text(v[5])) + BACK_PAD,
                    ITEM_BOX, BACK_DIM);
        }
        stats(g, v, left, CORNER, statsRight);
        item(g, v, left, itemY);
        if (box) {
            Graphics.wideOrigin(Graphics.sceneX() + (Graphics.sceneWidth() - W) / 2, Graphics.sceneY());
            renderPopup(g, Graphics.sceneHeight() - 8);
        } else {
            pauseButton(g);
        }
    }

    private static void stats(Graphics g, int[] v, int left, int rowY, int right) {
        Font bold = MenuTheme.bold();
        int anchor = Graphics.TOP | Graphics.LEFT;
        int textY = rowY + (16 - bold.getHeight()) / 2 + MenuTheme.TEXT_DROP;
        g.setFont(bold);
        if (v[1] <= MAX_HEARTS) {
            for (int i = 0; i < v[1]; i++) {
                sprite(g, HEART, left + i * HEART_STEP, rowY + 1, i < v[0]);
            }
        } else {
            sprite(g, HEART, left, rowY + 1, true);
            g.setColor(MenuTheme.COLOR_VALUE);
            g.drawString(v[0] + "/" + v[1], left + 13, textY, anchor);
        }
        String saved = v[3] + "/" + v[4];
        int x = right - bold.stringWidth(saved);
        g.setColor(MenuTheme.COLOR_VALUE);
        g.drawString(saved, x, textY, anchor);
        x -= 16;
        sprite(g, PATIENT, x, rowY + 1, true);
        String keys = String.valueOf(v[2]);
        x -= 10 + bold.stringWidth(keys);
        g.setColor(MenuTheme.COLOR_VALUE);
        g.drawString(keys, x, textY, anchor);
        sprite(g, KEY, x - 18, rowY + 4, true);
    }

    private static void item(Graphics g, int[] v, int x, int boxY) {
        Font plain = MenuTheme.plain();
        g.setColor(MenuTheme.COLOR_PANEL);
        g.fillRect(x, boxY, ITEM_BOX, ITEM_BOX);
        g.setColor(v[6] >= 0 ? MenuTheme.COLOR_ACCENT : MenuTheme.COLOR_RULE);
        g.fillRect(x, boxY, ITEM_BOX, 1);
        short[] item = v[6] >= 0 ? LightBridge.spriteAttr(v[6]) : null;
        if (item != null) {
            sprite(g, v[6], x + (ITEM_BOX - item[2]) / 2, boxY + 1 + (ITEM_BOX - item[3]) / 2, true);
        }
        if (v[5] >= 0) {
            g.setFont(plain);
            g.setColor(MenuTheme.COLOR_TEXT);
            g.drawString(GameBridge.text(v[5]), x + ITEM_BOX + 8,
                    boxY + (ITEM_BOX - plain.getHeight()) / 2 + MenuTheme.TEXT_DROP, Graphics.TOP | Graphics.LEFT);
        }
    }

    private static void pauseButton(Graphics g) {
        g.setColor(MenuTheme.COLOR_PANEL);
        g.fillRect(pauseX, pauseY, ITEM_BOX, ITEM_BOX);
        g.setColor(MenuTheme.COLOR_VALUE);
        g.fillRect(pauseX + 6, pauseY + 5, 2, ITEM_BOX - 10);
        g.fillRect(pauseX + ITEM_BOX - 8, pauseY + 5, 2, ITEM_BOX - 10);
    }

    private static void renderPopup(Graphics g, int bottom) {
        String[] raw = GameBridge.popupLines();
        int portrait = GameBridge.popupPortrait();
        short[] face = portrait >= 0 ? LightBridge.spriteAttr(portrait) : null;
        Font bold = MenuTheme.bold();
        Font plain = MenuTheme.plain();
        int anchor = Graphics.TOP | Graphics.LEFT;

        String speaker = null;
        int first = 0;
        if (face != null && raw.length > 1 && raw[0].trim().endsWith(":")) {
            speaker = raw[0].trim();
            speaker = speaker.substring(0, speaker.length() - 1).trim().toUpperCase();
            first = 1;
        }
        StringBuilder text = new StringBuilder();
        boolean paragraph = false;
        for (int i = first; i < raw.length; i++) {
            String line = raw[i].trim();
            if (line.isEmpty()) {
                paragraph = text.length() > 0;
                continue;
            }
            if (text.length() > 0) {
                text.append(paragraph ? "\n\n" : " ");
            }
            paragraph = false;
            text.append(line);
        }

        int left = MenuTheme.MARGIN, right = W - MenuTheme.MARGIN;
        int textX = face != null ? left + 8 + face[2] + 10 : left + 12;
        String[] lines = MenuOverlay.wrapText(plain, text.toString(), right - 10 - textX);
        int textH = (speaker != null ? LINE_STEP : 0) + lines.length * LINE_STEP;
        int height = Math.max(textH, face != null ? face[3] : 0) + 18;
        int y = Math.max(4, bottom - height);

        g.setClip(0, 0, W, H);
        g.setColor(MenuTheme.COLOR_PANEL);
        g.fillRect(left, y, right - left, height);
        g.setColor(MenuTheme.COLOR_ACCENT);
        g.fillRect(left, y, right - left, 1);
        if (face != null) {
            sprite(g, portrait, left + 8, y + 9, true);
        }
        int ty = y + 9 + (face != null ? Math.max(0, (face[3] - textH) / 2) : 0);
        if (speaker != null) {
            g.setFont(bold);
            g.setColor(MenuTheme.COLOR_SECTION);
            g.drawString(speaker, textX, ty, anchor);
            ty += LINE_STEP;
        }
        g.setFont(plain);
        g.setColor(MenuTheme.COLOR_VALUE);
        for (String line : lines) {
            g.drawString(line, textX, ty, anchor);
            ty += LINE_STEP;
        }
        g.setColor(MenuTheme.COLOR_ACCENT);
        for (int r = 0; r < 4; r++) {
            g.fillRect(right - 16 + r, y + height - 10 + r, 8 - 2 * r, 1);
        }
    }

    private static void sprite(Graphics g, int id, int x, int y, boolean bright) {
        short[] a = LightBridge.spriteAttr(id);
        Object sheet = LightBridge.spriteImage(id);
        if (a == null || !(sheet instanceof Image)) {
            return;
        }
        if (!bright && id == HEART && Graphics.drawPack("heartEmpty", x + a[4], y + a[5])) {
            return;
        }
        g.drawRegion((Image) sheet, a[0], a[1], a[2], a[3], bright ? 0 : Graphics.DIMMED, x, y,
                Graphics.TOP | Graphics.LEFT);
    }
}
