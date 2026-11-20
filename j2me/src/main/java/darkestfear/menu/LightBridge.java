package darkestfear.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class LightBridge {

    private static boolean ready;
    private static boolean failed;

    private static Class<?> levelCls;
    private static Method mGetCurrentScreen;
    private static Field fGrid;
    private static Field fCamera;
    private static Field fEntities;
    private static Field fBright;
    private static Field fPlayH;
    private static Field fPlayer;
    private static Field fEnemy;
    private static Field fCharX;
    private static Field fCharY;
    private static Field fCharFacing;
    private static Field fCharCol;
    private static Field fCharRow;
    private static Field fCharH;
    private static Field fCreature;
    private static Field fBoss;
    private static Field fFootprints;
    private static Field fInter;
    private static Field fItems;
    private static Field fPlates;
    private static Field fTriggers;
    private static Field fCarried;
    private static Field fFearTime;
    private static Field fFearLimit;
    private static Field fInterCount;
    private static Method mNpcSpriteId;
    private static Field fLevelIndex;
    private static Method mTileSpriteId;
    private static Field fSpriteAttr;
    private static Field fSpriteImages;

    public static final class LightState {
        public final byte[][] grid;
        public final int rows;
        public final int cols;
        public final int camX;
        public final int camY;
        public final int playH;
        public final byte[][] entities;
        public final int entCount;
        public final int playerRow;
        public final int playerCol;
        public final byte[][][] footprints;
        public final byte[][] interactables;

        LightState(byte[][] grid, int rows, int cols, int camX, int camY, int playH,
                byte[][] entities, int entCount, int playerRow, int playerCol,
                byte[][][] footprints, byte[][] interactables) {
            this.grid = grid;
            this.rows = rows;
            this.cols = cols;
            this.camX = camX;
            this.camY = camY;
            this.playH = playH;
            this.entities = entities;
            this.entCount = entCount;
            this.playerRow = playerRow;
            this.playerCol = playerCol;
            this.footprints = footprints;
            this.interactables = interactables;
        }
    }

    private static Class<?> resCls;
    private static Field fSpriteTable;
    private static Field fDirty;
    private static Field fBoxI, fBoxJ, fBoxK, fBoxL;
    private static boolean floorResolved;
    private static boolean floorFailed;
    private static int[] origBright;
    private static int[] flattened;
    private static short[] origDark;
    private static boolean floorApplied;
    private static int forceRebake;
    private static int prevPlayerRow = Integer.MIN_VALUE;
    private static int prevPlayerCol = Integer.MIN_VALUE;
    private static final int DARK_TILE_SPRITE = 0x60;
    // Levels without darkness damage set the timer limit to ~10^8 ms instead of switching it off.
    private static final int NO_DARKNESS_DAMAGE = 60000;

    private LightBridge() {
    }

    private static boolean resolveFloor() {
        if (floorResolved) {
            return true;
        }
        if (floorFailed || !available() || fBright == null) {
            return false;
        }
        try {
            resCls = Class.forName("b", false, LightBridge.class.getClassLoader());
            for (Field f : resCls.getDeclaredFields()) {
                if (f.getType() == short[][].class) {
                    f.setAccessible(true);
                    fSpriteTable = f;
                    break;
                }
            }
            try {
                fDirty = field("g", boolean.class);
                fBoxI = field("I", byte.class);
                fBoxJ = field("J", byte.class);
                fBoxK = field("K", byte.class);
                fBoxL = field("L", byte.class);
            } catch (Throwable t) {
                fDirty = null;
            }
            floorResolved = fSpriteTable != null;
            if (!floorResolved) {
                floorFailed = true;
            }
            return floorResolved;
        } catch (Throwable t) {
            floorFailed = true;
            return false;
        }
    }

    public static void flattenFloor(int playerRow, int playerCol) {
        if (!resolveFloor()) {
            return;
        }
        try {
            int[] bright = (int[]) fBright.get(null);
            short[][] sprites = (short[][]) fSpriteTable.get(null);
            if (bright == null || bright.length < 2 || sprites == null) {
                return;
            }
            if (bright != flattened) {
                origBright = bright.clone();
                flattened = bright;
            }
            int max = origBright.length - 1;
            int brightId = origBright[max];
            if (brightId < 0 || brightId >= sprites.length || sprites[brightId] == null) {
                return;
            }
            for (int i = 1; i < max; i++) {
                bright[i] = brightId;
            }
            if (DARK_TILE_SPRITE < sprites.length) {
                if (origDark == null) {
                    origDark = sprites[DARK_TILE_SPRITE];
                }
                sprites[DARK_TILE_SPRITE] = sprites[brightId];
            }
            if (!floorApplied) {
                forceRebake = 3;
            }
            floorApplied = true;
            if (playerRow != prevPlayerRow || playerCol != prevPlayerCol) {
                prevPlayerRow = playerRow;
                prevPlayerCol = playerCol;
                if (forceRebake < 2) {
                    forceRebake = 2;
                }
            }
            rebake();
        } catch (Throwable t) {
            floorFailed = true;
        }
    }

    public static void restoreFloor() {
        if (!floorApplied || !floorResolved) {
            return;
        }
        try {
            int[] bright = (int[]) fBright.get(null);
            short[][] sprites = (short[][]) fSpriteTable.get(null);
            if (bright != null && origBright != null && bright.length == origBright.length) {
                System.arraycopy(origBright, 0, bright, 0, bright.length);
            }
            if (sprites != null && origDark != null && DARK_TILE_SPRITE < sprites.length) {
                sprites[DARK_TILE_SPRITE] = origDark;
            }
            floorApplied = false;
            forceRebake = 3;
        } catch (Throwable t) {
        }
    }

    public static void rebakeRestoredFloor() {
        if (floorApplied || !floorResolved) {
            return;
        }
        try {
            rebake();
        } catch (Throwable t) {
        }
    }

    private static void rebake() throws IllegalAccessException {
        if (forceRebake <= 0 || fDirty == null) {
            return;
        }
        Object level = currentLevel();
        if (level == null) {
            return;
        }
        if (fBoxI != null && fBoxJ != null && fBoxK != null && fBoxL != null) {
            fBoxI.setByte(level, (byte) 0);
            fBoxJ.setByte(level, (byte) 0);
            fBoxK.setByte(level, (byte) 127);
            fBoxL.setByte(level, (byte) 127);
        }
        fDirty.setBoolean(level, true);
        forceRebake--;
    }

    static synchronized void init() {
        if (ready || failed) {
            return;
        }
        try {
            ClassLoader cl = LightBridge.class.getClassLoader();
            Class<?> appCls = Class.forName("l", false, cl);
            Class<?> screenCls = Class.forName("r", false, cl);
            levelCls = Class.forName("i", false, cl);
            mGetCurrentScreen = findGetter(appCls, "a", screenCls);
            mGetCurrentScreen.setAccessible(true);
            fGrid = field("g", byte[][].class);
            fCamera = field("g", int[].class);
            fEntities = field("a", byte[][].class);
            fBright = field("a", int[].class);
            fPlayH = field("a", int.class);
            try {
                fFootprints = field("a", byte[][][].class);
            } catch (Throwable t) {
                fFootprints = null;
            }
            fInter = fieldOrNull("c", byte[][].class);
            fItems = fieldOrNull("e", byte[][].class);
            fPlates = fieldOrNull("b", byte[][].class);
            fTriggers = fieldOrNull("d", byte[][].class);
            fCarried = fieldOrNull("k", byte.class);
            fFearTime = fieldOrNull("e", int.class);
            fFearLimit = fieldOrNull("f", int.class);
            fInterCount = fieldOrNull("h", byte.class);
            try {
                mNpcSpriteId = levelCls.getDeclaredMethod("a", byte.class);
                mNpcSpriteId.setAccessible(true);
            } catch (Throwable t) {
                mNpcSpriteId = null;
            }
            fLevelIndex = fieldOrNull("D", byte.class);
            resolveSpriteAttrs(cl);
            resolvePlayer();
            ready = true;
        } catch (Throwable t) {
            failed = true;
        }
    }

    static boolean available() {
        init();
        return ready;
    }

    static Object currentLevel() {
        if (!available()) {
            return null;
        }
        try {
            Object s = mGetCurrentScreen.invoke(null);
            return s != null && levelCls.isInstance(s) ? s : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static LightState read() {
        Object level = currentLevel();
        if (level == null) {
            return null;
        }
        try {
            byte[][] grid = (byte[][]) fGrid.get(level);
            int[] cam = (int[]) fCamera.get(level);
            int playH = fPlayH.getInt(null);
            byte[][] ents = (byte[][]) fEntities.get(level);
            if (grid == null || grid.length == 0 || cam == null || cam.length < 2) {
                return null;
            }
            int rows = grid.length;
            int cols = grid[0].length;
            int entCount = ents != null && ents.length >= 6 && ents[0] != null ? ents[0].length : 0;

            int playerRow = -1, playerCol = -1;
            if (fPlayer != null && fCharX != null && fCharY != null) {
                Object player = fPlayer.get(level);
                if (player != null) {
                    int px = fCharX.getInt(player);
                    int py = fCharY.getInt(player);
                    int pc = px / 16;
                    int pr = py / 16;
                    if (pr >= 0 && pr < rows && pc >= 0 && pc < cols) {
                        playerRow = pr;
                        playerCol = pc;
                    }
                }
            }

            byte[][][] footprints = null;
            if (fFootprints != null) {
                try {
                    footprints = (byte[][][]) fFootprints.get(level);
                } catch (Throwable t) {
                    footprints = null;
                }
            }

            byte[][] interactables = null;
            if (fInter != null) {
                try {
                    interactables = (byte[][]) fInter.get(level);
                } catch (Throwable t) {
                    interactables = null;
                }
            }

            return new LightState(grid, rows, cols, cam[0], cam[1],
                    playH > 0 ? playH : javax.microedition.lcdui.Graphics.VIRTUAL_HEIGHT,
                    ents, entCount, playerRow, playerCol, footprints, interactables);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void resolveSpriteAttrs(ClassLoader cl) {
        try {
            Class<?> tileCls = Class.forName("c", false, cl);
            for (Method m : tileCls.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && m.getReturnType() == int.class
                        && p.length == 1 && p[0] == byte.class) {
                    m.setAccessible(true);
                    mTileSpriteId = m;
                    break;
                }
            }
            Class<?> resCls = Class.forName("b", false, cl);
            for (Field f : resCls.getDeclaredFields()) {
                if (f.getName().equals("a") && f.getType() == short[][].class) {
                    f.setAccessible(true);
                    fSpriteAttr = f;
                } else if (f.getName().equals("b") && f.getType() == Object[].class) {
                    f.setAccessible(true);
                    fSpriteImages = f;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static final int[] overhangCache = new int[128];

    public static int tileOverhangCells(int code) {
        if (code >= 0) {
            return 0;
        }
        int i = -code;
        if (i >= overhangCache.length) {
            return 0;
        }
        int cached = overhangCache[i];
        if (cached != 0) {
            return cached - 1;
        }
        int[] box = tileSpriteBox(code);
        if (box == null) {
            return 0;
        }
        int cells = box[1] / 16;
        if (cells < 0) {
            cells = 0;
        } else if (cells > 4) {
            cells = 4;
        }
        overhangCache[i] = cells + 1;
        return cells;
    }

    public static boolean[] computeWallCover(byte[][] grid, int rows, int cols) {
        boolean[] mask = new boolean[rows * cols];
        for (int r = 0; r < rows; r++) {
            byte[] row = grid[r];
            for (int c = 0; c < cols; c++) {
                if (row[c] < 0) {
                    mask[r * cols + c] = true;
                    int up = tileOverhangCells(row[c]);
                    for (int k = 1; k <= up; k++) {
                        int rr = r - k;
                        if (rr >= 0) {
                            mask[rr * cols + c] = true;
                        }
                    }
                }
            }
        }
        return mask;
    }

    public static int tileSpriteId(int tileCode) {
        if (mTileSpriteId == null) {
            return -1;
        }
        try {
            return (Integer) mTileSpriteId.invoke(null, (byte) tileCode);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static boolean[] tileSprite;

    public static boolean isTileSprite(int spriteId) {
        if (tileSprite == null) {
            int count = spriteCount();
            if (count <= 0 || mTileSpriteId == null) {
                return false;
            }
            boolean[] t = new boolean[count];
            for (int code = -128; code < 0; code++) {
                int sid = tileSpriteId(code);
                if (sid >= 0 && sid < count) {
                    t[sid] = true;
                }
            }
            tileSprite = t;
        }
        return spriteId >= 0 && spriteId < tileSprite.length && tileSprite[spriteId];
    }

    public static boolean isTileObjectSheet(Object img) {
        return img != null && img == spriteImage(tileSpriteId(-1));
    }

    public static int spriteAt(Object img, int srcX, int srcY, int w, int h) {
        if (img == null || fSpriteAttr == null || fSpriteImages == null) {
            return -1;
        }
        try {
            short[][] table = (short[][]) fSpriteAttr.get(null);
            Object[] imgs = (Object[]) fSpriteImages.get(null);
            if (table == null || imgs == null) {
                return -1;
            }
            int origin = -1, contain = -1;
            for (int id = 0; id < table.length; id++) {
                short[] a = table[id];
                if (a == null || a.length < 7 || a[6] < 0 || a[6] >= imgs.length || imgs[a[6]] != img) {
                    continue;
                }
                if (srcX == a[0] && srcY == a[1]) {
                    if (w == a[2] && h == a[3]) {
                        return id;
                    }
                    if (origin < 0) {
                        origin = id;
                    }
                } else if (contain < 0 && srcX >= a[0] && srcX < a[0] + a[2] && srcY >= a[1] && srcY < a[1] + a[3]) {
                    contain = id;
                }
            }
            return origin >= 0 ? origin : contain;
        } catch (Throwable t) {
            return -1;
        }
    }

    public static void normalizeIdlePlayerColumn() {
        if (!available() || fPlayer == null || fCharX == null || fCharY == null
                || fCharCol == null || fCharRow == null || fCharH == null) {
            return;
        }
        Object level = currentLevel();
        if (level == null) {
            return;
        }
        try {
            Object p = fPlayer.get(level);
            if (p == null) {
                return;
            }
            int x = fCharX.getInt(p);
            int y = fCharY.getInt(p);
            int col = fCharCol.getByte(p);
            int row = fCharRow.getByte(p);
            if (x == col * 16 && y == row * 16 && fCharH.getByte(p) != col) {
                fCharH.setByte(p, (byte) col);
            }
        } catch (Throwable t) {
        }
    }

    public static int playHeight() {
        if (!available() || fPlayH == null) {
            return -1;
        }
        try {
            return fPlayH.getInt(null);
        } catch (Throwable t) {
            return -1;
        }
    }

    public static int spriteCount() {
        if (fSpriteAttr == null) {
            return 0;
        }
        try {
            short[][] a = (short[][]) fSpriteAttr.get(null);
            return a == null ? 0 : a.length;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static short[] spriteAttr(int spriteId) {
        if (fSpriteAttr == null) {
            return null;
        }
        try {
            short[][] a = (short[][]) fSpriteAttr.get(null);
            if (a == null || spriteId < 0 || spriteId >= a.length) {
                return null;
            }
            return a[spriteId];
        } catch (Throwable t) {
            return null;
        }
    }

    public static int[] floorSpriteTable() {
        if (origBright != null) {
            return origBright;
        }
        if (fBright == null) {
            return null;
        }
        try {
            return (int[]) fBright.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int[] spriteFrames(Object img) {
        int count = spriteCount();
        int[] frames = new int[count * 5];
        int n = 0;
        for (int id = 0; id < count; id++) {
            short[] a = spriteAttr(id);
            boolean tile = isTileSprite(id);
            if (a == null || spriteImage(id) != img) {
                continue;
            }
            frames[n++] = a[0];
            frames[n++] = a[1];
            frames[n++] = a[2];
            frames[n++] = a[3];
            frames[n++] = tile ? 1 : 0;
        }
        return java.util.Arrays.copyOf(frames, n);
    }

    public static Object spriteImage(int spriteId) {
        short[] a = spriteAttr(spriteId);
        if (a == null || a.length < 7 || fSpriteImages == null) {
            return null;
        }
        try {
            Object[] imgs = (Object[]) fSpriteImages.get(null);
            int idx = a[6];
            if (imgs == null || idx < 0 || idx >= imgs.length) {
                return null;
            }
            return imgs[idx];
        } catch (Throwable t) {
            return null;
        }
    }

    public static int[] tileSpriteBox(int tileCode) {
        if (mTileSpriteId == null || fSpriteAttr == null) {
            return null;
        }
        try {
            int sid = (Integer) mTileSpriteId.invoke(null, (byte) tileCode);
            short[][] attr = (short[][]) fSpriteAttr.get(null);
            if (attr == null || sid < 0 || sid >= attr.length) {
                return null;
            }
            short[] a = attr[sid];
            if (a == null || a.length < 6) {
                return null;
            }
            return new int[] { a[4], a[5], a[2], a[3] };
        } catch (Throwable t) {
            return null;
        }
    }

    private static void resolvePlayer() {
        try {
            Class<?> saCls = null;
            for (Field f : levelCls.getDeclaredFields()) {
                Class<?> t = f.getType();
                if (t.isPrimitive() || t.isArray() || t == levelCls) {
                    continue;
                }
                if (declaredField(t, "i", int.class) != null
                        && declaredField(t, "j", int.class) != null
                        && declaredField(t, "g", byte.class) != null) {
                    saCls = t;
                    break;
                }
            }
            if (saCls == null) {
                return;
            }
            fCharX = declaredField(saCls, "i", int.class);
            fCharY = declaredField(saCls, "j", int.class);
            fCharFacing = declaredField(saCls, "e", byte.class);
            fCharCol = declaredField(saCls, "f", byte.class);
            fCharRow = declaredField(saCls, "g", byte.class);
            fCharH = declaredField(saCls, "h", byte.class);
            for (Field f : levelCls.getDeclaredFields()) {
                if (f.getType() != saCls) {
                    continue;
                }
                f.setAccessible(true);
                switch (f.getName()) {
                    case "a": fPlayer = f; break;
                    case "b": fEnemy = f; break;
                    case "c": fCreature = f; break;
                    case "d": fBoss = f; break;
                    default: break;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static byte[][] itemTable() {
        return objectTable(fItems);
    }

    public static byte[][] plateTable() {
        return objectTable(fPlates);
    }

    public static byte[][] triggerTable() {
        return objectTable(fTriggers);
    }

    public static int carriedLight() {
        Object level = fCarried == null ? null : currentLevel();
        if (level == null) {
            return -2;
        }
        try {
            return fCarried.getByte(level);
        } catch (Throwable t) {
            return -2;
        }
    }

    public static boolean jumpToLevel(int level) {
        try {
            ClassLoader cl = LightBridge.class.getClassLoader();
            Field current = null;
            for (Field f : Class.forName("q", false, cl).getDeclaredFields()) {
                if (f.getName().equals("b") && f.getType() == int.class
                        && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    current = f;
                }
            }
            if (current == null) {
                return false;
            }
            java.lang.reflect.Method enqueue =
                    Class.forName("l", false, cl).getDeclaredMethod("a", int.class, Object.class);
            current.setAccessible(true);
            enqueue.setAccessible(true);
            current.setInt(null, level);
            enqueue.invoke(null, 17, null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static int levelIndex() {
        Object level = fLevelIndex == null ? null : currentLevel();
        if (level == null) {
            return -1;
        }
        try {
            return fLevelIndex.getByte(level);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static byte[][] objectTable(Field f) {
        Object level = f == null ? null : currentLevel();
        if (level == null) {
            return null;
        }
        try {
            return (byte[][]) f.get(level);
        } catch (Throwable t) {
            return null;
        }
    }

    /** How far the game's darkness-damage timer has run, 0..1; 0 when the player's tile is lit. */
    public static float darknessProgress() {
        Object level = currentLevel();
        if (level == null || fFearTime == null || fFearLimit == null || fPlayer == null
                || fCharRow == null || fCharCol == null) {
            return 0f;
        }
        try {
            Object p = fPlayer.get(level);
            byte[][] grid = (byte[][]) fGrid.get(level);
            if (p == null || grid == null) {
                return 0f;
            }
            int row = fCharRow.getByte(p), col = fCharCol.getByte(p);
            if (row < 0 || row >= grid.length || col < 0 || col >= grid[row].length || grid[row][col] != 0) {
                return 0f;
            }
            int time = fFearTime.getInt(level), limit = fFearLimit.getInt(level);
            if (time <= 0 || limit <= 0 || limit > NO_DARKNESS_DAMAGE) {
                return 0f;
            }
            return Math.min(1f, time / (float) limit);
        } catch (Throwable t) {
            return 0f;
        }
    }

    /** How many rows of the interactable table the game itself draws; it can be fewer than the table holds. */
    public static int interactableCount() {
        Object level = currentLevel();
        if (level == null || fInterCount == null) {
            return 0;
        }
        try {
            return fInterCount.getByte(level);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int npcSpriteId(int variant) {
        Object level = currentLevel();
        if (level == null || mNpcSpriteId == null) {
            return -1;
        }
        try {
            return (Integer) mNpcSpriteId.invoke(level, (byte) variant);
        } catch (Throwable t) {
            return -1;
        }
    }

    public static int[] actor(int which) {
        Field f = which == 0 ? fPlayer : which == 1 ? fEnemy : which == 2 ? fCreature
                : which == 3 ? fBoss : null;
        if (f == null || fCharX == null || fCharY == null) {
            return null;
        }
        Object level = currentLevel();
        if (level == null) {
            return null;
        }
        try {
            Object a = f.get(level);
            if (a == null) {
                return null;
            }
            int px = fCharX.getInt(a);
            int py = fCharY.getInt(a);
            int facing = fCharFacing != null ? fCharFacing.getByte(a) : 0;
            int col = fCharCol != null ? fCharCol.getByte(a) : px / 16;
            int row = fCharRow != null ? fCharRow.getByte(a) : py / 16;
            return new int[] { px, py, facing, col, row };
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field declaredField(Class<?> cls, String name, Class<?> type) {
        for (Field f : cls.getDeclaredFields()) {
            if (f.getName().equals(name) && f.getType() == type) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    private static Method findGetter(Class<?> owner, String name, Class<?> ret) {
        for (Method m : owner.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getReturnType() == ret
                    && m.getParameterTypes().length == 0) {
                return m;
            }
        }
        throw new RuntimeException("method not found: " + name);
    }

    private static Field field(String name, Class<?> type) {
        for (Field f : levelCls.getDeclaredFields()) {
            if (f.getName().equals(name) && f.getType() == type) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new RuntimeException("field not found: " + name + " " + type);
    }

    private static Field fieldOrNull(String name, Class<?> type) {
        try {
            return field(name, type);
        } catch (Throwable t) {
            return null;
        }
    }
}
