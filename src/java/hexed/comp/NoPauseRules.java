package hexed.comp;

import mindustry.game.Gamemode;
import mindustry.game.Rules;

public class NoPauseRules extends Rules {
    @Override
    public Gamemode mode() {
        return Gamemode.pvp;
    }
}
