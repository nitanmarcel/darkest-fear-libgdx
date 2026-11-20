package darkestfear.lwjgl3.debug;

import imgui.type.ImBoolean;

interface DebugPanel {

    String title();

    ImBoolean open();

    void draw();

    default void dispose() {
    }
}
