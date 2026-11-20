package darkestfear.menu;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

final class MenuTheme {

    static final int COLOR_BG = 0x0A0A0A;
    static final int COLOR_PANEL = 0x141414;
    static final int COLOR_TITLE = 0xE0E0E0;
    static final int COLOR_ITEM = 0x9A9A9A;
    static final int COLOR_VALUE = 0xBEBEBE;
    static final int COLOR_SELECTED = 0xFFFFFF;
    static final int COLOR_DISABLED = 0x555555;
    static final int COLOR_BAR = 0x4A0000;
    static final int COLOR_ACCENT = 0xAA1E1E;
    static final int COLOR_SECTION = 0x963C3C;
    static final int COLOR_TEXT = 0x8C8C8C;
    static final int COLOR_RULE = 0x282828;
    static final int COLOR_BUTTON = 0x222222;

    static final int W = Graphics.VIRTUAL_WIDTH;
    static final int H = Graphics.VIRTUAL_HEIGHT;
    static final int MARGIN = 8;
    static final int TEXT_X = 16;
    static final int TITLE_Y = 12;
    static final int RULE_Y = 38;
    static final int LIST_TOP = 44;
    static final int ROW_H = 24;
    static final int TEXT_DROP = 3;

    private MenuTheme() {
    }

    static Font titleFont() {
        return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    }

    static Font bold() {
        return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
    }

    static Font plain() {
        return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    }

    static final int BACK_W = 14;

    static void header(Graphics g, String title, boolean back) {
        g.setFont(titleFont());
        g.setColor(COLOR_TITLE);
        if (back) {
            backMark(g, TEXT_X, TITLE_Y + titleFont().getHeight() / 2 + 1);
        }
        g.drawString(title, TEXT_X + (back ? BACK_W : 0), TITLE_Y, Graphics.TOP | Graphics.LEFT);
        g.setColor(COLOR_BAR);
        g.fillRect(MARGIN, RULE_Y, W - 2 * MARGIN, 1);
    }

    static void panelHeader(Graphics g, String title, int top) {
        g.setColor(COLOR_BAR);
        g.fillRect(0, top, W, 1);
        g.setFont(bold());
        g.setColor(COLOR_TITLE);
        backMark(g, TEXT_X, top + 5 + TEXT_DROP + bold().getHeight() / 2 - 1);
        g.drawString(title, TEXT_X + BACK_W, top + 5 + TEXT_DROP, Graphics.TOP | Graphics.LEFT);
    }

    static boolean onBack(float vx, float vy, int headerTop, int headerBottom) {
        return vy >= headerTop && vy < headerBottom && vx < W / 2;
    }

    private static void backMark(Graphics g, int x, int cy) {
        for (int r = 0; r < 5; r++) {
            g.fillRect(x + 4 - r, cy - 5 + r, 2, 1);
            g.fillRect(x + 4 - r, cy + 4 - r, 2, 1);
        }
    }

    static void selectionBar(Graphics g, int y, int height) {
        g.setColor(COLOR_BAR);
        g.fillRect(MARGIN, y, W - 2 * MARGIN, height);
        g.setColor(COLOR_ACCENT);
        g.fillRect(MARGIN, y, 2, height);
    }

    static void scrollBar(Graphics g, int top, int view, int scroll, int total) {
        if (total <= view) {
            return;
        }
        int x = W - 5;
        g.setColor(COLOR_RULE);
        g.fillRect(x, top, 2, view);
        g.setColor(COLOR_ACCENT);
        g.fillRect(x, top + scroll * view / total, 2, view * view / total);
    }

    static void chevron(Graphics g, int x, int cy, boolean left) {
        for (int r = 0; r < 4; r++) {
            int dx = left ? 3 - r : r;
            g.fillRect(x + dx, cy - 4 + r, 2, 1);
            g.fillRect(x + dx, cy + 3 - r, 2, 1);
        }
    }
}
