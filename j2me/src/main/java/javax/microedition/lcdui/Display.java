package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;

public class Display {

    private static final Display INSTANCE = new Display();

    private static Displayable current;

    private Display() {
    }

    public static Display getDisplay(MIDlet m) {
        return INSTANCE;
    }

    public void setCurrent(Displayable next) {
        current = next;
    }

    public Displayable getCurrent() {
        return current;
    }

    static Displayable current() {
        return current;
    }
}
