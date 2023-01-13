package rewrite.world;

import arc.func.Cons;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.game.Rules;
import mindustry.world.Block;
import mindustry.world.Tiles;
import mindustry.maps.filters.GenerateFilter.GenerateInput;

import static rewrite.utils.Utils.*;

public class SerpuloGenerator extends Generator {

    public SerpuloGenerator(String name, Block[][] terrain, Cons<Rules> ruleSetter) {
        super(name, Planets.serpulo, terrain, ruleSetter);
    }

    @Override
    public void generateOres(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input, getOreFilters(-0.04f, 0f,
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
    }

    public void generateRivers(Tiles tiles, GenerateInput input) {
        applyFilters(tiles, input);
    }
}