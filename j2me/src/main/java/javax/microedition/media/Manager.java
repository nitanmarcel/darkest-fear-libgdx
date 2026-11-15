package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;

public final class Manager {

    public interface PlayerFactory {
        Player createPlayer(InputStream stream) throws IOException, MediaException;
    }

    private static PlayerFactory factory;

    public static void setPlayerFactory(PlayerFactory f) {
        factory = f;
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (factory == null) {
            throw new MediaException("no PlayerFactory set");
        }
        return factory.createPlayer(stream);
    }
}
