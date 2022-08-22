package ml.darkdustry.hexed;

import mindustry.content.Blocks;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.mod.Plugin;

public class Hexed extends Plugin {
    public static final Rules rules = new Rules() {
        @Override
        public Gamemode mode() {
            return Gamemode.pvp;
        }
    };

    @Override
    public void init() {
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        // rules.enemyCoreBuildRadius = Hex.diameter * tilesize / 2f;
        rules.unitDamageMultiplier = 1.25f;
        rules.logicUnitBuild = false;
        rules.pvp = false;
        rules.attackMode = true;
        rules.canGameOver = false;
        rules.coreCapture = true;
        rules.reactorExplosions = true;
        rules.fire = false;

        rules.bannedBlocks.add(Blocks.ripple);
        rules.modeName = "Hexed";
    }
}