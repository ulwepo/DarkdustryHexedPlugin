package hexed;

import arc.func.Intc2;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Structs;
import arc.util.Tmp;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.world.Tiles;
import mindustry.world.blocks.storage.CoreBlock;

import static arc.Core.app;
import static arc.math.Mathf.chance;
import static hexed.Hex.*;
import static hexed.Main.*;
import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;
import static mindustry.content.Liquids.water;
import static mindustry.content.Planets.serpulo;
import static mindustry.world.blocks.environment.SteamVent.offsets;

public class HexedGenerator {

    public static void generate(Tiles tiles) {
        int width = tiles.width, height = tiles.height;

        type.apply(tiles);

        getHexes((x, y) -> {
            // вырезаем хекс
            Geometry.circle(x, y, width, height, Hex.radius * 2, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, Hex.radius * 2, cx, cy)) tiles.getn(cx, cy).remove();
            });

            for (int side = 0; side < 3; side++) {
                float angle = side * 120f - 30f;

                Tmp.v1.trnsExact(angle, spacing + 12).add(x, y);
                if (!Structs.inBounds((int) Tmp.v1.x, (int) Tmp.v1.y, width, height)) continue;

                Tmp.v1.trnsExact(angle, spacing / 2f + 7).add(x, y);
                Bresenham2.line(x, y, (int) Tmp.v1.x, (int) Tmp.v1.y, (cx, cy) -> Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).remove()));
            }
        });

        // убираем стенки с жидкостных блоков и добавляем немного декораций
        tiles.eachTile(tile -> {
            var floor = tile.floor();
            var block = tile.block();

            if (floor.liquidDrop != null) tile.remove();

            if (block == air && floor.decoration != air && chance(0.02d))
                tile.setBlock(floor.decoration);

            if (block == air && vents.containsKey(floor) && chance(0.0075d))
                for (var point : offsets)
                    tiles.getc(tile.x + point.x, tile.y + point.y).setFloor(vents.get(floor).asFloor());
        });

        // меняем пол в центре хекса
        getHexes((x, y) -> {
            var corePlace = tiles.getn(x, y).getLinkedTilesAs(coreNucleus, new Seq<>());

            boolean hasWater = corePlace.contains(tile -> tile.floor().liquidDrop == water);

            corePlace.each(tile -> {
                tile.remove();
                tile.setFloor(hasWater ? sandWater.asFloor() : coreZone.asFloor());
            });
        });

        state.map = new Map(StringMap.of(
                "name", type.name,
                "author", "Hexed Plugin",
                "description", "A map for Darkdustry Hexed. Automatically generated."
        ));
    }

    public static void getHexes(Intc2 intc) {
        float h = Mathf.sqrt3 * spacing / 4f;
        for (int x = 0; x < size / spacing - 2; x++) {
            for (int y = 0; y < size / h - 2; y++) {
                int cx = (int) (x * spacing * 1.5f + (y % 2) * spacing * 0.75f) + spacing / 2;
                int cy = (int) (y * h) + spacing / 2;

                intc.get(cx, cy);
            }
        }
    }

    public static void applyFilters(Tiles tiles, GenerateFilter... filters) {
        var input = new GenerateInput();

        for (var filter : filters) {
            filter.randomize();
            input.begin(tiles.width, tiles.height, tiles::getn);
            filter.apply(tiles, input);
        }
    }

    public static void loadout(Player player, Hex hex) {
        var loadout = type.planet == serpulo ? serpuloLoadout : erekirLoadout;
        var start = type.planet == serpulo ? serpuloStart : erekirStart;

        var coreTile = start.tiles.find(stile -> stile.block instanceof CoreBlock);
        int sx = hex.x - coreTile.x, sy = hex.y - coreTile.y;

        start.tiles.each(stile -> {
            var tile = world.tile(stile.x + sx, stile.y + sy);
            if (tile == null) return;

            tile.setNet(stile.block, player.team(), stile.rotation);
            tile.getLinkedTiles(t -> t.setFloorNet(t.floor().isDeep() ? darkPanel3 : tile.floor()));

            tile.build.configureAny(stile.config);

            if (stile == coreTile)
                loadout.each(stack -> Call.setItem(tile.build, stack.item, stack.amount));
        });

        app.post(() -> Call.setCameraPosition(player.con, hex.wx, hex.wy));
    }
}