package rewrite.world;

import arc.func.Cons;
import arc.math.Mathf;
import arc.math.geom.*;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Tmp;
import mindustry.content.Blocks;
import mindustry.game.Rules;
import mindustry.maps.Map;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.type.Planet;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.environment.Floor;

import static arc.math.geom.Geometry.d4c;
import static mindustry.Vars.state;
import static rewrite.Main.*;
import static rewrite.utils.Utils.*;

public abstract class Generator {

    public static final Seq<Generator> all = new Seq<>();

    public final String name;
    public final Planet planet;

    public final Block[][] terrain;
    public final Cons<Rules> ruleSetter;

    public Generator(String name, Planet planet, Block[][] terrain, Cons<Rules> ruleSetter) {
        this.name = name;
        this.planet = planet;

        this.terrain = terrain;
        this.ruleSetter = ruleSetter;

        all.add(this);
    }

    public void generate(Tiles tiles) {
        int seed1 = Mathf.random(999999999), seed2 = Mathf.random(999999999);

        tiles.each((x, y) -> {
            int temp = noise2d(seed1, x, y, terrain.length);
            int elev = noise2d(seed2, x, y, terrain[temp].length);

            tiles.set(x, y, new Tile(x, y, floor(temp, elev), Blocks.air, block(temp, elev)));
        });

        getHexes((x, y) -> {
            Geometry.circle(x, y, tiles.width, tiles.height, diameter, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, diameter, cx, cy)) tiles.getc(cx, cy).remove();
            });

            for (float angle = 0f; angle < 360f; angle += 120f) {
                Tmp.v1.trnsExact(angle - 30f, 90f).add(x, y);
                if (!tiles.in((int) Tmp.v1.x, (int) Tmp.v1.y)) continue;

                Tmp.v1.trnsExact(angle - 30f, 46f).add(x, y);
                Bresenham2.line(x, y, (int) Tmp.v1.x, (int) Tmp.v1.y, (cx, cy) -> Geometry.circle(cx, cy, tiles.width, tiles.height, 3, (c2x, c2y) -> tiles.getc(c2x, c2y).remove()));
            }
        });

        var input = new GenerateInput();
        generateOres(tiles, input);
        generateLandscape(tiles, input);

        generateDecorations(tiles);
        generateCorePlatforms(tiles);

        postGenerate(tiles);

        state.map = new Map(StringMap.of("name", name, "author", "Darkness6030"));
    }

    public void generateOres(Tiles tiles, GenerateInput input) {}
    public void generateLandscape(Tiles tiles, GenerateInput input) {}

    public void generateDecorations(Tiles tiles) {
        tiles.eachTile(tile -> {
            if (!Mathf.chance(0.01f)) return;

            for (var point2 : d4c) {
                var nearby = tile.nearby(point2);
                if (nearby == null || nearby.solid() || nearby.floor().isLiquid) return;
            }

            tile.setBlock(tile.floor().decoration);
        });
    }

    public void generateCorePlatforms(Tiles tiles) {
        getHexes((x, y) -> tiles.get(x, y).getLinkedTilesAs(ConstructBlock.get(5), tile -> {
            tile.remove();
            tile.setFloor(Blocks.coreZone.asFloor());
        }));
    }

    public void postGenerate(Tiles tiles) {
        tiles.eachTile(tile -> {
            if (tile.floor().isLiquid)
                tile.remove();

            if (tile.block().itemDrop != null)
                tile.clearOverlay();
        });
    }

    public void applyRules(Rules rules) {
        planet.applyRules(rules);

        defaultRuleSetter.get(rules); // first apply default rules
        ruleSetter.get(rules); // then apply custom rules
    }

    public Floor floor(int temp, int elev) {
        return terrain[temp][elev].asFloor();
    }

    public Block block(int temp, int elev) {
        return floor(temp, elev).wall;
    }
}