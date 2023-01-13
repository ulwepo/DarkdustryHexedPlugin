package rewrite;

import arc.math.geom.Intersector;
import arc.math.geom.Position;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import static mindustry.Vars.*;
import static rewrite.Main.*;

public class Hex implements Position {

    public final int x, y;
    public final int id;

    public final float[] progress = new float[256];

    public Fraction controller;

    public Hex(int x, int y) {
        this.x = x;
        this.y = y;

        this.id = hexes.add(this).size;
    }

    public void update() {
        if (hasCore()) return;


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
}