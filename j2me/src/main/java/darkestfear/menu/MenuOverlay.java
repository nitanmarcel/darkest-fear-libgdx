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


    private static final int ACT_CONTINUE = 0;
    private static final int ACT_NEW_GAME = 1;
    private static final int ACT_SETTINGS = 2;
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

    private static boolean isQuestion(int menuId) {
        return menuId == 5 || menuId == 7 || menuId == 9 || menuId == 10;
    }

    public static boolean showsGame() {
        int menuId = GameBridge.currentMenuId();
        return (menuId == MENU_SETTINGS || ((menuId == MENU_MAIN || isQuestion(menuId)) && Graphics.wide()))
                && LightBridge.pausedLevel() != null;
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

    private static final int TITLE_GAP = 30;
    private static final int PAUSE_HEAD = 38;
    private static final float PAUSE_DIM = 0.62f;
    private static final int TITLE_LIST_MIN = 180;
    private static final int TITLE_SIDE = 16;
    private static boolean wideDrawn;
    private static float originX;

    public static void renderWide(Graphics g) {
        int wide = Graphics.wideWidth();
        wideDrawn = true;
        boolean settings = GameBridge.currentMenuId() == MENU_SETTINGS;
        if (settings || !showsGame()) {
            g.setClip(0, 0, wide, H);
            g.setColor(isCustom(GameBridge.currentMenuId()) ? 0 : MenuTheme.COLOR_BG);
            g.fillRect(0, 0, wide, H);
        }
        if (!settings && showsGame() && isQuestion(GameBridge.currentMenuId())) {
            g.setClip(0, 0, wide, H);
            Graphics.dim(0, 0, wide, H, PAUSE_DIM);
        }
        int pane = settings ? Math.min(SettingsMenu.PANE_MAX, wide - 2 * TITLE_SIDE - W - SettingsMenu.PANE_GAP) : 0;
        SettingsMenu.pane = pane;
        originX = settings ? (wide - W - SettingsMenu.PANE_GAP - pane) / 2 : (wide - W) / 2;
        Graphics.wideOrigin(originX, 0);
        render(g);
    }

    private static Vector2 point(int screenX, int screenY) {
        if (!wideDrawn) {
            return Graphics.toVirtual(screenX, screenY);
        }
        Vector2 p = Graphics.toWide(screenX, screenY);
        p.x -= originX;
        return p;
    }

    public static void render(Graphics g) {
        Object menu = GameBridge.currentMenu();
        if (menu == null) {
            return;
        }
        wideDrawn = Graphics.wide();
        if (!wideDrawn) {
            SettingsMenu.pane = 0;
        }
        int menuId = GameBridge.currentMenuId();
        if (menuId != lastMenuId) {
            lastMenuId = menuId;
            selected = 0;
            cardFocus = 0;
            SettingsMenu.opened();
        }

        int top = showsGame() && !wideDrawn ? SettingsMenu.PANEL_TOP : 0;
        g.setClip(0, 0, W, H);
        if (!wideDrawn) {
            g.setColor(MenuTheme.COLOR_BG);
            g.fillRect(0, top, W, H - top);
        }

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
        if (mirrorHalf > 0 && (code == Keys.LEFT || code == Keys.RIGHT)) {
            Object menu = GameBridge.currentMenu();
            int target = GameBridge.menuSelected(menu) + (code == Keys.RIGHT ? mirrorHalf : -mirrorHalf);
            if (target >= 0 && target < GameBridge.menuItemCount(menu) && GameBridge.menuItemEnabled(menu, target)) {
                GameBridge.menuSetSelected(menu, target);
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
                selected = (selected - 1 + n) % n;
                return true;
            case Keys.DOWN:
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
        if (wideDrawn && showsGame()) {
            int wide = Graphics.wideWidth();
            int height = items.length * MenuTheme.ROW_H + PAUSE_HEAD + 10;
            int top = (H - height) / 2;
            originX = (wide - W) / 2;
            Graphics.wideOrigin(0, 0);
            g.setClip(0, 0, wide, H);
            Graphics.dim(0, 0, wide, H, PAUSE_DIM);
            Graphics.wideOrigin(originX, 0);
            g.setClip(0, 0, W, H);
            g.setColor(MenuTheme.COLOR_PANEL);
            g.fillRect(0, top, W, height);
            g.setColor(MenuTheme.COLOR_ACCENT);
            g.fillRect(0, top, W, 1);
            g.setFont(MenuTheme.titleFont());
            g.setColor(MenuTheme.COLOR_TITLE);
            g.drawString("Paused", MenuTheme.TEXT_X, top + 10, Graphics.TOP | Graphics.LEFT);
            rowHeight = MenuTheme.ROW_H;
            rowTop = top + PAUSE_HEAD;
        } else if (poster && wideDrawn) {
            int room = Graphics.wideWidth() - 2 * TITLE_SIDE - TITLE_GAP;
            int artW = Math.min(splash[2] * (H * 86 / 100) / splash[3], room - TITLE_LIST_MIN);
            int artH = artW * splash[3] / splash[2];
            int listW = Math.min(W, room - artW);
            int left = TITLE_SIDE + (room - artW - listW) / 2;
            Graphics.wideOrigin(0, 0);
            g.setClip(0, 0, Graphics.wideWidth(), H);
            g.drawRegionScaled((javax.microedition.lcdui.Image) splashSheet, splash[0], splash[1], splash[2], splash[3],
                    left, (H - artH) / 2, artW, artH);
            originX = left + artW + TITLE_GAP;
            Graphics.wideOrigin(originX, 0);
            g.setClip(0, 0, listW, H);
            rowHeight = MenuTheme.ROW_H;
            rowTop = (H - items.length * rowHeight) / 2;
        } else if (poster) {
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
    private static int mirrorHalf;
    private static final int MENU_LEVELS = 1;
    private static final int COLUMN_GAP = 16;

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
            lines[i] = selectable ? GameBridge.menuItemLines(menu, i) : wrapText(plain, join(GameBridge.menuItemLines(menu, i)), W - 2 * MenuTheme.TEXT_X);
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
        int listTop = MenuTheme.LIST_TOP;
        int avail = VIEW_BOTTOM - listTop;
        int half = (count + 1) / 2;
        boolean columns = wideDrawn && menuId == MENU_LEVELS && !textOnly
                && Graphics.wideWidth() >= 2 * W + COLUMN_GAP + 2 * TITLE_SIDE && half * (MenuTheme.ROW_H + 2) <= avail;
        mirrorHalf = columns ? half : 0;
        if (columns) {
            originX = (Graphics.wideWidth() - 2 * W - COLUMN_GAP) / 2;
            Graphics.wideOrigin(originX, 0);
            g.setClip(0, 0, 2 * W + COLUMN_GAP, H);
        }
        MenuTheme.header(g, title == null ? "" : title, mirrorBack, columns ? 2 * W + COLUMN_GAP : W);
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
            tops[i] = columns && i >= half ? tops[i - half] : total;
            if (!columns || i < half) {
                total += heights[i] + 2;
            }
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
            if (columns && i == half) {
                Graphics.wideOrigin(originX + W + COLUMN_GAP, 0);
                g.setClip(0, listTop, W, avail);
            }
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
        if (columns) {
            Graphics.wideOrigin(originX, 0);
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
        Vector2 p = point(screenX, screenY);
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
        Vector2 p = point(screenX, screenY);
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
        int first = 0;
        if (mirrorHalf > 0 && vx >= W + COLUMN_GAP) {
            vx -= W + COLUMN_GAP;
            first = mirrorHalf;
        } else if (mirrorHalf > 0) {
            count = mirrorHalf;
        }
        if (vx < LIST_X || vx > LIST_X + LIST_W
                || vy < mirrorListTop || vy > mirrorListTop + mirrorAvail) {
            return -1;
        }
        int contentY = (int) vy - mirrorListTop + mirrorScrollY;
        for (int i = first; i < count && i < mirrorTops.length; i++) {
            if (contentY >= mirrorTops[i] && contentY < mirrorTops[i] + mirrorHeights[i]) {
                return i;
            }
        }
        return -1;
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
