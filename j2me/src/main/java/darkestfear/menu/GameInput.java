package darkestfear.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Preferences;

import javax.microedition.lcdui.Canvas;

public final class GameInput {

    public static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3, PRIMARY = 4, CANCEL = 5;

    private static final int[] CODES = {Keys.UP, Keys.DOWN, Keys.LEFT, Keys.RIGHT, -5};
    private static final int PAUSE_CODE = -6;
    private static final int SKIP_LIMIT = 64;

    private static final int[] DEFAULT_KEYS = {Keys.W, Keys.S, Keys.A, Keys.D, Keys.E, Keys.Q};
    private static final String PREFS = "Controls";

    public static boolean gamepad;

    private static int[] keys;
    private static int capturing = -1;

    private GameInput() {
    }

    private static int[] keys() {
        if (keys == null) {
            keys = DEFAULT_KEYS.clone();
            Preferences prefs = Gdx.app.getPreferences(PREFS);
            for (int i = 0; i < keys.length; i++) {
                keys[i] = prefs.getInteger("key" + i, keys[i]);
            }
        }
        return keys;
    }

    private static void save() {
        Preferences prefs = Gdx.app.getPreferences(PREFS);
        for (int i = 0; i < keys.length; i++) {
            prefs.putInteger("key" + i, keys[i]);
        }
        prefs.flush();
    }

    private static int fixedAction(int key) {
        switch (key) {
            case Keys.UP:
                return UP;
            case Keys.DOWN:
                return DOWN;
            case Keys.LEFT:
                return LEFT;
            case Keys.RIGHT:
                return RIGHT;
            case Keys.ENTER:
            case Keys.SPACE:
                return PRIMARY;
            case Keys.ESCAPE:
            case Keys.BACK:
                return CANCEL;
            default:
                return -1;
        }
    }

    private static int action(int key) {
        int fixed = fixedAction(key);
        if (fixed >= 0) {
            return fixed;
        }
        int[] bound = keys();
        for (int i = 0; i < bound.length; i++) {
            if (bound[i] == key) {
                return i;
            }
        }
        return -1;
    }

    public static boolean keyDown(int key) {
        if (capturing >= 0) {
            if (key == Keys.ESCAPE || key == Keys.BACK) {
                capturing = -1;
            } else if (fixedAction(key) < 0) {
                bind(capturing, key);
                capturing = -1;
            }
            return true;
        }
        int action = action(key);
        if (action >= 0) {
            press(action);
        }
        return action >= 0;
    }

    public static boolean keyUp(int key) {
        int action = action(key);
        if (action >= 0) {
            release(action);
        }
        return action >= 0;
    }

    private static void bind(int action, int key) {
        int[] bound = keys();
        for (int i = 0; i < bound.length; i++) {
            if (bound[i] == key) {
                bound[i] = bound[action];
            }
        }
        bound[action] = key;
        save();
    }

    static void capture(int action) {
        capturing = action;
    }

    static void stopCapture() {
        capturing = -1;
    }

    static boolean capturing(int action) {
        return capturing == action;
    }

    static String keyName(int action) {
        return Keys.toString(keys()[action]);
    }

    static void resetKeys() {
        keys = DEFAULT_KEYS.clone();
        save();
    }

    public static void press(int action) {
        if (action == CANCEL) {
            cancel();
        } else {
            Canvas.dispatchKeyPressed(CODES[action]);
        }
    }

    public static void release(int action) {
        if (action != CANCEL) {
            Canvas.dispatchKeyReleased(CODES[action]);
        }
    }

    private static void cancel() {
        if (MenuOverlay.active()) {
            Canvas.dispatchKeyPressed(Keys.ESCAPE);
        } else if (GameBridge.inLevel()) {
            Canvas.dispatchKeyPressed(PAUSE_CODE);
            Canvas.dispatchKeyReleased(PAUSE_CODE);
        } else if (GameBridge.inCutscene()) {
            skipCutscene();
        } else {
            Canvas.dispatchKeyPressed(CODES[PRIMARY]);
            Canvas.dispatchKeyReleased(CODES[PRIMARY]);
        }
    }

    static void skipCutscene() {
        for (int i = 0; i < SKIP_LIMIT && GameBridge.pendingCutscene(); i++) {
            Canvas.dispatchKeyPressed(CODES[PRIMARY]);
            Canvas.dispatchKeyReleased(CODES[PRIMARY]);
        }
    }
}
