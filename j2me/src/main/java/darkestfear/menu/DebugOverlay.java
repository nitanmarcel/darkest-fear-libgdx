package darkestfear.menu;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public final class DebugOverlay {

    private static final int TILE = 16;

    private static final int COLOR_WALL = 0x404060;
    private static final int COLOR_OVERHANG = 0x805020;

    private static final int COLOR_LIGHT_ON = 0xFFCC33;
    private static final int COLOR_LIGHT_OFF = 0x7A6020;
    private static final int COLOR_ITEM = 0x33CCFF;
    private static final int COLOR_INTER_GOAL = 0x44FF66;
    private static final int COLOR_INTER_DIALOG = 0x4499FF;
    private static final int COLOR_INTER_SCRIPT = 0xFF9933;
    private static final int COLOR_PLATE = 0xCC66FF;
    private static final int COLOR_TRIGGER = 0xFF5555;
    private static final int COLOR_WORLD = 0xDDDDCC;
    private static final int COLOR_PLAYER = 0x66FF66;
    private static final int COLOR_STALKER = 0xFF3333;
    private static final int COLOR_CREATURE = 0xCC66FF;
    private static final int COLOR_BOSS = 0xFF3333;

    public static final String[] FAMILIES = {
            "Grid", "Lights", "Decor", "NPCs", "Triggers", "Cutscenes", "Actors", "World"
    };
    private static final int GRID = 0, LIGHTS = 1, ITEMS = 2, INTER = 3, PLATES = 4, TRIGGERS = 5,
            ACTORS = 6, WORLD = 7;
    private static final boolean[] familyOn = new boolean[FAMILIES.length];

    private static Font font;

    private static final String[] LIGHT_NAMES = {
            "Lantern", "Flashlight", "Mirror", "Ambient Light", "Window", "Fireplace"
    };
    private static final String[] ITEM_NAMES = {
            "Blood Pool", "Blood Streak", "Corpse (bloody)", "Corpse", "Corpses (pile)"
    };
    private static final String[] ACTOR_NAMES = {"Player (Thomas)", "Monster", "The First One", "Boss"};
    private static final String[] NPC_NAMES = {
            null, "Patient 6", null, "Patient U7", "Susan's Assistant", null, null, "Doctor", "Patient 821",
            "Doctor", "Nina", "Nina", "Girl", "Patient 43", "Patient Z9", "Patient 21", "Patient X2", "Patient 09"
    };
    private static final String[] STORY_NAMES = {
            null, "Girl", "Nina", "Nina's Sister", "Susan", "Helen", "Patient 52", "Nina (lobby watch)",
            "Janitor", "Janitor's Son"
    };
    private static final int[] ACTOR_COLORS = {COLOR_PLAYER, COLOR_STALKER, COLOR_CREATURE, COLOR_BOSS};

    private DebugOverlay() {
    }

    public static boolean family(int i) {
        return i >= 0 && i < familyOn.length && familyOn[i];
    }

    public static void setFamily(int i, boolean on) {
        if (i >= 0 && i < familyOn.length) {
            familyOn[i] = on;
        }
    }

    public static void setAllFamilies(boolean on) {
        for (int i = 0; i < familyOn.length; i++) {
            familyOn[i] = on;
        }
    }

    public static boolean gizmosActive() {
        for (boolean on : familyOn) {
            if (on) {
                return true;
            }
        }
        return false;
    }

    public static void render(Graphics g) {
        if (!gizmosActive()) {
            return;
        }
        LightBridge.LightState s = LightBridge.read();
        if (s == null || s.grid == null || s.grid.length == 0) {
            return;
        }
        int ox = Graphics.VIRTUAL_WIDTH / 2 - s.camX;
        int oy = (s.playH > 0 ? s.playH : Graphics.VIRTUAL_HEIGHT) / 2 - s.camY;
        if (font == null) {
            font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        }
        g.setFont(font);
        labelCount = 0;
        boolean[] wall = familyOn[GRID] ? LightBridge.computeWallCover(s.grid, s.rows, s.cols) : null;
        if (familyOn[GRID]) {
            drawGridShapes(g, s.grid, s.rows, s.cols, ox, oy, wall);
        }
        drawEntities(g, s, ox, oy);
        if (familyOn[GRID]) {
            drawGridText(g, s.grid, s.rows, s.cols, ox, oy, wall);
        }
        flushLabels(g);
    }

    private static void drawEntities(Graphics g, LightBridge.LightState s, int ox, int oy) {
        int carried = LightBridge.carriedLight();
        if (familyOn[LIGHTS]) {
            drawLights(g, s, ox, oy, carried);
        }
        if (familyOn[ITEMS]) {
            drawItems(g, s, ox, oy);
        }
        if (familyOn[INTER]) {
            drawInteractables(g, s, ox, oy);
        }
        if (familyOn[PLATES]) {
            drawPlates(g, s, ox, oy);
        }
        if (familyOn[TRIGGERS]) {
            drawTriggers(g, s, ox, oy);
        }
        if (familyOn[WORLD]) {
            drawWorld(g, s, ox, oy);
        }
        if (familyOn[ACTORS]) {
            drawActors(g, ox, oy, carried);
        }
    }

    private static void drawGridShapes(Graphics g, byte[][] grid, int rows, int cols, int ox, int oy, boolean[] wall) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = c * TILE + ox - TILE / 2;
                int sy = r * TILE + oy - TILE / 2;
                if (offscreen(sx, sy)) {
                    continue;
                }
                g.setColor(0x224422);
                g.drawRect(sx, sy, TILE, TILE);
                if (grid[r][c] >= 0 && wall[r * cols + c]) {
                    g.setColor(COLOR_OVERHANG);
                    g.drawRect(sx + 2, sy + 2, TILE - 4, TILE - 4);
                }
            }
        }
    }

    private static void drawGridText(Graphics g, byte[][] grid, int rows, int cols, int ox, int oy, boolean[] wall) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sx = c * TILE + ox - TILE / 2;
                int sy = r * TILE + oy - TILE / 2;
                if (offscreen(sx, sy)) {
                    continue;
                }
                byte v = grid[r][c];
                if (v < 0) {
                    g.setColor(COLOR_WALL);
                    g.drawString("#", sx + 5, sy + 2, Graphics.TOP | Graphics.LEFT);
                } else if (v > 0 && !wall[r * cols + c]) {
                    g.setColor(gridColor(v));
                    g.drawString(Integer.toString(v), sx + 5, sy + 2, Graphics.TOP | Graphics.LEFT);
                }
            }
        }
    }

    private static void drawLights(Graphics g, LightBridge.LightState s, int ox, int oy, int carried) {
        byte[][] t = s.entities;
        if (t == null || t.length < 6 || t[0] == null) {
            return;
        }
        for (int i = 0; i < s.entCount && i < t[0].length; i++) {
            int row = t[0][i], col = t[1][i], radius = t[2][i], mode = t[3][i], on = t[4][i], type = t[5][i];
            if (!inGrid(row, col, s)) {
                continue;
            }
            String base = type >= 0 && type < LIGHT_NAMES.length ? LIGHT_NAMES[type] : "Light " + type;
            String state = on != 0 ? " [ON]" : " [OFF]";
            if (type == 2) {
                state = on != 0 ? " [REFLECTING]" : " [idle]";
            } else if (type == 4) {
                state = on != 0 ? " [OPEN]" : " [closed]";
            } else if (type == 5 && mode != 1 && mode != 3) {
                base += " ?!";
            }
            String label = base + dirGlyph(mode) + state + " r=" + radius;
            if (i == carried) {
                label += " (carried)";
            }
            marker(g, col * TILE + ox, row * TILE + oy, on != 0 ? COLOR_LIGHT_ON : COLOR_LIGHT_OFF, label, PRIO_LIGHT);
        }
    }

    private static void drawItems(Graphics g, LightBridge.LightState s, int ox, int oy) {
        byte[][] t = LightBridge.itemTable();
        if (t == null || t.length < 3 || t[0] == null) {
            return;
        }
        for (int i = 0; i < t[0].length; i++) {
            int row = t[0][i], col = t[1][i], variant = t[2][i];
            if (!inGrid(row, col, s)) {
                continue;
            }
            String base = variant >= 0 && variant < ITEM_NAMES.length ? ITEM_NAMES[variant] : "Decor " + variant;
            String label = s.grid[row][col] > 0 ? base : base + " hidden";
            marker(g, col * TILE + ox, row * TILE + oy, COLOR_ITEM, label, PRIO_ITEM);
        }
    }

    private static void drawInteractables(Graphics g, LightBridge.LightState s, int ox, int oy) {
        byte[][] t = s.interactables;
        if (t == null || t.length < 4 || t[0] == null) {
            return;
        }
        for (int i = 0; i < t[0].length; i++) {
            int row = t[0][i], col = t[1][i], state = t[2][i], variant = t[3][i];
            if (!inGrid(row, col, s)) {
                continue;
            }
            int color;
            String base;
            int tile = s.grid[row][col];
            boolean lockedIn = tile == -19 || tile == -38;
            if (variant > 17) {
                color = COLOR_INTER_GOAL;
                base = tile == -19 ? "Patient (in closet)" : tile == -38 ? "Child (in cage)"
                        : variant == 19 ? "Child" : "Patient";
            } else if (variant < 0) {
                color = COLOR_INTER_SCRIPT;
                String n = -variant < STORY_NAMES.length ? STORY_NAMES[-variant] : null;
                base = n != null ? n : "Story NPC #" + (-variant);
            } else {
                color = COLOR_INTER_DIALOG;
                String n = variant < NPC_NAMES.length ? NPC_NAMES[variant] : null;
                base = n != null ? n : "NPC (unused " + variant + ")";
            }
            String label = base + (state >= 1 ? " [talked]" : " [new]");
            if (variant > 17) {
                label += lockedIn ? " locked in" : tile > 1 ? " SAVED" : " in dark";
            }
            marker(g, col * TILE + ox, row * TILE + oy, color, label, PRIO_INTER);
        }
    }

    private static void drawPlates(Graphics g, LightBridge.LightState s, int ox, int oy) {
        byte[][] t = LightBridge.plateTable();
        if (t == null || t.length < 5 || t[0] == null) {
            return;
        }
        for (int i = 0; i < t[0].length; i++) {
            int pr = t[0][i], pc = t[1][i], gr = t[2][i], gc = t[3][i], open = t[4][i];
            if (!inGrid(pr, pc, s)) {
                continue;
            }
            int pcx = pc * TILE + ox, pcy = pr * TILE + oy;
            g.setColor(COLOR_PLATE);
            if (inGrid(gr, gc, s)) {
                int gcx = gc * TILE + ox, gcy = gr * TILE + oy;
                g.drawLine(pcx, pcy, gcx, gcy);
                g.drawRect(gcx - 6, gcy - 6, 11, 11);
                int gt = s.grid[gr][gc];
                String gl = gt == -16 ? "Trigger Door (front)" : gt == -17 ? "Trigger Door (side)"
                        : "Trigger Door (open)";
                pushLabel(gcx - 6, gcy - 16, COLOR_PLATE, gl, PRIO_INTER);
            }
            marker(g, pcx, pcy, COLOR_PLATE, open != 0 ? "Trigger [pressed]" : "Trigger [idle]", PRIO_INTER);
        }
    }

    private static void drawTriggers(Graphics g, LightBridge.LightState s, int ox, int oy) {
        byte[][] t = LightBridge.triggerTable();
        if (t == null || t.length < 4 || t[0] == null) {
            return;
        }
        for (int i = 0; i < t[0].length; i++) {
            int row = t[0][i], col = t[1][i], fired = t[2][i], eventId = t[3][i];
            if (!inGrid(row, col, s)) {
                continue;
            }
            String name = eventId == 2 ? "Cutscene: phone call from Helen"
                    : eventId == 3 ? "Cutscene: Thomas is bitten"
                    : eventId == 9 ? "Cutscene: Susan and the needle" : "Cutscene Trigger #" + eventId;
            marker(g, col * TILE + ox, row * TILE + oy, COLOR_TRIGGER,
                    name + (fired != 0 ? " [fired]" : " [armed]"), PRIO_INTER);
        }
    }

    private static void drawWorld(Graphics g, LightBridge.LightState s, int ox, int oy) {
        byte[][] grid = s.grid;
        for (int r = 0; r < s.rows; r++) {
            for (int c = 0; c < s.cols; c++) {
                int code = grid[r][c];
                if (code >= 0) {
                    continue;
                }
                String label = worldLabel(code);
                if (label == null) {
                    continue;
                }
                int cx = c * TILE + ox, cy = r * TILE + oy;
                if (offscreen(cx, cy)) {
                    continue;
                }
                marker(g, cx, cy, COLOR_WORLD, label, PRIO_WORLD);
            }
        }
    }

    private static String worldLabel(int code) {
        switch (code) {
            case -7: return "Box";
            case -8: return "Closet (empty)";
            case -9: return "Closet (medikit)";
            case -10: return "Closet (key)";
            case -11: return "Closet (opened)";
            case -12: return "Door (front)";
            case -13: return "Door (side)";
            case -14: return "Locked Door (front)";
            case -15: return "Locked Door (side)";
            case -16: return "Trigger Door (front)";
            case -17: return "Trigger Door (side)";
            case -19: return "Closet (patient inside)";
            case -27: return "Bed (head)";
            case -28: return "Bed (foot)";
            case -29: return "Table";
            case -30: return "Table (papers)";
            case -31: return "Table (lab glassware)";
            case -32: return "Phone";
            case -33: case -34: return "Chair";
            case -35: return "Machine";
            case -36: return "Cage (empty)";
            case -37: return "Cage (opened)";
            case -38: return "Cage (child inside)";
            case -39: return "IV Stand";
            case -100: return "Exit Door (front)";
            case -101: return "Exit Door (side)";
            case -102: return "Exit Stairs";
            case -103: return "Window (closed, front)";
            case -104: return "Window (closed, side)";
            case -105: return "Window (open, front)";
            case -106: return "Window (open, side)";
            default:
                return code >= -26 && code <= -20 ? "Book (dialog " + (-code - 2) + ")" : null;
        }
    }

    private static void drawActors(Graphics g, int ox, int oy, int carried) {
        for (int which = 0; which < 4; which++) {
            int[] a = LightBridge.actor(which);
            if (a == null) {
                continue;
            }
            int cx = a[0] + ox, cy = a[1] + oy;
            if (offscreen(cx, cy)) {
                continue;
            }
            int color = ACTOR_COLORS[which];
            g.setColor(color);
            g.drawRect(cx - 7, cy - 13, 14, 16);
            int facing = a[2];
            int tx = cx, ty = cy;
            switch (facing) {
                case 1: tx = cx + 8; break;
                case 2: ty = cy + 8; break;
                case 3: tx = cx - 8; break;
                case 4: ty = cy - 8; break;
                default: break;
            }
            g.drawLine(cx, cy, tx, ty);
            String name = ACTOR_NAMES[which];
            if (which == 3) {
                int lvl = LightBridge.levelIndex();
                name = lvl == 12 ? "Spine Girl" : lvl == 14 ? "Helen (creature)" : name;
            }
            String label = name + dirGlyph(facing);
            if (which == 0) {
                label += carried >= 0 ? " carrying " + carriedName(carried) : " empty";
            }
            pushLabel(cx - 7, cy - 23, color, label, PRIO_ACTOR);
        }
    }

    private static String carriedName(int index) {
        LightBridge.LightState s = LightBridge.read();
        byte[][] t = s != null ? s.entities : null;
        if (t == null || t.length < 6 || index >= t[5].length) {
            return "L" + index;
        }
        int type = t[5][index];
        return type >= 0 && type < LIGHT_NAMES.length ? LIGHT_NAMES[type] : "L" + index;
    }

    private static void marker(Graphics g, int cx, int cy, int color, String label, int prio) {
        g.setColor(color);
        g.drawRect(cx - TILE / 2, cy - TILE / 2, TILE - 1, TILE - 1);
        pushLabel(cx - TILE / 2, cy - TILE / 2 - 10, color, label, prio);
    }

    private static final int PRIO_ACTOR = 5, PRIO_LIGHT = 4, PRIO_INTER = 3, PRIO_WORLD = 2, PRIO_ITEM = 1;

    private static final int MAX_LABELS = 1024;
    private static final int[] labelX = new int[MAX_LABELS];
    private static final int[] labelY = new int[MAX_LABELS];
    private static final int[] labelW = new int[MAX_LABELS];
    private static final int[] labelColor = new int[MAX_LABELS];
    private static final int[] labelPrio = new int[MAX_LABELS];
    private static final String[] labelText = new String[MAX_LABELS];
    private static int labelCount;

    private static final int[] accX = new int[MAX_LABELS];
    private static final int[] accY = new int[MAX_LABELS];
    private static final int[] accW = new int[MAX_LABELS];
    private static int accCount;

    private static void pushLabel(int x, int y, int color, String text, int prio) {
        if (text == null || labelCount >= MAX_LABELS) {
            return;
        }
        int w = font != null ? font.stringWidth(text) : text.length() * 4;
        int h = font != null ? font.getHeight() : 8;
        if (x + w > Graphics.VIRTUAL_WIDTH) {
            x = Graphics.VIRTUAL_WIDTH - w;
        }
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (y > Graphics.VIRTUAL_HEIGHT - h) {
            y = Graphics.VIRTUAL_HEIGHT - h;
        }
        labelX[labelCount] = x;
        labelY[labelCount] = y;
        labelW[labelCount] = w;
        labelColor[labelCount] = color;
        labelPrio[labelCount] = prio;
        labelText[labelCount] = text;
        labelCount++;
    }

    private static void flushLabels(Graphics g) {
        if (labelCount == 0) {
            return;
        }
        int h = font != null ? font.getHeight() : 8;
        accCount = 0;
        for (int prio = PRIO_ACTOR; prio >= PRIO_ITEM; prio--) {
            for (int i = 0; i < labelCount; i++) {
                if (labelPrio[i] != prio || overlapsAccepted(labelX[i], labelY[i], labelW[i], h)) {
                    continue;
                }
                accX[accCount] = labelX[i];
                accY[accCount] = labelY[i];
                accW[accCount] = labelW[i];
                accCount++;
                String t = labelText[i];
                int x = labelX[i], y = labelY[i];
                g.setColor(0x000000);
                g.drawString(t, x - 1, y, Graphics.TOP | Graphics.LEFT);
                g.drawString(t, x + 1, y, Graphics.TOP | Graphics.LEFT);
                g.drawString(t, x, y - 1, Graphics.TOP | Graphics.LEFT);
                g.drawString(t, x, y + 1, Graphics.TOP | Graphics.LEFT);
                g.setColor(labelColor[i]);
                g.drawString(t, x, y, Graphics.TOP | Graphics.LEFT);
            }
        }
    }

    private static boolean overlapsAccepted(int x, int y, int w, int h) {
        for (int i = 0; i < accCount; i++) {
            if (x < accX[i] + accW[i] && x + w > accX[i] && y < accY[i] + h && y + h > accY[i]) {
                return true;
            }
        }
        return false;
    }

    private static String dirGlyph(int mode) {
        switch (mode) {
            case 1: return " >";
            case 2: return " v";
            case 3: return " <";
            case 4: return " ^";
            default: return "";
        }
    }

    private static boolean inGrid(int row, int col, LightBridge.LightState s) {
        return row >= 0 && row < s.rows && col >= 0 && col < s.cols;
    }

    private static boolean offscreen(int sx, int sy) {
        return sx < -TILE || sx > Graphics.VIRTUAL_WIDTH || sy < -TILE || sy > Graphics.VIRTUAL_HEIGHT;
    }

    public static String statusLine() {
        LightBridge.LightState s = LightBridge.read();
        if (s == null) {
            return null;
        }
        int interN = count(s.interactables);
        int itemN = count(LightBridge.itemTable());
        int plateN = count(LightBridge.plateTable());
        int trigN = count(LightBridge.triggerTable());
        return modeName() + "  cam " + s.camX + "," + s.camY + "  grid " + s.cols + "x" + s.rows
                + "  L" + s.entCount + " I" + interN + " it" + itemN + " pl" + plateN + " tr" + trigN;
    }

    private static int count(byte[][] table) {
        return table != null && table.length > 0 && table[0] != null ? table[0].length : 0;
    }

    private static String modeName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < FAMILIES.length; i++) {
            if (familyOn[i]) {
                sb.append(FAMILIES[i].toLowerCase()).append(' ');
            }
        }
        return sb.length() == 0 ? "off" : sb.toString().trim();
    }

    private static int gridColor(int v) {
        int g = Math.min(255, 80 + v * 25);
        return (0x30 << 16) | (g << 8) | 0x30;
    }
}
