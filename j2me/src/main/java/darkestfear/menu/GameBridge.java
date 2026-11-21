package darkestfear.menu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

public final class GameBridge {

    private static boolean ready;
    private static boolean failed;

    private static Class<?> menuCls;
    private static Class<?> levelCls;
    private static Class<?> cutsceneCls;
    private static Class<?> popupCls;
    private static Class<?> overlayCls;
    private static Field fOverlayEvent;
    private static Field fOverlayRedraw;
    private static Field fPendingScreen;

    private static Method mGetCurrentScreen;
    private static Method mEnqueue;
    private static Method mShow;
    private static Method mExit;
    private static Method mLoad;
    private static Method mGetText;
    private static final int MENU_MUSIC = 28;

    private static Method mSetSound;
    private static Method mLoadSounds;
    private static Method mPlaySound;
    private static Field fController;
    private static Method mSetSelected;
    private static Field fMenuId;
    private static Field fCanContinue;
    private static Field fSoundEnabled;

    private static Field fMenuItems;
    private static Field fMenuTitle;
    private static Field fMenuSelected;
    private static Field fMenuSoftLabels;
    private static Field fItemOptions;
    private static Field fItemIndex;
    private static Field fItemEnabled;
    private static Field fItemType;
    private static Field fPopupUnder;
    private static Field fPopupBlocks;
    private static Field fTextIcons;
    private static Field fCaptionTop, fCaptionShown, fCaptionTexts, fCaptionIndex;
    private static Field fTextLines;
    private static Field fTextFont;
    private static Field fTextHeight;
    private static Field fMenuScroll;
    private static Field fMenuViewTop;
    private static Field fMenuViewHeight;
    private static Field fMenuTitleTop;

    private GameBridge() {
    }

    static synchronized void init() {
        if (ready || failed) {
            return;
        }
        try {
            Class<?> appCls = Class.forName("l", true, GameJar.loader());
            menuCls = Class.forName("f", true, GameJar.loader());
            levelCls = Class.forName("i", false, GameJar.loader());
            cutsceneCls = Class.forName("p", false, GameJar.loader());
            popupCls = Class.forName("o", false, GameJar.loader());
            overlayCls = Class.forName("a", false, GameJar.loader());
            fOverlayEvent = findField(overlayCls, "a", int.class);
            fOverlayEvent.setAccessible(true);
            fOverlayRedraw = findField(overlayCls, "b", boolean.class);
            fOverlayRedraw.setAccessible(true);
            Class<?> screenCls = Class.forName("r", true, GameJar.loader());
            fPendingScreen = findField(appCls, "b", screenCls);
            fPendingScreen.setAccessible(true);
            Class<?> resCls = Class.forName("b", true, GameJar.loader());
            Class<?> gcCls = Class.forName("q", true, GameJar.loader());
            Class<?> soundCls = Class.forName("k", true, GameJar.loader());
            Class<?> itemCls = Class.forName("h", true, GameJar.loader());
            Class<?> textCls = Class.forName("d", true, GameJar.loader());

            mGetCurrentScreen = find(appCls, "a", new Class<?>[]{}, screenCls, true);
            mEnqueue = appCls.getDeclaredMethod("a", int.class, Object.class);
            mShow = appCls.getDeclaredMethod("a", screenCls);
            mExit = find(appCls, "a", new Class<?>[]{}, void.class, true);
            mLoad = find(menuCls, "a", new Class<?>[]{int.class}, menuCls, true);
            mGetText = find(resCls, "a", new Class<?>[]{int.class}, String.class, true);
            mSetSound = find(soundCls, "a", new Class<?>[]{boolean.class}, void.class, true);
            mPlaySound = find(soundCls, "a", new Class<?>[]{int.class, int.class}, void.class, true);
            mLoadSounds = find(gcCls, "c", new Class<?>[]{}, void.class, false);
            fController = findField(menuCls, "a", gcCls);
            mSetSelected = find(menuCls, "b", new Class<?>[]{int.class}, void.class, false);
            fMenuId = menuCls.getDeclaredField("f");
            fCanContinue = findField(gcCls, "d", boolean.class);
            fSoundEnabled = findField(soundCls, "a", boolean.class);

            fMenuItems = findField(menuCls, "a", Vector.class);
            fMenuTitle = findField(menuCls, "a", String.class);
            fMenuSelected = menuCls.getDeclaredField("g");
            fMenuSoftLabels = findField(menuCls, "b", String[].class);
            fItemOptions = findArrayField(itemCls, "a", textCls);
            fItemIndex = findField(itemCls, "a", int.class);
            fItemEnabled = findField(itemCls, "a", boolean.class);
            fItemType = findField(itemCls, "a", byte.class);
            fPopupUnder = findField(popupCls, "a", screenCls);
            fPopupBlocks = findArrayField(popupCls, "a", textCls);
            fTextIcons = findField(textCls, "a", int[].class);
            fPopupUnder.setAccessible(true);
            fPopupBlocks.setAccessible(true);
            fTextIcons.setAccessible(true);
            fCaptionTop = findField(cutsceneCls, "e", int.class);
            fCaptionShown = findField(cutsceneCls, "d", boolean.class);
            fCaptionTexts = findField(cutsceneCls, "n", int[].class);
            fCaptionIndex = findField(cutsceneCls, "w", int.class);
            for (Field f : new Field[]{fCaptionTop, fCaptionShown, fCaptionTexts, fCaptionIndex}) {
                f.setAccessible(true);
            }
            fTextLines = findField(textCls, "a", String[].class);
            fTextFont = findField(textCls, "a", javax.microedition.lcdui.Font.class);
            fTextHeight = textCls.getDeclaredField("e");
            fMenuScroll = menuCls.getDeclaredField("p");
            fMenuViewTop = menuCls.getDeclaredField("t");
            fMenuViewHeight = menuCls.getDeclaredField("v");
            fMenuTitleTop = menuCls.getDeclaredField("r");

            for (Method m : new Method[]{mGetCurrentScreen, mEnqueue, mShow, mExit, mLoad, mGetText,
                    mSetSound, mSetSelected}) {
                m.setAccessible(true);
            }
            for (Field f : new Field[]{fMenuId, fCanContinue, fSoundEnabled, fMenuItems, fMenuTitle,
                    fMenuSelected, fMenuSoftLabels, fItemOptions, fItemIndex, fItemEnabled, fItemType, fTextLines,
                    fTextFont, fTextHeight, fMenuScroll, fMenuViewTop, fMenuViewHeight,
                    fMenuTitleTop}) {
                f.setAccessible(true);
            }

            ready = true;
        } catch (Throwable t) {
            failed = true;
        }
    }

    static boolean available() {
        init();
        return ready;
    }

    static int currentMenuId() {
        if (!available()) {
            return -1;
        }
        try {
            Object screen = mGetCurrentScreen.invoke(null);
            if (screen != null && menuCls.isInstance(screen)) {
                return fMenuId.getInt(screen);
            }
        } catch (Throwable t) {
        }
        return -1;
    }

    public static boolean inLevel() {
        return currentScreenIs(levelCls);
    }

    public static boolean inCutscene() {
        return currentScreenIs(cutsceneCls);
    }

    public static boolean inOverlay() {
        return currentScreenIs(overlayCls);
    }

    static boolean inPopup() {
        return currentScreenIs(popupCls);
    }

    public static Object popupLevel() {
        if (!available()) {
            return null;
        }
        try {
            Object screen = mGetCurrentScreen.invoke(null);
            if (screen == null || !popupCls.isInstance(screen)) {
                return null;
            }
            Object under = fPopupUnder.get(screen);
            return under != null && levelCls.isInstance(under) ? under : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static String[] popupLines() {
        try {
            Object[] blocks = (Object[]) fPopupBlocks.get(mGetCurrentScreen.invoke(null));
            java.util.List<String> out = new java.util.ArrayList<>();
            for (Object block : blocks) {
                String[] lines = (String[]) fTextLines.get(block);
                if (lines != null) {
                    out.addAll(java.util.Arrays.asList(lines));
                }
            }
            return out.toArray(new String[0]);
        } catch (Throwable t) {
            return EMPTY;
        }
    }

    static int popupPortrait() {
        try {
            Object[] blocks = (Object[]) fPopupBlocks.get(mGetCurrentScreen.invoke(null));
            int[] icons = blocks.length > 0 ? (int[]) fTextIcons.get(blocks[0]) : null;
            return icons != null && icons.length > 0 ? icons[0] : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    static int captionTop() {
        try {
            return fCaptionTop.getInt(mGetCurrentScreen.invoke(null));
        } catch (Throwable t) {
            return -1;
        }
    }

    static String caption() {
        try {
            Object scene = mGetCurrentScreen.invoke(null);
            if (!fCaptionShown.getBoolean(scene)) {
                return null;
            }
            int[] texts = (int[]) fCaptionTexts.get(scene);
            int index = fCaptionIndex.getInt(scene);
            return texts != null && index >= 0 && index < texts.length ? text(texts[index]) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean pendingCutscene() {
        if (!available()) {
            return false;
        }
        try {
            Object screen = fPendingScreen.get(null);
            return screen != null && cutsceneCls.isInstance(screen);
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean inDialog() {
        if (!available()) {
            return false;
        }
        try {
            Object screen = mGetCurrentScreen.invoke(null);
            return screen != null && overlayCls.isInstance(screen) && fOverlayEvent.getInt(screen) != -1;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean currentScreenIs(Class<?> type) {
        if (!available()) {
            return false;
        }
        try {
            Object screen = mGetCurrentScreen.invoke(null);
            return screen != null && type.isInstance(screen);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void keepOverlay() {
        if (!available()) {
            return;
        }
        try {
            Object screen = mGetCurrentScreen.invoke(null);
            if (screen != null && overlayCls.isInstance(screen)) {
                fOverlayRedraw.setBoolean(screen, true);
            }
        } catch (Throwable t) {
        }
    }

    static boolean soundEnabled() {
        if (!available()) {
            return false;
        }
        try {
            return fSoundEnabled.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    static void setSoundEnabled(boolean enabled) {
        if (!available()) {
            return;
        }
        try {
            mSetSound.invoke(null, enabled);
            mLoadSounds.setAccessible(true);
            mPlaySound.setAccessible(true);
            fController.setAccessible(true);
            mLoadSounds.invoke(fController.get(null));
            mPlaySound.invoke(null, MENU_MUSIC, -1);
        } catch (Throwable t) {
        }
    }

    static boolean canContinue() {
        if (!available()) {
            return false;
        }
        try {
            return fCanContinue.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    static String text(int id) {
        if (!available()) {
            return null;
        }
        try {
            return (String) mGetText.invoke(null, id);
        } catch (Throwable t) {
            return null;
        }
    }

    static void enqueueEvent(int event) {
        if (!available()) {
            return;
        }
        try {
            mEnqueue.invoke(null, event, null);
        } catch (Throwable t) {
        }
    }

    static void openMenu(int menuId) {
        if (!available()) {
            return;
        }
        try {
            Object menu = mLoad.invoke(null, menuId);
            if (menu != null) {
                mShow.invoke(null, menu);
            }
        } catch (Throwable t) {
        }
    }

    static Object currentMenu() {
        if (!available()) {
            return null;
        }
        try {
            Object screen = mGetCurrentScreen.invoke(null);
            return screen != null && menuCls.isInstance(screen) ? screen : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static String menuTitle(Object menu) {
        try {
            return (String) fMenuTitle.get(menu);
        } catch (Throwable t) {
            return null;
        }
    }

    static int menuSelected(Object menu) {
        try {
            return fMenuSelected.getInt(menu);
        } catch (Throwable t) {
            return 0;
        }
    }

    static int menuItemCount(Object menu) {
        try {
            return ((Vector<?>) fMenuItems.get(menu)).size();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static final String[] EMPTY = new String[0];

    static String[] menuItemLines(Object menu, int index) {
        try {
            Object item = ((Vector<?>) fMenuItems.get(menu)).elementAt(index);
            Object[] options = (Object[]) fItemOptions.get(item);
            int optIndex = fItemIndex.getInt(item);
            if (optIndex < 0 || optIndex >= options.length) {
                optIndex = 0;
            }
            String[] lines = (String[]) fTextLines.get(options[optIndex]);
            return lines != null ? lines : EMPTY;
        } catch (Throwable t) {
            return EMPTY;
        }
    }

    static javax.microedition.lcdui.Font menuItemFont(Object menu, int index) {
        try {
            Object item = ((Vector<?>) fMenuItems.get(menu)).elementAt(index);
            Object[] options = (Object[]) fItemOptions.get(item);
            int optIndex = fItemIndex.getInt(item);
            if (optIndex < 0 || optIndex >= options.length) {
                optIndex = 0;
            }
            return (javax.microedition.lcdui.Font) fTextFont.get(options[optIndex]);
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean menuItemEnabled(Object menu, int index) {
        try {
            Object item = ((Vector<?>) fMenuItems.get(menu)).elementAt(index);
            return fItemEnabled.getBoolean(item);
        } catch (Throwable t) {
            return true;
        }
    }

    static int menuItemType(Object menu, int index) {
        try {
            Object item = ((Vector<?>) fMenuItems.get(menu)).elementAt(index);
            return fItemType.getByte(item);
        } catch (Throwable t) {
            return 1;
        }
    }

    static void setMenuView(Object menu, int top, int height) {
        try {
            fMenuViewTop.setInt(menu, top);
            fMenuViewHeight.setInt(menu, height);
        } catch (Throwable t) {
        }
    }

    static int menuItemHeight(Object menu, int index) {
        try {
            Object item = ((Vector<?>) fMenuItems.get(menu)).elementAt(index);
            Object[] options = (Object[]) fItemOptions.get(item);
            int optIndex = fItemIndex.getInt(item);
            if (optIndex < 0 || optIndex >= options.length) {
                optIndex = 0;
            }
            return fTextHeight.getInt(options[optIndex]) + 4;
        } catch (Throwable t) {
            return 0;
        }
    }

    static int menuScroll(Object menu) {
        try {
            return fMenuScroll.getInt(menu);
        } catch (Throwable t) {
            return 0;
        }
    }

    static int menuViewTop(Object menu) {
        try {
            return fMenuViewTop.getInt(menu);
        } catch (Throwable t) {
            return 84;
        }
    }

    static int menuTitleTop(Object menu) {
        try {
            return fMenuTitleTop.getInt(menu);
        } catch (Throwable t) {
            return 0;
        }
    }

    static int menuViewHeight(Object menu) {
        try {
            return fMenuViewHeight.getInt(menu);
        } catch (Throwable t) {
            return javax.microedition.lcdui.Graphics.CLASSIC_HEIGHT - 110;
        }
    }

    static void setMenuScroll(Object menu, int value) {
        try {
            fMenuScroll.setInt(menu, value);
        } catch (Throwable t) {
        }
    }

    static String menuSoftLabel(Object menu, int slot) {
        try {
            String[] labels = (String[]) fMenuSoftLabels.get(menu);
            return labels != null && slot < labels.length ? labels[slot] : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static void menuSetSelected(Object menu, int index) {
        try {
            mSetSelected.invoke(menu, index);
        } catch (Throwable t) {
        }
    }

    private static Field findArrayField(Class<?> owner, String name, Class<?> componentType) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getName().equals(name) && f.getType().isArray()
                    && f.getType().getComponentType() == componentType) {
                return f;
            }
        }
        throw new RuntimeException("array field not found: " + name);
    }

    private static Method find(Class<?> owner, String name, Class<?>[] params, Class<?> ret, boolean mustBeStatic) {
        for (Method m : owner.getDeclaredMethods()) {
            if (!m.getName().equals(name) || m.getReturnType() != ret) {
                continue;
            }
            if (mustBeStatic && !Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Class<?>[] pt = m.getParameterTypes();
            if (pt.length != params.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < pt.length; i++) {
                if (pt[i] != params[i]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return m;
            }
        }
        throw new RuntimeException("method not found: " + name);
    }

    private static Field findField(Class<?> owner, String name, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getName().equals(name) && f.getType() == type) {
                return f;
            }
        }
        throw new RuntimeException("field not found: " + name);
    }
}
