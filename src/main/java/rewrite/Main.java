package rewrite;

import arc.Events;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Timer;
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

        Events.on(BlockBuildEndEvent.class, event -> {
            Hex hex = hexes.min(h -> event.tile.dst(h));
            if (hex == null || hex.hasCore()) return;

            int speed = 1; // TODO for Darkness: different capture speed for different blocks
            hex.increaseSpeed(event.unit.team, event.breaking ? -speed : speed); // negative speed if the block was broken
        });

        Events.on(BlockDestroyEvent.class, event -> {
            Hex hex = hexes.min(h -> event.tile.dst(h));
            if (hex == null) return;

            int speed = 1;
            hex.increaseSpeed(event.tile.team(), -speed);
        });

        Events.on(PlayEvent.class, event -> Generators.erekir.applyRules(state.rules));

        Events.run(Trigger.update, () -> {

        });

        Timer.schedule(() -> hexes.each(Hex::update), 0f, 1f);

        world.loadGenerator(size, size, Generators.erekir::generate);

        netServer.openServer();
    }
}