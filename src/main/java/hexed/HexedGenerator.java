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
import mindustry.world.Tile;
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

            var corePlace = new Seq<Tile>();

            for (int cx = x - 2; cx <= x + 2; cx++)
                for (int cy = y - 2; cy <= y + 2; cy++)
                    corePlace.add(tiles.getn(cx, cy));

            boolean hasWater = corePlace.contains(tile -> tile.floor().liquidDrop == water);

            // меняем пол в центре хекса
            corePlace.each(tile -> tile.setFloor((hasWater ? sandWater.asFloor() : coreZone.asFloor())));
        });

        // убираем стенки с жидкостных блоков и добавляем немного декораций
        tiles.eachTile(tile -> {
            if (tile.floor().liquidDrop != null)
                tile.remove();

            if (tile.block() == air && tile.floor().decoration != air && chance(0.02d))
                tile.setBlock(tile.floor().decoration);
        });

        state.map = new Map(StringMap.of(
                "name", type.name,
                "author", "Hexed Plugin",
                "description", "A map for Darkdustry Hexed. Automatically generated."
        ));
    }

    public static void getHexes(Intc2 intc) {
        float h = Mathf.sqrt3 * spacing / 2;
        for (int x = 0; x < size / spacing - 2; x++) {
            for (int y = 0; y < size / (h / 2) - 2; y++) {
                int cx = (int) (x * spacing * 1.5 + (y % 2) * spacing * 3.0 / 4) + spacing / 2;
                int cy = (int) (y * h / 2) + spacing / 2;

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
        var start = type.planet == serpulo ? serpuloStart : erekirStart;
        var coreTile = start.tiles.find(stile -> stile.block instanceof CoreBlock);
        int sx = hex.x - coreTile.x, sy = hex.y - coreTile.y;

        start.tiles.each(stile -> {
            var tile = world.tile(stile.x + sx, stile.y + sy);
            if (tile == null) return;

            tile.setNet(stile.block, player.team(), stile.rotation);
            tile.getLinkedTiles(new Seq<>()).each(t -> t.floor().isDeep(), t -> t.setFloorNet(darkPanel3));

            if (stile.config != null) tile.build.configureAny(stile.config);

            if (stile == coreTile) for (var stack : state.rules.loadout) {
                Call.setItem(tile.build, stack.item, stack.amount);
            }
        });

        app.post(() -> Call.setCameraPosition(player.con, hex.wx, hex.wy));
    }
}
