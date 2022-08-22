package ml.darkdustry.hexed;

import arc.util.Log;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.mod.Plugin;
import ml.darkdustry.hexed.components.Bundles;

public class HexedMain extends Plugin {
    @Override
    public void init() {
        updateRules();
        Bundles.init();
    }

    public static final Rules rules = new Rules() {
        @Override
        public Gamemode mode() {
            return Gamemode.pvp;
        }
    };

    public static void info(String message, Object... object) {
        Log.infoTag("Hexed", Strings.format(message, object));
    }

    private void updateRules() {
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