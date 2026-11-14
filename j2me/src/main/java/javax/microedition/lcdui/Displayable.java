package javax.microedition.lcdui;

public abstract class Displayable {

    public int getWidth() {
        return Graphics.VIRTUAL_WIDTH; //Gdx.graphics.getWidth();
        //throw new UnsupportedOperationException("not implemented");
    }

    public int getHeight() {
        return Graphics.VIRTUAL_HEIGHT; //Gdx.graphics.getHeight();
        //throw new UnsupportedOperationException("not implemented");
    }

    public void addCommand(Command cmd) {
        System.out.println(String.format("Displayable:addCommand %s", cmd));
        //throw new UnsupportedOperationException("not implemented");
    }

    public void setCommandListener(CommandListener l) {
        System.out.println(String.format("Displayable:addCommand %s", l.getClass()));
        //throw new UnsupportedOperationException("not implemented");
    }
}
