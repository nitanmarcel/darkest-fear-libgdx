package darkestfear.lwjgl3.debug;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

import javax.microedition.lcdui.DebugBuffers;
import javax.microedition.lcdui.Graphics;

final class BuffersPanel implements DebugPanel {

    private final ImBoolean open = new ImBoolean(false);
    private final ImBoolean flipV = new ImBoolean(true);
    private final ImInt selected = new ImInt(2);
    private final ImString exportDir = new ImString("debug_captures", 512);
    private String lastExport = "";

    @Override
    public String title() {
        return "Buffers";
    }

    @Override
    public ImBoolean open() {
        return open;
    }

    @Override
    public void draw() {
        String[] names = DebugBuffers.names();
        if (selected.get() >= names.length) {
            selected.set(0);
        }
        ImGui.combo("Buffer", selected, names);
        ImGui.sameLine();
        ImGui.checkbox("Flip V", flipV);

        ImGui.setNextItemWidth(220f);
        ImGui.inputText("Dir", exportDir);
        ImGui.sameLine();
        if (ImGui.button("Export")) {
            String path = DebugBuffers.export(selected.get(), exportDir.get());
            lastExport = path != null ? "Saved: " + path : "Export failed";
        }
        if (!lastExport.isEmpty()) {
            ImGui.textDisabled(lastExport);
        }

        int which = selected.get();
        Graphics.setSceneClassifyDebug(which == DebugBuffers.classifyIndex());
        int handle = DebugBuffers.handle(which);
        int w = DebugBuffers.width(which);
        int h = DebugBuffers.height(which);

        ImGui.separator();
        if (handle == 0 || w == 0 || h == 0) {
            ImGui.textDisabled("(" + names[which] + " not available yet)");
            return;
        }

        ImGui.text(names[which] + "  " + w + "x" + h);

        float availW = ImGui.getContentRegionAvailX();
        float availH = ImGui.getContentRegionAvailY();
        if (availW < 16f) {
            availW = 16f;
        }
        if (availH < 16f) {
            availH = 16f;
        }
        float scale = Math.min(availW / w, availH / h);
        float drawW = w * scale;
        float drawH = h * scale;

        if (flipV.get()) {
            ImGui.image(handle, drawW, drawH, 0f, 1f, 1f, 0f);
        } else {
            ImGui.image(handle, drawW, drawH, 0f, 0f, 1f, 1f);
        }
    }
}
