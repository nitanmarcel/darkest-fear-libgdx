package darkestfear.menu;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.ResourcePack;

public final class SoundEvents {

    private static final int FIREPLACE = 5, PORTABLE_LAMPS = 2, PATIENT = 17, WELL_LIT = 1, BOX = -7;
    private static final int PLAYER = 0, STALKER = 1, BOSS = 3;
    private static final int CREATURE_LEVEL = 5, PHONE_LEVEL = 7, SPINE_LEVEL = 12, HELEN_LEVEL = 14;
    private static final int CREATURE_DEAD = 4, SPINE_DEAD = 7, HELEN_DEAD = 6, PHONE_EVENT = 2;
    private static final int DEBRIS_X = 0, DEBRIS_Y = 1, DEBRIS_DEAD = 8, DEBRIS_HOMING = 9, DEBRIS_REACH = 8;
    private static final float SILENT = 0.08f, PHONE_FLOOR = 0.3f, PHONE_FULL = 4f, BOSS_STEP = 0.75f;
    private static final int CUTSCENE_CATCH_UP = 20, TILE = 16, BOSS_INTRO = 2000;
    private static final long AMBIENT_GAP = 4000, AMBIENT_SPREAD = 6000;

    private static Object level, started;
    private static byte[][] grid;
    private static byte[] on, plates;
    private static int[] dead = new int[0];
    private static int carried, hurt, saved, pendingSaved, stalkerDying, creatureLight, bossPhase, bossX, bossY;
    private static int debrisTimer, phoneFired, scene = -1, sceneTick, sceneLine;
    private static long ambientAt;
    private static float gain;

    private SoundEvents() {
    }

    public static void tick() {
        if (!GameBridge.soundEnabled()) {
            level = null;
            scene = -1;
            quiet();
            return;
        }
        gain = LightBridge.soundLevel() / 100f;
        int[] cutscene = GameBridge.cutscene();
        if (cutscene == null) {
            scene = -1;
        } else {
            if (cutscene[0] != scene || cutscene[1] < sceneTick) {
                scene = cutscene[0];
                sceneTick = sceneLine = -1;
            }
            if (cutscene[2] > sceneLine) {
                sceneLine = cutscene[2];
                play("cutscene." + scene + ".line" + (sceneLine + 1));
            }
            sceneTick = Math.max(sceneTick, cutscene[1] - CUTSCENE_CATCH_UP);
            while (sceneTick < cutscene[1]) {
                play("cutscene." + scene + "." + ++sceneTick);
            }
        }
        LightBridge.LightState s = GameBridge.inLevel() && !MenuOverlay.active() ? LightBridge.read() : null;
        if (s == null || s.entities == null) {
            quiet();
            return;
        }
        Object current = LightBridge.currentLevel();
        boolean known = current == level && grid.length == s.rows && grid[0].length == s.cols && on.length == s.entCount;
        int index = LightBridge.levelIndex();
        if (current != started) {
            started = current;
            ambientAt = 0;
            play("level_start." + index);
        }

        boolean pushed = false;
        for (int row = 0; known && row < s.rows; row++) {
            for (int col = 0; col < s.cols; col++) {
                int was = grid[row][col], now = s.grid[row][col];
                if (was == now) {
                    continue;
                }
                if (was <= -12 && was >= -17 && now >= 0) {
                    play("door_open");
                } else if (was >= 0 && (now == -16 || now == -17)) {
                    play("door_close");
                } else if ((was == -8 || was == -9 || was == -10 || was == -19) && now == -11) {
                    play("closet_open");
                } else if ((was == -36 || was == -38) && now == -37) {
                    play("cage_open");
                } else if ((was == -103 && now == -105) || (was == -104 && now == -106)) {
                    play("window_open");
                } else if (now == BOX && !pushed) {
                    pushed = true;
                    play("box_push");
                }
            }
        }

        int held = LightBridge.carriedLight();
        if (known && held != carried) {
            if (held < 0) {
                play("item_drop");
            } else if (held < s.entCount) {
                play(s.entities[5][held] >= PORTABLE_LAMPS || on[held] != 0 ? "item_take" : "lamp_light");
            }
        }
        float nearest = 0f;
        for (int i = 0; i < s.entCount; i++) {
            if (s.entities[5][i] != FIREPLACE || s.entities[4][i] == 0) {
                continue;
            }
            if (known && on[i] == 0) {
                play("fireplace_light");
            }
            nearest = Math.max(nearest, near(s.entities[1][i] - s.playerCol, s.entities[0][i] - s.playerRow, 1f));
        }
        ResourcePack.loop("fireplace_burning", nearest < SILENT ? 0f : nearest * gain);

        byte[][] plateTable = LightBridge.plateTable();
        byte[] pressed = plateTable != null && plateTable.length > 4 ? plateTable[4] : new byte[0];
        for (int i = 0; known && plates.length == pressed.length && i < pressed.length; i++) {
            if (plates[i] == 0 && pressed[i] != 0) {
                play("trigger");
            }
        }

        int hurtNow = LightBridge.hurtTimer();
        if (known && hurt == 0 && hurtNow > 0) {
            play("hit");
            play("hit_voice");
        }

        int lit = 0;
        byte[][] people = s.interactables;
        for (int i = 0; people != null && people.length > 3 && i < people[3].length; i++) {
            int row = people[0][i], col = people[1][i];
            if (people[3][i] > PATIENT && row >= 0 && row < s.rows && col >= 0 && col < s.cols && s.grid[row][col] > WELL_LIT) {
                lit++;
            }
        }
        if (!known) {
            saved = pendingSaved = lit;
        } else if (lit == pendingSaved && lit != saved) {
            play(lit > saved ? "patient_lit" : "patient_unlit");
            saved = lit;
        }
        pendingSaved = lit;

        int[] thomas = LightBridge.actor(PLAYER), stalker = LightBridge.actor(STALKER), boss = LightBridge.actor(BOSS);
        boolean complete = LightBridge.peek("l", boolean.class) != 0;

        int dying = LightBridge.stalker("h");
        boolean walking = LightBridge.stalker("e") > 1 || LightBridge.stalker("g") > 1;
        if (known && dying > 0 && stalkerDying == 0) {
            play("monster_die");
        }
        if (stalker != null && walking && LightBridge.peek("h", boolean.class) == 0) {
            int x = stalker[0] - s.camX + Graphics.VIRTUAL_WIDTH / 2, y = stalker[1] - s.camY + s.playH / 2;
            if (x >= 0 && x < Graphics.VIRTUAL_WIDTH && y >= 0 && y < s.playH) {
                ResourcePack.play("monster", gain, true);
            }
        }

        int light = LightBridge.peek("u", byte.class);
        if (index == CREATURE_LEVEL) {
            if (known && light > creatureLight) {
                play(light < CREATURE_DEAD ? "creature_hit" : "creature_die");
            }
            ambient("creature_idle", complete);
        }

        int phase = LightBridge.peek("v", byte.class);
        if ((index == SPINE_LEVEL || index == HELEN_LEVEL) && boss != null && thomas != null) {
            if (known && (boss[0] != bossX || boss[1] != bossY)) {
                float step = Math.min(BOSS_STEP, near(boss[0] - thomas[0], boss[1] - thomas[1], 2f * TILE));
                ResourcePack.play(index == SPINE_LEVEL ? "spine_move" : "helen_move", gain * step, true);
            }
            if (known && phase > bossPhase && index == SPINE_LEVEL && phase >= SPINE_DEAD) {
                play("spine_die");
            } else if (known && phase > bossPhase && index == HELEN_LEVEL) {
                play("helen_hit");
                if (phase == HELEN_DEAD) {
                    play("slime_hit");
                    play("helen_hit_voice");
                }
            }
            boolean waiting = LightBridge.peek("h", byte.class) != 0 || LightBridge.peek("o", int.class) < BOSS_INTRO;
            ambient(index == SPINE_LEVEL ? "spine_idle" : "helen_idle", complete || waiting);
            bossX = boss[0];
            bossY = boss[1];
        }

        int[][] debris = index == CREATURE_LEVEL || index == SPINE_LEVEL || index == HELEN_LEVEL ? LightBridge.debris() : null;
        int timer = LightBridge.peek("t", int.class);
        if (debris != null && debris.length > DEBRIS_HOMING && thomas != null) {
            if (known && timer > 0 && (debrisTimer == 0 || timer < debrisTimer)) {
                ResourcePack.play("slime_throw", gain * near(debris[DEBRIS_X][0] - thomas[0], debris[DEBRIS_Y][0] - thomas[1],
                        2f * TILE), false);
            }
            boolean struck = false;
            for (int i = 0; known && dead.length == debris[DEBRIS_DEAD].length && i < dead.length; i++) {
                struck |= dead[i] == 0 && debris[DEBRIS_DEAD][i] != 0 && debris[DEBRIS_HOMING][i] == 1
                        && Math.abs(debris[DEBRIS_X][i] - thomas[0]) <= DEBRIS_REACH
                        && Math.abs(debris[DEBRIS_Y][i] - thomas[1]) <= DEBRIS_REACH;
            }
            if (struck) {
                play("slime_hit");
            }
            dead = debris[DEBRIS_DEAD].clone();
        }

        float ring = 0f;
        byte[][] triggers = index == PHONE_LEVEL ? LightBridge.triggerTable() : null;
        for (int i = 0; triggers != null && triggers.length > 3 && i < triggers[3].length; i++) {
            if (triggers[3][i] != PHONE_EVENT) {
                continue;
            }
            if (triggers[2][i] == 0) {
                ring = Math.max(PHONE_FLOOR, near(triggers[1][i] - s.playerCol, triggers[0][i] - s.playerRow, PHONE_FULL));
            } else if (known && phoneFired == 0) {
                play("phone_answer");
            }
            phoneFired = triggers[2][i];
        }
        ResourcePack.loop("phone_ring", ring * gain);

        level = current;
        if (!known) {
            grid = new byte[s.rows][];
        }
        for (int row = 0; row < s.rows; row++) {
            grid[row] = s.grid[row].clone();
        }
        on = s.entCount == 0 ? new byte[0] : java.util.Arrays.copyOf(s.entities[4], s.entCount);
        plates = pressed.clone();
        carried = held;
        hurt = hurtNow;
        stalkerDying = dying;
        creatureLight = light;
        bossPhase = phase;
        debrisTimer = timer;
    }

    private static void play(String event) {
        ResourcePack.play(event, gain, false);
    }

    private static void quiet() {
        ResourcePack.loop("fireplace_burning", 0f);
        ResourcePack.loop("phone_ring", 0f);
    }

    private static float near(float dx, float dy, float full) {
        return Math.min(1f, full / (float) Math.sqrt(dx * dx + dy * dy));
    }

    private static void ambient(String event, boolean over) {
        long now = System.currentTimeMillis();
        if (over || now < ambientAt) {
            return;
        }
        if (ambientAt != 0) {
            ResourcePack.play(event, gain, true);
        }
        ambientAt = now + AMBIENT_GAP + (long) (Math.random() * AMBIENT_SPREAD);
    }
}
