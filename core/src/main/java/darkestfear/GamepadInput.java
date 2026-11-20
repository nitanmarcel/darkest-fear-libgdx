package darkestfear;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.ControllerMapping;

import darkestfear.menu.GameInput;

public class GamepadInput extends ControllerAdapter {

    private static final float STICK_ON = 0.5f;
    private static final float STICK_OFF = 0.35f;
    private static final float STICKY = 1.1f;

    private int stick = -1;

    public GamepadInput() {
        GameInput.gamepad = com.badlogic.gdx.controllers.Controllers.getControllers().size > 0;
    }

    @Override
    public void connected(Controller controller) {
        GameInput.gamepad = true;
    }

    @Override
    public boolean buttonDown(Controller controller, int button) {
        int action = action(controller.getMapping(), button);
        if (action >= 0) {
            GameInput.press(action);
        }
        return action >= 0;
    }

    @Override
    public boolean buttonUp(Controller controller, int button) {
        int action = action(controller.getMapping(), button);
        if (action >= 0) {
            GameInput.release(action);
        }
        return action >= 0;
    }

    @Override
    public boolean axisMoved(Controller controller, int axis, float value) {
        ControllerMapping m = controller.getMapping();
        if (axis != m.axisLeftX && axis != m.axisLeftY) {
            return false;
        }
        float x = controller.getAxis(m.axisLeftX), y = controller.getAxis(m.axisLeftY);
        float ax = Math.abs(x), ay = Math.abs(y);
        int direction = -1;
        if (Math.max(ax, ay) >= (stick >= 0 ? STICK_OFF : STICK_ON)) {
            boolean wasHorizontal = stick == GameInput.LEFT || stick == GameInput.RIGHT;
            boolean horizontal = stick < 0 ? ax > ay : wasHorizontal ? ax * STICKY > ay : ax > ay * STICKY;
            direction = horizontal ? (x < 0 ? GameInput.LEFT : GameInput.RIGHT) : (y < 0 ? GameInput.UP : GameInput.DOWN);
        }
        if (direction != stick) {
            if (stick >= 0) {
                GameInput.release(stick);
            }
            if (direction >= 0) {
                GameInput.press(direction);
            }
            stick = direction;
        }
        return true;
    }

    @Override
    public void disconnected(Controller controller) {
        GameInput.gamepad = com.badlogic.gdx.controllers.Controllers.getControllers().size > 0;
        if (stick >= 0) {
            GameInput.release(stick);
            stick = -1;
        }
    }

    private static int action(ControllerMapping m, int button) {
        if (button == m.buttonA) {
            return GameInput.PRIMARY;
        }
        if (button == m.buttonB || button == m.buttonStart) {
            return GameInput.CANCEL;
        }
        if (button == m.buttonDpadUp) {
            return GameInput.UP;
        }
        if (button == m.buttonDpadDown) {
            return GameInput.DOWN;
        }
        if (button == m.buttonDpadLeft) {
            return GameInput.LEFT;
        }
        if (button == m.buttonDpadRight) {
            return GameInput.RIGHT;
        }
        return -1;
    }
}
