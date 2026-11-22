package darkestfear.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.dx.command.dexer.Main;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import dalvik.system.DexClassLoader;

import darkestfear.DarkestFearGame;
import darkestfear.GameFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.media.Manager;

public class AndroidLauncher extends AndroidApplication implements GameFiles {

    private static final int PICK = 1;
    private static final String DEXED = "game.dex.jar";

    private Chosen chosen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Manager.setPlayerFactory((stream, packExtension) -> new AndroidPlayer(stream));
        if (!new File(getFilesDir(), DarkestFearGame.JAR).exists()) {
            new File(getCodeCacheDir(), DEXED).delete();
        }
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new DarkestFearGame(this), config);
    }

    @Override
    public File dir() {
        return getFilesDir();
    }

    @Override
    public void choose(String extension, Chosen chosen) {
        this.chosen = chosen;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        runOnUiThread(() -> startActivityForResult(intent, PICK));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        new Thread(() -> {
            File picked = new File(getCacheDir(), "picked.jar");
            try (InputStream in = getContentResolver().openInputStream(data.getData());
                 OutputStream out = new FileOutputStream(picked)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } catch (IOException e) {
                Log.e("DarkestFear", "reading the chosen file failed", e);
            }
            chosen.file(picked);
        }).start();
    }

    @Override
    public ClassLoader load(File jar) throws Exception {
        File dexed = new File(getCodeCacheDir(), DEXED);
        if (!dexed.exists() || dexed.lastModified() < jar.lastModified()) {
            dexed.delete();
            Main.Arguments args = new Main.Arguments();
            args.outName = dexed.getPath();
            args.jarOutput = true;
            args.fileNames = new String[]{jar.getPath()};
            args.makeOptionsObjects();
            if (Main.run(args) != 0) {
                throw new IOException("conversion failed");
            }
            // Android 14 refuses to load code from a writable file.
            dexed.setReadOnly();
        }
        return new DexClassLoader(dexed.getPath(), null, null, getClassLoader());
    }
}
