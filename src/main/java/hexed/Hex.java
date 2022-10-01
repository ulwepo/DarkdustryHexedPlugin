package hexed;

import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.math.geom.Position;
import hexed.HexData.PlayerData;
import mindustry.game.Team;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Arrays;

import static hexed.Main.type;
import static mindustry.Vars.*;

public class Hex {

    public static final int size = 516;
    public static final int radius = 37;
    public static final int spacing = 78;

    public final int id;
    public final int x, y;
    public final float wx, wy;
    public final float[] progress = new float[256];

    public PlayerData controller;

    public Hex(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.wx = x * tilesize;
        this.wy = y * tilesize;
    }

    public void updateController() {
        controller = findController();
    }

    public float getProgressPercent(Team team) {
        return progress[team.id] / Main.itemRequirement * 100;
    }

    public boolean hasCore() {
        return world.tile(x, y).team() != Team.derelict && world.tile(x, y).block() instanceof CoreBlock;
    }

    public PlayerData findController() {
        if (hasCore()) return HexData.getData(world.tile(x, y).team());

        Arrays.fill(progress, 0);

        for (int cx = x - radius; cx < x + radius; cx++) {
            for (int cy = y - radius; cy < y + radius; cy++) {
                var tile = world.tile(cx, cy);
                if (tile != null && tile.synthetic() && contains(tile) && tile.block().requirements != null) {
                    for (var stack : tile.block().requirements) {
                        progress[tile.team().id] += stack.amount * Mathf.sqrt(stack.item.cost);
                    }
                }
            }
        }

        var data = state.teams.getActive()
                .filter(team -> team.team != Team.derelict)
                .max(team -> progress[team.team.id]);

        if (data != null && progress[data.team.id] >= Main.itemRequirement) {
            world.tile(x, y).setNet(type.planet.defaultCore, data.team, 0);
            return HexData.getData(data.team);
        }

        return null;
    }

    public boolean contains(float x, float y) {
        return Intersector.isInsideHexagon(wx, wy, radius * 2 * tilesize, x, y);
    }

    public boolean contains(Position position) {
        return contains(position.getX(), position.getY());
    }
}
