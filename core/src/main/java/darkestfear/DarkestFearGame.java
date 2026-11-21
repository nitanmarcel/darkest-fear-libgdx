package darkestfear;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.ScreenUtils;

import darkestfear.debug.Debug;
import darkestfear.menu.GameJar;
import darkestfear.menu.SetupScreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

public class DarkestFearGame extends ApplicationAdapter {

    public static final String JAR = "game.jar";

    private final GameFiles files;
    private Object midlet;
    private Graphics screen;
    private boolean paused;

    public DarkestFearGame(GameFiles files) {
        this.files = files;
    }

    @Override
    public void create() {
        Graphics.init();
        screen = new Graphics(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchUp(int x, int y, int pointer, int button) {
                if (SetupScreen.onButton(x, y)) {
                    choose();
                }
                return true;
            }

            @Override
            public boolean keyDown(int k) {
                if (k == Keys.ENTER || k == Keys.SPACE) {
                    choose();
                }
                return true;
            }
        });
        Debug.ui().init();
        File installed = new File(files.dir(), JAR);
        if (installed.exists()) {
            open(installed);
        }
    }

    private void choose() {
        if (!SetupScreen.busy) {
            files.choose("jar", this::open);
        }
    }

    public void open(File source) {
        if (midlet != null || SetupScreen.busy) {
            return;
        }
        SetupScreen.busy = true;
        new Thread(() -> {
            String problem = GameJar.problem(source);
            ClassLoader loader = null;
            if (problem == null) {
                try {
                    File installed = new File(files.dir(), JAR);
                    if (!source.equals(installed)) {
                        install(source, installed);
                    }
                    loader = files.load(installed);
                } catch (Exception e) {
                    Gdx.app.error("DarkestFear", "preparing the game failed", e);
                    problem = "The game couldn't be prepared: " + e.getMessage();
                }
            }
            ClassLoader ready = loader;
            String why = problem;
            Gdx.app.postRunnable(() -> {
                if (ready == null) {
                    SetupScreen.problem = why;
                    SetupScreen.busy = false;
                } else {
                    start(ready);
                }
            });
        }).start();
    }

    private static void install(File source, File installed) throws IOException {
        installed.getParentFile().mkdirs();
        File part = new File(installed.getPath() + ".part");
        try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(part)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        installed.delete();
        if (!part.renameTo(installed)) {
            throw new IOException("can't write " + installed);
        }
    }

    private void start(ClassLoader loader) {
        GameJar.use(loader, new File(files.dir(), JAR));
        GameJar.packChooser = () -> files.choose("zip",
                chosen -> Gdx.app.postRunnable(() -> javax.microedition.lcdui.ResourcePack.install(chosen)));
        J2meInput input = new J2meInput();
        Gdx.input.setCatchKey(Keys.BACK, true);
        Gdx.input.setInputProcessor(new InputMultiplexer(new GestureDetector(input), input));
        com.badlogic.gdx.controllers.Controllers.addListener(new GamepadInput());
        try {
            midlet = Class.forName("RMIDlet", true, loader).getDeclaredConstructor().newInstance();
            midlet.getClass().getMethod("startApp").invoke(midlet);
        } catch (Throwable t) {
            Gdx.app.error("DarkestFear", "RMIDlet.startApp() failed", t);
        }
        SetupScreen.busy = false;
    }

    @Override
    public void render() {
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        if (midlet == null) {
            Graphics.beginWide();
            try {
                SetupScreen.render(screen);
            } finally {
                Graphics.endWide();
            }
            return;
        }
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
