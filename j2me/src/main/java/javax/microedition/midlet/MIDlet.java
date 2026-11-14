package javax.microedition.midlet;

import com.badlogic.gdx.Gdx;

public abstract class MIDlet {

    protected MIDlet() {
    }

    protected abstract void startApp() throws MIDletStateChangeException;

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final void notifyPaused() {
    }

    public final void notifyDestroyed() {
        Gdx.app.exit();
        System.exit(0);
    }
}
