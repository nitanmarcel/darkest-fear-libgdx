package darkestfear.android;

import javax.microedition.media.control.VolumeControl;

public class AndroidVolumeControl implements VolumeControl {

    private final AndroidPlayer _Player;

    public AndroidVolumeControl(AndroidPlayer player) {
        _Player = player;
    }

    @Override
    public int setLevel(int level) {
        if (_Player == null) {
            return 0;
        }
        level = Math.max(0, Math.min(127, level));
        _Player.setVolume(level);
        return level;
    }
}
