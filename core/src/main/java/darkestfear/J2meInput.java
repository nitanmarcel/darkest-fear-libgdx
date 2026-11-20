package darkestfear;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;

import darkestfear.menu.GameInput;

import javax.microedition.lcdui.Canvas;

public class J2meInput extends InputAdapter implements GestureListener {

    @Override
    public boolean keyDown(int k) {
        if (!GameInput.keyDown(k)) {
            Canvas.dispatchKeyPressed(phoneKey(k));
        }
        return true;
    }

    @Override
    public boolean keyUp(int k) {
        if (!GameInput.keyUp(k)) {
            Canvas.dispatchKeyReleased(phoneKey(k));
        }
        return true;
    }
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

    private static int phoneKey(int k) {
        return k >= Keys.NUM_0 && k <= Keys.NUM_9 ? '0' + k - Keys.NUM_0 : k;
    }
}
