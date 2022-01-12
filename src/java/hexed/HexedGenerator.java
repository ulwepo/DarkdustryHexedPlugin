package hexed;

import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Bresenham2;
import arc.math.geom.Geometry;
import arc.math.geom.Intersector;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Reflect;
import arc.util.Structs;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.game.Rules;
import mindustry.graphics.CacheLayer;
import mindustry.maps.Map;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.maps.filters.OreFilter;
import mindustry.maps.filters.RiverNoiseFilter;
import mindustry.maps.filters.ScatterFilter;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;

import static hexed.Main.mode;
import static mindustry.Vars.maps;
import static mindustry.Vars.state;

public class HexedGenerator implements Cons<Tiles> {

    public int width = Hex.size, height = Hex.size;

    @Override
    public void get(Tiles tiles) {
        Seq<GenerateFilter> ores = new Seq<>();
        maps.addDefaultOres(ores);
        ores.each(o -> ((OreFilter) o).threshold -= 0.05f);
        ores.insert(0, new OreFilter() {{
            ore = Blocks.oreScrap;
            scl += 2 / 2.1f;
        }});
        ores.each(GenerateFilter::randomize);
        GenerateInput in = new GenerateInput();
        IntSeq hex = getHex();

        int s1 = Mathf.random(0, 10000);
        int s2 = Mathf.random(0, 10000);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int temp = Mathf.clamp((int) ((Simplex.noise2d(s1, 12, 0.6, 1.0 / 400, x, y) - 0.5) * 10 * mode.blocks.length), 0, mode.blocks.length - 1);
                int elev = Mathf.clamp((int) (((Simplex.noise2d(s2, 12, 0.6, 1.0 / 700, x, y) - 0.5) * 10 + 0.15f) * mode.blocks[0].length), 0, mode.blocks[0].length - 1);

                Block floor = mode.floors[temp][elev];
                Block wall = mode.blocks[temp][elev];
                Block ore = Blocks.air;

                for (GenerateFilter f : ores) {
                    in.floor = Blocks.stone;
                    in.block = wall;
                    in.overlay = ore;
                    in.x = x;
                    in.y = y;
                    in.width = in.height = Hex.size;
                    f.apply(in);
                    if (in.overlay != Blocks.air) {
                        ore = in.overlay;
                    }
                }
                if (floor.cacheLayer == CacheLayer.cryofluid || floor.cacheLayer == CacheLayer.tar || floor.cacheLayer == CacheLayer.slag || floor.cacheLayer == CacheLayer.water)
                    ore = Blocks.air;
                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
            }
        }

        for (int i = 0; i < hex.size; i++) {
            int x = Point2.x(hex.get(i));
            int y = Point2.y(hex.get(i));
            Geometry.circle(x, y, width, height, Hex.diameter, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, Hex.diameter, cx, cy)) {
                    Tile tile = tiles.getn(cx, cy);
                    tile.setBlock(Blocks.air);
                }
            });

            Angles.circle(3, 360f / 3 / 2f - 90, f -> {
                Tmp.v1.trnsExact(f, Hex.spacing + 12);
                if (Structs.inBounds(x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, width, height)) {
                    Tmp.v1.trnsExact(f, Hex.spacing / 2f + 7);
                    Bresenham2.line(x, y, x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, (cx, cy) -> Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).setBlock(Blocks.air)));
                }
            });
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = tiles.getn(x, y);
                Block wall = tile.block();
                Block floor = tile.floor();

                if (wall == Blocks.air) {
                    if (Mathf.chance(0.03f)) {
                        if (floor == Blocks.sand) wall = Blocks.sandBoulder;
                        else if (floor == Blocks.stone) wall = Blocks.boulder;
                        else if (floor == Blocks.shale) wall = Blocks.shaleBoulder;
                        else if (floor == Blocks.darksand) wall = Blocks.boulder;
                        else if (floor == Blocks.moss) wall = Blocks.sporeCluster;
                        else if (floor == Blocks.ice) wall = Blocks.snowBoulder;
                        else if (floor == Blocks.snow) wall = Blocks.snowBoulder;
                    }
                }
                tile.setBlock(wall);
            }
        }

        if (mode == Mode.rived) {
            RiverNoiseFilter noise = new RiverNoiseFilter();
            Reflect.set(noise, "floor", Blocks.sand);
            Reflect.set(noise, "floor2", Blocks.water);

            noise.randomize();
            in.begin(width, height, tiles::getn);
            noise.apply(tiles, in);
            for (Tile tile : tiles) {
                if (tile.floor().cacheLayer == CacheLayer.water) tile.setBlock(Blocks.air);
            }
        }

        if (mode == Mode.winter) {
            RiverNoiseFilter noise = new RiverNoiseFilter();
            Reflect.set(noise, "floor", Blocks.ice);
            Reflect.set(noise, "floor2", Blocks.water);

            noise.randomize();
            in.begin(width, height, tiles::getn);
            noise.apply(tiles, in);
            for (Tile tile : tiles) {
                if (tile.floor().cacheLayer == CacheLayer.water) tile.setBlock(Blocks.air);
            }
        }

        if (mode == Mode.nuclear) {
            ScatterFilter scatter = new ScatterFilter();
            Reflect.set(scatter, "flooronto", Blocks.snow);
            Reflect.set(scatter, "floor", Blocks.iceSnow);
            Reflect.set(scatter, "block", Blocks.whiteTreeDead);

            scatter.randomize();
            in.begin(width, height, tiles::getn);
            scatter.apply(tiles, in);
        }

        for (int i = 0; i < hex.size; i++) {
            int x = Point2.x(hex.get(i));
            int y = Point2.y(hex.get(i));

            int offsetX = x - 2;
            int offsetY = y - 2;
            for (int x5 = offsetX; x5 < offsetX + 5; x5++) {
                for (int y5 = offsetY; y5 < offsetY + 5; y5++) {
                    tiles.get(x5, y5).setFloor(Blocks.metalFloor5.asFloor());
                }
            }
        }

        state.map = new Map(StringMap.of("name", getModeName(mode), "author", "[gray]Skykatik"));
    }

    public IntSeq getHex() {
        IntSeq array = new IntSeq();
        double h = Math.sqrt(3) * Hex.spacing / 2;
        for (int x = 0; x < width / Hex.spacing - 2; x++) {
            for (int y = 0; y < height / (h / 2) - 2; y++) {
                int cx = (int) (x * Hex.spacing * 1.5 + (y % 2) * Hex.spacing * 3.0 / 4) + Hex.spacing / 2;
                int cy = (int) (y * h / 2) + Hex.spacing / 2;
                array.add(Point2.pack(cx, cy));
            }
        }
        return array;
    }

    public String getModeName(Mode mapMode) {
        return switch (mapMode) {
            case def -> "\uE861 [gold]Hexed Arena";
            case oilFlats -> "\uF826 [accent]Oil Flats";
            case winter -> "\uF825 [cyan]Winter";
            case rived -> "\uF828 [accent]Rivers";
            case lavaLand -> "\uF827 [orange]Lavaland";
            case spore -> "\uF82B [purple]Spores";
            case nuclear -> "\uF7A9 [scarlet]Nuclear";
        };
    }

    public enum Mode {
        // elevation --->
        // temperature
        // |
        // v
        def(new Block[][] {
                {Blocks.sand, Blocks.sand, Blocks.darksand, Blocks.sand, Blocks.darksand, Blocks.grass},
                {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.moss, Blocks.sand},
                {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.grass, Blocks.shale},
                {Blocks.darksandTaintedWater, Blocks.darksandTaintedWater, Blocks.moss, Blocks.moss, Blocks.sporeMoss, Blocks.stone},
                {Blocks.ice, Blocks.iceSnow, Blocks.snow, Blocks.dacite, Blocks.hotrock, Blocks.darksand}
        }, new Block[][] {
                {Blocks.stoneWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.sporeWall, Blocks.sandWall},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.stoneWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowWall, Blocks.stoneWall, Blocks.duneWall}
        }),

        oilFlats(new Block[][] {
                {Blocks.sand, Blocks.darksand, Blocks.sand, Blocks.shale, Blocks.sand},
                {Blocks.shale, Blocks.sand, Blocks.tar, Blocks.sand, Blocks.darksand},
                {Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksand},
                {Blocks.tar, Blocks.sand, Blocks.tar, Blocks.darksand, Blocks.sand},
                {Blocks.darksand, Blocks.shale, Blocks.darksand, Blocks.shale, Blocks.sand}
        }, new Block[][] {
                {Blocks.sandWall, Blocks.duneWall, Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall},
                {Blocks.shaleWall, Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall, Blocks.duneWall},
                {Blocks.duneWall, Blocks.sandWall, Blocks.sandWall, Blocks.sandWall, Blocks.duneWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall, Blocks.duneWall, Blocks.sandWall},
                {Blocks.duneWall, Blocks.shaleWall, Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall}
        }, rules -> {
            rules.reactorExplosions = false;
            rules.damageExplosions = false;
            rules.fire = false;
        }),

        winter(new Block[][] {
                {Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.darksand, Blocks.snow},
                {Blocks.ice, Blocks.water, Blocks.darksand, Blocks.iceSnow, Blocks.iceSnow},
                {Blocks.water, Blocks.water, Blocks.iceSnow, Blocks.water, Blocks.darksand},
                {Blocks.darksand, Blocks.water, Blocks.snow, Blocks.darksand, Blocks.darksand},
                {Blocks.snow, Blocks.darksand, Blocks.darksand, Blocks.snow, Blocks.iceSnow}
        }, new Block[][] {
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowPine, Blocks.iceWall, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.iceWall, Blocks.snowWall},
                {Blocks.snowPine, Blocks.snowWall, Blocks.snowPine, Blocks.iceWall, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowPine, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowPine, Blocks.snowWall, Blocks.iceWall, Blocks.snowPine}
        }, rules -> {

        }),

        rived(new Block[][] {
                {Blocks.sand, Blocks.sand, Blocks.stone, Blocks.dirt, Blocks.sand, Blocks.grass},
                {Blocks.darksandWater, Blocks.dirt, Blocks.darksand, Blocks.taintedWater, Blocks.grass, Blocks.grass},
                {Blocks.water, Blocks.darksand, Blocks.darksand, Blocks.water, Blocks.grass, Blocks.grass},
                {Blocks.darksandTaintedWater, Blocks.taintedWater, Blocks.stone, Blocks.stone, Blocks.grass, Blocks.stone},
                {Blocks.sand, Blocks.sand, Blocks.stone, Blocks.dirt, Blocks.dirt, Blocks.grass}
        }, new Block[][] {
                {Blocks.sandWall, Blocks.sandWall, Blocks.stoneWall, Blocks.dirtWall, Blocks.sandWall, Blocks.pine},
                {Blocks.dirtWall, Blocks.dirtWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.dirtWall, Blocks.duneWall, Blocks.dirtWall, Blocks.duneWall, Blocks.stoneWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.stoneWall, Blocks.sandWall, Blocks.pine, Blocks.pine}
        }, rules -> {

        }),

        lavaLand(new Block[][] {
                {Blocks.sand, Blocks.basalt, Blocks.sand, Blocks.darksand},
                {Blocks.sand, Blocks.darksand, Blocks.sand, Blocks.darksand},
                {Blocks.craters, Blocks.darksand, Blocks.shale, Blocks.darksand},
                {Blocks.slag, Blocks.magmarock, Blocks.slag, Blocks.darksand},
                {Blocks.darksand, Blocks.darksand, Blocks.hotrock, Blocks.sand}
        }, new Block[][] {
                {Blocks.duneWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall},
                {Blocks.duneWall, Blocks.sandWall, Blocks.sandWall, Blocks.stoneWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall},
                {Blocks.daciteWall, Blocks.sandWall, Blocks.daciteWall, Blocks.sandWall},
                {Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall}
        }, rules -> {

        }),

        spore(new Block[][] {
                {Blocks.moss, Blocks.sporeMoss, Blocks.sand, Blocks.moss},
                {Blocks.moss, Blocks.dacite, Blocks.taintedWater, Blocks.sporeMoss},
                {Blocks.darksandTaintedWater, Blocks.taintedWater, Blocks.moss, Blocks.hotrock},
                {Blocks.darksand, Blocks.sand, Blocks.darksandWater, Blocks.darksand},
                {Blocks.moss, Blocks.moss, Blocks.sporeMoss, Blocks.darksand}
        }, new Block[][] {
                {Blocks.sporeWall, Blocks.sporeWall, Blocks.sandWall, Blocks.sporeWall},
                {Blocks.duneWall, Blocks.sandWall, Blocks.sporeWall, Blocks.sporeWall},
                {Blocks.duneWall, Blocks.sporeWall, Blocks.duneWall, Blocks.sporeWall},
                {Blocks.duneWall, Blocks.sandWall, Blocks.sporeWall, Blocks.sandWall},
                {Blocks.sporeWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sporeWall}
        }, rules -> {

        }),

        nuclear(new Block[][] {
                {Blocks.stone, Blocks.shale, Blocks.moss, Blocks.darksand},
                {Blocks.craters, Blocks.stone, Blocks.taintedWater, Blocks.sand},
                {Blocks.shale, Blocks.sand, Blocks.craters, Blocks.sand},
                {Blocks.slag, Blocks.moss, Blocks.cryofluid, Blocks.snow},
                {Blocks.shale, Blocks.hotrock, Blocks.dacite, Blocks.darksand}
        }, new Block[][] {
                {Blocks.stoneWall, Blocks.shaleWall, Blocks.sporePine, Blocks.duneWall},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.sporeWall, Blocks.sandWall},
                {Blocks.shaleWall, Blocks.sandWall, Blocks.stoneWall, Blocks.sandWall},
                {Blocks.darkMetal, Blocks.sporePine, Blocks.darkMetal, Blocks.snowWall},
                {Blocks.shaleWall, Blocks.stoneWall, Blocks.dirtWall, Blocks.duneWall}
        }, rules -> {
            rules.lighting = true;
            rules.enemyLights = false;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.5f)
        });

        final Block[][] floors;
        final Block[][] blocks;

        final Cons<Rules> rules;

        Mode(Block[][] floors, Block[][] blocks, Cons<Rules> rules) {
            this.floors = floors;
            this.blocks = blocks;
            this.rules = rules;
        }

        Mode(Block[][] floors, Block[][] blocks) {
            this(floors, blocks, rules -> {});
        }

        public Rules applyRules(Rules rule) {
            rules.get(rule);
            return rule;
        }
    }
}
