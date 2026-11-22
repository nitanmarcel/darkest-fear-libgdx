package darkestfear.lwjgl3.debug;

import darkestfear.menu.Cheats;

import imgui.ImGui;
import imgui.type.ImBoolean;

final class GameplayPanel implements DebugPanel {

    private final ImBoolean open = new ImBoolean(false);

    @Override
    public String title() {
        return "Gameplay";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        if (ImGui.checkbox("Invulnerability", Cheats.safe())) {
            Cheats.setSafe(!Cheats.safe());
        }
        int[] health = Cheats.health();
        ImGui.textDisabled("Blocks all damage and enemy spawns.");
        ImGui.separator();
        ImGui.text(health == null ? "No active level" : "Health: " + health[0] + " / " + health[1]);
        if (ImGui.button("Damage")) {
            Cheats.hit();
        }
        ImGui.sameLine();
        if (ImGui.button("Heal")) {
            Cheats.heal();
        }
        ImGui.sameLine();
        if (ImGui.button("Skip Cutscene")) {
            Cheats.skipCutscene();
        }
    }
}
