package javax.microedition.rms;

public class RecordStore {

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }

    public static void deleteRecordStore(String recordStoreName) throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }

    public void closeRecordStore() throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }

    public int getNumRecords() throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }

    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }

    public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }

    public byte[] getRecord(int recordId) throws RecordStoreException {
        throw new UnsupportedOperationException("not implemented");
    }
}
