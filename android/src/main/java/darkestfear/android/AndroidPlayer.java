package darkestfear.android;

import java.io.InputStream;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

public class AndroidPlayer implements Player {

    public AndroidPlayer(InputStream stream) {
    }

    @Override
    public Control getControl(String controlType) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void prefetch() throws MediaException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void start() throws MediaException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void stop() throws MediaException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void deallocate() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void setLoopCount(int count) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public int getState() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
