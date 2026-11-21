package darkestfear.lwjgl3.debug;

import darkestfear.menu.SoundBoard;

import imgui.ImGui;
import imgui.type.ImBoolean;

final class SoundsPanel implements DebugPanel {

    private final ImBoolean open = new ImBoolean(false);
    private final ImBoolean loop = new ImBoolean(false);

    @Override
    public String title() {
        return "Sounds";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        int[] ids = SoundBoard.ids();
        if (ids.length == 0) {
            ImGui.textDisabled("No sounds registered.");
            if (ImGui.button("Load")) {
                SoundBoard.load();
            }
            return;
        }
        ImGui.checkbox("Loop", loop);
        ImGui.sameLine();
        if (ImGui.button("Stop")) {
            SoundBoard.stop();
        }
        ImGui.separator();
        for (int id : ids) {
            if (id < 0) {
                continue;
            }
            if (ImGui.button("Play##" + id)) {
                SoundBoard.play(id, loop.get());
            }
            ImGui.sameLine();
            ImGui.text("Sound " + id);
        }
    }
}
