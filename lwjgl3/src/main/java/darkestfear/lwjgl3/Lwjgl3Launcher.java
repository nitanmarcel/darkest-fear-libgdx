package darkestfear.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import darkestfear.DarkestFearGame;
import darkestfear.GameFiles;
import darkestfear.debug.Debug;
import darkestfear.lwjgl3.audio.PlayerCompat;
import darkestfear.lwjgl3.debug.ImGuiDebugUi;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import javax.microedition.media.Manager;

public class Lwjgl3Launcher implements GameFiles {
    public static void main(String[] args) {
        Manager.setPlayerFactory(PlayerCompat::new);

        if (Boolean.getBoolean("darkestfear.debug")) {
            Debug.install(new ImGuiDebugUi());
        }

        DarkestFearGame game = new DarkestFearGame(new Lwjgl3Launcher());
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DarkestFear");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void filesDropped(String[] dropped) {
                game.open(new File(dropped[0]));
            }
        });
        new Lwjgl3Application(game, config);
    }

    @Override
    public File dir() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA"), "DarkestFear");
        }
        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/DarkestFear");
        }
        String data = System.getenv("XDG_DATA_HOME");
        return new File(data == null || data.isEmpty() ? home + "/.local/share" : data, "DarkestFear");
    }

    @Override
    public void choose(String extension, Chosen chosen) {
        new Thread(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filter = stack.mallocPointer(1);
                filter.put(stack.UTF8("*." + extension)).flip();
                path = TinyFileDialogs.tinyfd_openFileDialog("Choose a file", "", filter, "*." + extension, false);
            }
            if (path != null) {
                chosen.file(new File(path));
            }
        }).start();
    }

    @Override
    public ClassLoader load(File jar) throws Exception {
        return new URLClassLoader(new URL[]{jar.toURI().toURL()}, Lwjgl3Launcher.class.getClassLoader());
    }
}
