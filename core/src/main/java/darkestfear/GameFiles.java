package darkestfear;

import java.io.File;

public interface GameFiles {

    interface Chosen {
        void file(File file);
    }

    File dir();

    void choose(String extension, Chosen chosen);

    ClassLoader load(File jar) throws Exception;
}
