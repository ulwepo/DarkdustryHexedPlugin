package hexed;

import arc.func.Cons;
import arc.func.Floatc;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Bresenham2;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Structs;
import arc.util.Tmp;
import hexed.generation.GenerationType;
import hexed.generation.GenerationTypes;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.content.Weathers;
import mindustry.game.Rules;
import mindustry.game.Schematic;
import mindustry.maps.Map;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.type.Planet;
import mindustry.type.Weather.WeatherEntry;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;

import static hexed.Main.*;
import static mindustry.Vars.*;

public class HexedGenerator {

    public static void generate(Tiles tiles) {
        GenerationType type = GenerationTypes.beta;

        int width = tiles.width, height = tiles.height;
        tiles.each((x, y) -> tiles.set(x, y, new Tile(x, y, type.defaultFloor, Blocks.air, type.defaultBlock)));

        getHexes().each(packed -> {
            int x = Point2.x(packed), y = Point2.y(packed);

            // вырезаем хекс
            Geometry.circle(x, y, width, height, Hex.diameter, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, Hex.diameter, cx, cy)) tiles.getn(cx, cy).remove();
            });

            // вырезаем проходы
            circle(3, 360f / 3 / 2f - 90, f -> { // что это ._.
                Tmp.v1.trnsExact(f, Hex.spacing + 12);
                if (!Structs.inBounds(x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, width, height)) return;

                Tmp.v1.trnsExact(f, Hex.spacing / 2f + 7);
                Bresenham2.line(x, y, x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, (cx, cy) -> Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).remove()));
            });

            // меняем пол в центре хексов
            for (int cx = x - 2; cx <= x + 2; cx++)
                for (int cy = y - 2; cy <= y + 2; cy++)
                    tiles.getn(cx, cy).setFloor(Blocks.coreZone.asFloor());
        });

        type.apply(tiles);

        GenerateInput input = new GenerateInput();
        getOres().addAll(getDefaultFilters()).addAll(mode.filters).each(filter -> {
            filter.randomize();
            input.begin(width, height, tiles::getn);
            filter.apply(tiles, input);
        });

        state.map = new Map(StringMap.of(
                "name", mode.displayName,
                "author", "[cyan]\uE810 [royal]Darkness [cyan]\uE810",
                "description", "A map for Darkdustry Hexed. Automatically generated."
        ));
    }

    public static IntSeq getHexes() {
        IntSeq array = new IntSeq();
        float h = Mathf.sqrt3 * Hex.spacing / 2;
        for (int x = 0; x < Hex.size / Hex.spacing - 2; x++) {
            for (int y = 0; y < Hex.size / (h / 2) - 2; y++) {
                int cx = (int) (x * Hex.spacing * 1.5 + (y % 2) * Hex.spacing * 3.0 / 4) + Hex.spacing / 2;
                int cy = (int) (y * h / 2) + Hex.spacing / 2;
                array.add(Point2.pack(cx, cy));
            }
        }
        return array;
    }

    public static Seq<GenerateFilter> getOres() {
        Seq<GenerateFilter> filters = new Seq<>();
        for (Block block : mode.planet == Planets.serpulo ? serpuloOres : erekirOres) {
            filters.add(new OreFilter() {{
                threshold = block.asFloor().oreThreshold - 0.04f;
                scl = block.asFloor().oreScale + 8f;
                ore = block;
            }});
        }

        return filters;
    }

    public static Seq<GenerateFilter> getDefaultFilters() {
        Seq<GenerateFilter> filters = new Seq<>();
        content.blocks().each(block -> block.isFloor() && block.inEditor && block.asFloor().decoration != Blocks.air, block -> {
            var filter = new ScatterFilter();
            filter.flooronto = block.asFloor();
            filter.block = block.asFloor().decoration;
            filters.add(filter);
        });

        return filters;
    }

    public static void circle(int points, float offset, Floatc cons) {
        for (int i = 0; i < points; i++)
            cons.get(offset + i * 360f / points);
    }
}
