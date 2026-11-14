package javax.microedition.media;

public interface Player extends Controllable {

    void prefetch() throws MediaException;

    void start() throws MediaException;

    void stop() throws MediaException;

    void deallocate();

    void close();

    void setLoopCount(int count);

    int getState();
}
