package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {

    protected Canvas() {
    }

    public int getGameAction(int keyCode) {
        throw new UnsupportedOperationException("not implemented");
    }

    public final void repaint() {
        throw new UnsupportedOperationException("not implemented");
    }

    public final void serviceRepaints() {
        throw new UnsupportedOperationException("not implemented");
    }

    protected abstract void paint(Graphics g);

    protected void keyPressed(int keyCode) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected void keyReleased(int keyCode) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected void showNotify() {
        throw new UnsupportedOperationException("not implemented");
    }

    protected void hideNotify() {
        throw new UnsupportedOperationException("not implemented");
    }
}
