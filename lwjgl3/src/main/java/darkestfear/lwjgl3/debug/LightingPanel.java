package darkestfear.lwjgl3.debug;

import imgui.ImGui;
import imgui.type.ImBoolean;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.LightTuning;

final class LightingPanel implements DebugPanel {

    private final ImBoolean open = new ImBoolean(true);
    private final float[] actor = new float[1];
    private final float[] wall = new float[1];
    private final float[] ambient = new float[1];

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
        boolean enhanced = Graphics.isLightingEnabled();
        if (ImGui.checkbox("Enhanced lighting", enhanced)) {
            Graphics.setLightingEnabled(!enhanced);
        }
        ImGui.separatorText("Level");
        if (!enhanced) {
            ImGui.beginDisabled();
        }
        boolean full = LightTuning.ambient >= 1f;
        if (ImGui.checkbox("Full bright", full)) {
            LightTuning.ambient = full ? 0f : 1f;
        }
        ambient[0] = LightTuning.ambient;
        if (ImGui.sliderFloat("Ambient boost", ambient, 0f, 1f)) {
            LightTuning.ambient = ambient[0];
        }
        if (!enhanced) {
            ImGui.endDisabled();
            ImGui.textDisabled("Requires Enhanced lighting.");
        }
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
