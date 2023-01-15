package rewrite.utils;

import arc.func.Boolf;
import arc.func.Intc2;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.noise.Simplex;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.maps.filters.OreFilter;
import mindustry.world.*;
import mindustry.world.blocks.environment.OreBlock;
import rewrite.filters.WallOreFilter;

import static rewrite.Main.*;

public class Utils {

    public static boolean anyWithin(Tiles tiles, int x, int y, int radius, Boolf<Tile> boolf) {
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cy = -radius; cy <= radius; cy++) {
                if (!Mathf.within(cx, cy, radius)) continue;

                var tile = tiles.get(cx + x, cy + y);
                if (tile != null && boolf.get(tile)) return true;
            }
        }

        return false;
    }

    public static void applyFilters(Tiles tiles, GenerateInput input, GenerateFilter... filters) {
        for (var filter : filters) applyFilter(tiles, input, filter);
    }

    public static void applyFilters(Tiles tiles, GenerateInput input, Seq<? extends GenerateFilter> filters) {
        filters.each(filter -> applyFilter(tiles, input, filter));
    }

    public static void applyFilter(Tiles tiles, GenerateInput input, GenerateFilter filter) {
        input.begin(tiles.width, tiles.height, tiles::get);

        filter.randomize();
        filter.apply(tiles, input);
    }

    public static Seq<OreFilter> getOreFilters(float oreThreshold, float oreScale, Block... ores) {
        return Seq.with(ores)
                .map(OreBlock.class::cast)
                .map(block -> new WallOreFilter() {{
                    threshold = block.oreThreshold + oreThreshold;
                    scl = block.oreScale + oreScale;
                    ore = block;

                    wallOre = block.wallOre;
                }});
    }

    public static int noise2d(int seed, int x, int y, int length) {
        return Mathf.clamp((int) ((Simplex.noise2d(seed, 12, 0.6f, 1f / 400f, x, y) - 0.5f) * 10 * length), 0, length - 1);
    }

    public static void getHexes(Intc2 cons) {
        float height = Mathf.sqrt3 * spacing / 4f;

        for (int x = 0; x < size / spacing - 2; x++) {
            for (int y = 0; y < size / height - 2; y++) {
                int cx = (int) (x * spacing * 1.5f + (y % 2) * spacing * 0.75f) + spacing / 2;
                int cy = (int) (y * height) + spacing / 2;

                cons.get(cx, cy);
            }
        }
    }
}