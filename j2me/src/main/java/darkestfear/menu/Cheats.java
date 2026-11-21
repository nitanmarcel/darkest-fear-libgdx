package darkestfear.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Cheats {

    private static volatile boolean safe;
    private static Object calmed;
    private static int aggression;
    private static int kept;

    private Cheats() {
    }

    public static boolean safe() {
        return safe;
    }

    public static void setSafe(boolean on) {
        safe = on;
        if (!on && calmed != null && calmed == LightBridge.currentLevel()) {
            try {
                field("m", int.class).setInt(calmed, aggression);
            } catch (Throwable ignored) {
            }
        }
        if (!on) {
            calmed = null;
        }
    }

    public static void hit() {
        Object level = LightBridge.currentLevel();
        try {
            field("h", int.class).setInt(level, 0);
            call("h", 1);
            kept = field("A", byte.class).getByte(level);
        } catch (Throwable ignored) {
        }
    }

    public static void heal() {
        Object level = LightBridge.currentLevel();
        try {
            kept = field("z", byte.class).getByte(level);
            field("A", byte.class).setByte(level, (byte) kept);
        } catch (Throwable ignored) {
        }
    }

    public static int[] health() {
        Object level = LightBridge.currentLevel();
        try {
            return new int[] {field("A", byte.class).getByte(level), field("z", byte.class).getByte(level)};
        } catch (Throwable t) {
            return null;
        }
    }

    public static void skipCutscene() {
        GameInput.skipCutscene();
    }

    public static void tick() {
        Object level = safe ? LightBridge.currentLevel() : null;
        if (level == null) {
            return;
        }
        try {
            Field monster = field("m", int.class);
            Field health = field("A", byte.class);
            if (level != calmed) {
                calmed = level;
                aggression = monster.getInt(level);
                kept = health.getByte(level);
            }
            monster.setInt(level, 0);
            field("e", int.class).setInt(level, 0);
            if (health.getByte(level) < kept) {
                health.setByte(level, (byte) kept);
                field("h", int.class).setInt(level, 0);
            } else {
                kept = health.getByte(level);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Field field(String name, Class<?> type) throws Exception {
        for (Field f : LightBridge.currentLevel().getClass().getDeclaredFields()) {
            if (f.getName().equals(name) && f.getType() == type) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void call(String name, int value) {
        Object level = LightBridge.currentLevel();
        try {
            for (Method m : level.getClass().getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals(name) && m.getReturnType() == void.class && p.length == 1 && p[0] == int.class) {
                    m.setAccessible(true);
                    m.invoke(level, value);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
