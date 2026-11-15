package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

final class GlThread {
    private static volatile Thread glThread;

    private GlThread() {}

    public static void init() {
        glThread = Thread.currentThread();
    }

    static <T> T call(final Callable<T> work) {
        if (Thread.currentThread() == glThread) {
            return run(work);
        }
        final Object[] result = new Object[1];
        final Throwable[] error = new Throwable[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Gdx.app.postRunnable(() -> {
            glThread = Thread.currentThread();
            try {
                result[0] = work.call();
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (error[0] != null) throw new RuntimeException(error[0]);
        @SuppressWarnings("unchecked")
        T t = (T) result[0];
        return t;
    }

    private static <T> T run(Callable<T> work) {
        try {
            return work.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
