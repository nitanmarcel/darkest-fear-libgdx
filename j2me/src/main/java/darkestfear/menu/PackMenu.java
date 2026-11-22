package darkestfear.menu;

import com.badlogic.gdx.Input.Keys;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.ResourcePack;

final class PackMenu {

    private static final int W = MenuTheme.W;
    private static final int H = MenuTheme.H;
    private static final int TEXT_X = MenuTheme.TEXT_X;
    private static final int ROW_H = MenuTheme.ROW_H;
    private static final int TEXT_DROP = MenuTheme.TEXT_DROP;
    private static final int HELP_STEP = 15;
    private static final int ARROW_W = 20;
    private static final int SCROLL_PAUSE = 1000, SCROLL_SPEED = 30;
    private static final String ADD = "Add a pack", REMOVE = "Remove";

    private static boolean open;
    private static int selected, focused = -1, scrollY;
    private static boolean removeArmed;
    private static int listTop, listBottom;
    private static long selectedAt;

    private PackMenu() {
    }

    static boolean isOpen() {
        return open;
    }

    static void open() {
        open = true;
        selected = 0;
        focused = -1;
        scrollY = 0;
        removeArmed = false;
        selectedAt = System.currentTimeMillis();
    }

    static void close() {
        open = false;
    }

    static String summary() {
        int on = 0;
        for (int i = 0; i < ResourcePack.count(); i++) {
            if (ResourcePack.enabled(i) && ResourcePack.problem(i) == null) {
                on++;
            }
        }
        return on == 0 ? "None" : on + " on";
    }

    private static int rows() {
        return ResourcePack.count() + 2;
    }

    private static String state(int pack) {
        return ResourcePack.problem(pack) != null ? "Not usable" : ResourcePack.enabled(pack) ? "On" : "Off";
    }

    private static String help() {
        int packs = ResourcePack.count();
        if (selected < packs) {
            String problem = ResourcePack.problem(selected);
            return problem != null ? problem
                    : "Choose it to turn it on or off. The arrows move it; higher packs win.";
        }
        if (selected == packs) {
            return "Adds a resource pack file to the list.";
        }
        return focused < 0 || focused >= packs ? "Choose a pack in the list first."
                : "Deletes " + ResourcePack.name(focused) + " from this device.";
    }

    static void render(Graphics g, int panelTop, int top, int bottom, int pane) {
        listTop = top;
        listBottom = bottom;
        int packs = ResourcePack.count(), count = rows();
        selected = Math.min(selected, count - 1);
        Font bold = MenuTheme.bold(), plain = MenuTheme.plain();
        int anchor = Graphics.TOP;
        if (panelTop > 0) {
            MenuTheme.panelHeader(g, "Resource packs", panelTop);
        } else {
            g.setClip(0, 0, W + SettingsMenu.PANE_GAP + pane, H);
            MenuTheme.header(g, "Resource packs", true, W + (pane > 0 ? SettingsMenu.PANE_GAP + pane : 0));
        }
        int view = listBottom - listTop, content = count * ROW_H;
        scrollY = Math.max(0, Math.min(scrollY, content - view));
        int textPad = (ROW_H - bold.getHeight()) / 2 + TEXT_DROP;
        g.setClip(0, listTop, W, view);
        for (int i = 0; i < count; i++) {
            int y = listTop + i * ROW_H - scrollY;
            boolean on = i == selected, pack = i < packs;
            boolean usable = pack ? ResourcePack.problem(i) == null : i == packs || (focused >= 0 && focused < packs);
            if (on) {
                MenuTheme.selectionBar(g, y, ROW_H);
            }
            int right = W - TEXT_X;
            if (pack && on) {
                arrow(g, right - 6, y + ROW_H / 2, false, i < packs - 1);
                arrow(g, right - 6 - ARROW_W, y + ROW_H / 2, true, i > 0);
                right -= 2 * ARROW_W;
            }
            String value = pack ? state(i) : i > packs && removeArmed ? "Press again" : "";
            g.setFont(on ? bold : plain);
            g.setColor(!usable ? MenuTheme.COLOR_DISABLED : on ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_VALUE);
            g.drawString(value, right, y + textPad, anchor | Graphics.RIGHT);
            int room = right - bold.stringWidth(value) - 8 - TEXT_X - 2;
            g.setFont(bold);
            g.setColor(!usable ? MenuTheme.COLOR_DISABLED : on ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_ITEM);
            String name = pack ? ResourcePack.name(i) : i == packs ? ADD : REMOVE;
            int over = bold.stringWidth(name) - room;
            if (on && over > 0) {
                int rowTop = Math.max(y, listTop), rowBottom = Math.min(y + ROW_H, listBottom);
                long time = (System.currentTimeMillis() - selectedAt) % (2 * SCROLL_PAUSE + over * 1000L / SCROLL_SPEED);
                int shift = (int) Math.max(0, Math.min(over, (time - SCROLL_PAUSE) * SCROLL_SPEED / 1000));
                g.setClip(TEXT_X + 2, rowTop, room, Math.max(0, rowBottom - rowTop));
                g.drawString(name, TEXT_X + 2 - shift, y + textPad, anchor | Graphics.LEFT);
                g.setClip(0, listTop, W, view);
            } else {
                g.drawString(fit(bold, name, room), TEXT_X + 2, y + textPad, anchor | Graphics.LEFT);
            }
        }
        g.setClip(0, 0, W, H);
        MenuTheme.scrollBar(g, listTop, view, scrollY, content);

        String title = selected < packs ? ResourcePack.name(selected) : selected == packs ? ADD : REMOVE;
        if (pane > 0) {
            int x = W + SettingsMenu.PANE_GAP;
            g.setClip(0, 0, x + pane, H);
            g.setColor(MenuTheme.COLOR_RULE);
            g.fillRect(W + SettingsMenu.PANE_GAP / 2, MenuTheme.LIST_TOP, 1, H - MenuTheme.LIST_TOP - 8);
            g.setFont(MenuTheme.titleFont());
            g.setColor(MenuTheme.COLOR_TITLE);
            int y = MenuTheme.LIST_TOP + 2;
            for (String line : MenuOverlay.wrapText(MenuTheme.titleFont(), title, pane)) {
                g.drawString(line, x, y, anchor | Graphics.LEFT);
                y += MenuTheme.titleFont().getHeight();
            }
            g.setFont(plain);
            g.setColor(MenuTheme.COLOR_TEXT);
            y += 6;
            for (String line : MenuOverlay.wrapText(plain, help(), pane)) {
                g.drawString(line, x, y, anchor | Graphics.LEFT);
                y += HELP_STEP;
            }
        } else if (panelTop == 0) {
            g.setColor(MenuTheme.COLOR_RULE);
            g.fillRect(MenuTheme.MARGIN, listBottom + 3, W - 2 * MenuTheme.MARGIN, 1);
            g.setFont(plain);
            g.setColor(MenuTheme.COLOR_TEXT);
            String[] help = MenuOverlay.wrapText(plain, (selected < packs ? title + "\n" : "") + help(), W - 2 * TEXT_X);
            for (int k = 0; k < help.length; k++) {
                g.drawString(help[k], TEXT_X, listBottom + 8 + k * HELP_STEP, anchor | Graphics.LEFT);
            }
        }
    }

    private static String fit(Font font, String text, int room) {
        if (font.stringWidth(text) <= room) {
            return text;
        }
        int keep = text.length();
        while (keep > 1 && font.stringWidth(text.substring(0, keep).trim() + "...") > room) {
            keep--;
        }
        return text.substring(0, keep).trim() + "...";
    }

    private static void arrow(Graphics g, int cx, int cy, boolean up, boolean usable) {
        g.setColor(usable ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_DISABLED);
        for (int r = 0; r < 5; r++) {
            g.fillRect(cx - r, up ? cy - 3 + r : cy + 2 - r, 2 * r + 1, 1);
        }
    }

    static void keyPressed(int code) {
        int packs = ResourcePack.count(), count = rows();
        switch (code) {
            case Keys.UP:
                select((selected - 1 + count) % count);
                break;
            case Keys.DOWN:
                select((selected + 1) % count);
                break;
            case Keys.LEFT:
                move(-1);
                break;
            case Keys.RIGHT:
                move(1);
                break;
            case -5:
            case -6:
            case Keys.ENTER:
            case Keys.SPACE:
                activate();
                break;
            case -7:
            case Keys.ESCAPE:
                close();
                break;
            default:
                break;
        }
    }

    private static void select(int row) {
        removeArmed &= row == selected;
        if (row != selected) {
            selectedAt = System.currentTimeMillis();
        }
        selected = row;
        if (row < ResourcePack.count()) {
            focused = row;
        }
        int view = listBottom - listTop;
        if (row * ROW_H < scrollY) {
            scrollY = row * ROW_H;
        } else if (row * ROW_H + ROW_H > scrollY + view) {
            scrollY = row * ROW_H + ROW_H - view;
        }
    }

    private static void move(int by) {
        int packs = ResourcePack.count();
        if (selected < packs && selected + by >= 0 && selected + by < packs) {
            ResourcePack.move(selected, by);
            select(selected + by);
        }
    }

    private static void activate() {
        int packs = ResourcePack.count();
        if (selected < packs) {
            if (ResourcePack.problem(selected) == null) {
                ResourcePack.toggle(selected);
            }
        } else if (selected == packs) {
            GameJar.packChooser.run();
        } else if (focused >= 0 && focused < packs) {
            if (removeArmed) {
                ResourcePack.remove(focused);
                focused = -1;
            }
            removeArmed = !removeArmed;
        }
    }

    private static int rowAt(float vx, float vy) {
        if (vx < MenuTheme.MARGIN || vx > W - MenuTheme.MARGIN || vy < listTop || vy >= listBottom) {
            return -1;
        }
        int row = ((int) vy - listTop + scrollY) / ROW_H;
        return row < rows() ? row : -1;
    }

    static void tap(float vx, float vy, boolean back) {
        if (back) {
            close();
            return;
        }
        int row = rowAt(vx, vy);
        if (row == -1) {
            return;
        }
        boolean was = row == selected, pack = row < ResourcePack.count();
        select(row);
        int right = W - TEXT_X;
        if (was && pack && vx >= right - 2 * ARROW_W) {
            move(vx >= right - ARROW_W ? 1 : -1);
        } else if (was || !pack) {
            activate();
        }
    }

    static void hover(float vx, float vy) {
        int row = rowAt(vx, vy);
        if (row != -1) {
            removeArmed &= row == selected;
            if (row != selected) {
                selectedAt = System.currentTimeMillis();
            }
            selected = row;
        }
    }

    static void scrollBy(int virtualPixels) {
        scrollY = Math.max(0, Math.min(scrollY + virtualPixels, Math.max(0, rows() * ROW_H - (listBottom - listTop))));
    }
}
