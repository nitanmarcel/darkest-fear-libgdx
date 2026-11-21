package darkestfear.menu;

import com.badlogic.gdx.math.Vector2;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public final class SetupScreen {

    private static final int W = MenuTheme.W;
    private static final int PAD = 14;
    private static final int BUTTON_H = 28;
    private static final int LINE_STEP = 15;
    private static final int GAP = 10;

    public static volatile String problem;
    public static volatile boolean busy;

    private static float buttonX, buttonY;

    private SetupScreen() {
    }

    public static void render(Graphics g) {
        int wide = Graphics.wideWidth();
        int tall = Graphics.wideHeight();
        Font titleFont = MenuTheme.titleFont();
        Font plain = MenuTheme.plain();
        Font bold = MenuTheme.bold();
        int inner = W - 2 * PAD;
        String shownProblem = problem;
        String[] text = MenuOverlay.wrapText(plain, busy ? "Preparing the game..."
                : "Add your own copy of the original phone game to play.\n\n"
                + GameJar.NAME + " " + GameJar.VERSION + " for 240x320 screens (.jar)"
                + (MenuOverlay.isDesktop() ? "\n\nYou can also drop the file onto this window." : ""), inner);
        String[] why = busy || shownProblem == null ? new String[0] : MenuOverlay.wrapText(plain, shownProblem, inner);
        int height = PAD + titleFont.getHeight() + GAP + text.length * LINE_STEP
                + (why.length > 0 ? GAP + why.length * LINE_STEP : 0)
                + (busy ? 0 : GAP + 4 + BUTTON_H) + PAD;
        int x = (wide - W) / 2;
        int y = (tall - height) / 2;

        g.setClip(0, 0, wide, tall);
        g.setColor(MenuTheme.COLOR_BG);
        g.fillRect(0, 0, wide, tall);
        g.setColor(MenuTheme.COLOR_PANEL);
        g.fillRect(x, y, W, height);
        g.setColor(MenuTheme.COLOR_ACCENT);
        g.fillRect(x, y, W, 1);

        int ty = y + PAD;
        g.setFont(titleFont);
        g.setColor(MenuTheme.COLOR_TITLE);
        g.drawString(GameJar.NAME, x + W / 2, ty, Graphics.TOP | Graphics.HCENTER);
        ty += titleFont.getHeight() + GAP;
        g.setFont(plain);
        g.setColor(MenuTheme.COLOR_VALUE);
        for (String line : text) {
            g.drawString(line, x + W / 2, ty, Graphics.TOP | Graphics.HCENTER);
            ty += LINE_STEP;
        }
        if (why.length > 0) {
            ty += GAP;
            g.setColor(MenuTheme.COLOR_SECTION);
            for (String line : why) {
                g.drawString(line, x + W / 2, ty, Graphics.TOP | Graphics.HCENTER);
                ty += LINE_STEP;
            }
        }
        if (busy) {
            buttonY = -1;
            return;
        }
        buttonX = x + PAD;
        buttonY = ty + GAP + 4;
        g.setColor(MenuTheme.COLOR_BAR);
        g.fillRect((int) buttonX, (int) buttonY, inner, BUTTON_H);
        g.setColor(MenuTheme.COLOR_ACCENT);
        g.fillRect((int) buttonX, (int) buttonY, inner, 1);
        g.setFont(bold);
        g.setColor(MenuTheme.COLOR_SELECTED);
        g.drawString("Choose file", x + W / 2, (int) buttonY + (BUTTON_H - bold.getHeight()) / 2 + MenuTheme.TEXT_DROP,
                Graphics.TOP | Graphics.HCENTER);
    }

    public static boolean onButton(int screenX, int screenY) {
        Vector2 p = Graphics.toWide(screenX, screenY);
        return buttonY >= 0 && p.x >= buttonX && p.x < buttonX + W - 2 * PAD && p.y >= buttonY && p.y < buttonY + BUTTON_H;
    }
}
