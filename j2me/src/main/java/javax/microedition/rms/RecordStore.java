package javax.microedition.rms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Base64Coder;

public class RecordStore {

    private static final Preferences _prefs = Gdx.app.getPreferences("RecordStore");

    private final String name;

    private RecordStore(String name) {
        this.name = name;
    }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException {
        return new RecordStore(recordStoreName);
    }

    public static void deleteRecordStore(String recordStoreName) throws RecordStoreException {
        _prefs.remove(recordStoreName + ".rec1");
        _prefs.flush();
    }

    public void closeRecordStore() throws RecordStoreException {
    }

    public int getNumRecords() throws RecordStoreException {
        return _prefs.contains(key(1)) ? 1 : 0;
    }

    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
        return 1;
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException {
        _prefs.putString(key(recordId), encode(newData));
        _prefs.flush();
    }

    public byte[] getRecord(int recordId) throws RecordStoreException {
        String record = _prefs.getString(key(recordId), "");
        if (record.isEmpty()) {
            return null;
        }
        return decode(record);
    }

    private String key(int recordId) {
        return name + ".rec" + recordId;
    }

    String encode(byte[] data) {
        return new String(Base64Coder.encode(data));
    }

    byte[] decode(String s) {
        return Base64Coder.decode(s);
    }
}
