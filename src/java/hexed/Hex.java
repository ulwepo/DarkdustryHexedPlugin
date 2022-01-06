package hexed;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.util.Timekeeper;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.gen.Call;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Arrays;

import static mindustry.Vars.*;

public class Hex {

    private final float[] progress = new float[256];

    public static final int size = 516;
    public static final int diameter = 74;
    public static final int radius = diameter / 2;
    public static final int spacing = 78;

    public final int id;
    public final int x, y;
    public final float wx, wy;
    public final float rad = radius * tilesize;

    public Team controller;

    public Timekeeper spawnTime = new Timekeeper(Main.spawnDelay);

    public Hex(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        wx = x * tilesize;
        wy = y * tilesize;
    }

    public void updateController() {
        controller = findController();
    }

    public float getProgressPercent(Team team) {
        return (progress[team.id] / Main.itemRequirement * 100);
    }

    public boolean hasCore() {
        return (world.tile(x, y).team() != Team.derelict && world.tile(x, y).block() instanceof CoreBlock);
    }

    public Team findController() {
        if (hasCore()) return world.tile(x, y).team();

        Arrays.fill(progress, 0);

        for (int cx = x - radius; cx < x + radius; cx++) {
            for (int cy = y - radius; cy < y + radius; cy++) {
                Tile tile = world.tile(cx, cy);
                if (tile != null && tile.synthetic() && contains(tile) && tile.block().requirements != null) {
                    for (ItemStack stack : tile.block().requirements) {
                        progress[tile.team().id] += stack.amount * stack.item.cost;
                    }
                }
            }
        }

        TeamData data = state.teams.getActive().max(t -> progress[t.team.id]);
        if (data != null && data.team != Team.derelict && progress[data.team.id] >= Main.itemRequirement) {
            world.tile(x, y).setNet(Blocks.coreShard, data.team, 0);
            return data.team;
        }
        return null;
    }

    public void destroy() {
        Call.effect(Fx.rocketSmokeLarge, x, y, 0, Color.white);
        world.tiles.eachTile(tile -> {
            if (tile.build != null && tile.block() != Blocks.air && contains(tile)) {
                tile.removeNet();
            }
        });
    }

    public boolean contains(float x, float y) {
        return Intersector.isInsideHexagon(wx, wy, rad * 2, x, y);
    }

    public boolean contains(Tile tile) {
        return contains(tile.worldx(), tile.worldy());
    }
}
