package hexed.generation;

import arc.func.Cons;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Structs;
import arc.util.noise.Ridged;
import mindustry.content.*;
import mindustry.game.Rules;
import mindustry.game.Schematic;
import mindustry.maps.filters.*;
import mindustry.type.Planet;
import mindustry.type.Weather;
import mindustry.world.*;

import static hexed.Main.*;

// Старый генератор, только для теста
public class LegacyGenerator {

    public static Mode mode;

    public static void generate(Tiles tiles) {
        mode = Structs.random(Mode.values());

        int s1 = Mathf.random(10000);
        int s2 = Mathf.random(10000);

        for (int x = 0; x < tiles.width; x++) {
            for (int y = 0; y < tiles.height; y++) {
                int temp = Mathf.clamp((int) (Ridged.noise2d(s1, x, y, 12, 0.6, 1.0 / 400) * 10 * mode.floors.length), 0, mode.floors.length - 1);
                int elev = Mathf.clamp((int) (Ridged.noise2d(s2, x, y, 12, 0.6, 1.0 / 400) * 10 * mode.floors[0].length), 0, mode.floors[0].length - 1);

                Block floor = mode.floors[temp][elev];
                Block wall = floor.asFloor().wall;
                Block ore = Blocks.air;

                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
            }
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
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart),

        oilFlats("[white]\uF826 [accent]Oil Flats", new Block[][] {
                {Blocks.sand, Blocks.darksand, Blocks.sand, Blocks.shale, Blocks.sand},
                {Blocks.shale, Blocks.sand, Blocks.tar, Blocks.sand, Blocks.darksand},
                {Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksand},
                {Blocks.tar, Blocks.sand, Blocks.tar, Blocks.darksand, Blocks.sand},
                {Blocks.darksand, Blocks.shale, Blocks.darksand, Blocks.shale, Blocks.sand}
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart, rules -> {
            rules.weather.add(new Weather.WeatherEntry() {{
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
        }, new GenerateFilter[] {
                new RiverNoiseFilter() {{
                    floor = Blocks.darksand;
                    floor2 = Blocks.darksandWater;
                }}
        }, Planets.serpulo, serpuloStart, rules -> {
            rules.weather.add(new Weather.WeatherEntry() {{
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
        }, new GenerateFilter[] {
                new RiverNoiseFilter() {{
                    floor = Blocks.sand;
                    floor2 = Blocks.water;
                }}
        }, Planets.serpulo, serpuloStart, rules -> {
            rules.weather.add(new Weather.WeatherEntry() {{
                weather = Weathers.rain;
                minFrequency = 20f;
                maxFrequency = 60f;
                minDuration = 5f;
                maxDuration = 15f;
                cooldown = 30f;
                intensity = 0.75f;
            }}, new Weather.WeatherEntry() {{
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
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart),

        spores("[white]\uF82B [purple]Spores", new Block[][] {
                {Blocks.moss, Blocks.sporeMoss, Blocks.sand, Blocks.moss},
                {Blocks.moss, Blocks.dacite, Blocks.taintedWater, Blocks.sporeMoss},
                {Blocks.darksandTaintedWater, Blocks.taintedWater, Blocks.moss, Blocks.hotrock},
                {Blocks.darksand, Blocks.sand, Blocks.darksandWater, Blocks.darksand},
                {Blocks.moss, Blocks.moss, Blocks.sporeMoss, Blocks.darksand}
        }, new GenerateFilter[] {}, Planets.serpulo, serpuloStart, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.3f);

            rules.weather.add(new Weather.WeatherEntry() {{
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
        }, new GenerateFilter[] {
                new ScatterFilter() {{
                    flooronto = Blocks.snow;
                    floor = Blocks.ice;
                    block = Blocks.whiteTreeDead;
                }}
        }, Planets.serpulo, serpuloStart, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.6f);

            rules.weather.add(new Weather.WeatherEntry() {{
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
        }, new GenerateFilter[] {}, Planets.erekir, erekirStart, rules -> {

        });

        final String displayName;

        final Block[][] floors;

        final GenerateFilter[] filters;

        final Planet planet;
        final Schematic startScheme;

        final Cons<Rules> customRules;

        Mode(String displayName, Block[][] floors, GenerateFilter[] filters, Planet planet, Schematic startScheme, Cons<Rules> customRules) {
            this.displayName = displayName;
            this.floors = floors;
            this.filters = filters;
            this.planet = planet;
            this.startScheme = startScheme;
            this.customRules = customRules;
        }

        Mode(String displayName, Block[][] floors, GenerateFilter[] filters, Planet planet, Schematic startScheme) {
            this(displayName, floors, filters, planet, startScheme, rules -> {});
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
