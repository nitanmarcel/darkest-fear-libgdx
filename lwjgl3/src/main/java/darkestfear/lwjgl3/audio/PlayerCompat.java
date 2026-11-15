package darkestfear.lwjgl3.audio;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

public class PlayerCompat implements Player {
    private InputStream _Stream;
    private Sequencer _Sequencer;
    private Synthesizer _Synthesizer;
    private Sequence _Sequence;

    private final ExecutorService _Executor = Executors.newSingleThreadExecutor();

    public PlayerCompat(InputStream stream) {
        _Stream = stream;
    }

    @Override
    public Control getControl(String controlType) {
        return new VolumeControlCompat(_Synthesizer);
    }

    @Override
    public void prefetch() throws MediaException {
        _Executor.execute(() -> {
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
        });
    }

    @Override
    public void start() throws MediaException {
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.start();
        });
    }

    @Override
    public void stop() throws MediaException {
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.stop();
        });
    }

    @Override
    public void deallocate() {
        _Sequencer = null;
    }

    @Override
    public void close() {
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.close();
        });
        _Executor.shutdown();
    }

    @Override
    public void setLoopCount(int count) {
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.setLoopCount(count);
        });
    }

    @Override
    public int getState() {
        return _Sequencer != null && _Sequencer.isRunning() ? 1 : 0;
    }
}
