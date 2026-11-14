package darkestfear;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public class DarkestFearGame extends ApplicationAdapter {

    private Object midlet;
    private Graphics screen;

    @Override
    public void create() {
        Graphics.init();
        screen = new Graphics(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(new J2meInput());

        try {
            midlet = Class.forName("RMIDlet").getDeclaredConstructor().newInstance();
            midlet.getClass().getMethod("startApp").invoke(midlet);
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.startApp() failed", t);
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        Canvas.renderFrame(screen);
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
        Gdx.app.exit();
        System.exit(0);
    }

    @Override
    public void resize(int width, int height) {
        Graphics.resized(width, height);
    }
}
