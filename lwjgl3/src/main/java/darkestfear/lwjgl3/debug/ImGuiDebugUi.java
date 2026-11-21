package darkestfear.lwjgl3.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;

import darkestfear.debug.DebugUi;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;

import java.util.Arrays;
import java.util.List;

public class ImGuiDebugUi implements DebugUi {

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private InputProcessor suspendedProcessor;

    private final List<DebugPanel> panels = Arrays.asList(
            new StatusPanel(), new ShowPanel(), new LightingPanel(), new BuffersPanel(), new TestRoomPanel(), new SoundsPanel(), new GameplayPanel());
    private final ImBoolean showMetrics = new ImBoolean(false);
    private final ImBoolean showDemo = new ImBoolean(false);
    private boolean visible = true;

    @Override
    public void init() {
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        long windowHandle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.getFonts().addFontDefault();
        io.getFonts().build();
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 150");
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            visible = !visible;
        }

        if (suspendedProcessor != null) {
            Gdx.input.setInputProcessor(suspendedProcessor);
            suspendedProcessor = null;
        }

        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        if (visible) {
            drawMenuBar();
            drawPanels();
            if (showMetrics.get()) {
                ImGui.showMetricsWindow(showMetrics);
            }
            if (showDemo.get()) {
                ImGui.showDemoWindow(showDemo);
            }
        }

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().getWantCaptureKeyboard() || ImGui.getIO().getWantCaptureMouse()) {
            suspendedProcessor = Gdx.input.getInputProcessor();
            Gdx.input.setInputProcessor(null);
        }
    }

    private void drawMenuBar() {
        if (!ImGui.beginMainMenuBar()) {
            return;
        }
        if (ImGui.beginMenu("Windows")) {
            for (DebugPanel panel : panels) {
                ImBoolean open = panel.open();
                if (ImGui.menuItem(panel.title(), "", open.get())) {
                    open.set(!open.get());
                }
            }
            ImGui.endMenu();
        }
        if (ImGui.beginMenu("Tools")) {
            if (ImGui.menuItem("ImGui Metrics", "", showMetrics.get())) {
                showMetrics.set(!showMetrics.get());
            }
            if (ImGui.menuItem("ImGui Demo", "", showDemo.get())) {
                showDemo.set(!showDemo.get());
            }
            ImGui.endMenu();
        }
        ImGui.text("   " + Gdx.graphics.getFramesPerSecond() + " FPS   (F1 to hide)");
        ImGui.endMainMenuBar();
    }

    private void drawPanels() {
        for (DebugPanel panel : panels) {
            if (!panel.open().get()) {
                continue;
            }
            if (ImGui.begin(panel.title(), panel.open())) {
                panel.draw();
            }
            ImGui.end();
        }
    }

    @Override
    public void dispose() {
        for (DebugPanel panel : panels) {
            panel.dispose();
        }
        imGuiGl3 = null;
        imGuiGlfw = null;
        try {
            ImGui.destroyContext();
        } catch (Throwable ignored) {
        }
    }
}
