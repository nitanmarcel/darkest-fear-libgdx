package javax.microedition.lcdui;

import com.badlogic.gdx.Input.Keys;

import darkestfear.menu.GameControls;
import darkestfear.menu.MenuOverlay;

public abstract class Canvas extends Displayable {

    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;

    private static final Object FRAME_LOCK = new Object();
    private static long frameCount;

    protected Canvas() {
    }

    protected abstract void paint(Graphics g);

    public final void repaint() {
    }

    public final void repaint(int x, int y, int width, int height) {
    }

    public final void serviceRepaints() {
        long target;
        synchronized (FRAME_LOCK) {
            target = frameCount + 1;
        }
        synchronized (FRAME_LOCK) {
            while (frameCount < target) {
                try {
                    FRAME_LOCK.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static void renderFrame(Graphics g) {
        MenuOverlay.applyPending();
        Displayable d = Display.current();
        g.begin();
        try {
            if (MenuOverlay.active()) {
                MenuOverlay.render(g);
            } else if (d instanceof Canvas) {
                ((Canvas) d).paint(g);
            }
        } finally {
            g.end();
        }
        if (GameControls.active()) {
            GameControls.poll();
            GameControls.renderHud(g);
        } else {
            GameControls.reset();
        }
        synchronized (FRAME_LOCK) {
            frameCount++;
            FRAME_LOCK.notifyAll();
        }
    }

    public static void dispatchKeyPressed(int keyCode) {
        if (MenuOverlay.active() && MenuOverlay.keyPressed(keyCode)) {
            return;
        }
        Displayable d = Display.current();
        if (d instanceof Canvas) {
            ((Canvas) d).keyPressed(keyCode);
        }
    }

    public static void dispatchScroll(int amountY) {
        if (MenuOverlay.active()) {
            MenuOverlay.scroll(amountY);
        }
    }

    public static boolean dispatchTap(int screenX, int screenY) {
        if (MenuOverlay.active()) {
            MenuOverlay.tap(screenX, screenY);
            return true;
        }
        return false;
    }

    public static boolean dispatchPan(float screenDeltaY) {
        if (MenuOverlay.active()) {
            MenuOverlay.pan(screenDeltaY);
            return true;
        }
        return false;
    }

    public static void dispatchHover(int screenX, int screenY) {
        if (MenuOverlay.active()) {
            MenuOverlay.hover(screenX, screenY);
        }
    }

    public static void dispatchKeyReleased(int keyCode) {
        if (MenuOverlay.active() && MenuOverlay.consumesInput()) {
            return;
        }
        Displayable d = Display.current();
        if (d instanceof Canvas) {
            ((Canvas) d).keyReleased(keyCode);
        }
    }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
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
            case Keys.CENTER:
                return FIRE;
            default:
                return 0;
        }
    }

    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case UP:
                return Keys.UP;
            case DOWN:
                return Keys.DOWN;
            case LEFT:
                return Keys.LEFT;
            case RIGHT:
                return Keys.RIGHT;
            case FIRE:
                return Keys.ENTER;
            default:
                return 0;
        }
    }

    public boolean hasRepeatEvents() {
        return true;
    }

    public boolean isDoubleBuffered() {
        return true;
    }

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    protected void pointerPressed(int x, int y) {
    }

    protected void pointerReleased(int x, int y) {
    }

    protected void pointerDragged(int x, int y) {
    }

    protected void showNotify() {
    }

    protected void hideNotify() {
    }
}
