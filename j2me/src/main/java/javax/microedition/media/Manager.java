package javax.microedition.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.ResourcePack;

public final class Manager {

    public interface PlayerFactory {
        Player createPlayer(InputStream stream, String packExtension) throws IOException, MediaException;
    }

    private static PlayerFactory factory;

    public static void setPlayerFactory(PlayerFactory f) {
        factory = f;
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (factory == null) {
            throw new MediaException("no PlayerFactory set");
        }
        byte[] data = stream.readAllBytes();
        ResourcePack.Audio audio = ResourcePack.audio(data);
        return factory.createPlayer(new ByteArrayInputStream(audio == null ? data : audio.data),
                audio == null ? null : audio.extension);
    }
}
