package hexed.generation;

import arc.func.Cons;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.game.Rules;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.type.Planet;
import mindustry.world.Block;
import mindustry.world.Tiles;

public class GenerationType {

    public final String name;
    public final Planet planet;

    public final Block defaultFloor;
    public final Block defaultBlock;

    public final Cons<Rules> ruleSetter;
    public final Seq<GenerateFilter> filters = new Seq<>();

    public GenerationType(String name, Planet planet, Block floor, Block block, Cons<Rules> ruleSetter, GenerateFilter... filters) {
        this.name = name;
        this.planet = planet;

        this.defaultFloor = floor;
        this.defaultBlock = block;

        this.ruleSetter = ruleSetter;
        this.filters.addAll(filters);
    }

    public GenerationType(String name, Planet planet, Cons<Rules> ruleSetter, GenerateFilter... filters) {
        this(name, planet, Blocks.stone, Blocks.stoneWall, ruleSetter, filters);
    }

    public void apply(Tiles tiles) {
        GenerateInput input = new GenerateInput();
        filters.each(filter -> {
            filter.randomize();
            input.begin(tiles.width, tiles.height, tiles::getn);
            filter.apply(tiles, input);
        });
    }

    public void applyRules(Rules rules) {
        ruleSetter.get(rules);
    }
}
