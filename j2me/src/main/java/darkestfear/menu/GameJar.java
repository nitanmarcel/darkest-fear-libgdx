package darkestfear.menu;

import com.badlogic.gdx.Gdx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class GameJar {
    public static final String NAME = "Darkest Fear";
    public static final String VERSION = "1.55";
    private static final String CODE = "4b0cfcbf501bd0df9f10fd0c8eb95076060334d889a084fe90ac03c1356837e6";

    private static ClassLoader loader;
    private static File file;
    public static Runnable packChooser;

    private GameJar() {
    }

    public static ClassLoader loader() {
        return loader;
    }

    public static void use(ClassLoader gameLoader, File installed) {
        loader = gameLoader;
        file = installed;
    }

    public static File file() {
        return file;
    }

    static void remove() {
        file.delete();
        Gdx.app.exit();
    }

    public static String problem(File file) {
        try (JarFile jar = new JarFile(file)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return "This file is not a phone game.";
            }
            Attributes attrs = manifest.getMainAttributes();
            String name = attrs.getValue("MIDlet-Name");
            String version = attrs.getValue("MIDlet-Version");
            if (name == null) {
                return "This file is not a phone game.";
            }
            if (!NAME.equals(name.trim())) {
                return "This is " + name.trim() + ", not " + NAME + ".";
            }
            if (version == null || !VERSION.equals(version.trim())) {
                return "This is " + NAME + " " + (version == null ? "of an unknown version" : version.trim())
                        + ". Version " + VERSION + " is needed.";
            }
            if (!CODE.equals(code(jar))) {
                return "This is a different build of " + NAME + " " + VERSION + ". The 240x320 build is needed.";
            }
            return null;
        } catch (IOException e) {
            return "This file can't be opened as a jar.";
        }
    }

    private static String code(JarFile jar) throws IOException {
        TreeSet<String> names = new TreeSet<>();
        for (JarEntry e : java.util.Collections.list(jar.entries())) {
            if (e.getName().endsWith(".class")) {
                names.add(e.getName());
            }
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        byte[] buffer = new byte[8192];
        for (String name : names) {
            digest.update(name.getBytes(StandardCharsets.UTF_8));
            try (InputStream in = jar.getInputStream(jar.getEntry(name))) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
