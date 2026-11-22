package darkestfear.menu;

import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class SoundBoard {

    private SoundBoard() {
    }

    public static int[] ids() {
        try {
            for (Field f : sound().getDeclaredFields()) {
                if (f.getType() == int[].class && Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    int[] ids = (int[]) f.get(null);
                    return ids == null ? new int[0] : ids.clone();
                }
            }
        } catch (Throwable ignored) {
        }
        return new int[0];
    }

    public static void load() {
        try {
            Method wrap = null, register = sound().getDeclaredMethod("a", byte[].class, int.class);
            for (Method m : Class.forName("b", false, GameJar.loader()).getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == int.class && m.getReturnType() == Object.class) {
                    wrap = m;
                }
            }
            wrap.setAccessible(true);
            register.setAccessible(true);
            try (DataInputStream in = new DataInputStream(GameJar.loader().getResourceAsStream("h"))) {
                for (int id = 0; in.available() > 0; id++) {
                    in.readInt();
                    String file = in.readUTF();
                    in.readInt();
                    if (file.endsWith(".mid")) {
                        register.invoke(null, ((Object[]) wrap.invoke(null, id))[0], id);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void play(int id, boolean loop) {
        try {
            Method play = sound().getDeclaredMethod("a", int.class, int.class);
            play.setAccessible(true);
            play.invoke(null, id, loop ? -1 : 1);
        } catch (Throwable ignored) {
        }
    }

    public static void stop() {
        try {
            for (Method m : sound().getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == void.class && Modifier.isPublic(m.getModifiers())) {
                    m.setAccessible(true);
                    m.invoke(null);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Class<?> sound() throws ClassNotFoundException {
        return Class.forName("k", false, GameJar.loader());
    }
}
