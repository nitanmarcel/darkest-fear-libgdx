package javax.microedition.rms;

import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class RecordStore {

    private static Preferences _prefs = Gdx.app.getPreferences("RecordStore");

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException {
        return new RecordStore();
    }

    public static void deleteRecordStore(String recordStoreName) throws RecordStoreException {
        _prefs.clear();
        // throw new UnsupportedOperationException("not implemented");
    }

    public void closeRecordStore() throws RecordStoreException {
        //throw new UnsupportedOperationException("not implemented");
    }

    public int getNumRecords() throws RecordStoreException {
        return 1;
        //throw new UnsupportedOperationException("not implemented");
    }

    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
        return 1;
        //throw new UnsupportedOperationException("not implemented");
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException {
        _prefs.getString(".rec" + recordId, encode(newData));
        //throw new UnsupportedOperationException("not implemented");
    }

    public byte[] getRecord(int recordId) throws RecordStoreException {
        String record = _prefs.getString(".rec" + recordId);
        if (record.isEmpty()) return null;
        return decode(record);
        //throw new UnsupportedOperationException("not implemented");
    }

    String encode(byte[] data) {
        return new String(Base64Coder.encode(data));
    }

    byte[] decode(String s) {
        return Base64Coder.decode(s);
    }
}
