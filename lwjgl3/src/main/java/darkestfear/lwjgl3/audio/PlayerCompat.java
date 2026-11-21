package darkestfear.lwjgl3.audio;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;

public class PlayerCompat implements Player {
    private InputStream _Stream;
    private Sequencer _Sequencer;
    private Synthesizer _Synthesizer;
    private Sequence _Sequence;
    private volatile boolean _Started;

    private final ExecutorService _Executor = Executors.newSingleThreadExecutor();

    public PlayerCompat(InputStream stream) {
        _Stream = stream;
    }

    @Override
    public Control getControl(String controlType) {
        return new VolumeControlCompat(this);
    }

    void setVolume(int level) {
        _Executor.execute(() -> {
            if (_Synthesizer == null || !_Synthesizer.isOpen()) {
                return;
            }
            int volume = (int) Math.round(127 * Math.sqrt(level / 100.0));
            for (MidiChannel channel : _Synthesizer.getChannels()) {
                if (channel != null) {
                    channel.controlChange(7, volume);
                }
            }
        });
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
        _Started = true;
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.start();
        });
    }

    @Override
    public void stop() throws MediaException {
        _Started = false;
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.stop();
        });
    }

    @Override
    public void deallocate() {
    }

    @Override
    public void close() {
        _Started = false;
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.close();
            if (_Synthesizer != null)
                _Synthesizer.close();
            _Sequencer = null;
        });
        _Executor.shutdown();
    }

    @Override
    public void setLoopCount(int count) {
        _Executor.execute(() -> {
            if (_Sequencer != null)
                _Sequencer.setLoopCount(count < 0 ? Sequencer.LOOP_CONTINUOUSLY : Math.max(0, count - 1));
        });
    }

    @Override
    public int getState() {
        return _Started ? 1 : 0;
    }
}
