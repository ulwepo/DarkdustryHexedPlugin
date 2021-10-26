package org.darkdustry.mindustry.hexed.comp;

import mindustry.game.Gamemode;
import mindustry.game.Rules;

public class NoPauseRules extends Rules {

	public Gamemode mode() {
		return Gamemode.pvp;
	}
}
