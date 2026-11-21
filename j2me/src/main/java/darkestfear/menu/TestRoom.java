package darkestfear.menu;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.microedition.lcdui.Graphics;

public final class TestRoom {

    private static final int MAPS = 23;
    private static final String MAPS_FILE = "aw.bin";
    private static final int WIDTH = 40, HEIGHT = 32, TILE = 16;
    private static final int TILES_RIGHT = 20, TILES_BOTTOM = 18;
    private static final int LIGHT = 69, LIGHT_STEP = 5;
    private static final int[] GALLERY_SHEETS = {1, 7, 8, 9};
    private static final int[][] GALLERY = {
        {1, TILES_BOTTOM + 1, TILES_RIGHT, HEIGHT - 2},
        {TILES_RIGHT + 1, 1, WIDTH - 2, HEIGHT - 2},
    };
    private static final int GAP = 6;

    private static final int[][] ROWS = {
        {2, 2, -1, 4, -2, 6, -3, 8, -4, 10, -5, 12, -6, 14, -40},
        {4, 2, -7, 4, -8, 6, -11, 8, -12, 10, -13, 12, -100, 14, -101, 16, -102},
        {6, 2, -27, 4, -29, 6, -30, 8, -31, 10, -32, 12, -20, 14, -33, 16, -34, 18, -35},
        {7, 2, -28},
        {9, 2, -36, 4, -37, 6, -39, 8, -18, 10, -16, 12, -17, 14, -105, 16, -106},
        {11, 2, 5, 4, 15, 6, 25, 8, 35, 10, 45, 12, 55, 14, 75, 15, -1, 16, 75},
        {13, 2, -42, 4, -45, 6, -51, 8, -53, 10, -59, 12, -62, 14, -61},
        {15, 2, -75, 4, -76, 6, -77, 8, -78, 10, -79},
        {17, 10, -99},
    };

    private static byte[] original;
    private static int[] sheetOf;
    private static Method drawSprite, loadSheet;

    private TestRoom() {
    }

    public static boolean loaded() {
        return original != null;
    }

    public static boolean load(int level) {
        try {
            Object[] resources = resources();
            if (original == null) {
                original = resources[MAPS] instanceof byte[] ? (byte[]) resources[MAPS] : read();
            }
            ByteArrayOutputStream maps = new ByteArrayOutputStream();
            int at = 0;
            for (int i = 0; at < original.length; i++) {
                int size = 2 + original[at] * original[at + 1];
                if (i == level) {
                    maps.write(room());
                } else {
                    maps.write(original, at, size);
                }
                at += size;
            }
            resources[MAPS] = maps.toByteArray();
            return LightBridge.jumpToLevel(level);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void restore() {
        try {
            if (original != null) {
                resources()[MAPS] = original;
                original = null;
            }
        } catch (Throwable ignored) {
        }
    }

    public static void paint(Graphics g) {
        LightBridge.LightState s = original == null ? null : LightBridge.read();
        if (s == null || s.cols != WIDTH || s.rows != HEIGHT) {
            return;
        }
        try {
            if (sheetOf == null) {
                index();
            }
            int originX = Graphics.VIRTUAL_WIDTH / 2 - s.camX, originY = s.playH / 2 - s.camY;
            g.setClip(0, 0, Graphics.VIRTUAL_WIDTH, s.playH);
            int region = 0, x = 0, y = 0, rowHeight = 0;
            for (int id = 0; id < sheetOf.length && region < GALLERY.length; id++) {
                if (sheetOf[id] == 0) {
                    continue;
                }
                short[] a = LightBridge.spriteAttr(id);
                if (a == null) {
                    loadSheet.invoke(null, sheetOf[id]);
                    a = LightBridge.spriteAttr(id);
                    if (a == null) {
                        continue;
                    }
                }
                int[] area = GALLERY[region];
                int left = area[0] * TILE, top = area[1] * TILE, width = (area[2] - area[0]) * TILE;
                if (x > 0 && x + a[2] > width) {
                    x = 0;
                    y += rowHeight + GAP;
                    rowHeight = 0;
                }
                if (y + a[3] > (area[3] - area[1]) * TILE) {
                    region++;
                    x = y = rowHeight = 0;
                    id--;
                    continue;
                }
                drawSprite.invoke(null, left + x + a[4] + originX, top + y + a[5] + originY, id, g);
                x += a[2] + GAP;
                rowHeight = Math.max(rowHeight, a[3]);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void index() throws Exception {
        Class<?> resources = Class.forName("b", false, GameJar.loader());
        for (Method m : resources.getDeclaredMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 4 && p[3] == Graphics.class) {
                m.setAccessible(true);
                drawSprite = m;
            } else if (p.length == 1 && p[0] == int.class && m.getReturnType() == Object.class) {
                m.setAccessible(true);
                loadSheet = m;
            }
        }
        int[] offset = new int[GALLERY_SHEETS[GALLERY_SHEETS.length - 1] + 1];
        String[] file = new String[offset.length];
        try (DataInputStream in = new DataInputStream(GameJar.loader().getResourceAsStream("h"))) {
            for (int i = 0; i < offset.length; i++) {
                offset[i] = in.readInt();
                file[i] = in.readUTF();
                in.readInt();
            }
        }
        int[] sheets = new int[0];
        for (int sheet : GALLERY_SHEETS) {
            try (DataInputStream in = new DataInputStream(GameJar.loader().getResourceAsStream(file[sheet]))) {
                in.skipBytes(offset[sheet]);
                int first = in.readShort(), last = in.readShort();
                if (last > sheets.length) {
                    sheets = java.util.Arrays.copyOf(sheets, last);
                }
                java.util.Arrays.fill(sheets, first, last, sheet);
            }
        }
        sheetOf = sheets;
    }

    private static Object[] resources() throws Exception {
        for (Field f : Class.forName("b", false, GameJar.loader()).getDeclaredFields()) {
            if (f.getName().equals("b") && f.getType() == Object[].class) {
                f.setAccessible(true);
                return (Object[]) f.get(null);
            }
        }
        throw new NoSuchFieldException();
    }

    private static byte[] read() throws Exception {
        try (InputStream in = GameJar.loader().getResourceAsStream(MAPS_FILE)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static byte[] room() {
        byte[][] grid = new byte[HEIGHT][WIDTH];
        for (int row = 0; row < HEIGHT; row++) {
            grid[row][0] = -1;
            grid[row][WIDTH - 1] = -1;
        }
        for (int col = 1; col < WIDTH - 1; col++) {
            grid[0][col] = -2;
            grid[HEIGHT - 1][col] = -2;
        }
        grid[0][0] = -6;
        grid[HEIGHT - 1][0] = -2;
        grid[HEIGHT - 1][WIDTH - 1] = -3;
        grid[HEIGHT - 2][0] = -4;
        grid[2][0] = -3;
        grid[3][0] = 78;
        grid[8][0] = -3;
        grid[9][0] = 79;
        grid[0][5] = 75;
        grid[0][15] = 75;
        for (int[] row : ROWS) {
            for (int i = 1; i < row.length; i += 2) {
                grid[row[0]][row[i]] = (byte) row[i + 1];
            }
        }
        for (int row = LIGHT_STEP / 2; row < HEIGHT - 1; row += LIGHT_STEP) {
            for (int col = LIGHT_STEP / 2; col < WIDTH - 1; col += LIGHT_STEP) {
                if (grid[row][col] == 0) {
                    grid[row][col] = LIGHT;
                }
            }
        }
        byte[] record = new byte[2 + WIDTH * HEIGHT];
        record[0] = WIDTH;
        record[1] = HEIGHT;
        for (int row = 0; row < HEIGHT; row++) {
            System.arraycopy(grid[row], 0, record, 2 + row * WIDTH, WIDTH);
        }
        return record;
    }
}
