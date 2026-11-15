package darkestfear;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;

import javax.microedition.lcdui.Canvas;

public class J2meInput extends InputAdapter implements GestureListener {

    @Override public boolean keyDown(int k) { Canvas.dispatchKeyPressed(toJ2me(k)); return true; }
    @Override public boolean keyUp(int k)   { Canvas.dispatchKeyReleased(toJ2me(k)); return true; }
    @Override public boolean scrolled(float amountX, float amountY) { Canvas.dispatchScroll(Math.round(amountY)); return true; }
    @Override public boolean mouseMoved(int x, int y) { Canvas.dispatchHover(x, y); return false; }

    @Override public boolean tap(float x, float y, int count, int button) { return Canvas.dispatchTap((int) x, (int) y); }
    @Override public boolean pan(float x, float y, float deltaX, float deltaY) { return Canvas.dispatchPan(deltaY); }

    @Override public boolean touchDown(float x, float y, int pointer, int button) { return false; }
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean pinch(Vector2 a, Vector2 b, Vector2 c, Vector2 d) { return false; }
    @Override public void pinchStop() { }

    private int toJ2me(int k) {
        switch (k) {
            case Keys.NUM_0: return 48;
            case Keys.NUM_1: return 49;
            case Keys.NUM_2: return 50;
            case Keys.NUM_3: return 51;
            case Keys.NUM_4: return 52;
            case Keys.NUM_5: return 53;
            case Keys.NUM_6: return 54;
            case Keys.NUM_7: return 55;
            case Keys.NUM_8: return 56;
            case Keys.NUM_9: return 57;
            case Keys.Q: return -6;
            case Keys.E: return -7;
            case Keys.ENTER:
            case Keys.SPACE:
                return -5;
            default: return k;
        }
    }
}
