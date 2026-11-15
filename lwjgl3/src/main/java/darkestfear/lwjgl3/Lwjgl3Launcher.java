package darkestfear.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import darkestfear.DarkestFearGame;
import darkestfear.lwjgl3.audio.PlayerCompat;

import javax.microedition.media.Manager;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Manager.setPlayerFactory(PlayerCompat::new);

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DarkestFear");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new DarkestFearGame(), config);
    }
}
