// Scale2x applied twice: rounds exact-colour corners without blending, so every output pixel is a source pixel.
// Algorithm by Andrea Mazzoleni (scale2x.it), implemented here from its published rules.
package javax.microedition.lcdui;

import com.badlogic.gdx.graphics.Pixmap;

final class SpriteUpscaler {

    static final int SCALE = 4;

    private SpriteUpscaler() {
    }

    static Pixmap upscale(Pixmap sheet, int[] frames) {
        int sw = sheet.getWidth(), sh = sheet.getHeight();
        Pixmap out = new Pixmap(sw * SCALE, sh * SCALE, Pixmap.Format.RGBA8888);
        out.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < sh; y++) {
            for (int x = 0; x < sw; x++) {
                out.setColor(sheet.getPixel(x, y));
                out.fillRectangle(x * SCALE, y * SCALE, SCALE, SCALE);
            }
        }
        for (int f = 0; f + 4 < frames.length; f += 5) {
            int fx = Math.max(0, frames[f]), fy = Math.max(0, frames[f + 1]);
            int fw = Math.min(frames[f + 2], sw - fx), fh = Math.min(frames[f + 3], sh - fy);
            if (fw <= 0 || fh <= 0) {
                continue;
            }
            int m = frames[f + 4] != 0 ? 1 : 0;
            int w = fw + 2 * m, h = fh + 2 * m;
            int[] src = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = sheet.getPixel(fx + Math.max(0, Math.min(fw - 1, x - m)),
                            fy + Math.max(0, Math.min(fh - 1, y - m)));
                    if ((c & 255) != 0) {
                        src[y * w + x] = c;
                    }
                }
            }
            int[] four = scale2x(scale2x(src, w, h), w * 2, h * 2);
            int ow = w * SCALE, skip = m * SCALE;
            for (int y = 0; y < fh * SCALE; y++) {
                for (int x = 0; x < fw * SCALE; x++) {
                    out.drawPixel(fx * SCALE + x, fy * SCALE + y, four[(y + skip) * ow + x + skip]);
                }
            }
        }
        return out;
    }

    private static int[] scale2x(int[] src, int w, int h) {
        int[] out = new int[4 * w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int E = src[y * w + x];
                int B = y > 0 ? src[(y - 1) * w + x] : 0, H = y < h - 1 ? src[(y + 1) * w + x] : 0;
                int D = x > 0 ? src[y * w + x - 1] : 0, F = x < w - 1 ? src[y * w + x + 1] : 0;
                boolean corners = B != H && D != F;
                int o = 2 * y * 2 * w + 2 * x;
                out[o] = corners && D == B ? D : E;
                out[o + 1] = corners && B == F ? F : E;
                out[o + 2 * w] = corners && D == H ? D : E;
                out[o + 2 * w + 1] = corners && H == F ? F : E;
            }
        }
        return out;
    }
}
