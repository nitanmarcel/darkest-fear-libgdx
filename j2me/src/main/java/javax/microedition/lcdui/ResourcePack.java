package javax.microedition.lcdui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import darkestfear.menu.GameJar;
import darkestfear.menu.LightBridge;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ResourcePack {

    private static final String FOLDER = "packs", SETTINGS = "Packs", ORDER = "order";
    private static final String[] THEMES = {"wood", "stone", "tile"};
    private static final int[] THEME_SHEETS = {3, 4, 5};
    private static final String[] CUES = {"ember", "handle"};
    private static final String[] AUDIO = {"mp3", "ogg", "wav"};
    private static final int GUTTER = 1;
    private static final int LIGHT_LEVELS = 8;
    private static final float UNKNOWN_LENGTH = 1f;
    private static final int PLAYED_KEPT = 12;

    private static final class Art {
        Pixmap image, normal;
        int scale;
        int x, y, w, h, ax, ay;
        boolean anchored;
        float[] cues, placed;
    }

    public static final class Audio {
        public byte[] data;
        public String extension;
    }

    private static final class Effect {
        final List<Sound> sounds = new ArrayList<>();
        final List<Float> seconds = new ArrayList<>();
        float volume;
        long busyUntil, loopId;
    }

    private static final class Sheet {
        Pixmap source;
        int[] ids;
        short[][] attrs;
        String theme;
        boolean upscaled;
        boolean applied;
    }

    private static final class Pack {
        File file;
        String name, id, problem;
        boolean enabled = true;
        int scale;
        final Map<String, Pixmap> images = new HashMap<>();
        final Map<Integer, Art[]> sprites = new HashMap<>();
        final Map<String, Art[]> floors = new HashMap<>();
        final Map<String, Art[]> shades = new HashMap<>();
        final Map<String, Art> hud = new HashMap<>();
        final Map<Integer, Audio> sounds = new HashMap<>();
        final Map<String, Effect> events = new HashMap<>();
        final List<File> extracted = new ArrayList<>();

        boolean used() {
            return enabled && problem == null;
        }
    }

    private static boolean tried;
    private static final List<Pack> packs = new ArrayList<>();
    private static final Map<Pixmap, Texture> textures = new HashMap<>();
    private static final Map<Image, Sheet> sheets = new WeakHashMap<>();
    private static String theme = THEMES[0];
    private static final Map<String, Effect> loops = new HashMap<>();
    private static final java.util.ArrayDeque<String> played = new java.util.ArrayDeque<>();

    private ResourcePack() {
    }

    static void sync() {
        ensure();
        short[][] table = LightBridge.spriteTable();
        Object[] loaded = LightBridge.spriteSheets();
        if (packs.isEmpty() || table == null || loaded == null) {
            return;
        }
        for (int i = 0; i < THEMES.length; i++) {
            if (THEME_SHEETS[i] < loaded.length && loaded[THEME_SHEETS[i]] != null) {
                theme = THEMES[i];
            }
        }
        for (int group = 0; group < loaded.length; group++) {
            if (!(loaded[group] instanceof Image)) {
                continue;
            }
            Image image = (Image) loaded[group];
            Sheet sheet = sheets.get(image);
            if (sheet == null) {
                sheet = read(image, table, group);
                sheets.put(image, sheet);
            }
            if (sheet.ids.length > 0 && !(sheet.applied && sheet.theme.equals(theme)
                    && sheet.upscaled == Graphics.upscaleEnabled)) {
                apply(image, sheet, table);
            }
        }
    }

    public static Audio audio(byte[] original) {
        ensure();
        int id = packs.isEmpty() ? -1 : LightBridge.soundId(original);
        for (Pack pack : packs) {
            Audio audio = id >= 0 && pack.used() ? pack.sounds.get(id) : null;
            if (audio != null) {
                return audio;
            }
        }
        return null;
    }

    public static void play(String event, float gain, boolean unlessPlaying) {
        Effect effect = effect(event);
        long now = System.currentTimeMillis();
        if (effect == null || (unlessPlaying && now < effect.busyUntil)) {
            return;
        }
        int pick = (int) (Math.random() * effect.sounds.size());
        effect.sounds.get(pick).play(Math.min(1f, gain * effect.volume));
        effect.busyUntil = now + (long) (effect.seconds.get(pick) * 1000);
        played.addFirst(event);
        if (played.size() > PLAYED_KEPT) {
            played.removeLast();
        }
    }

    public static String[] events() {
        ensure();
        java.util.TreeSet<String> names = new java.util.TreeSet<>();
        for (Pack pack : packs) {
            if (pack.used()) {
                names.addAll(pack.events.keySet());
            }
        }
        return names.toArray(new String[0]);
    }

    public static String[] played() {
        return played.toArray(new String[0]);
    }

    public static void loop(String event, float gain) {
        Effect effect = gain > 0f ? effect(event) : null, playing = loops.get(event);
        if (effect != playing) {
            if (playing != null) {
                playing.sounds.get(0).stop(playing.loopId);
            }
            if (effect == null) {
                loops.remove(event);
            } else {
                loops.put(event, effect);
                effect.loopId = effect.sounds.get(0).loop(0f);
            }
        }
        if (effect != null) {
            effect.sounds.get(0).setVolume(effect.loopId, Math.min(1f, gain * effect.volume));
        }
    }

    private static Effect effect(String event) {
        ensure();
        for (Pack pack : packs) {
            Effect effect = pack.used() ? pack.events.get(event) : null;
            if (effect != null) {
                return effect;
            }
        }
        return null;
    }

    public static int count() {
        ensure();
        return packs.size();
    }

    public static String name(int index) {
        return packs.get(index).name;
    }

    public static boolean enabled(int index) {
        return packs.get(index).enabled;
    }

    public static String problem(int index) {
        return packs.get(index).problem;
    }

    public static void toggle(int index) {
        packs.get(index).enabled = !packs.get(index).enabled;
        save();
        reload();
    }

    public static void move(int index, int by) {
        if (index + by < 0 || index + by >= packs.size()) {
            return;
        }
        packs.add(index + by, packs.remove(index));
        save();
        reload();
    }

    public static void remove(int index) {
        packs.remove(index).file.delete();
        save();
        reload();
    }

    public static void install(File source) {
        ensure();
        File folder = folder();
        folder.mkdirs();
        String id = id(source);
        File target = null;
        for (Pack pack : packs) {
            if (id != null && id.equals(pack.id)) {
                target = pack.file;
            }
        }
        boolean update = target != null;
        for (int n = 1; target == null || (!update && target.exists()); n++) {
            target = new File(folder, "pack-" + n + ".zip");
        }
        File part = new File(folder, target.getName() + ".part");
        try {
            java.nio.file.Files.copy(source.toPath(), part.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            java.nio.file.Files.move(part.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Gdx.app.error("ResourcePack", "copying " + source + " failed", e);
            part.delete();
            return;
        }
        save(update ? null : target.getName());
        reload();
    }

    private static String id(File file) {
        try (ZipFile zip = new ZipFile(file); InputStream in = zip.getInputStream(zip.getEntry("pack.json"))) {
            return new JsonReader().parse(in).getString("id", null);
        } catch (Exception e) {
            return null;
        }
    }

    private static File folder() {
        return new File(GameJar.file().getParentFile(), FOLDER);
    }

    private static void ensure() {
        if (!tried) {
            tried = true;
            load();
        }
    }

    private static void save() {
        save(null);
    }

    private static void save(String first) {
        StringBuilder order = new StringBuilder(first == null ? "" : "+" + first + "\n");
        for (Pack pack : packs) {
            order.append(pack.enabled ? '+' : '-').append(pack.file.getName()).append('\n');
        }
        Gdx.app.getPreferences(SETTINGS).putString(ORDER, order.toString()).flush();
    }

    private static void reload() {
        short[][] table = LightBridge.spriteTable();
        for (Map.Entry<Image, Sheet> entry : sheets.entrySet()) {
            Sheet sheet = entry.getValue();
            if (!sheet.applied) {
                continue;
            }
            for (int i = 0; table != null && i < sheet.ids.length; i++) {
                System.arraycopy(sheet.attrs[i], 0, table[sheet.ids[i]], 0, 6);
            }
            entry.getKey().repack(sheet.source, null, null);
        }
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        loops.clear();
        for (Pack pack : packs) {
            for (Pixmap image : pack.images.values()) {
                image.dispose();
            }
            for (Effect effect : pack.events.values()) {
                for (Sound sound : effect.sounds) {
                    sound.dispose();
                }
            }
            for (File file : pack.extracted) {
                file.delete();
            }
        }
        sheets.clear();
        textures.clear();
        packs.clear();
        tried = false;
        SceneCompositor.sheetsChanged();
    }

    private static void load() {
        File[] files = folder().listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) {
            return;
        }
        List<String> order = new ArrayList<>(java.util.Arrays.asList(
                Gdx.app.getPreferences(SETTINGS).getString(ORDER, "").split("\n")));
        java.util.Arrays.sort(files, (a, b) -> Integer.compare(place(order, a), place(order, b)));
        for (File file : files) {
            Pack pack = new Pack();
            pack.file = file;
            pack.name = file.getName();
            pack.enabled = !order.contains("-" + file.getName());
            read(pack);
            packs.add(pack);
        }
    }

    private static int place(List<String> order, File file) {
        int on = order.indexOf("+" + file.getName()), off = order.indexOf("-" + file.getName());
        return on >= 0 ? on : off >= 0 ? off : Integer.MAX_VALUE;
    }

    private static void read(Pack pack) {
        File file = pack.file;
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry manifest = zip.getEntry("pack.json");
            if (manifest == null) {
                throw new IOException("it has no pack.json");
            }
            JsonValue json;
            try (InputStream in = zip.getInputStream(manifest)) {
                json = new JsonReader().parse(in);
            }
            int packScale = json.getInt("scale", 0);
            pack.name = json.getString("name", file.getName());
            pack.id = json.getString("id", null);
            if (pack.id == null || pack.id.isEmpty()) {
                throw new IOException("it has no id");
            }
            if (json.getInt("format", 0) != 1 || packScale < 1 || SpriteUpscaler.SCALE % packScale != 0) {
                throw new IOException("its format or scale isn't supported");
            }
            for (JsonValue entry = json.getChild("sprites"); entry != null; entry = entry.next) {
                JsonValue themed = entry.get("theme");
                Art[] arts = new Art[THEMES.length];
                for (int i = 0; i < THEMES.length; i++) {
                    JsonValue value = themed == null ? entry : themed.get(THEMES[i]);
                    if (value == null) {
                        throw new IOException("sprite " + entry.name + " has no " + THEMES[i] + " version");
                    }
                    arts[i] = art(pack, packScale, zip, value, "sprite " + entry.name);
                }
                pack.sprites.put(Integer.parseInt(entry.name), arts);
            }
            for (JsonValue floor = json.getChild("floors"); floor != null; floor = floor.next) {
                List<Art> list = new ArrayList<>();
                for (JsonValue tile = floor.child; tile != null; tile = tile.next) {
                    list.add(art(pack, packScale, zip, tile, floor.name + " floor"));
                }
                pack.floors.put(floor.name, list.toArray(new Art[0]));
            }
            for (JsonValue shade = json.getChild("shades"); shade != null; shade = shade.next) {
                List<Art> list = new ArrayList<>();
                for (JsonValue step = shade.child; step != null; step = step.next) {
                    list.add(art(pack, packScale, zip, step, shade.name + " floor shading"));
                }
                if (list.size() != LIGHT_LEVELS - 1) {
                    throw new IOException(shade.name + " floor shading needs " + (LIGHT_LEVELS - 1) + " steps");
                }
                pack.shades.put(shade.name, list.toArray(new Art[0]));
            }
            for (JsonValue piece = json.getChild("hud"); piece != null; piece = piece.next) {
                pack.hud.put(piece.name, art(pack, packScale, zip, piece, "HUD piece " + piece.name));
            }
            for (JsonValue sound = json.getChild("sounds"); sound != null; sound = sound.next) {
                String name = sound.getString("file", null), what = "sound " + sound.name;
                if (name == null) {
                    throw new IOException(what + " names no file");
                }
                pack.sounds.put(Integer.parseInt(sound.name), audio(zip, name, what));
            }
            for (JsonValue event = json.getChild("events"); event != null; event = event.next) {
                Effect effect = new Effect();
                effect.volume = event.getFloat("volume", 1f);
                String what = "event " + event.name;
                for (JsonValue name = event.getChild("files"); name != null; name = name.next) {
                    Audio audio = audio(zip, name.asString(), what);
                    File copy = File.createTempFile("sound", "." + audio.extension);
                    copy.deleteOnExit();
                    pack.extracted.add(copy);
                    java.nio.file.Files.write(copy.toPath(), audio.data);
                    effect.sounds.add(Gdx.audio.newSound(Gdx.files.absolute(copy.getPath())));
                    effect.seconds.add(seconds(audio));
                }
                if (effect.sounds.isEmpty()) {
                    throw new IOException(what + " names no files");
                }
                pack.events.put(event.name, effect);
            }
            pack.scale = packScale;
        } catch (Exception e) {
            pack.problem = "This pack can't be used: " + e.getMessage() + ".";
        }
    }

    private static Audio audio(ZipFile zip, String name, String what) throws IOException {
        Audio audio = new Audio();
        audio.extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (!java.util.Arrays.asList(AUDIO).contains(audio.extension)) {
            throw new IOException(what + " isn't an MP3, OGG or WAV file");
        }
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException(what + " needs " + name + ", which is missing");
        }
        try (InputStream in = zip.getInputStream(entry)) {
            audio.data = in.readAllBytes();
        }
        return audio;
    }

    private static float seconds(Audio audio) {
        byte[] d = audio.data;
        int rate = 0;
        for (int at = 12; audio.extension.equals("wav") && at + 8 <= d.length; ) {
            int size = (d[at + 4] & 255) | (d[at + 5] & 255) << 8 | (d[at + 6] & 255) << 16 | (d[at + 7] & 255) << 24;
            String chunk = new String(d, at, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (size < 0) {
                break;
            }
            if (chunk.equals("fmt ") && at + 20 <= d.length) {
                rate = (d[at + 16] & 255) | (d[at + 17] & 255) << 8 | (d[at + 18] & 255) << 16 | (d[at + 19] & 255) << 24;
            } else if (chunk.equals("data") && rate > 0) {
                return Math.min(size, d.length - at - 8) / (float) rate;
            }
            at += 8 + size + (size & 1);
        }
        return UNKNOWN_LENGTH;
    }

    static float[] cues(int id) {
        Art art = art(id);
        if (art == null) {
            return null;
        }
        if (art.placed == null) {
            art.placed = art.cues.clone();
            for (int i = 0; i < art.placed.length; i += 5) {
                art.placed[i + 1] = (art.placed[i + 1] + Math.floorMod(-art.ax, art.scale)) / art.scale;
                art.placed[i + 2] = (art.placed[i + 2] + Math.floorMod(-art.ay, art.scale)) / art.scale;
                art.placed[i + 3] /= art.scale;
                art.placed[i + 4] /= art.scale;
            }
        }
        return art.placed;
    }

    static float[] tone(int id) {
        Art art = art(id);
        if (art == null) {
            return null;
        }
        float[] tone = new float[4];
        int[] low = {255, 255, 255}, high = {0, 0, 0};
        for (int y = 0; y < art.h; y++) {
            for (int x = 0; x < art.w; x++) {
                int c = art.image.getPixel(art.x + x, art.y + y);
                for (int k = 0; k < 3; k++) {
                    int v = c >>> (24 - 8 * k) & 255;
                    low[k] = Math.min(low[k], v);
                    high[k] = Math.max(high[k], v);
                }
            }
        }
        for (int k = 0; k < 3; k++) {
            tone[k] = (low[k] + high[k]) / 510f;
            tone[3] = Math.max(tone[3], (high[k] - low[k]) / 510f + 1f / 255f);
        }
        return tone;
    }

    static boolean drawFloor(byte[][] levels, int rows, int cols, int originX, int originY) {
        Art[] tiles = null, steps = null;
        for (Pack pack : packs) {
            if (pack.used() && tiles == null && pack.floors.get(theme) != null && pack.floors.get(theme).length > 0) {
                tiles = pack.floors.get(theme);
            }
            if (pack.used() && steps == null) {
                steps = pack.shades.get(theme);
            }
        }
        if (tiles == null || (levels != null && steps == null)) {
            return false;
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Art tile = tiles[(row * 31 + col * 17) % tiles.length];
                draw(tile, col * Graphics.TILE + originX, row * Graphics.TILE + originY, true);
            }
        }
        for (int row = 0; levels != null && row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int level = levels[row][col];
                if (level < 0) {
                    level = 0;
                    for (int k = 0; k < 4; k++) {
                        int r = row + (k == 0 ? -1 : k == 1 ? 1 : 0), c = col + (k == 2 ? -1 : k == 3 ? 1 : 0);
                        if (r >= 0 && r < rows && c >= 0 && c < cols) {
                            level = Math.max(level, levels[r][c]);
                        }
                    }
                }
                if (level < LIGHT_LEVELS - 1) {
                    draw(steps[LIGHT_LEVELS - 2 - level], col * Graphics.TILE + originX, row * Graphics.TILE + originY, false);
                }
            }
        }
        return true;
    }

    static boolean drawHud(String name, int anchorX, int anchorY, boolean upright) {
        for (Pack pack : packs) {
            Art art = pack.used() ? pack.hud.get(name) : null;
            if (art == null) {
                continue;
            }
            float w = art.w / (float) art.scale, h = art.h / (float) art.scale;
            float x = anchorX - art.ax / (float) art.scale, y = anchorY - art.ay / (float) art.scale;
            Texture texture = texture(art);
            if (upright) {
                float iw = 1f / texture.getWidth(), ih = 1f / texture.getHeight();
                Graphics.batch.draw(texture, x, y, w, h, art.x * iw, art.y * ih, (art.x + art.w) * iw, (art.y + art.h) * ih);
            } else {
                Graphics.batch.draw(texture, x, y, w, h, art.x, art.y, art.w, art.h, false, true);
            }
            return true;
        }
        return false;
    }

    private static Texture texture(Art art) {
        Texture texture = textures.get(art.image);
        if (texture == null) {
            texture = new Texture(art.image);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.put(art.image, texture);
        }
        return texture;
    }

    private static void draw(Art art, int cx, int cy, boolean centred) {
        float w = art.w / (float) art.scale, h = art.h / (float) art.scale;
        float x = cx - (art.anchored || !centred ? art.ax / (float) art.scale : w / 2f);
        float y = cy - (art.anchored || !centred ? art.ay / (float) art.scale : h / 2f);
        if (x + w <= 0 || x >= Graphics.VIRTUAL_WIDTH || y + h <= 0 || y >= Graphics.playAreaH) {
            return;
        }
        Graphics.batch.draw(texture(art), x, y, w, h, art.x, art.y, art.w, art.h, false, true);
    }

    private static Pixmap image(Pack pack, ZipFile zip, String name, String what) throws IOException {
        Pixmap image = pack.images.get(name);
        if (image == null) {
            ZipEntry entry = zip.getEntry(name);
            if (entry == null) {
                throw new IOException(what + " needs " + name + ", which is missing");
            }
            try (InputStream in = zip.getInputStream(entry)) {
                byte[] bytes = in.readAllBytes();
                image = new Pixmap(bytes, 0, bytes.length);
            }
            pack.images.put(name, image);
        }
        return image;
    }

    private static Art art(Pack pack, int scale, ZipFile zip, JsonValue value, String what) throws IOException {
        String name = value.getString("file", null), normal = value.getString("normal", null);
        if (name == null && normal == null) {
            throw new IOException(what + " names no file");
        }
        Art art = new Art();
        art.scale = scale;
        art.normal = normal == null ? null : image(pack, zip, normal, what);
        art.image = name == null ? null : image(pack, zip, name, what);
        if (name == null) {
            name = normal;
        } else if (normal != null && (art.normal.getWidth() != art.image.getWidth() || art.normal.getHeight() != art.image.getHeight())) {
            throw new IOException(what + " has a normal map that isn't the size of " + name);
        }
        Pixmap sized = art.image != null ? art.image : art.normal;
        JsonValue rect = value.get("rect");
        art.w = sized.getWidth();
        art.h = sized.getHeight();
        if (rect != null) {
            int[] r = rect.asIntArray();
            if (r.length != 4 || r[0] < 0 || r[1] < 0 || r[2] < 1 || r[3] < 1
                    || r[0] + r[2] > art.w || r[1] + r[3] > art.h) {
                throw new IOException(what + " has a rect outside " + name);
            }
            art.x = r[0];
            art.y = r[1];
            art.w = r[2];
            art.h = r[3];
        }
        JsonValue anchor = value.get("anchor");
        if (anchor != null) {
            int[] a = anchor.asIntArray();
            if (a.length != 2) {
                throw new IOException(what + " has a bad anchor");
            }
            art.ax = a[0];
            art.ay = a[1];
            art.anchored = true;
        }
        JsonValue cues = value.get("cues");
        art.cues = new float[cues == null ? 0 : cues.size * 5];
        int n = 0;
        for (JsonValue cue = cues == null ? null : cues.child; cue != null; cue = cue.next) {
            int kind = java.util.Arrays.asList(CUES).indexOf(cue.getString("type", ""));
            int[] r = cue.get("rect") == null ? new int[0] : cue.get("rect").asIntArray();
            if (kind < 0 || r.length != 4) {
                throw new IOException(what + " has a bad cue");
            }
            art.cues[n++] = kind;
            for (int k = 0; k < 4; k++) {
                art.cues[n++] = r[k];
            }
        }
        return art;
    }

    private static Sheet read(Image image, short[][] table, int group) {
        Sheet sheet = new Sheet();
        sheet.source = image.pixmap();
        List<Integer> ids = new ArrayList<>();
        boolean replaced = false;
        for (int id = 0; id < table.length; id++) {
            if (table[id] != null && table[id].length > 6 && table[id][6] == group) {
                ids.add(id);
            }
        }
        sheet.ids = new int[ids.size()];
        sheet.attrs = new short[ids.size()][];
        for (int i = 0; i < sheet.ids.length; i++) {
            sheet.ids[i] = ids.get(i);
            sheet.attrs[i] = table[sheet.ids[i]].clone();
            replaced |= facing(sheet.ids[i]) != null;
        }
        if (!replaced) {
            sheet.ids = new int[0];
        }
        return sheet;
    }

    static boolean replaces(Image image, int x, int y, int w, int h) {
        int id = LightBridge.spriteAt(image, x, y, w, h);
        return id >= 0 && art(id) != null;
    }

    private static Art art(int id) {
        return entry(id, true);
    }

    private static Art facing(int id) {
        return entry(id, false);
    }

    private static Art entry(int id, boolean artOnly) {
        for (Pack pack : packs) {
            Art[] themed = pack.used() ? pack.sprites.get(id) : null;
            for (int i = 0; themed != null && i < THEMES.length; i++) {
                if (THEMES[i].equals(theme) && !(artOnly && themed[i].image == null)) {
                    return themed[i];
                }
            }
        }
        return null;
    }

    private static void apply(Image image, Sheet sheet, short[][] table) {
        int n = sheet.ids.length;
        Art[] arts = new Art[n];
        int[] padX = new int[n], padY = new int[n], x = new int[n], y = new int[n], w = new int[n], h = new int[n];
        int width = 256;
        for (int i = 0; i < n; i++) {
            Art art = arts[i] = art(sheet.ids[i]);
            short[] a = sheet.attrs[i];
            if (art == null) {
                w[i] = a[2];
                h[i] = a[3];
            } else {
                if (!art.anchored) {
                    art.ax = a[4] * art.scale;
                    art.ay = a[5] * art.scale;
                    art.anchored = true;
                }
                padX[i] = Math.floorMod(-art.ax, art.scale);
                padY[i] = Math.floorMod(-art.ay, art.scale);
                w[i] = (art.w + padX[i] + art.scale - 1) / art.scale;
                h[i] = (art.h + padY[i] + art.scale - 1) / art.scale;
            }
            width = Math.max(width, w[i] + 2 * GUTTER);
        }
        int cx = GUTTER, cy = GUTTER, row = 0;
        for (int i = 0; i < n; i++) {
            if (cx + w[i] + GUTTER > width) {
                cx = GUTTER;
                cy += row + GUTTER;
                row = 0;
            }
            x[i] = cx;
            y[i] = cy;
            cx += w[i] + GUTTER;
            row = Math.max(row, h[i]);
        }
        Pixmap small = new Pixmap(width, cy + row + GUTTER, Pixmap.Format.RGBA8888);
        small.setBlending(Pixmap.Blending.None);
        List<Integer> plain = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            short[] a = sheet.attrs[i];
            if (arts[i] == null) {
                small.drawPixmap(sheet.source, x[i], y[i], a[0], a[1], a[2], a[3]);
                plain.add(x[i]);
                plain.add(y[i]);
                plain.add(w[i]);
                plain.add(h[i]);
                plain.add(LightBridge.isTileSprite(sheet.ids[i]) ? 1 : 0);
            } else {
                shrink(arts[i], padX[i], padY[i], small, x[i], y[i], w[i], h[i]);
            }
        }
        int[] plainFrames = new int[Graphics.upscaleEnabled ? plain.size() : 0];
        for (int i = 0; i < plainFrames.length; i++) {
            plainFrames[i] = plain.get(i);
        }
        Pixmap big = SpriteUpscaler.upscale(small, plainFrames);
        big.setBlending(Pixmap.Blending.None);
        for (int i = 0; i < n; i++) {
            Art art = arts[i];
            int up = art == null ? 0 : SpriteUpscaler.SCALE / art.scale;
            if (art != null) {
                big.setColor(0);
                big.fillRectangle(x[i] * SpriteUpscaler.SCALE, y[i] * SpriteUpscaler.SCALE,
                        w[i] * SpriteUpscaler.SCALE, h[i] * SpriteUpscaler.SCALE);
            }
            for (int py = 0; art != null && py < art.h; py++) {
                for (int px = 0; px < art.w; px++) {
                    big.setColor(SceneCompositor.offGlow(art.image.getPixel(art.x + px, art.y + py)));
                    big.fillRectangle((x[i] * art.scale + padX[i] + px) * up, (y[i] * art.scale + padY[i] + py) * up, up, up);
                }
            }
        }
        for (int i = 0; i < n; i++) {
            short[] a = table[sheet.ids[i]];
            a[0] = (short) x[i];
            a[1] = (short) y[i];
            a[2] = (short) w[i];
            a[3] = (short) h[i];
            if (arts[i] != null) {
                a[4] = (short) ((arts[i].ax + padX[i]) / arts[i].scale);
                a[5] = (short) ((arts[i].ay + padY[i]) / arts[i].scale);
            } else {
                a[4] = sheet.attrs[i][4];
                a[5] = sheet.attrs[i][5];
            }
        }
        Pixmap surface = SpriteUpscaler.shape(big, LightBridge.spriteFrames(image));
        surface.setBlending(Pixmap.Blending.None);
        for (int i = 0; i < n; i++) {
            Art map = facing(sheet.ids[i]), art = arts[i];
            if (map == null || map.normal == null) {
                continue;
            }
            int up = SpriteUpscaler.SCALE / map.scale, grow = art == null ? SpriteUpscaler.SCALE : SpriteUpscaler.SCALE / art.scale;
            int ox = art == null ? x[i] * grow : (x[i] * art.scale + padX[i]) * grow;
            int oy = art == null ? y[i] * grow : (y[i] * art.scale + padY[i]) * grow;
            if (map.w * up != (art == null ? w[i] : art.w) * grow || map.h * up != (art == null ? h[i] : art.h) * grow) {
                continue;
            }
            for (int py = 0; py < map.h; py++) {
                for (int px = 0; px < map.w; px++) {
                    int facing = map.normal.getPixel(map.x + px, map.y + py);
                    if ((facing & 255) == 0) {
                        continue;
                    }
                    int red = facing >>> 24, green = 255 - (facing >>> 16 & 255);
                    for (int k = 0; k < up * up; k++) {
                        int sx = ox + px * up + k % up, sy = oy + py * up + k / up;
                        surface.drawPixel(sx, sy, red << 24 | green << 16 | (surface.getPixel(sx, sy) & 0xffff));
                    }
                }
            }
        }
        Texture detailed = new Texture(big);
        detailed.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Texture shape = new Texture(surface);
        shape.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        big.dispose();
        surface.dispose();
        image.repack(small, detailed, shape);
        sheet.theme = theme;
        sheet.upscaled = Graphics.upscaleEnabled;
        sheet.applied = true;
        SceneCompositor.sheetsChanged();
    }

    private static void shrink(Art art, int padX, int padY, Pixmap out, int ox, int oy, int w, int h) {
        int scale = art.scale;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                long r = 0, g = 0, b = 0, a = 0;
                for (int sy = 0; sy < scale; sy++) {
                    for (int sx = 0; sx < scale; sx++) {
                        int px = x * scale + sx - padX, py = y * scale + sy - padY;
                        if (px < 0 || py < 0 || px >= art.w || py >= art.h) {
                            continue;
                        }
                        int c = art.image.getPixel(art.x + px, art.y + py);
                        int alpha = c & 255;
                        r += (c >>> 24) * alpha;
                        g += (c >>> 16 & 255) * alpha;
                        b += (c >>> 8 & 255) * alpha;
                        a += alpha;
                    }
                }
                int color = a == 0 ? 0 : (int) (r / a) << 24 | (int) (g / a) << 16 | (int) (b / a) << 8
                        | (int) (a / (scale * scale));
                out.drawPixel(ox + x, oy + y, color);
            }
        }
    }
}
