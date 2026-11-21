// Scale2x applied twice: rounds exact-colour corners without blending, so every output pixel is a source pixel.
// Algorithm by Andrea Mazzoleni (scale2x.it), implemented here from its published rules.
package javax.microedition.lcdui;

import com.badlogic.gdx.graphics.Pixmap;

final class SpriteUpscaler {

    static final int SCALE = 4;

    private static final float BEVEL = 10f, SLOPE = 3f;
    static final float SHAPE_RANGE = 16f;
    private static final int THIN_WINDOW = 8;
    private static final float THIN = 6f;

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

    /**
     * Describes each sprite's surface for lighting, from the outline of the upscaled sheet: red and green hold which
     * way the surface faces, blue how far the pixel is from the outline, alpha whether the part is thick enough to
     * take an edge light. Pixels outside any frame face straight up and take none.
     */
    static Pixmap shape(Pixmap upscaled, int[] frames) {
        Pixmap out = new Pixmap(upscaled.getWidth(), upscaled.getHeight(), Pixmap.Format.RGBA8888);
        out.setBlending(Pixmap.Blending.None);
        out.setColor(0x8080ff00);
        out.fill();
        for (int f = 0; f + 4 < frames.length; f += 5) {
            int fx = Math.max(0, frames[f]) * SCALE, fy = Math.max(0, frames[f + 1]) * SCALE;
            int w = Math.min(frames[f + 2] * SCALE, upscaled.getWidth() - fx);
            int h = Math.min(frames[f + 3] * SCALE, upscaled.getHeight() - fy);
            if (w <= 0 || h <= 0) {
                continue;
            }
            boolean continues = frames[f + 4] != 0;
            boolean[] solid = new boolean[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    solid[y * w + x] = (upscaled.getPixel(fx + x, fy + y) & 255) != 0;
                }
            }
            float[] depth = depth(solid, w, h, continues);
            float[] height = new float[w * h];
            for (int i = 0; i < w * h; i++) {
                float t = Math.min(depth[i] / BEVEL, 1f);
                height[i] = solid[i] ? t * (2f - t) : 0f;
            }
            height = blur(blur(height, w, h), w, h);
            float[] thickness = spread(depth, w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (!solid[i]) {
                        continue;
                    }
                    float hx = height[y * w + Math.min(w - 1, x + 1)] - height[y * w + Math.max(0, x - 1)];
                    float hy = height[Math.min(h - 1, y + 1) * w + x] - height[Math.max(0, y - 1) * w + x];
                    float nx = -hx * SLOPE, ny = -hy * SLOPE;
                    float inv = (float) (1.0 / Math.sqrt(nx * nx + ny * ny + 1f));
                    int r = Math.round((nx * inv * 0.5f + 0.5f) * 255f), g = Math.round((ny * inv * 0.5f + 0.5f) * 255f);
                    int b = Math.round(Math.min(depth[i] / SHAPE_RANGE, 1f) * 255f);
                    out.drawPixel(fx + x, fy + y, r << 24 | g << 16 | b << 8 | (thickness[i] >= THIN ? 255 : 0));
                }
            }
        }
        return out;
    }

    /** Distance in pixels from each solid pixel to the nearest empty one, by a two-pass 3-4 chamfer. */
    private static float[] depth(boolean[] solid, int w, int h, boolean continues) {
        float far = 1e6f, border = continues ? far : 0f;
        float[] d = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            d[i] = solid[i] ? far : 0f;
        }
        for (int pass = 0; pass < 2; pass++) {
            int s = pass == 0 ? 1 : -1;
            for (int y = pass == 0 ? 0 : h - 1; y >= 0 && y < h; y += s) {
                for (int x = pass == 0 ? 0 : w - 1; x >= 0 && x < w; x += s) {
                    int i = y * w + x;
                    if (d[i] == 0f) {
                        continue;
                    }
                    float v = d[i];
                    v = Math.min(v, at(d, x - s, y, w, h, border) + 3f);
                    v = Math.min(v, at(d, x, y - s, w, h, border) + 3f);
                    v = Math.min(v, at(d, x - s, y - s, w, h, border) + 4f);
                    v = Math.min(v, at(d, x + s, y - s, w, h, border) + 4f);
                    d[i] = v;
                }
            }
        }
        for (int i = 0; i < w * h; i++) {
            d[i] = Math.min(d[i] / 3f, 4f * SHAPE_RANGE);
        }
        return d;
    }

    private static float at(float[] d, int x, int y, int w, int h, float border) {
        return x >= 0 && x < w && y >= 0 && y < h ? d[y * w + x] : border;
    }

    private static float[] blur(float[] a, int w, int h) {
        float[] out = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float sum = 0f;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        sum += a[Math.max(0, Math.min(h - 1, y + dy)) * w + Math.max(0, Math.min(w - 1, x + dx))];
                    }
                }
                out[y * w + x] = sum / 9f;
            }
        }
        return out;
    }

    /** The largest depth within THIN_WINDOW pixels of each pixel: how thick the part it belongs to is. */
    private static float[] spread(float[] a, int w, int h) {
        float[] rows = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float m = 0f;
                for (int k = Math.max(0, x - THIN_WINDOW); k <= Math.min(w - 1, x + THIN_WINDOW); k++) {
                    m = Math.max(m, a[y * w + k]);
                }
                rows[y * w + x] = m;
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float m = 0f;
                for (int k = Math.max(0, y - THIN_WINDOW); k <= Math.min(h - 1, y + THIN_WINDOW); k++) {
                    m = Math.max(m, rows[k * w + x]);
                }
                out[y * w + x] = m;
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
