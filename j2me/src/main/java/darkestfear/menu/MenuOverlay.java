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
            + "Menus - mouse, arrows, Enter to choose, Esc to go back\n"
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

    private static final int W = MenuTheme.W;
    private static final int H = MenuTheme.H;
    private static final int LIST_X = MenuTheme.MARGIN;
    private static final int LIST_W = W - 2 * MenuTheme.MARGIN;
    private static final int VIEW_BOTTOM = H - 8;
    private static final int CARD_X = 20;
    private static final int CARD_PAD = 12;
    private static final int BUTTON_H = 26;
    private static final int LINE_STEP = 15;
    private static final int TITLE_ART = 0x9A;
    private static final int SPLASH_ART = 147;
    private static final int TITLE_ART_Y = 240;

    private static volatile String[] labels = new String[0];
    private static volatile int[] actions = new int[0];
    private static volatile int selected;

    private static int lastMenuId = Integer.MIN_VALUE;
    private static int rowTop;
    private static int rowHeight;
    private static int customCount;
    private static int mirrorCount;

    private static volatile boolean pendingFullscreen;
    private static javax.microedition.lcdui.Image splashImage;
    private static short[] splashAttr;
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

    public static boolean showsGame() {
        return GameBridge.currentMenuId() == MENU_SETTINGS && LightBridge.pausedLevel() != null;
    }

    public static void keepSplash() {
        if (splashAttr != null) {
            return;
        }
        short[] attr = LightBridge.spriteAttr(SPLASH_ART);
        Object sheet = LightBridge.spriteImage(SPLASH_ART);
        if (attr != null && sheet instanceof javax.microedition.lcdui.Image) {
            splashImage = (javax.microedition.lcdui.Image) sheet;
            splashAttr = attr.clone();
        }
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
            cardFocus = 0;
            SettingsMenu.opened();
        }

        int top = showsGame() ? SettingsMenu.PANEL_TOP : 0;
        g.setClip(0, 0, W, H);
        g.setColor(MenuTheme.COLOR_BG);
        g.fillRect(0, top, W, H - top);

        if (menuId == MENU_SETTINGS) {
            SettingsMenu.render(g, top);
        } else if (isCustom(menuId)) {
            renderCustom(g, menuId);
        } else {
            renderMirror(g, menu, menuId);
        }
    }

    public static void scroll(int amountY) {
        Object menu = GameBridge.currentMenu();
        if (menu != null && GameBridge.currentMenuId() == MENU_SETTINGS) {
            SettingsMenu.scroll(amountY);
            return;
        }
        if (menu == null || isCustom(GameBridge.currentMenuId())) {
            return;
        }
        int maxScroll = Math.max(0, mirrorTotal - mirrorAvail);
        int target = Math.max(0, Math.min(GameBridge.menuScroll(menu) + amountY * SCROLL_STEP, maxScroll));
        GameBridge.setMenuScroll(menu, target);
    }

    private static boolean mirrorKey(int code) {
        if (cardButtonY >= 0) {
            switch (code) {
                case Keys.LEFT:
                case Keys.RIGHT:
                case Keys.A:
                case Keys.D:
                case Keys.TAB:
                    cardFocus = 1 - cardFocus;
                    return true;
                case -5:
                case Keys.ENTER:
                case Keys.SPACE:
                    Canvas.dispatchKeyPressed(cardFocus == 0 ? -6 : -7);
                    return true;
                case Keys.ESCAPE:
                    Canvas.dispatchKeyPressed(-7);
                    return true;
                default:
                    return false;
            }
        }
        if (code == Keys.ESCAPE) {
            if (mirrorBack) {
                Canvas.dispatchKeyPressed(-7);
            }
            return true;
        }
        return false;
    }

    public static boolean keyPressed(int code) {
        if (!isCustom(GameBridge.currentMenuId())) {
            return mirrorKey(code);
        }
        if (GameBridge.currentMenuId() == MENU_SETTINGS) {
            return SettingsMenu.keyPressed(code);
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

    private static void renderCustom(Graphics g, int menuId) {
        build(menuId);
        String[] items = labels;

        short[] splash = splashAttr;
        Object splashSheet = splashImage;
        boolean poster = splash != null;
        if (poster) {
            g.setColor(0);
            g.fillRect(0, 0, W, H);
            g.drawRegion((javax.microedition.lcdui.Image) splashSheet, splash[0], splash[1], splash[2], splash[3], 0,
                    (W - splash[2]) / 2, 0, Graphics.TOP | Graphics.LEFT);
            rowHeight = Math.min(MenuTheme.ROW_H, (H - splash[3] - 2) / Math.max(1, items.length));
            rowTop = splash[3] + 1;
        } else {
            MenuTheme.header(g, textOr(1, "Darkest Fear"), false);
            short[] art = LightBridge.spriteAttr(TITLE_ART);
            Object sheet = LightBridge.spriteImage(TITLE_ART);
            if (art != null && sheet instanceof javax.microedition.lcdui.Image) {
                g.drawRegion((javax.microedition.lcdui.Image) sheet, art[0], art[1], art[2], art[3], 0,
                        W / 2 - art[4], TITLE_ART_Y - art[5], Graphics.TOP | Graphics.LEFT);
            }
            rowHeight = MenuTheme.ROW_H;
            rowTop = MenuTheme.LIST_TOP;
        }
        Font font = MenuTheme.bold();
        g.setFont(font);
        customCount = items.length;
        int pad = (rowHeight - font.getHeight()) / 2 + MenuTheme.TEXT_DROP;
        for (int i = 0; i < items.length; i++) {
            int y = rowTop + i * rowHeight;
            if (i == selected) {
                MenuTheme.selectionBar(g, y, rowHeight);
            }
            g.setColor(i == selected ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_ITEM);
            g.drawString(items[i], MenuTheme.TEXT_X + 2, y + pad, Graphics.TOP | Graphics.LEFT);
        }
    }

    private static int[] mirrorTops = new int[0];
    private static int[] mirrorHeights = new int[0];
    private static int mirrorScrollY;
    private static int mirrorListTop;
    private static int mirrorAvail;
    private static int mirrorTotal;
    private static int cardButtonY = -1;
    private static int cardFocus;
    private static boolean mirrorBack;

    private static final int SCROLL_STEP = 24;

    private static void renderMirror(Graphics g, Object menu, int menuId) {
        int count = GameBridge.menuItemCount(menu);
        String title = GameBridge.menuTitle(menu);
        String left = GameBridge.menuSoftLabel(menu, 0);
        String right = GameBridge.menuSoftLabel(menu, 1);
        Font plain = MenuTheme.plain();
        Font bold = MenuTheme.bold();

        boolean textOnly = true;
        String[][] lines = new String[count][];
        int textLines = 0;
        for (int i = 0; i < count; i++) {
            boolean selectable = GameBridge.menuItemType(menu, i) != 0;
            textOnly &= !selectable;
            lines[i] = menuId == MENU_HELP && i == 0 ? helpLines(plain)
                    : selectable ? GameBridge.menuItemLines(menu, i) : wrapText(plain, join(GameBridge.menuItemLines(menu, i)), W - 2 * MenuTheme.TEXT_X);
            textLines += lines[i].length;
        }
        boolean question = textOnly && left != null && !left.isEmpty() && right != null && !right.isEmpty();
        cardButtonY = -1;
        mirrorCount = 0;
        if (question && textLines <= 9) {
            renderCard(g, title, lines, left, right);
            return;
        }

        mirrorBack = right != null && !right.isEmpty();
        MenuTheme.header(g, title == null ? "" : title, mirrorBack);
        int listTop = MenuTheme.LIST_TOP;
        int avail = VIEW_BOTTOM - listTop;
        GameBridge.setMenuView(menu, listTop, avail);

        int[] heights = new int[count];
        int[] tops = new int[count];
        int total = 0;
        for (int i = 0; i < count; i++) {
            boolean selectable = GameBridge.menuItemType(menu, i) != 0;
            heights[i] = selectable ? GameBridge.menuItemHeight(menu, i) : Math.max(1, lines[i].length) * LINE_STEP + 4;
            if (heights[i] <= 0) {
                heights[i] = MenuTheme.ROW_H;
            }
            tops[i] = total;
            total += heights[i] + 2;
        }
        int scrollY = Math.max(0, Math.min(GameBridge.menuScroll(menu), Math.max(0, total - avail)));
        int sel = GameBridge.menuSelected(menu);

        mirrorTops = tops;
        mirrorHeights = heights;
        mirrorScrollY = scrollY;
        mirrorListTop = listTop;
        mirrorAvail = avail;
        mirrorTotal = total;
        mirrorCount = textOnly ? 0 : count;

        g.setClip(0, listTop, W, avail);
        for (int i = 0; i < count; i++) {
            int y = listTop + tops[i] - scrollY;
            if (y + heights[i] < listTop || y > listTop + avail) {
                continue;
            }
            if (GameBridge.menuItemType(menu, i) == 0) {
                g.setFont(plain);
                g.setColor(MenuTheme.COLOR_TEXT);
                for (int k = 0; k < lines[i].length; k++) {
                    g.drawString(lines[i][k], MenuTheme.TEXT_X, y + 2 + k * LINE_STEP, Graphics.TOP | Graphics.LEFT);
                }
                continue;
            }
            boolean enabled = GameBridge.menuItemEnabled(menu, i);
            boolean on = i == sel;
            if (on) {
                MenuTheme.selectionBar(g, y, heights[i]);
            }
            String label = lines[i].length > 0 ? lines[i][0] : "";
            String value = null;
            int gap = label.indexOf("   ");
            if (gap > 0 && lines[i].length == 1) {
                value = label.substring(gap).trim();
                label = label.substring(0, gap);
            }
            int color = !enabled ? MenuTheme.COLOR_DISABLED : on ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_ITEM;
            int pad = (heights[i] - lines[i].length * bold.getHeight()) / 2 + MenuTheme.TEXT_DROP;
            g.setFont(bold);
            g.setColor(color);
            g.drawString(label, MenuTheme.TEXT_X + 2, y + pad, Graphics.TOP | Graphics.LEFT);
            for (int k = 1; k < lines[i].length; k++) {
                g.drawString(lines[i][k], MenuTheme.TEXT_X + 2, y + pad + k * bold.getHeight(), Graphics.TOP | Graphics.LEFT);
            }
            if (value != null) {
                g.setFont(on ? bold : plain);
                g.setColor(!enabled ? MenuTheme.COLOR_DISABLED : on ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_VALUE);
                g.drawString(value, W - MenuTheme.TEXT_X, y + pad, Graphics.TOP | Graphics.RIGHT);
            }
        }
        g.setClip(0, 0, W, H);

        MenuTheme.scrollBar(g, listTop, avail, scrollY, total);
    }

    private static void renderCard(Graphics g, String title, String[][] lines, String left, String right) {
        Font titleFont = MenuTheme.titleFont();
        Font plain = MenuTheme.plain();
        Font bold = MenuTheme.bold();
        int inner = W - 2 * CARD_X - 2 * CARD_PAD;
        int textLines = 0;
        for (int i = 0; i < lines.length; i++) {
            lines[i] = wrapText(plain, join(lines[i]), inner);
            textLines += lines[i].length;
        }
        String[] titleLines = wrapText(titleFont, title == null ? "" : title, inner);
        int titleH = titleLines.length * titleFont.getHeight();
        int height = CARD_PAD + titleH + 6 + textLines * LINE_STEP + CARD_PAD + BUTTON_H + CARD_PAD;
        int y = (H - height) / 2;
        int w = W - 2 * CARD_X;

        g.setColor(MenuTheme.COLOR_PANEL);
        g.fillRect(CARD_X, y, w, height);
        g.setColor(MenuTheme.COLOR_ACCENT);
        g.fillRect(CARD_X, y, w, 1);

        int ty = y + CARD_PAD;
        g.setFont(titleFont);
        g.setColor(MenuTheme.COLOR_TITLE);
        for (String line : titleLines) {
            g.drawString(line, W / 2, ty, Graphics.TOP | Graphics.HCENTER);
            ty += titleFont.getHeight();
        }
        ty += 6;
        g.setFont(plain);
        g.setColor(MenuTheme.COLOR_VALUE);
        for (String[] block : lines) {
            for (String line : block) {
                g.drawString(line, W / 2, ty, Graphics.TOP | Graphics.HCENTER);
                ty += LINE_STEP;
            }
        }

        cardButtonY = y + height - CARD_PAD - BUTTON_H;
        int bw = (w - 3 * CARD_PAD) / 2;
        int pad = (BUTTON_H - bold.getHeight()) / 2 + MenuTheme.TEXT_DROP;
        g.setFont(bold);
        for (int slot = 0; slot < 2; slot++) {
            int bx = CARD_X + CARD_PAD + slot * (bw + CARD_PAD);
            g.setColor(slot == cardFocus ? MenuTheme.COLOR_BAR : MenuTheme.COLOR_BUTTON);
            g.fillRect(bx, cardButtonY, bw, BUTTON_H);
            if (slot == cardFocus) {
                g.setColor(MenuTheme.COLOR_ACCENT);
                g.fillRect(bx, cardButtonY, bw, 1);
            }
            g.setColor(MenuTheme.COLOR_SELECTED);
            g.drawString(slot == 0 ? left : right, bx + bw / 2, cardButtonY + pad, Graphics.TOP | Graphics.HCENTER);
        }
    }

    private static String join(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(part.isEmpty() || sb.charAt(sb.length() - 1) == '\n' ? "" : " ");
            }
            sb.append(part.isEmpty() ? "\n\n" : part);
        }
        return sb.toString();
    }

    public static void tap(int screenX, int screenY) {
        Vector2 p = Graphics.toVirtual(screenX, screenY);
        int menuId = GameBridge.currentMenuId();
        if (menuId == MENU_SETTINGS) {
            SettingsMenu.tap(p.x, p.y);
            return;
        }
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
        if (cardButtonY >= 0) {
            if (p.y >= cardButtonY && p.y < cardButtonY + BUTTON_H && p.x >= CARD_X && p.x < W - CARD_X) {
                Canvas.dispatchKeyPressed(p.x < W / 2 ? -6 : -7);
            }
            return;
        }
        int row = hoverRowMirror(p.x, p.y, mirrorCount);
        if (row != -1) {
            GameBridge.menuSetSelected(menu, row);
            Canvas.dispatchKeyPressed(-5);
            return;
        }
        if (mirrorBack && MenuTheme.onBack(p.x, p.y, 0, MenuTheme.RULE_Y)) {
            Canvas.dispatchKeyPressed(-7);
        }
    }

    public static void pan(float screenDeltaY) {
        Object menu = GameBridge.currentMenu();
        if (menu != null && GameBridge.currentMenuId() == MENU_SETTINGS) {
            SettingsMenu.scrollBy(-Math.round(screenDeltaY * Graphics.screenToWorldScaleY()));
            return;
        }
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
        if (menuId == MENU_SETTINGS) {
            SettingsMenu.hover(p.x, p.y);
            return;
        }
        if (isCustom(menuId)) {
            int row = hoverRowCustom(p.x, p.y, customCount);
            if (row != -1) {
                selected = row;
            }
            return;
        }
        if (cardButtonY >= 0) {
            if (p.y >= cardButtonY && p.y < cardButtonY + BUTTON_H && p.x >= CARD_X && p.x < W - CARD_X) {
                cardFocus = p.x < W / 2 ? 0 : 1;
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

    static String[] wrapText(Font font, String text, int maxWidth) {
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
            default:
                break;
        }
    }

    static void toggleFullscreen() {
        pendingFullscreen = true;
    }

    static void back() {
        if (GameBridge.currentMenuId() == MENU_SETTINGS) {
            GameBridge.openMenu(0);
        } else if (GameBridge.currentMenuId() == MENU_MAIN && GameBridge.canContinue()) {
            GameBridge.enqueueEvent(52);
        }
    }

    static boolean isDesktop() {
        return Gdx.app != null && Gdx.app.getType() == Application.ApplicationType.Desktop;
    }

    private static void build(int menuId) {
        List<String> ls = new ArrayList<>();
        List<Integer> as = new ArrayList<>();

        if (GameBridge.canContinue()) {
            add(ls, as, textOr(3, "Continue"), ACT_CONTINUE);
        }
        add(ls, as, textOr(2, "New Game"), ACT_NEW_GAME);
        add(ls, as, textOr(4, "Settings"), ACT_SETTINGS);
        add(ls, as, textOr(5, "Help"), ACT_HELP);
        add(ls, as, textOr(6, "About"), ACT_ABOUT);
        add(ls, as, textOr(7, "Exit"), ACT_EXIT);

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

    static String textOr(int id, String fallback) {
        String s = GameBridge.text(id);
        return s != null && !s.isEmpty() ? s : fallback;
    }
}
