package darkestfear.lwjgl3.audio;

import javax.microedition.media.control.VolumeControl;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;

public class VolumeControlCompat implements VolumeControl {
    private Synthesizer _Synthesizer;

    public VolumeControlCompat(Synthesizer synthesizer) {
        _Synthesizer = synthesizer;
    }

    @Override
    public int setLevel(int level) {
        if (_Synthesizer == null || !_Synthesizer.isOpen()) {
            return 0;
        }

        level = Math.max(0, Math.min(127, level));

        for (MidiChannel channel : _Synthesizer.getChannels()) {
            if (channel != null) {
                channel.controlChange(7, level);
            }
        }
        return 1;
    }
}
