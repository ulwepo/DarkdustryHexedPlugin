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
import arc.util.Structs;
import arc.util.Tmp;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Weathers;
import mindustry.game.Rules;
import mindustry.maps.Map;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.maps.filters.OreFilter;
import mindustry.maps.filters.RiverNoiseFilter;
import mindustry.maps.filters.ScatterFilter;
import mindustry.type.ItemStack;
import mindustry.type.Weather.WeatherEntry;
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

                for (GenerateFilter filter : ores) {
                    in.floor = Blocks.stone;
                    in.block = wall;
                    in.overlay = ore;
                    in.x = x;
                    in.y = y;
                    in.width = in.height = Hex.size;
                    filter.apply(in);
                    if (in.overlay != Blocks.air && !floor.asFloor().isLiquid) {
                        ore = in.overlay;
                    }
                }

                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
            }
        }

        for (int i = 0; i < hex.size; i++) {
            int x = Point2.x(hex.get(i));
            int y = Point2.y(hex.get(i));
            Geometry.circle(x, y, width, height, Hex.diameter, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, Hex.diameter, cx, cy)) {
                    Tile tile = tiles.getn(cx, cy);
                    tile.setAir();
                }
            });

            Angles.circle(3, 360f / 3 / 2f - 90, f -> {
                Tmp.v1.trnsExact(f, Hex.spacing + 12);
                if (Structs.inBounds(x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, width, height)) {
                    Tmp.v1.trnsExact(f, Hex.spacing / 2f + 7);
                    Bresenham2.line(x, y, x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, (cx, cy) -> Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).setAir()));
                }
            });
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile tile = tiles.getn(x, y);
                Block floor = tile.floor();
                Block wall = tile.block();

                if (wall == Blocks.air && Mathf.chance(0.02f)) {
                    if (floor == Blocks.moss) wall = Blocks.sporeCluster;
                    else if (floor.asFloor().decoration != null) wall = floor.asFloor().decoration;
                }

                tile.setBlock(wall);
            }
        }

        if (mode == Mode.rivers) {
            RiverNoiseFilter noise = new RiverNoiseFilter() {{
                floor = Blocks.sand;
                floor2 = Blocks.water;
            }};

            noise.randomize();
            in.begin(width, height, tiles::getn);
            noise.apply(tiles, in);
            for (Tile tile : tiles) {
                if (tile.floor().isLiquid) tile.setAir();
            }
        }

        if (mode == Mode.winter) {
            RiverNoiseFilter noise = new RiverNoiseFilter() {{
                floor = Blocks.darksand;
                floor2 = Blocks.darksandWater;
            }};

            noise.randomize();
            in.begin(width, height, tiles::getn);
            noise.apply(tiles, in);
            for (Tile tile : tiles) {
                if (tile.floor().isLiquid) tile.setAir();
            }
        }

        if (mode == Mode.nuclear) {
            ScatterFilter scatter = new ScatterFilter() {{
                flooronto = Blocks.snow;
                floor = Blocks.ice;
                block = Blocks.whiteTreeDead;
            }};

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
                    Tile tile = tiles.get(x5, y5);
                    tile.setAir();
                    tile.setFloor(Blocks.metalFloor5.asFloor());
                }
            }
        }

        state.map = new Map(StringMap.of("name", mode.displayName, "author", "[gray]Skykatik", "description", "A map for Darkdustry Hexed. Automatically generated."));
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

    public enum Mode {
        // elevation --->
        // temperature
        // |
        // v
        def("[white]\uE861 [gold]Hexed Arena", new Block[][] {
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

        oilFlats("[white]\uF826 [accent]Oil Flats", new Block[][] {
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

            rules.weather.add(new WeatherEntry() {{
                weather = Weathers.sandstorm;
                minFrequency = 14f;
                maxFrequency = 42f;
                minDuration = 3.5f;
                maxDuration = 10.5f;
                cooldown = 21f;
                intensity = 0.5f;
            }});
        }),

        winter("[white]\uF825 [cyan]Winter", new Block[][] {
                {Blocks.iceSnow, Blocks.dacite, Blocks.snow, Blocks.darksand, Blocks.snow, Blocks.darksand},
                {Blocks.darksand, Blocks.cryofluid, Blocks.darksand, Blocks.iceSnow, Blocks.iceSnow, Blocks.dacite},
                {Blocks.snow, Blocks.darksandTaintedWater, Blocks.dacite, Blocks.cryofluid, Blocks.darksand, Blocks.snow},
                {Blocks.ice, Blocks.darksandTaintedWater, Blocks.snow, Blocks.ice, Blocks.darksand, Blocks.ice},
                {Blocks.snow, Blocks.darksand, Blocks.ice, Blocks.grass, Blocks.iceSnow, Blocks.darksand}
        }, new Block[][] {
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowPine, Blocks.duneWall, Blocks.snowWall, Blocks.duneWall},
                {Blocks.duneWall, Blocks.daciteWall, Blocks.duneWall, Blocks.iceWall, Blocks.snowWall, Blocks.daciteWall},
                {Blocks.snowPine, Blocks.snowWall, Blocks.snowPine, Blocks.iceWall, Blocks.duneWall, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowPine, Blocks.snowWall, Blocks.iceWall},
                {Blocks.iceWall, Blocks.duneWall, Blocks.snowWall, Blocks.pine, Blocks.snowPine, Blocks.duneWall}
        }, rules -> {
            rules.weather.add(new WeatherEntry() {{
                weather = Weathers.snow;
                minFrequency = 20f;
                maxFrequency = 60f;
                minDuration = 5f;
                maxDuration = 15f;
                cooldown = 30f;
                intensity = 0.5f;
            }});
        }),

        rivers("[white]\uF828 [accent]Rivers", new Block[][] {
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
            rules.loadout = ItemStack.list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 250, Items.silicon, 200, Items.titanium, 50);

            rules.weather.add(new WeatherEntry() {{
                weather = Weathers.rain;
                minFrequency = 20f;
                maxFrequency = 60f;
                minDuration = 5f;
                maxDuration = 15f;
                cooldown = 30f;
                intensity = 0.75f;
            }}, new WeatherEntry() {{
                weather = Weathers.fog;
                intensity = 0.5f;
                always = true;
            }});
        }),

        lavaLand("[white]\uF827 [orange]Lava Land", new Block[][] {
                {Blocks.sand, Blocks.basalt, Blocks.sand, Blocks.darksand, Blocks.sand},
                {Blocks.darksand, Blocks.sand, Blocks.darksand, Blocks.shale, Blocks.darksand},
                {Blocks.craters, Blocks.slag, Blocks.shale, Blocks.darksand, Blocks.sand},
                {Blocks.sand, Blocks.magmarock, Blocks.slag, Blocks.hotrock, Blocks.sand},
                {Blocks.darksand, Blocks.slag, Blocks.darksand, Blocks.sand, Blocks.darksand}
        }, new Block[][] {
                {Blocks.sandWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.sandWall},
                {Blocks.duneWall, Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall, Blocks.duneWall},
                {Blocks.sandWall, Blocks.daciteWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.daciteWall, Blocks.stoneWall, Blocks.daciteWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.duneWall, Blocks.daciteWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall}
        }),

        spores("[white]\uF82B [purple]Spores", new Block[][] {
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
            rules.lighting = true;
            rules.enemyLights = false;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.3f);
        }),

        nuclear("[white]\uF7A9 [scarlet]Nuclear", new Block[][] {
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
            rules.bannedBlocks.addAll(Blocks.microProcessor, Blocks.logicProcessor, Blocks.hyperProcessor);

            rules.lighting = true;
            rules.enemyLights = false;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.6f);
        });

        final String displayName;

        final Block[][] floors;
        final Block[][] blocks;

        final Cons<Rules> rules;

        Mode(String displayName, Block[][] floors, Block[][] blocks, Cons<Rules> rules) {
            this.displayName = displayName;
            this.floors = floors;
            this.blocks = blocks;
            this.rules = rules;
        }

        Mode(String displayName, Block[][] floors, Block[][] blocks) {
            this(displayName, floors, blocks, rules -> {});
        }

        public Rules applyRules(Rules base) {
            rules.get(base);
            return base;
        }
    }
}
