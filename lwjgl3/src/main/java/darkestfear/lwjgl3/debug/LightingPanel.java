package darkestfear.lwjgl3.debug;

import imgui.ImGui;
import imgui.type.ImBoolean;

import javax.microedition.lcdui.LightTuning;

final class LightingPanel implements DebugPanel {

    private final ImBoolean open = new ImBoolean(true);
    private final float[] actor = new float[1];
    private final float[] wall = new float[1];

    @Override
    public String title() {
        return "Lighting";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        ImGui.separatorText("Brightness");
        actor[0] = LightTuning.actorPixel;
        if (ImGui.sliderFloat("Actors lit per pixel", actor, 0f, 1f)) {
            LightTuning.actorPixel = actor[0];
        }
        wall[0] = LightTuning.wallSoft;
        if (ImGui.sliderFloat("Wall light softness", wall, 0f, 1f)) {
            LightTuning.wallSoft = wall[0];
        }
    }
}
