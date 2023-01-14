package rewrite;

import arc.math.geom.Intersector;
import arc.math.geom.Position;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import static mindustry.Vars.*;
import static rewrite.Main.*;

public class Hex implements Position {

    public final int x, y;
    public final int id;

    public final int[] capture = new int[256];

    public Fraction controller;

    public Hex(int x, int y) {
        this.x = x;
        this.y = y;

        this.id = hexes.add(this).size;
    }

    public void update() {
        if (hasCore()) return;

        for (Fraction fraction : fractions) {
            increaseProgress(fraction.team, getSpeed(fraction.team));
            if (getProgress(fraction.team) >= 1000) captured(fraction);
        }
    }

    public boolean hasCore() {
        return buildOn() instanceof CoreBuild core && core.team != Team.derelict;
    }

    public Tile tileOn() {
        return world.tile(x, y);
    }

    public Building buildOn() {
        return world.build(x, y);
    }

    public boolean in(Position position) {
        return in(position.getX(), position.getY());
    }

    public boolean in(float x, float y) {
        return Intersector.isInsideHexagon(getX(), getY(), diameter * tilesize, x, y);
    }

    @Override
    public float getX() {
        return x * tilesize;
    }

    @Override
    public float getY() {
        return y * tilesize;
    }

    // region progress

    public void captured(Fraction fraction) {
        this.controller = fraction;
        tileOn().setNet(Blocks.coreBastion, fraction.team, 0);
    }

    /** Returns capturing progress of the team in tenths of a percent. */
    public int getProgress(Team team) {
        return capture[team.id] & 0xFFFF; // second part of integer
    }

    /** Increases capture progress of the team by the given amount. */
    public void increaseProgress(Team team, int amount) {
        capture[team.id] += amount;
    }

    /** Returns capture speed of the team in tenths of a percent. */
    public int getSpeed(Team team) {
        return capture[team.id] >> 16;
    }

    /** Increases capture speed of the team by the given amount. */
    public void increaseSpeed(Team team, int amount) {
        capture[team.id] += amount << 16;
    }

    // endregion
}