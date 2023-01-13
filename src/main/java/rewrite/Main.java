package rewrite;

import arc.Events;
import arc.func.Cons;
import arc.struct.Seq;
import mindustry.game.EventType.*;
import mindustry.game.Rules;
import mindustry.mod.Plugin;
import rewrite.utils.Decorations;
import rewrite.world.Generators;
import useful.Bundle;

import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final int size = 516;
    public static final int spacing = 78;
    public static final int radius = 37, diameter = radius * 2;

    public static final Seq<Hex> hexes = new Seq<>();
    public static final Seq<Fraction> fractions = new Seq<>();

    public static final Cons<Rules> defaultRuleSetter = rules -> {
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        rules.enemyCoreBuildRadius = radius * tilesize;

        rules.logicUnitBuild = true;
        rules.pvp = false;
        rules.canGameOver = false;
        rules.coreCapture = true;
        rules.reactorExplosions = true;
        rules.damageExplosions = true;
        rules.fire = false;

        rules.modeName = "Hexed";
    };

    @Override
    public void init() {
        Bundle.load(Main.class);
        Decorations.load();

        Generators.load();

        Events.on(PlayerJoin.class, event -> {

        });

        Events.on(PlayerLeave.class, event -> {

        });

        Events.on(PlayEvent.class, event -> Generators.erekir.applyRules(state.rules));

        Events.run(Trigger.update, () -> {

        });

        world.loadGenerator(size, size, Generators.erekir::generate);

        netServer.openServer();
    }
}