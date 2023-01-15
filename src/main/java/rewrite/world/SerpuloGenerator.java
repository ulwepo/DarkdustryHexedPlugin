package rewrite.world;

import arc.func.Cons;
import arc.math.Mathf;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.game.Rules;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.world.Block;
import mindustry.world.Tiles;

import static rewrite.utils.Decorations.*;
import static rewrite.utils.Utils.*;

public class SerpuloGenerator extends Generator {

    public Block riverFloor = Blocks.air, riverFloor2 = Blocks.air, riverBlock = Blocks.air;

    public SerpuloGenerator(String name, Block[][] terrain, Cons<Rules> ruleSetter) {
        super(name, Planets.serpulo, terrain, rules -> {
            rules.loadout = null; // TODO

            ruleSetter.get(rules);
        });
    }

    @Override
    public void generateOres(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input, getOreFilters(-0.04f, 4f,
                Blocks.oreCopper,
                Blocks.oreLead,
                Blocks.oreScrap,
                Blocks.oreCoal,
                Blocks.oreTitanium,
                Blocks.oreThorium
        ));
    }

    @Override
    public void generateLandscape(Tiles tiles, GenerateInput input) {
        generateRivers(tiles, input);
        generateSlag(tiles, input);
        generateMoss(tiles, input);
        generateTrees(tiles);
    }

    public void generateRivers(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input, new RiverNoiseFilter() {{
            floor = riverFloor;
            floor2 = riverFloor2;
            block = riverBlock;
        }});
    }

    public void generateSlag(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input, new NoiseFilter() {{
            scl = 20f;
            threshold = 0.6f;

            floor = Blocks.slag;
            block = Blocks.air;
            target = Blocks.magmarock;
        }});
    }

    public void generateMoss(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input, new ScatterFilter() {{
            chance = 0.1f;

            flooronto = Blocks.moss;
            floor = Blocks.sporeMoss;
        }});
    }

    public void generateTrees(Tiles tiles) {
        tiles.eachTile(tile -> {
            if (!Mathf.chance(0.05f)) return;

            var tree = trees.get(tile.floor());
            if (tree == null || !tile.solid()) return;

            tile.setBlock(tree);
        });

        tiles.eachTile(tile -> {
            if (!Mathf.chance(0.005f)) return;

            var tree = deadTrees.get(tile.floor());
            if (tree == null || !tile.solid()) return;

            tile.setBlock(tree);
        });
    }
}