package hexed;

import arc.func.Cons;
import arc.func.Floatc;
import arc.graphics.Color;
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
import mindustry.content.Planets;
import mindustry.content.Weathers;
import mindustry.game.Rules;
import mindustry.game.Schematic;
import mindustry.maps.Map;
import mindustry.maps.filters.GenerateFilter;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.maps.filters.OreFilter;
import mindustry.maps.filters.RiverNoiseFilter;
import mindustry.maps.filters.ScatterFilter;
import mindustry.maps.generators.BasicGenerator;
import mindustry.type.Planet;
import mindustry.type.Weather.WeatherEntry;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;

import static hexed.Main.*;
import static mindustry.Vars.*;

public class HexedGenerator implements Cons<Tiles> {

    public int width = Hex.size, height = Hex.size;

    @Deprecated(since = "test")
    public boolean testingBasicGenerator = true;

    @Override
    public void get(Tiles tiles) {

        if (testingBasicGenerator) return;

        int s1 = Mathf.random(10000);
        int s2 = Mathf.random(10000);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int temp = Mathf.clamp((int) ((Simplex.noise2d(s1, 12, 0.6, 1.0 / 400, x, y) - 0.5) * 10 * mode.blocks.length), 0, mode.blocks.length - 1);
                int elev = Mathf.clamp((int) (((Simplex.noise2d(s2, 12, 0.6, 1.0 / 700, x, y) - 0.5) * 10 + 0.15f) * mode.blocks[0].length), 0, mode.blocks[0].length - 1);

                Block floor = mode.floors[temp][elev];
                Block wall = mode.blocks[temp][elev];
                Block ore = Blocks.air;

                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
            }
        }

        IntSeq hexes = getHexes();
        GenerateInput in = new GenerateInput();

        for (int i = 0; i < hexes.size; i++) {
            int x = Point2.x(hexes.get(i));
            int y = Point2.y(hexes.get(i));

            Geometry.circle(x, y, width, height, Hex.diameter, (cx, cy) -> {
                if (Intersector.isInsideHexagon(x, y, Hex.diameter, cx, cy)) {
                    tiles.getn(cx, cy).remove();
                }
            });

            circle(3, 360f / 3 / 2f - 90, f -> {
                Tmp.v1.trnsExact(f, Hex.spacing + 12);
                if (Structs.inBounds(x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, width, height)) {
                    Tmp.v1.trnsExact(f, Hex.spacing / 2f + 7);
                    Bresenham2.line(x, y, x + (int) Tmp.v1.x, y + (int) Tmp.v1.y, (cx, cy) -> Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).remove()));
                }
            });
        }

        Seq<GenerateFilter> filters = getOres().addAll(getDefaultFilters()).addAll(mode.filters);
        for (GenerateFilter filter : filters) {
            filter.randomize();
            in.begin(width, height, tiles::getn);
            filter.apply(tiles, in);
        }

        for (int i = 0; i < hexes.size; i++) {
            int x = Point2.x(hexes.get(i));
            int y = Point2.y(hexes.get(i));

            for (int cx = x - 2; cx < x + 3; cx++) {
                for (int cy = y - 2; cy < y + 3; cy++) {
                    Tile tile = tiles.getn(cx, cy);
                    tile.remove();
                    tile.setFloor(Blocks.coreZone.asFloor());
                }
            }
        }

        state.map = new Map(StringMap.of("name", mode.displayName, "author", "[cyan]\uE810 [royal]Darkness [cyan]\uE810", "description", "A map for Darkdustry Hexed. Automatically generated."));
    }

    public IntSeq getHexes() {
        IntSeq array = new IntSeq();
        float h = Mathf.sqrt3 * Hex.spacing / 2;
        for (int x = 0; x < width / Hex.spacing - 2; x++) {
            for (int y = 0; y < height / (h / 2) - 2; y++) {
                int cx = (int) (x * Hex.spacing * 1.5 + (y % 2) * Hex.spacing * 3.0 / 4) + Hex.spacing / 2;
                int cy = (int) (y * h / 2) + Hex.spacing / 2;
                array.add(Point2.pack(cx, cy));
            }
        }
        return array;
    }

    public Seq<GenerateFilter> getOres() {
        Seq<Block> ores = mode.planet == Planets.serpulo ? serpuloOres : erekirOres;
        Seq<GenerateFilter> filters = new Seq<>();
        for (Block block : ores) {
            filters.add(new OreFilter() {{
                threshold = block.asFloor().oreThreshold - 0.04f;
                scl = block.asFloor().oreScale + 8f;
                ore = block;
            }});
        }

        return filters;
    }

    public Seq<GenerateFilter> getDefaultFilters() {
        Seq<GenerateFilter> filters = new Seq<>();
        for (Block block : content.blocks()) {
            if (block.isFloor() && block.inEditor && block.asFloor().decoration != Blocks.air) {
                var filter = new ScatterFilter();
                filter.flooronto = block.asFloor();
                filter.block = block.asFloor().decoration;
                filters.add(filter);
            }
        }

        return filters;
    }

    public void circle(int points, float offset, Floatc cons) {
        for (int i = 0; i < points; i++) {
            cons.get(offset + i * 360f / points);
        }
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
                {Blocks.ice, Blocks.iceSnow, Blocks.snow, Blocks.dacite, Blocks.hotrock, Blocks.darksand},
                {Blocks.arkyicStone, Blocks.arkyicStone, Blocks.arkyicStone, Blocks.arkyicStone, Blocks.arkyicStone, Blocks.arkyicStone}
        }, new Block[][] {
                {Blocks.stoneWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.sporeWall, Blocks.sandWall},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.sporeWall, Blocks.sporeWall, Blocks.sporePine, Blocks.sporeWall, Blocks.sporeWall, Blocks.stoneWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowWall, Blocks.stoneWall, Blocks.duneWall},
                {Blocks.arkyicWall, Blocks.arkyicWall, Blocks.arkyicWall, Blocks.arkyicWall, Blocks.arkyicWall, Blocks.arkyicWall}
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart),

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
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart, rules -> {
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
        }, new GenerateFilter[] {
                new RiverNoiseFilter() {{
                    floor = Blocks.darksand;
                    floor2 = Blocks.darksandWater;
                }}
        }, Planets.serpulo, serpuloStart, rules -> {
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
                {Blocks.sand, Blocks.stone, Blocks.sand, Blocks.dirt, Blocks.sand, Blocks.grass},
                {Blocks.darksandWater, Blocks.dirt, Blocks.darksand, Blocks.mud, Blocks.grass, Blocks.grass},
                {Blocks.water, Blocks.darksand, Blocks.darksand, Blocks.water, Blocks.sand, Blocks.grass},
                {Blocks.darksandTaintedWater, Blocks.taintedWater, Blocks.stone, Blocks.sand, Blocks.grass, Blocks.stone},
                {Blocks.dirt, Blocks.sand, Blocks.stone, Blocks.sand, Blocks.dirt, Blocks.grass}
        }, new Block[][] {
                {Blocks.sandWall, Blocks.stoneWall, Blocks.sandWall, Blocks.dirtWall, Blocks.sandWall, Blocks.pine},
                {Blocks.dirtWall, Blocks.dirtWall, Blocks.duneWall, Blocks.dirtWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.duneWall, Blocks.sandWall, Blocks.pine},
                {Blocks.stoneWall, Blocks.dirtWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.stoneWall},
                {Blocks.dirtWall, Blocks.sandWall, Blocks.stoneWall, Blocks.sandWall, Blocks.pine, Blocks.pine}
        }, new GenerateFilter[] {
                new RiverNoiseFilter() {{
                    floor = Blocks.sand;
                    floor2 = Blocks.water;
                }}
        }, Planets.serpulo, serpuloStart, rules -> {
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
                {Blocks.darksand, Blocks.shale, Blocks.darksand, Blocks.sand, Blocks.darksand}
        }, new Block[][] {
                {Blocks.sandWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.sandWall},
                {Blocks.duneWall, Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall, Blocks.duneWall},
                {Blocks.sandWall, Blocks.daciteWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.daciteWall, Blocks.stoneWall, Blocks.daciteWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.duneWall, Blocks.shaleWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall}
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart),

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
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.3f);

            rules.weather.add(new WeatherEntry() {{
                weather = Weathers.sporestorm;
                minFrequency = 14f;
                maxFrequency = 42f;
                minDuration = 3.5f;
                maxDuration = 10.5f;
                cooldown = 21f;
                intensity = 0.5f;
            }});
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
        }, new GenerateFilter[] {
                new ScatterFilter() {{
                    flooronto = Blocks.snow;
                    floor = Blocks.ice;
                    block = Blocks.whiteTreeDead;
                }}
        }, Planets.serpulo, serpuloStart, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.6f);

            rules.weather.add(new WeatherEntry() {{
                weather = Weathers.suspendParticles;
                intensity = 2.5f;
                always = true;
            }});
        }),

        erekir("[white]\uF75C [#b8510d]Erekir", new Block[][] {
                {Blocks.rhyolite, Blocks.beryllicStone, Blocks.arkyicStone, Blocks.rhyolite},
                {Blocks.crystallineStone, Blocks.rhyolite, Blocks.arkyicStone, Blocks.carbonStone},
                {Blocks.beryllicStone, Blocks.carbonStone, Blocks.rhyoliteCrater, Blocks.carbonVent},
                {Blocks.ferricStone, Blocks.arkyicStone, Blocks.crystalFloor, Blocks.ferricStone},
                {Blocks.beryllicStone, Blocks.redIce, Blocks.ferricStone, Blocks.carbonStone}
        }, new Block[][] {
                {Blocks.rhyoliteWall, Blocks.beryllicStoneWall, Blocks.arkyicWall, Blocks.rhyoliteWall},
                {Blocks.crystallineStoneWall, Blocks.rhyoliteWall, Blocks.arkyicWall, Blocks.graphiticWall},
                {Blocks.beryllicStoneWall, Blocks.graphiticWall, Blocks.rhyoliteWall, Blocks.carbonWall},
                {Blocks.ferricStoneWall, Blocks.arkyicWall, Blocks.crystallineStoneWall, Blocks.ferricStoneWall},
                {Blocks.beryllicStoneWall, Blocks.redIceWall, Blocks.ferricStoneWall, Blocks.carbonWall}
        }, new GenerateFilter[] {}, Planets.erekir, erekirStart, rules -> {

        });

        final String displayName;

        final Block[][] floors;
        final Block[][] blocks;

        final GenerateFilter[] filters;

        final Planet planet;
        final Schematic startScheme;

        final Cons<Rules> customRules;

        Mode(String displayName, Block[][] floors, Block[][] blocks, GenerateFilter[] filters, Planet planet, Schematic startScheme, Cons<Rules> customRules) {
            this.displayName = displayName;
            this.floors = floors;
            this.blocks = blocks;
            this.filters = filters;
            this.planet = planet;
            this.startScheme = startScheme;
            this.customRules = customRules;
        }

        Mode(String displayName, Block[][] floors, Block[][] blocks, GenerateFilter[] filters, Planet planet, Schematic startScheme) {
            this(displayName, floors, blocks, filters, planet, startScheme, rules -> {});
        }

        public Rules applyRules(Rules rules) {
            customRules.get(rules);
            rules.env = planet.defaultEnv;
            rules.hiddenBuildItems.clear();
            rules.hiddenBuildItems.addAll(planet.hiddenItems);
            return rules;
        }
    }
}
