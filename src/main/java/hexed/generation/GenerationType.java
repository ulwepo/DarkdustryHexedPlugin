package hexed.generation;

import arc.struct.Seq;
import arc.util.Structs;
import mindustry.content.Blocks;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.maps.filters.NoiseFilter;
import mindustry.world.Block;
import mindustry.world.Tiles;

public class GenerationType {

    public Block[] blocks;
    public Block[] floors = Structs.arr(Blocks.sand, Blocks.darksand, Blocks.moss, Blocks.hotrock);

    public void apply(Tiles tiles) {
        var filters = new Seq<NoiseFilter>();

        for (var f : floors) {
            filters.add(new NoiseFilter() {{
                floor = f;
                block = Blocks.air;
                target = Blocks.air;
            }});
        }

        GenerateInput input = new GenerateInput();
        filters.each(filter -> {
            filter.randomize();
            input.begin(tiles.width, tiles.height, tiles::getn);
            filter.apply(tiles, input);
        });
    }
}
