package darkestfear.menu;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

final class SettingsMenu {

    private interface Getter {
        int get();
    }

    private interface Setter {
        void set(int value);
    }

    private interface Check {
        boolean ok();
    }

    private interface Text {
        String get();
    }

    private static final class Setting {
        final String section;
        final String label;
        final String[] values;
        final Getter getter;
        final Setter setter;
        final String help;
        Check available;
        Text unavailableHelp;
        Text shown;
        Text liveHelp;

        Setting(String section, String label, String[] values, Getter getter, Setter setter, String help) {
            this.section = section;
            this.label = label;
            this.values = values;
            this.getter = getter;
            this.setter = setter;
            this.help = help;
        }

        Setting needs(Check check, String why) {
            return needs(check, () -> why);
        }

        Setting needs(Check check, Text why) {
            available = check;
            unavailableHelp = why;
            return this;
        }

        String help() {
            return !usable() ? unavailableHelp.get() : liveHelp != null ? liveHelp.get() : help;
        }

        boolean usable() {
            return available == null || available.ok();
        }
    }

    private static final String[] OFF_ON = {"Off", "On"};

    private static final int W = MenuTheme.W;
    private static final int H = MenuTheme.H;
    private static final int MARGIN = MenuTheme.MARGIN;
    private static final int TEXT_X = MenuTheme.TEXT_X;
    static final int PANEL_TOP = 150;
    static final int PANE_MAX = 260;
    static final int PANE_GAP = 24;
    static int pane;
    private static final int PANEL_LIST_TOP = PANEL_TOP + 26;
    private static final int PANEL_LIST_BOTTOM = MenuTheme.H - 6;
    private static final int LIST_BOTTOM = 258;
    private static int listTop = MenuTheme.LIST_TOP;
    private static int listBottom = LIST_BOTTOM;
    private static final int HELP_Y = LIST_BOTTOM + 8;
    private static final int HELP_STEP = 15;
    private static final int ROW_H = MenuTheme.ROW_H;
    private static final int SECTION_H = 20;
    private static final int WHEEL_STEP = 24;
    private static final int TEXT_DROP = MenuTheme.TEXT_DROP;

    private static Setting[] settings;
    private static boolean listedGamepad;
    private static boolean removeArmed;
    private static int[] rowY;
    private static int contentHeight;
    private static int selected;
    private static int scrollY;

    private SettingsMenu() {
    }

    private static Setting[] settings() {
        if (settings != null && listedGamepad == GameInput.gamepad) {
            return settings;
        }
        listedGamepad = GameInput.gamepad;
        selected = 0;
        scrollY = 0;
        List<Setting> list = new ArrayList<>();
        list.add(new Setting("Graphics", "Lighting", new String[] {"Original", "Enhanced"},
                () -> Graphics.isLightingEnabled() ? 1 : 0, v -> Graphics.setLightingEnabled(v == 1),
                "Enhanced adds coloured light and soft shadows. Original is the game as it shipped."));
        list.add(new Setting("Graphics", "Upscale", OFF_ON,
                () -> Graphics.isUpscaleEnabled() ? 1 : 0, v -> Graphics.setUpscaleEnabled(v == 1),
                "Rounds the corners of sprites, walls and furniture. Off shows the original pixels."));
        list.add(new Setting("Graphics", "Cues", OFF_ON,
                () -> Graphics.isCuesEnabled() ? 1 : 0, v -> Graphics.setCuesEnabled(v == 1),
                "Marks doors, closets and unlit lamps, and warns you when the dark is about to hurt.")
                .needs(Graphics::isLightingEnabled, "Needs Lighting set to Enhanced."));
        Setting packs = new Setting("Graphics", "Resource packs", null, null, v -> PackMenu.open(),
                "Replace the game's pictures with packs you add. Packs higher in the list win.");
        packs.shown = PackMenu::summary;
        list.add(packs);
        String needsWide = MenuOverlay.isDesktop() ? "Needs a wider window." : "Turn your phone sideways to use this.";
        list.add(new Setting("Display", "View", new String[] {"Original", "Expanded"},
                () -> Graphics.isExpandedView() ? 1 : 0, v -> Graphics.setExpandedView(v == 1),
                "Expanded shows more of the level on wide screens. Original keeps the game's own view.")
                .needs(Graphics::wide, needsWide));
        list.add(new Setting("Display", "Zoom", new String[] {"Far", "Near", "Closer"},
                Graphics::zoom, Graphics::setZoom,
                "Near and Closer make the level bigger. Far shows more of it at once.")
                .needs(() -> Graphics.wide() && Graphics.isExpandedView(),
                        () -> Graphics.wide() ? "Needs View set to Expanded." : needsWide));
        if (MenuOverlay.isDesktop()) {
            list.add(new Setting("Display", "Fullscreen", OFF_ON,
                    () -> Gdx.graphics.isFullscreen() ? 1 : 0, v -> MenuOverlay.toggleFullscreen(),
                    "Switch between a window and the whole screen."));
        }
        if (MenuOverlay.isDesktop()) {
            String[] names = {"Move up", "Move down", "Move left", "Move right", "Interact", "Pause / back"};
            for (int i = 0; i < names.length; i++) {
                int action = i;
                Setting key = new Setting("Controls", names[i], null, null, v -> GameInput.capture(action),
                        "Choose this and press the new key. Arrows, Enter, Space and Esc always work as well.");
                key.shown = () -> GameInput.capturing(action) ? "Press a key" : GameInput.keyName(action);
                list.add(key);
            }
            list.add(new Setting("Controls", "Reset keys", null, null, v -> GameInput.resetKeys(),
                    "Puts every key back to its default."));
        }
        if (GameInput.gamepad) {
            String[][] pad = {{"Move", "Stick / D-pad"}, {"Interact", "A"}, {"Pause / back", "B / Start"}};
            for (String[] row : pad) {
                Setting button = new Setting("Gamepad", row[0], null, null, v -> { },
                        "Gamepad buttons are fixed for now.");
                button.shown = () -> row[1];
                list.add(button);
            }
        }
        list.add(new Setting("Audio", "Sound", OFF_ON,
                () -> GameBridge.soundEnabled() ? 1 : 0, v -> GameBridge.setSoundEnabled(v == 1),
                "Music and sound effects."));
        list.add(new Setting("Game", MenuOverlay.textOr(18, "Reset Game"), null, null, v -> GameBridge.openMenu(9),
                "Deletes your saved progress."));
        Setting remove = new Setting("Game", "Remove game file", null, null, v -> {
            if (removeArmed) {
                GameJar.remove();
            }
            removeArmed = true;
        }, "Removes the original game file and closes the app. Your progress is kept.");
        remove.shown = () -> removeArmed ? "Press again" : "";
        list.add(remove);
        settings = list.toArray(new Setting[0]);

        rowY = new int[settings.length];
        int y = 0;
        for (int i = 0; i < settings.length; i++) {
            if (i == 0 || !settings[i].section.equals(settings[i - 1].section)) {
                y += SECTION_H;
            }
            rowY[i] = y;
            y += ROW_H;
        }
        contentHeight = y;
        return settings;
    }

    static void opened() {
        GameInput.stopCapture();
        PackMenu.close();
        removeArmed = false;
        selected = 0;
        scrollY = 0;
    }

    static void render(Graphics g, int panelTop) {
        Setting[] all = settings();
        boolean panel = panelTop > 0;
        listTop = panel ? PANEL_LIST_TOP : MenuTheme.LIST_TOP;
        listBottom = panel || pane > 0 ? PANEL_LIST_BOTTOM : LIST_BOTTOM;
        if (PackMenu.isOpen()) {
            PackMenu.render(g, panelTop, listTop, listBottom, pane);
            return;
        }
        Font bold = MenuTheme.bold();
        Font plain = MenuTheme.plain();
        int top = Graphics.TOP;

        if (panel) {
            MenuTheme.panelHeader(g, MenuOverlay.textOr(4, "Settings"), panelTop);
        } else {
            g.setClip(0, 0, W + PANE_GAP + pane, H);
            MenuTheme.header(g, MenuOverlay.textOr(4, "Settings"), true, W + (pane > 0 ? PANE_GAP + pane : 0));
        }

        int view = listBottom - listTop;
        scrollY = Math.max(0, Math.min(scrollY, contentHeight - view));
        int textPad = (ROW_H - bold.getHeight()) / 2 + TEXT_DROP;
        g.setClip(0, listTop, W, view);
        for (int i = 0; i < all.length; i++) {
            Setting s = all[i];
            int y = listTop + rowY[i] - scrollY;
            if (i == 0 || !s.section.equals(all[i - 1].section)) {
                g.setFont(plain);
                g.setColor(MenuTheme.COLOR_SECTION);
                g.drawString(s.section.toUpperCase(), TEXT_X, y - plain.getHeight() + TEXT_DROP, top | Graphics.LEFT);
            }
            boolean on = i == selected, usable = s.usable();
            if (on) {
                MenuTheme.selectionBar(g, y, ROW_H);
            }
            g.setFont(bold);
            g.setColor(!usable ? MenuTheme.COLOR_DISABLED : on ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_ITEM);
            g.drawString(s.label, TEXT_X + 2, y + textPad, top | Graphics.LEFT);
            int right = W - TEXT_X;
            boolean arrows = on && usable && s.values != null;
            if (arrows) {
                MenuTheme.chevron(g, right - 4, y + ROW_H / 2, false);
                right -= 10;
            }
            if (s.values != null || s.shown != null) {
                String value = s.values != null ? s.values[s.getter.get()] : s.shown.get();
                g.setFont(on ? bold : plain);
                g.setColor(!usable ? MenuTheme.COLOR_DISABLED : on ? MenuTheme.COLOR_SELECTED : MenuTheme.COLOR_VALUE);
                g.drawString(value, right, y + textPad, top | Graphics.RIGHT);
                if (arrows) {
                    MenuTheme.chevron(g, right - bold.stringWidth(value) - 10, y + ROW_H / 2, true);
                }
            }
        }
        g.setClip(0, 0, W, H);

        MenuTheme.scrollBar(g, listTop, view, scrollY, contentHeight);

        Setting current = all[selected];
        if (pane > 0) {
            int x = W + PANE_GAP;
            g.setClip(0, 0, x + pane, H);
            g.setColor(MenuTheme.COLOR_RULE);
            g.fillRect(W + PANE_GAP / 2, MenuTheme.LIST_TOP, 1, H - MenuTheme.LIST_TOP - 8);
            g.setFont(MenuTheme.titleFont());
            g.setColor(MenuTheme.COLOR_TITLE);
            g.drawString(current.label, x, MenuTheme.LIST_TOP + 2, top | Graphics.LEFT);
            g.setFont(plain);
            g.setColor(MenuTheme.COLOR_TEXT);
            int y = MenuTheme.LIST_TOP + 30;
            for (String line : MenuOverlay.wrapText(plain, current.help(), pane)) {
                g.drawString(line, x, y, top | Graphics.LEFT);
                y += HELP_STEP;
            }
            int play = LightBridge.playHeight();
            if (MenuOverlay.showsGame() && play > 0) {
                y += 10;
                int sceneW = Graphics.VIRTUAL_WIDTH;
                int pw = Math.min(pane, sceneW), ph = Math.min(H - y - 10, play);
                if (ph > 40) {
                    int[] thomas = LightBridge.actor(0);
                    int cx = thomas == null ? sceneW / 2 : thomas[0] + 8 - Graphics.sceneScrollX();
                    int cy = thomas == null ? play / 2 : thomas[1] + 8 - Graphics.sceneScrollY();
                    Graphics.drawGame(Math.max(0, Math.min(sceneW - pw, cx - pw / 2)),
                            Math.max(0, Math.min(play - ph, cy - ph / 2)), pw, ph, x, y);
                }
            }
        } else if (!panel) {
            g.setColor(MenuTheme.COLOR_RULE);
            g.fillRect(MARGIN, listBottom + 3, W - 2 * MARGIN, 1);
            g.setFont(plain);
            g.setColor(MenuTheme.COLOR_TEXT);
            String[] help = MenuOverlay.wrapText(plain, current.help(), W - 2 * TEXT_X);
            for (int k = 0; k < help.length; k++) {
                g.drawString(help[k], TEXT_X, HELP_Y + k * HELP_STEP, top | Graphics.LEFT);
            }
        }
    }

    static boolean keyPressed(int code) {
        if (PackMenu.isOpen()) {
            PackMenu.keyPressed(code);
            return true;
        }
        Setting[] all = settings();
        switch (code) {
            case Keys.UP:
                select((selected - 1 + all.length) % all.length);
                break;
            case Keys.DOWN:
                select((selected + 1) % all.length);
                break;
            case Keys.LEFT:
                change(-1);
                break;
            case Keys.RIGHT:
            case -5:
            case -6:
            case Keys.ENTER:
            case Keys.SPACE:
                change(1);
                break;
            case -7:
            case Keys.ESCAPE:
                MenuOverlay.back();
                break;
            default:
                break;
        }
        return true;
    }

    private static void select(int index) {
        GameInput.stopCapture();
        selected = index;
        int view = listBottom - listTop;
        int topEdge = rowY[index] - (index == 0 || !settings[index].section.equals(settings[index - 1].section) ? SECTION_H : 0);
        if (topEdge < scrollY) {
            scrollY = topEdge;
        } else if (rowY[index] + ROW_H > scrollY + view) {
            scrollY = rowY[index] + ROW_H - view;
        }
    }

    private static void change(int step) {
        Setting s = settings()[selected];
        if (!s.usable()) {
            return;
        }
        if (s.values == null) {
            s.setter.set(0);
            return;
        }
        s.setter.set((s.getter.get() + step + s.values.length) % s.values.length);
    }

    private static int rowAt(float vx, float vy) {
        if (vx < MARGIN || vx > W - MARGIN || vy < listTop || vy >= listBottom) {
            return -1;
        }
        int y = (int) vy - listTop + scrollY;
        int[] tops = rowY;
        for (int i = 0; i < tops.length; i++) {
            if (y >= tops[i] && y < tops[i] + ROW_H) {
                return i;
            }
        }
        return -1;
    }

    static void tap(float vx, float vy) {
        settings();
        boolean panel = listTop != MenuTheme.LIST_TOP;
        if (PackMenu.isOpen()) {
            PackMenu.tap(vx, vy, MenuTheme.onBack(vx, vy, panel ? PANEL_TOP : 0, panel ? PANEL_LIST_TOP : MenuTheme.RULE_Y));
            return;
        }
        if (MenuTheme.onBack(vx, vy, panel ? PANEL_TOP : 0, panel ? PANEL_LIST_TOP : MenuTheme.RULE_Y)) {
            MenuOverlay.back();
            return;
        }
        int row = rowAt(vx, vy);
        if (row == -1) {
            return;
        }
        selected = row;
        Setting s = settings[row];
        boolean previous = s.values != null && s.values.length > 2 && vx >= W / 2 && vx < W * 3 / 4;
        change(previous ? -1 : 1);
    }

    static void hover(float vx, float vy) {
        settings();
        if (PackMenu.isOpen()) {
            PackMenu.hover(vx, vy);
            return;
        }
        int row = rowAt(vx, vy);
        if (row != -1 && row != selected) {
            GameInput.stopCapture();
            selected = row;
        }
    }

    static void scroll(int amountY) {
        scrollBy(amountY * WHEEL_STEP);
    }

    static void scrollBy(int virtualPixels) {
        settings();
        if (PackMenu.isOpen()) {
            PackMenu.scrollBy(virtualPixels);
            return;
        }
        scrollY = Math.max(0, Math.min(scrollY + virtualPixels, Math.max(0, contentHeight - (listBottom - listTop))));
    }
}
