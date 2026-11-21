package darkestfear.lwjgl3.debug;

import darkestfear.menu.TestRoom;

import imgui.ImGui;
import imgui.type.ImBoolean;

final class TestRoomPanel implements DebugPanel {

    private static final int LEVELS = 15;

    private final ImBoolean open = new ImBoolean(false);
    private final int[] level = {2};

    @Override
    public String title() {
        return "Test Room";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        ImGui.textWrapped("Replaces the target level with a room containing every tile and sprite. "
                + "The room uses the target level's theme.");
        ImGui.sliderInt("Target level", level, 0, LEVELS - 1);
        if (ImGui.button("Load")) {
            TestRoom.load(level[0]);
        }
        ImGui.sameLine();
        if (ImGui.button("Restore")) {
            TestRoom.restore();
        }
        ImGui.textDisabled(TestRoom.loaded() ? "Status: active" : "Status: inactive");
    }
}
