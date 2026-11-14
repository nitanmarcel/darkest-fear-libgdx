package javax.microedition.media;

import java.io.InputStream;

import javax.microedition.media.control.VolumeControlCompat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

public class PlayerCompat implements Player {
    private InputStream _Stream;
    private Sequencer _Sequencer;
    private Synthesizer _Synthesizer;
    private Sequence _Sequence;

    public PlayerCompat(InputStream stream) {
        _Stream = stream;
    }

	@Override
	public Control getControl(String controlType) {
	    return new VolumeControlCompat(_Synthesizer);
		//throw new UnsupportedOperationException("Unimplemented method 'getControl'");
	}

	@Override
	public void prefetch() throws MediaException {
    	try {
         _Sequence = MidiSystem.getSequence(_Stream);

         _Sequencer = MidiSystem.getSequencer(false);
         _Synthesizer = MidiSystem.getSynthesizer();

         _Sequencer.open();
         _Synthesizer.open();

         _Sequencer.getTransmitter().setReceiver(_Synthesizer.getReceiver());
         _Sequencer.setSequence(_Sequence);
    }
    	catch (Exception e) { }
        //throw new UnsupportedOperationException("Unimplemented method 'prefetch'");
	}

	@Override
	public void start() throws MediaException {
	    _Sequencer.start();
		//throw new UnsupportedOperationException("Unimplemented method 'start'");
	}

	@Override
	public void stop() throws MediaException {
	    _Sequencer.stop();
		//throw new UnsupportedOperationException("Unimplemented method 'stop'");
	}

	@Override
	public void deallocate() {
	    _Sequencer = null;
		// TODO Auto-generated method stub
		//throw new UnsupportedOperationException("Unimplemented method 'deallocate'");
	}

	@Override
	public void close() {
	    _Sequencer.close();
		//throw new UnsupportedOperationException("Unimplemented method 'close'");
	}

	@Override
	public void setLoopCount(int count) {
	    _Sequencer.setLoopCount(count);
		// throw new UnsupportedOperationException("Unimplemented method 'setLoopCount'");
	}

	@Override
	public int getState() {
		return _Sequencer.isRunning() ? 1 : 0;
		//throw new UnsupportedOperationException("Unimplemented method 'getState'");
	}
}
