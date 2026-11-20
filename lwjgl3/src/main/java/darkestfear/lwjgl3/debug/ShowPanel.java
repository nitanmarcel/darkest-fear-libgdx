package darkestfear.lwjgl3.debug;

import darkestfear.menu.DebugOverlay;

import imgui.ImGui;
import imgui.type.ImBoolean;

final class ShowPanel implements DebugPanel {

    private final ImBoolean open = new ImBoolean(false);

    @Override
    public String title() {
        return "Show";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        if (ImGui.button("All on")) {
            DebugOverlay.setAllFamilies(true);
        }
        ImGui.sameLine();
        if (ImGui.button("All off")) {
            DebugOverlay.setAllFamilies(false);
        }
        ImGui.separator();
        String[] families = DebugOverlay.FAMILIES;
        for (int i = 0; i < families.length; i++) {
            boolean on = DebugOverlay.family(i);
            if (ImGui.checkbox(families[i], on)) {
                DebugOverlay.setFamily(i, !on);
            }
        }
    }
}
