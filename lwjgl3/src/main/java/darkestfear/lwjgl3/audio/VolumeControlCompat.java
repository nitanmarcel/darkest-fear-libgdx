package darkestfear.lwjgl3.audio;

import javax.microedition.media.control.VolumeControl;

public class VolumeControlCompat implements VolumeControl {
    private final PlayerCompat _Player;

    public VolumeControlCompat(PlayerCompat player) {
        _Player = player;
    }

    @Override
    public int setLevel(int level) {
        level = Math.max(0, Math.min(100, level));
        _Player.setVolume(level);
        return level;
    }
}
