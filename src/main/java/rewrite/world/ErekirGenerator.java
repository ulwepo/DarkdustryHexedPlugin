package rewrite.world;

import arc.func.Cons;
import arc.math.Mathf;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.game.Rules;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.world.*;
import mindustry.world.blocks.environment.SteamVent;

import static mindustry.world.blocks.environment.SteamVent.offsets;
import static rewrite.utils.Decorations.*;
import static rewrite.utils.Utils.*;

public class ErekirGenerator extends Generator {

    public ErekirGenerator(String name, Block[][] terrain, Cons<Rules> ruleSetter) {
        super(name, Planets.erekir, terrain, rules -> {
            rules.loadout = null; // TODO

            ruleSetter.get(rules);
        });
    }

    @Override
    public void generateOres(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input, getOreFilters(-0.04f, 4f,
                Blocks.oreBeryllium,
                Blocks.oreTungsten,
                Blocks.oreThorium
        ));

        applyFilters(tiles, input, getOreFilters(-0.08f, 2f,
                Blocks.wallOreBeryllium,
                Blocks.wallOreTungsten,
                Blocks.wallOreThorium
        ));

        applyFilters(tiles, input,
                new NoiseFilter() {{
                    scl = 10f;
                    threshold = 0.55f;
                    floor = Blocks.air;
                    block = Blocks.graphiticWall;
                }},

                new ClearFilter() {{
                    target = Blocks.graphiticWall;
                    replace = Blocks.carbonStone;
                }}
        );
    }

    @Override
    public void generateLandscape(Tiles tiles, GenerateInput input) {
        generateLakes(tiles, input);
        generateVents(tiles);
        generateCrystals(tiles);
    }

    public void generateLakes(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input,
                new NoiseFilter() {{
                    threshold = 0.8f;
                    floor = Blocks.arkyciteFloor;
                    block = Blocks.air;
                }},

                new NoiseFilter() {{
                    threshold = 0.8f;
                    floor = Blocks.slag;
                    block = Blocks.air;
                }},

                new BlendFilter() {{
                    block = Blocks.arkyciteFloor;
                    floor = Blocks.arkyicStone;
                }},

                new BlendFilter() {{
                    block = Blocks.arkyciteFloor;
                    floor = Blocks.arkyicWall;
                }},

                new BlendFilter() {{
                    block = Blocks.slag;
                    floor = Blocks.regolith;
                }},

                new BlendFilter() {{
                    block = Blocks.slag;
                    floor = Blocks.regolithWall;
                }}
        );
    }

    public void generateVents(Tiles tiles) {
        tiles.eachTile(tile -> {
            if (!Mathf.chance(0.005f)) return;

            var vent = vents.get(tile.floor());
            if (vent == null || anyWithin(tiles, tile.x, tile.y, 3, other -> other.solid() || other.floor() instanceof SteamVent || other.floor().isLiquid)) return;

            for (var point : offsets)
                tile.nearby(point.x + 1, point.y + 1).setFloor(vent.asFloor());
        });
    }

    public void generateCrystals(Tiles tiles) {
        tiles.eachTile(tile -> {
            if (!Mathf.chance(0.05f)) return;

            var crystal = crystals.get(tile.floor());
            if (crystal == null || !tile.solid()) return;

            tile.setBlock(crystal);
        });
    }
}