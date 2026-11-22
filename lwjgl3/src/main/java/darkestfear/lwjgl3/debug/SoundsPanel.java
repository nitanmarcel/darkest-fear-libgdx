package darkestfear.lwjgl3.debug;

import darkestfear.menu.LightBridge;
import darkestfear.menu.SoundBoard;

import imgui.ImGui;
import imgui.type.ImBoolean;

import javax.microedition.lcdui.ResourcePack;

final class SoundsPanel implements DebugPanel {

    private static final float EVENT_LIST_HEIGHT = 220f;

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
        ImGui.separatorText("Music");
        int[] ids = SoundBoard.ids();
        if (ids.length == 0) {
            ImGui.textDisabled("No sounds registered.");
            if (ImGui.button("Load")) {
                SoundBoard.load();
            }
        } else {
            ImGui.checkbox("Loop", loop);
            ImGui.sameLine();
            if (ImGui.button("Stop")) {
                SoundBoard.stop();
            }
        }
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

        ImGui.separatorText("Pack events");
        String[] events = ResourcePack.events();
        if (events.length == 0) {
            ImGui.textDisabled("No enabled pack has events.");
            return;
        }
        ImGui.beginChild("events", 0f, EVENT_LIST_HEIGHT, true);
        for (String event : events) {
            if (ImGui.button("Play##" + event)) {
                ResourcePack.play(event, LightBridge.soundLevel() / 100f, false);
            }
            ImGui.sameLine();
            ImGui.text(event);
        }
        ImGui.endChild();
        ImGui.separatorText("Last played");
        for (String event : ResourcePack.played()) {
            ImGui.text(event);
        }
    }
}
