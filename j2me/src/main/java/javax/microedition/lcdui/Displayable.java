package javax.microedition.lcdui;

public abstract class Displayable {

    public int getWidth() {
        return Graphics.CLASSIC_WIDTH;
        //throw new UnsupportedOperationException("not implemented");
    }

    public int getHeight() {
        return Graphics.CLASSIC_HEIGHT;
        //throw new UnsupportedOperationException("not implemented");
    }
}
