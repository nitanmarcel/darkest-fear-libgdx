package darkestfear;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.ScreenUtils;

import darkestfear.debug.Debug;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public class DarkestFearGame extends ApplicationAdapter {

    private Object midlet;
    private Graphics screen;
    private boolean paused;

    @Override
    public void create() {
        Graphics.init();
        screen = new Graphics(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        J2meInput input = new J2meInput();
        Gdx.input.setInputProcessor(new InputMultiplexer(new GestureDetector(input), input));

        try {
            midlet = Class.forName("RMIDlet").getDeclaredConstructor().newInstance();
            midlet.getClass().getMethod("startApp").invoke(midlet);
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.startApp() failed", t);
        }

        Debug.ui().init();
    }

    @Override
    public void render() {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        Canvas.renderFrame(screen);
        Debug.ui().render();
    }

    @Override
    public void pause() {
        if (midlet == null) {
            return;
        }
        try {
            midlet.getClass().getMethod("pauseApp").invoke(midlet);
            paused = true;
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.pauseApp() failed", t);
        }
    }

    @Override
    public void resume() {
        if (midlet == null || !paused) {
            return;
        }
        try {
            midlet.getClass().getMethod("startApp").invoke(midlet);
            paused = false;
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.startApp() failed", t);
        }
    }

    @Override
    public void dispose() {
        Debug.ui().dispose();
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
