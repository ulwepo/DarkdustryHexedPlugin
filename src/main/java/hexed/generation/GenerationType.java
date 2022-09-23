package hexed.generation;

import arc.func.Cons;
import arc.struct.Seq;
import mindustry.game.Rules;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.type.Planet;
import mindustry.world.*;

import static arc.math.Mathf.*;
import static arc.util.noise.Simplex.noise2d;
import static hexed.Main.type;

public class GenerationType {

    public static final Seq<GenerationType> all = new Seq<>();

    public final String name;
    public final Planet planet;

    public final Cons<Rules> ruleSetter;
    public final Block[][] blocks;
    public final GenerateFilter[] filters;

    public GenerationType(String name, Planet planet, Cons<Rules> ruleSetter, Block[][] blocks, GenerateFilter... filters) {
        this.name = name;
        this.planet = planet;

        this.ruleSetter = ruleSetter;
        this.blocks = blocks;
        this.filters = filters;

        all.add(this);
    }

    public GenerationType(String name, Planet planet, Block[][] blocks, GenerateFilter... filters) {
        this(name, planet, rules -> {}, blocks, filters);
    }

    public static GenerationType next() {
        return all.random(type); // выбираем случайную карту из всех кроме текущей
    }

    public void apply(Tiles tiles) {
        int seed1 = random(10000), seed2 = random(10000);

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                int temp = clamp((int) ((noise2d(seed1, 12, 0.6, 1.0 / 400, x, y) - 0.5) * 10 * blocks.length), 0, blocks.length - 1);
                int elev = clamp((int) (((noise2d(seed2, 12, 0.6, 1.0 / 700, x, y) - 0.5) * 10 + 0.15f) * blocks[0].length), 0, blocks[0].length - 1);

                var floor = blocks[temp][elev];
                var wall = floor.asFloor().wall;

                tiles.set(x, y, new Tile(x, y, floor.id, 0, wall.id));
            }
        }

        var input = new GenerateInput();
        for (var filter : filters) {
            filter.randomize();
            input.begin(tiles.width, tiles.height, tiles::getn);
            filter.apply(tiles, input);
        }
    }

    public Rules applyRules(Rules rules) {
        ruleSetter.get(rules);
        return rules;
    }
}
