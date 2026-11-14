package darkestfear;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;

public class DarkestFearGame extends ApplicationAdapter {
    private Object midlet;

    @Override
    public void create() {
        try {
            midlet = Class.forName("RMIDlet").getDeclaredConstructor().newInstance();
            midlet.getClass().getMethod("startApp").invoke(midlet);
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.startApp() failed", t);
        }
    }

    @Override
    public void pause() {
        if (midlet == null) {
            return;
        }
        try {
            midlet.getClass().getMethod("pauseApp").invoke(midlet);
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.pauseApp() failed", t);
        }
    }

    @Override
    public void dispose() {
        if (midlet == null) {
            return;
        }
        try {
            midlet.getClass().getMethod("destroyApp", boolean.class).invoke(midlet, true);
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.destroyApp() failed", t);
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
    }
}
