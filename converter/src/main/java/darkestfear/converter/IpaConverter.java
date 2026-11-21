package darkestfear.converter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

public final class IpaConverter {

    private static final String[] ATLASES = {"LEVEL_GRAPHICS", "CHARACTERS", "HUD"};
    private static final String[] THEMES = {"wood", "stone", "tile"};
    private static final int SCALE = 2;

    private final Map<String, Map<String, Sprite>> atlases = new LinkedHashMap<>();

    private IpaConverter() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: df-convert <DarkestFear.ipa> <pack.zip>");
            System.exit(2);
        }
        try {
            new IpaConverter().convert(new File(args[0]), new File(args[1]));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void convert(File ipa, File pack) throws IOException {
        if (!ipa.isFile()) {
            throw new IOException(ipa + " doesn't exist.");
        }
        Map<String, byte[]> images = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(ipa)) {
            String data = null;
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (e.getName().matches("Payload/[^/]+\\.app/data/" + ATLASES[0] + "\\.dat")) {
                    data = e.getName().substring(0, e.getName().lastIndexOf('/') + 1);
                }
            }
            if (data == null) {
                throw new IOException("This isn't the Darkest Fear IPA: it has no " + ATLASES[0] + ".dat.");
            }
            for (String atlas : ATLASES) {
                images.put(atlas + ".png", straightAlpha(read(zip, data + atlas + ".png"), atlas));
                atlases.put(atlas, table(read(zip, data + atlas + ".dat")));
            }
        } catch (java.util.zip.ZipException e) {
            throw new IOException(ipa + " can't be opened as an IPA.");
        }

        String json = manifest();
        File parent = pack.getAbsoluteFile().getParentFile();
        File part = File.createTempFile("pack", ".part", parent);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(part.toPath()))) {
            out.putNextEntry(new ZipEntry("pack.json"));
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            for (Map.Entry<String, byte[]> image : images.entrySet()) {
                out.putNextEntry(new ZipEntry(image.getKey()));
                out.write(image.getValue());
                out.closeEntry();
            }
        }
        Files.move(part.toPath(), pack.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Wrote " + pack);
    }

    private static byte[] straightAlpha(byte[] png, String atlas) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(png));
        if (source == null) {
            throw new IOException(atlas + ".png in the IPA can't be read as an image.");
        }
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int c = source.getRGB(x, y), alpha = c >>> 24;
                if (alpha > 0 && alpha < 255) {
                    int r = Math.min(255, ((c >> 16 & 255) * 255 + alpha / 2) / alpha);
                    int g = Math.min(255, ((c >> 8 & 255) * 255 + alpha / 2) / alpha);
                    int b = Math.min(255, ((c & 255) * 255 + alpha / 2) / alpha);
                    c = alpha << 24 | r << 16 | g << 8 | b;
                }
                image.setRGB(x, y, c);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] read(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException("This isn't the Darkest Fear IPA: it has no " + name.substring(name.lastIndexOf('/') + 1) + ".");
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    private record Sprite(String file, int x, int y, int w, int h, int ax, int ay) {

        String json(boolean centred, String[] line, int cuesFrom) throws IOException {
            String anchor = (centred ? w / 2 : ax) + ", " + (centred ? h / 2 : ay);
            List<String> cues = new ArrayList<>();
            for (int i = cuesFrom; line != null && i < line.length; i++) {
                String[] note = line[i].split(":");
                if (note.length == 2 && note[0].equals("anchor") && note[1].matches("-?\\d+,-?\\d+")) {
                    anchor = note[1].replace(",", ", ");
                } else if (note.length == 2 && note[1].matches("\\d+,\\d+,\\d+,\\d+")) {
                    cues.add("{ \"type\": \"" + note[0] + "\", \"rect\": [" + note[1].replace(",", ", ") + "] }");
                } else {
                    throw new IOException("The mapping for sprite " + line[0] + " has a bad note: " + line[i]);
                }
            }
            StringBuilder json = new StringBuilder("{ \"file\": \"" + file + "\", \"rect\": [" + x + ", " + y + ", " + w + ", " + h
                    + "], \"anchor\": [" + anchor + "]");
            if (!cues.isEmpty()) {
                json.append(", \"cues\": [").append(String.join(", ", cues)).append("]");
            }
            return json.append(" }").toString();
        }
    }

    private static Map<String, Sprite> table(byte[] dat) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(dat));
        String file = name(in);
        int count = in.readUnsignedShort();
        Map<String, Sprite> sprites = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String name = name(in);
            sprites.put(name, new Sprite(file, in.readUnsignedShort(), in.readUnsignedShort(), in.readUnsignedShort(),
                    in.readUnsignedShort(), in.readShort(), in.readShort()));
        }
        return sprites;
    }

    private static String name(DataInputStream in) throws IOException {
        byte[] bytes = new byte[in.readUnsignedShort()];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private Sprite sprite(String name, String neededFor) throws IOException {
        int colon = name.indexOf(':');
        Sprite found = null;
        for (Map.Entry<String, Map<String, Sprite>> atlas : atlases.entrySet()) {
            if (colon >= 0 && !atlas.getKey().equals(name.substring(0, colon))) {
                continue;
            }
            Sprite sprite = atlas.getValue().get(name.substring(colon + 1));
            if (sprite == null) {
                continue;
            }
            if (found != null) {
                throw new IOException("Sprite " + name + ", needed for " + neededFor + ", is in more than one atlas.");
            }
            found = sprite;
        }
        if (found == null) {
            throw new IOException("The IPA has no sprite named " + name + ", needed for " + neededFor + ".");
        }
        return found;
    }

    private String manifest() throws IOException {
        Map<String, List<String[]>> mapping = mapping();
        StringBuilder json = new StringBuilder("{\n  \"format\": 1,\n  \"name\": \"Darkest Fear iPhone art\",\n  \"scale\": "
                + SCALE + ",\n\n  \"sprites\": {\n");
        Map<String, String[]> themed = new LinkedHashMap<>();
        List<String> entries = new ArrayList<>();
        for (String[] line : mapping.get("sprites")) {
            int at = line[0].indexOf('@');
            String id = at < 0 ? line[0] : line[0].substring(0, at);
            String what = "sprite " + id;
            if (at < 0 && !line[1].contains("*")) {
                entries.add("    \"" + id + "\": " + sprite(line[1], what).json(false, line, 2));
                continue;
            }
            String[] versions = themed.computeIfAbsent(id, k -> new String[THEMES.length]);
            for (int i = 0; i < THEMES.length; i++) {
                if (at < 0) {
                    versions[i] = sprite(line[1].replace("*", THEMES[i].toUpperCase()), what).json(false, line, 2);
                } else if (THEMES[i].equals(line[0].substring(at + 1))) {
                    versions[i] = sprite(line[1], what).json(false, line, 2);
                }
            }
        }
        for (Map.Entry<String, String[]> entry : themed.entrySet()) {
            List<String> versions = new ArrayList<>();
            for (int i = 0; i < THEMES.length; i++) {
                if (entry.getValue()[i] == null) {
                    throw new IOException("The mapping for sprite " + entry.getKey() + " has no " + THEMES[i] + " version.");
                }
                versions.add("      \"" + THEMES[i] + "\": " + entry.getValue()[i]);
            }
            entries.add("    \"" + entry.getKey() + "\": { \"theme\": {\n" + String.join(",\n", versions) + " } }");
        }
        json.append(String.join(",\n", entries)).append("\n  },\n\n  \"floors\": {\n");

        entries.clear();
        for (String[] line : mapping.get("floors")) {
            List<String> tiles = new ArrayList<>();
            for (int i = 1; i < line.length; i++) {
                tiles.add("      " + sprite(line[i], "the " + line[0] + " floor").json(true, null, 0));
            }
            entries.add("    \"" + line[0] + "\": [\n" + String.join(",\n", tiles) + " ]");
        }
        json.append(String.join(",\n", entries)).append("\n  },\n\n  \"shades\": {\n");

        entries.clear();
        for (String[] line : mapping.get("shades")) {
            List<String> steps = new ArrayList<>();
            for (int i = 1; i < line.length; i++) {
                steps.add("      " + sprite(line[i], "the " + line[0] + " floor shading").json(false, null, 0));
            }
            entries.add("    \"" + line[0] + "\": [\n" + String.join(",\n", steps) + " ]");
        }
        json.append(String.join(",\n", entries)).append("\n  },\n\n  \"hud\": {\n");

        entries.clear();
        for (String[] line : mapping.get("hud")) {
            entries.add("    \"" + line[0] + "\": " + sprite(line[1], "the HUD (" + line[0] + ")").json(false, null, 0));
        }
        return json.append(String.join(",\n", entries)).append("\n  }\n}\n").toString();
    }

    private static Map<String, List<String[]>> mapping() throws IOException {
        Map<String, List<String[]>> sections = new LinkedHashMap<>();
        List<String[]> section = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                IpaConverter.class.getResourceAsStream("/mapping.txt"), StandardCharsets.UTF_8))) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                line = line.trim();
                if (line.startsWith("[")) {
                    section = new ArrayList<>();
                    sections.put(line.substring(1, line.length() - 1), section);
                } else if (!line.isEmpty()) {
                    section.add(line.split("\\s+"));
                }
            }
        }
        return sections;
    }
}
