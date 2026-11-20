package darkestfear.debug;

public final class Debug {

    private static boolean enabled;
    private static DebugUi ui = DebugUi.NOOP;

    private Debug() {
    }

    public static void install(DebugUi impl) {
        ui = impl != null ? impl : DebugUi.NOOP;
        enabled = impl != null;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static DebugUi ui() {
        return ui;
    }
}
