package darkestfear.debug;

public interface DebugUi {

    void init();

    void render();

    void dispose();

    DebugUi NOOP = new DebugUi() {
        @Override
        public void init() {
        }

        @Override
        public void render() {
        }

        @Override
        public void dispose() {
        }
    };
}
