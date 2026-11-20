package darkestfear.menu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public final class MenuOverlay {

    private static final int MENU_MAIN = 0;
    private static final int MENU_SETTINGS = 2;
    private static final int MENU_HELP = 3;

    private static final String HELP_TEXT =
            "Controls:\n"
            + "Move - Arrow keys or 2,4,6,8\n"
            + "Interact / pick up - Enter or Space\n"
            + "Pause - Q\n"
            + "Soft keys - Q (confirm), E (back)\n"
            + "Menus - mouse, arrows, Enter\n"
            + "\n"
            + "Thomas is able to - open windows, light fireplaces, carry light sources, "
            + "carry mirrors, move boxes, open doors and interact with other characters.";

    private static String[] helpLines;

    private static final int ACT_CONTINUE = 0;
    private static final int ACT_NEW_GAME = 1;
    private static final int ACT_SETTINGS = 2;
    private static final int ACT_HELP = 3;
    private static final int ACT_ABOUT = 4;
    private static final int ACT_EXIT = 5;
    private static final int ACT_SOUND = 6;
    private static final int ACT_FULLSCREEN = 7;
    private static final int ACT_RESET = 8;
    private static final int ACT_BACK = 9;
    private static final int ACT_LIGHTING = 11;
    private static final int ACT_CUES = 12;

    private static final int COLOR_BG = 0x0A0A0A;
    private static final int COLOR_TITLE = 0xE0E0E0;
    private static final int COLOR_ITEM = 0x9A9A9A;
    private static final int COLOR_SELECTED = 0xFFFFFF;
    private static final int COLOR_DISABLED = 0x555555;
    private static final int COLOR_BAR = 0x4A0000;
    private static final int COLOR_SOFTKEY = 0xC08080;
    private static final int COLOR_SHADOW = 0x000000;

    private static final int W = Graphics.VIRTUAL_WIDTH;
    private static final int H = Graphics.VIRTUAL_HEIGHT;
    private static final int LIST_X = 16;
    private static final int LIST_W = W - 32;
    private static final int TITLE_Y = 44;
    private static final int SOFTKEY_Y = H - 22;

    private static volatile String[] labels = new String[0];
    private static volatile int[] actions = new int[0];
    private static volatile int selected;

    private static int lastMenuId = Integer.MIN_VALUE;
    private static int rowTop;
    private static int rowHeight;
    private static int customCount;
    private static int mirrorCount;

    private static volatile boolean pendingFullscreen;
    private static int windowedWidth = 800;
    private static int windowedHeight = 600;

    private MenuOverlay() {
    }

    public static boolean active() {
        return GameBridge.currentMenu() != null;
    }

    private static boolean isCustom(int menuId) {
        return menuId == MENU_MAIN || menuId == MENU_SETTINGS;
    }

    public static boolean consumesInput() {
        return isCustom(GameBridge.currentMenuId());
    }

    public static void applyPending() {
        if (!pendingFullscreen) {
            return;
        }
        pendingFullscreen = false;
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
        } else {
            windowedWidth = Gdx.graphics.getWidth();
            windowedHeight = Gdx.graphics.getHeight();
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }

    public static void render(Graphics g) {
        Object menu = GameBridge.currentMenu();
        if (menu == null) {
            return;
        }
        int menuId = GameBridge.currentMenuId();
        if (menuId != lastMenuId) {
            lastMenuId = menuId;
            selected = 0;
        }

        g.setClip(0, 0, W, H);
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, W, H);

        if (isCustom(menuId)) {
            renderCustom(g, menuId);
        } else {
            renderMirror(g, menu, menuId);
        }
    }

    public static void scroll(int amountY) {
        Object menu = GameBridge.currentMenu();
        if (menu == null || isCustom(GameBridge.currentMenuId())) {
            return;
        }
        int maxScroll = Math.max(0, mirrorTotal - mirrorAvail);
        int target = Math.max(0, Math.min(GameBridge.menuScroll(menu) + amountY * SCROLL_STEP, maxScroll));
        GameBridge.setMenuScroll(menu, target);
    }

    public static boolean keyPressed(int code) {
        if (!isCustom(GameBridge.currentMenuId())) {
            return false;
        }
        int n = labels.length;
        if (n == 0) {
            return true;
        }
        switch (code) {
            case Keys.UP:
            case Keys.W:
                selected = (selected - 1 + n) % n;
                return true;
            case Keys.DOWN:
            case Keys.S:
                selected = (selected + 1) % n;
                return true;
            case -5:
            case -6:
            case Keys.ENTER:
            case Keys.SPACE:
                activate(selected);
                return true;
            case -7:
            case Keys.ESCAPE:
                back();
                return true;
            default:
                return true;
        }
    }

    private static Font font() {
        return Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
    }

    private static void renderCustom(Graphics g, int menuId) {
        build(menuId);
        String[] items = labels;

        Font font = font();
        g.setFont(font);
        rowHeight = font.getHeight() + 12;
        int blockHeight = items.length * rowHeight;
        rowTop = Math.max(84, (H - blockHeight) / 2 + 16);

        String title = menuId == MENU_SETTINGS ? textOr(4, "Settings") : textOr(1, "Darkest Fear");
        drawShadowed(g, title, W / 2, TITLE_Y, COLOR_TITLE);

        customCount = items.length;

        for (int i = 0; i < items.length; i++) {
            int y = rowTop + i * rowHeight;
            if (i == selected) {
                g.setColor(COLOR_BAR);
                g.fillRect(LIST_X, y, LIST_W, rowHeight);
            }
            drawShadowed(g, items[i], W / 2, y + 6, i == selected ? COLOR_SELECTED : COLOR_ITEM);
        }
    }

    private static int[] mirrorTops = new int[0];
    private static int[] mirrorHeights = new int[0];
    private static int mirrorScrollY;
    private static int mirrorListTop;
    private static int mirrorAvail;
    private static int mirrorTotal;

    private static final int SCROLL_STEP = 24;

    private static void renderMirror(Graphics g, Object menu, int menuId) {
        int count = GameBridge.menuItemCount(menu);
        int sel = GameBridge.menuSelected(menu);

        g.setFont(font());
        String title = GameBridge.menuTitle(menu);
        if (title != null && !title.isEmpty()) {
            drawShadowed(g, title, W / 2, GameBridge.menuTitleTop(menu) + 3, COLOR_TITLE);
        }

        String[][] lines = new String[count][];
        Font[] fonts = new Font[count];
        int[] lineSteps = new int[count];
        int[] topPads = new int[count];
        int[] heights = new int[count];
        int[] tops = new int[count];
        int total = 0;
        for (int i = 0; i < count; i++) {
            if (menuId == MENU_HELP && i == 0) {
                fonts[i] = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
                lines[i] = helpLines(fonts[i]);
                lineSteps[i] = fonts[i].getHeight() + 2;
                topPads[i] = 4;
                heights[i] = Math.max(1, lines[i].length) * lineSteps[i] + 8;
            } else {
                lines[i] = GameBridge.menuItemLines(menu, i);
                Font f = GameBridge.menuItemFont(menu, i);
                fonts[i] = f != null ? f : font();
                lineSteps[i] = fonts[i].getHeight();
                topPads[i] = 2;
                heights[i] = GameBridge.menuItemHeight(menu, i);
                if (heights[i] <= 0) {
                    heights[i] = Math.max(1, lines[i].length) * lineSteps[i] + 4;
                }
            }
            tops[i] = total;
            total += heights[i] + 2;
        }

        int listTop = GameBridge.menuViewTop(menu);
        int avail = GameBridge.menuViewHeight(menu);
        int scrollY = GameBridge.menuScroll(menu);

        mirrorTops = tops;
        mirrorHeights = heights;
        mirrorScrollY = scrollY;
        mirrorListTop = listTop;
        mirrorAvail = avail;
        mirrorTotal = total;

        g.setClip(0, listTop, W, avail);
        for (int i = 0; i < count; i++) {
            int y = listTop + tops[i] - scrollY;
            if (y + heights[i] < listTop || y > listTop + avail) {
                continue;
            }
            boolean enabled = GameBridge.menuItemEnabled(menu, i);
            if (i == sel) {
                g.setColor(COLOR_BAR);
                g.fillRect(LIST_X, y, LIST_W, heights[i]);
            }
            int color = !enabled ? COLOR_DISABLED : (i == sel ? COLOR_SELECTED : COLOR_ITEM);
            g.setFont(fonts[i]);
            String[] ls = lines[i];
            for (int k = 0; k < ls.length; k++) {
                drawShadowed(g, ls[k], W / 2, y + topPads[i] + k * lineSteps[i], color);
            }
        }
        g.setClip(0, 0, W, H);

        if (scrollY > 0) {
            drawArrow(g, W / 2, listTop - 8, true, COLOR_SOFTKEY);
        }
        if (scrollY + avail < total) {
            drawArrow(g, W / 2, listTop + avail + 2, false, COLOR_SOFTKEY);
        }

        g.setFont(font());
        String left = GameBridge.menuSoftLabel(menu, 0);
        String right = GameBridge.menuSoftLabel(menu, 1);
        g.setColor(COLOR_SOFTKEY);
        if (left != null && !left.isEmpty()) {
            g.drawString(left, 4, SOFTKEY_Y, 16 | 4);
        }
        if (right != null && !right.isEmpty()) {
            g.drawString(right, W - 4, SOFTKEY_Y, 16 | 8);
        }

        mirrorCount = count;
    }

    public static void tap(int screenX, int screenY) {
        Vector2 p = Graphics.toVirtual(screenX, screenY);
        int menuId = GameBridge.currentMenuId();
        if (isCustom(menuId)) {
            int row = hoverRowCustom(p.x, p.y, customCount);
            if (row != -1) {
                selected = row;
                activate(row);
            }
            return;
        }
        Object menu = GameBridge.currentMenu();
        if (menu == null) {
            return;
        }
        int row = hoverRowMirror(p.x, p.y, mirrorCount);
        if (row != -1) {
            GameBridge.menuSetSelected(menu, row);
            Canvas.dispatchKeyPressed(-5);
            return;
        }
        int soft = hoverSoftkey(p.x, p.y);
        if (soft == 0) {
            Canvas.dispatchKeyPressed(-6);
        } else if (soft == 1) {
            Canvas.dispatchKeyPressed(-7);
        }
    }

    public static void pan(float screenDeltaY) {
        Object menu = GameBridge.currentMenu();
        if (menu == null || isCustom(GameBridge.currentMenuId())) {
            return;
        }
        int maxScroll = Math.max(0, mirrorTotal - mirrorAvail);
        int delta = Math.round(screenDeltaY * Graphics.screenToWorldScaleY());
        int target = Math.max(0, Math.min(GameBridge.menuScroll(menu) - delta, maxScroll));
        GameBridge.setMenuScroll(menu, target);
    }

    public static void hover(int screenX, int screenY) {
        Vector2 p = Graphics.toVirtual(screenX, screenY);
        int menuId = GameBridge.currentMenuId();
        if (isCustom(menuId)) {
            int row = hoverRowCustom(p.x, p.y, customCount);
            if (row != -1) {
                selected = row;
            }
            return;
        }
        Object menu = GameBridge.currentMenu();
        if (menu == null) {
            return;
        }
        int row = hoverRowMirror(p.x, p.y, mirrorCount);
        if (row != -1) {
            GameBridge.menuSetSelected(menu, row);
        }
    }

    private static int hoverRowCustom(float vx, float vy, int count) {
        if (vx < LIST_X || vx > LIST_X + LIST_W || vy < rowTop) {
            return -1;
        }
        int idx = (int) ((vy - rowTop) / rowHeight);
        if (idx >= 0 && idx < count) {
            return idx;
        }
        return -1;
    }

    private static int hoverRowMirror(float vx, float vy, int count) {
        if (vx < LIST_X || vx > LIST_X + LIST_W
                || vy < mirrorListTop || vy > mirrorListTop + mirrorAvail) {
            return -1;
        }
        int contentY = (int) vy - mirrorListTop + mirrorScrollY;
        for (int i = 0; i < count && i < mirrorTops.length; i++) {
            if (contentY >= mirrorTops[i] && contentY < mirrorTops[i] + mirrorHeights[i]) {
                return i;
            }
        }
        return -1;
    }

    private static String[] helpLines(Font font) {
        if (helpLines == null) {
            helpLines = wrapText(font, HELP_TEXT, LIST_W);
        }
        return helpLines;
    }

    private static String[] wrapText(Font font, String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String trial = line.length() == 0 ? word : line + " " + word;
                if (font.stringWidth(trial) <= maxWidth) {
                    line.setLength(0);
                    line.append(trial);
                } else {
                    if (line.length() > 0) {
                        out.add(line.toString());
                    }
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (line.length() > 0) {
                out.add(line.toString());
            }
        }
        return out.toArray(new String[0]);
    }

    private static void drawArrow(Graphics g, int cx, int cy, boolean up, int color) {
        g.setColor(color);
        int size = 4;
        for (int r = 0; r < size; r++) {
            int w = up ? (r * 2 + 1) : (size - r) * 2 - 1;
            g.fillRect(cx - w / 2, cy + r, w, 1);
        }
    }

    private static int hoverSoftkey(float vx, float vy) {
        if (vy < SOFTKEY_Y - 16) {
            return -1;
        }
        return vx < W / 2 ? 0 : 1;
    }

    private static void activate(int index) {
        if (index < 0 || index >= actions.length) {
            return;
        }
        switch (actions[index]) {
            case ACT_CONTINUE:
                GameBridge.enqueueEvent(52);
                break;
            case ACT_NEW_GAME:
                GameBridge.openMenu(1);
                break;
            case ACT_SETTINGS:
                GameBridge.openMenu(2);
                break;
            case ACT_HELP:
                GameBridge.openMenu(3);
                break;
            case ACT_ABOUT:
                GameBridge.openMenu(4);
                break;
            case ACT_EXIT:
                GameBridge.enqueueEvent(11);
                break;
            case ACT_SOUND:
                GameBridge.setSoundEnabled(!GameBridge.soundEnabled());
                break;
            case ACT_FULLSCREEN:
                pendingFullscreen = true;
                break;
            case ACT_LIGHTING:
                Graphics.setLightingEnabled(!Graphics.isLightingEnabled());
                break;
            case ACT_CUES:
                Graphics.setCuesEnabled(!Graphics.isCuesEnabled());
                break;
            case ACT_RESET:
                GameBridge.openMenu(9);
                break;
            case ACT_BACK:
                back();
                break;
            default:
                break;
        }
    }

    private static void back() {
        if (GameBridge.currentMenuId() == MENU_SETTINGS) {
            GameBridge.openMenu(0);
        }
    }

    private static boolean isDesktop() {
        return Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.Desktop;
    }

    private static void build(int menuId) {
        List<String> ls = new ArrayList<>();
        List<Integer> as = new ArrayList<>();

        if (menuId == MENU_SETTINGS) {
            add(ls, as, "Sound: " + (GameBridge.soundEnabled() ? "On" : "Off"), ACT_SOUND);
            if (isDesktop()) {
                add(ls, as, "Fullscreen: " + (Gdx.graphics.isFullscreen() ? "On" : "Off"), ACT_FULLSCREEN);
            }
            add(ls, as, "Lighting: " + (Graphics.isLightingEnabled() ? "Enhanced" : "Original"), ACT_LIGHTING);
            add(ls, as, "Cues: " + (Graphics.isCuesEnabled() ? "On" : "Off"), ACT_CUES);
            add(ls, as, textOr(18, "Reset Game"), ACT_RESET);
            add(ls, as, textOr(13, "Back"), ACT_BACK);
        } else {
            if (GameBridge.canContinue()) {
                add(ls, as, textOr(3, "Continue"), ACT_CONTINUE);
            }
            add(ls, as, textOr(2, "New Game"), ACT_NEW_GAME);
            add(ls, as, textOr(4, "Settings"), ACT_SETTINGS);
            add(ls, as, textOr(5, "Help"), ACT_HELP);
            add(ls, as, textOr(6, "About"), ACT_ABOUT);
            add(ls, as, textOr(7, "Exit"), ACT_EXIT);
        }

        String[] newLabels = ls.toArray(new String[0]);
        int[] newActions = new int[as.size()];
        for (int i = 0; i < newActions.length; i++) {
            newActions[i] = as.get(i);
        }
        labels = newLabels;
        actions = newActions;
        if (selected >= newLabels.length) {
            selected = newLabels.length - 1;
        }
        if (selected < 0) {
            selected = 0;
        }
    }

    private static void add(List<String> ls, List<Integer> as, String label, int action) {
        ls.add(label);
        as.add(action);
    }

    private static String textOr(int id, String fallback) {
        String s = GameBridge.text(id);
        return s != null && !s.isEmpty() ? s : fallback;
    }

    private static void drawShadowed(Graphics g, String text, int x, int y, int color) {
        int anchor = 16 | 1;
        g.setColor(COLOR_SHADOW);
        g.drawString(text, x + 1, y + 1, anchor);
        g.setColor(color);
        g.drawString(text, x, y, anchor);
    }
}
