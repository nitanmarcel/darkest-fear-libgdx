package javax.microedition.lcdui;

public abstract class Displayable {

    public int getWidth() {
        throw new UnsupportedOperationException("not implemented");
    }

    public int getHeight() {
        throw new UnsupportedOperationException("not implemented");
    }

    public void addCommand(Command cmd) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void setCommandListener(CommandListener l) {
        throw new UnsupportedOperationException("not implemented");
    }
}
