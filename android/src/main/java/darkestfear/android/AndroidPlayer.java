package darkestfear.android;

import android.media.MediaDataSource;
import android.media.MediaPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class AndroidPlayer implements Player {

    private InputStream _Stream;
    private MediaPlayer _Player;

    private byte[] _Data;

    private int _LoopCount = 0;
    private int _RemainingLoops = 0;

    private int _Volume = 127;

    private volatile boolean _Prepared = false;

    private final ExecutorService _Executor = Executors.newSingleThreadExecutor();

    public AndroidPlayer(InputStream stream) {
        _Stream = stream;
    }

    @Override
    public Control getControl(String controlType) {
        return new AndroidVolumeControl(this);
    }

    void setVolume(int level) {
        _Executor.execute(() -> {
            _Volume = Math.max(0, Math.min(127, level));
            applyVolume();
        });
    }

    private void applyVolume() {
        if (_Player == null) {
            return;
        }
        float volume = _Volume / 127.0f;
        try {
            _Player.setVolume(volume, volume);
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public void prefetch() throws MediaException {
        _Executor.execute(() -> {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();

                byte[] buffer = new byte[8192];
                int length;

                while ((length = _Stream.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }

                _Data = output.toByteArray();

                _Player = new MediaPlayer();

                _Player.setDataSource(new MediaDataSource() {

                    @Override
                    public int readAt(long position, byte[] buffer, int offset, int size) {
                        if (position >= _Data.length)
                            return -1;

                        int available = _Data.length - (int) position;
                        int length = Math.min(size, available);

                        System.arraycopy(_Data, (int) position, buffer, offset, length);

                        return length;
                    }

                    @Override
                    public long getSize() {
                        return _Data.length;
                    }

                    @Override
                    public void close() {
                    }
                });

                _Player.setOnCompletionListener(player -> {
                    if (_RemainingLoops > 0) {
                        _RemainingLoops--;

                        player.seekTo(0);
                        player.start();
                    }
                });

                _Player.prepare();
                _Prepared = true;
                applyVolume();

            }
            catch (Exception e) {
            }
        });
    }

    @Override
    public void start() throws MediaException {
        _Executor.execute(() -> {
            if (_Player != null && _Prepared) {
                _RemainingLoops = Math.max(0, _LoopCount - 1);
                _Player.setLooping(_LoopCount < 0);
                _Player.start();
            }
        });
    }

    @Override
    public void stop() throws MediaException {
        _Executor.execute(() -> {
            if (_Player != null && _Prepared && _Player.isPlaying())
                _Player.stop();
        });
    }

    @Override
    public void deallocate() {
        _Executor.execute(() -> {
            if (_Player != null) {
                _Player.release();
                _Player = null;
            }

            _Prepared = false;
        });
    }

    @Override
    public void close() {
        _Executor.execute(() -> {
            if (_Player != null) {
                _Player.release();
                _Player = null;
            }

            _Prepared = false;
            _Data = null;
        });

        _Executor.shutdown();
    }

    @Override
    public void setLoopCount(int count) {
        _Executor.execute(() -> {
            _LoopCount = count;
        });
    }

    @Override
    public int getState() {
        return _Player != null && _Prepared && _Player.isPlaying() ? 1 : 0;
    }
}