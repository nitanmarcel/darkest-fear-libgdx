package darkestfear.lwjgl3.debug;

import com.badlogic.gdx.Gdx;

import darkestfear.menu.DebugOverlay;
import darkestfear.menu.LightBridge;

import imgui.ImGui;
import imgui.type.ImBoolean;

final class StatusPanel implements DebugPanel {

    private static final int LEVELS = 15;

    private final ImBoolean open = new ImBoolean(true);
    private final int[] level = new int[1];

    @Override
    public String title() {
        return "Status";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        ImGui.text(Gdx.graphics.getFramesPerSecond() + " FPS  (" + Gdx.graphics.getWidth()
                + "x" + Gdx.graphics.getHeight() + ")");
        ImGui.separator();
        String status = DebugOverlay.statusLine();
        if (status != null) {
            ImGui.textWrapped(status);
        } else {
            ImGui.textDisabled("no active level");
        }
        ImGui.separator();
        ImGui.sliderInt("Level", level, 0, LEVELS - 1);
        ImGui.sameLine();
        if (ImGui.button("Go")) {
            LightBridge.jumpToLevel(level[0]);
        }
    }
}
