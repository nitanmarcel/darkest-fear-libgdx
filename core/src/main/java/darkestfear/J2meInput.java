package darkestfear;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import javax.microedition.lcdui.Canvas;

public class J2meInput extends InputAdapter {
    @Override public boolean keyDown(int k) { Canvas.dispatchKeyPressed(toJ2me(k)); return true; }
    @Override public boolean keyUp(int k)   { Canvas.dispatchKeyReleased(toJ2me(k)); return true; }
    @Override public boolean scrolled(float amountX, float amountY) { Canvas.dispatchScroll(Math.round(amountY)); return true; }

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
