package darkestfear.lwjgl3.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.microedition.media.Control;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

public class PackPlayer implements Player {
    private final FileHandle _File;
    private Music _Music;
    private volatile boolean _Started;

    public PackPlayer(byte[] data, String extension) {
        _File = new FileHandle("sound." + extension) {
            @Override
            public InputStream read() {
                return new ByteArrayInputStream(data);
            }
        };
    }

    @Override
    public Control getControl(String controlType) {
        return (VolumeControl) level -> {
            int clamped = Math.max(0, Math.min(100, level));
            Gdx.app.postRunnable(() -> {
                if (_Music != null)
                    _Music.setVolume(clamped / 100f);
            });
            return clamped;
        };
    }

    @Override
    public void prefetch() {
        Gdx.app.postRunnable(() -> {
            try {
                _Music = Gdx.audio.newMusic(_File);
            }
            catch (Exception e) { }
        });
    }

    @Override
    public void start() {
        _Started = true;
        Gdx.app.postRunnable(() -> {
            if (_Music != null)
                _Music.play();
        });
    }

    @Override
    public void stop() {
        _Started = false;
        Gdx.app.postRunnable(() -> {
            if (_Music != null)
                _Music.stop();
        });
    }

    @Override
    public void deallocate() {
    }

    @Override
    public void close() {
        _Started = false;
        Gdx.app.postRunnable(() -> {
            if (_Music != null)
                _Music.dispose();
            _Music = null;
        });
    }

    @Override
    public void setLoopCount(int count) {
        Gdx.app.postRunnable(() -> {
            if (_Music != null)
                _Music.setLooping(count < 0);
        });
    }

    @Override
    public int getState() {
        return _Started ? 1 : 0;
    }
}
