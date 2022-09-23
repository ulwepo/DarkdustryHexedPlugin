package hexed.generation;

import arc.graphics.Color;
import mindustry.content.Weathers;
import mindustry.maps.filters.RiverNoiseFilter;
import mindustry.maps.filters.ScatterFilter;
import mindustry.type.Weather.WeatherEntry;
import mindustry.world.Block;

import static mindustry.content.Blocks.*;
import static mindustry.content.Planets.*;

public class GenerationTypes {

    public static GenerationType arena, oilFlats, winter, rivers, lavaLand, spores, nuclear, wasteland;

    public static void load() {
        arena = new GenerationType("[white]\uE861 [gold]Hexed Arena", serpulo, new Block[][] {
                {sand, sand, darksand, sand, darksand, grass},
                {darksandWater, darksand, darksand, darksand, moss, sand},
                {darksandWater, darksand, darksand, darksand, grass, shale},
                {darksandTaintedWater, darksandTaintedWater, moss, moss, sporeMoss, stone},
                {ice, iceSnow, snow, dacite, hotrock, darksand},
                {ice, iceSnow, snow, dacite, hotrock, darksand}
        });

        oilFlats = new GenerationType("[white]\uF826 [accent]Oil Flats", serpulo, rules -> {
            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.sandstorm, 14f, 42f, 3.5f, 10.5f) {{
                intensity = 0.5f;
            }});
        }, new Block[][] {
                {sand, darksand, sand, shale, sand},
                {shale, sand, tar, sand, darksand},
                {darksand, sand, sand, sand, darksand},
                {tar, sand, tar, darksand, sand},
                {darksand, shale, darksand, shale, sand}
        });

        winter = new GenerationType("[white]\uF825 [cyan]Winter", serpulo, rules -> {
            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.snow, 20f, 60f, 5f, 15f) {{
                intensity = 0.5f;
            }});
        }, new Block[][] {
                {iceSnow, dacite, snow, darksand, snow, darksand},
                {darksand, cryofluid, darksand, iceSnow, iceSnow, dacite},
                {snow, darksandTaintedWater, dacite, cryofluid, darksand, snow},
                {ice, darksandTaintedWater, snow, ice, darksand, ice},
                {snow, darksand, ice, grass, iceSnow, darksand}
        }, new RiverNoiseFilter() {{
            floor = darksand;
            floor2 = darksandWater;
        }});


        rivers = new GenerationType("[white]\uF828 [accent]Rivers", serpulo, rules -> {
            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.rain, 20f, 60f, 5f, 15f) {{
                intensity = 0.7f;
            }}, new WeatherEntry(Weathers.fog) {{
                intensity = 0.5f;
                always = true;
            }});
        }, new Block[][] {
                {sand, stone, sand, dirt, sand, grass},
                {darksandWater, dirt, darksand, mud, grass, grass},
                {water, darksand, darksand, water, sand, grass},
                {darksandTaintedWater, taintedWater, stone, sand, grass, stone},
                {dirt, sand, stone, sand, dirt, grass}
        }, new RiverNoiseFilter() {{
            floor = sand;
            floor2 = water;
        }});

        lavaLand = new GenerationType("[white]\uF827 [orange]Lava Land", serpulo, new Block[][] {
                {sand, basalt, sand, darksand, sand},
                {darksand, sand, darksand, shale, darksand},
                {craters, slag, shale, darksand, sand},
                {sand, magmarock, slag, hotrock, sand},
                {darksand, shale, darksand, sand, darksand}
        });

        spores = new GenerationType("[white]\uF82B [purple]Spores", serpulo, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.3f);

            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.sporestorm, 14f, 42f, 3.5f, 10.5f) {{
                intensity = 0.5f;
            }});
        }, new Block[][] {
                {moss, sporeMoss, sand, moss},
                {moss, dacite, taintedWater, sporeMoss},
                {darksandTaintedWater, taintedWater, moss, hotrock},
                {darksand, sand, darksandWater, darksand},
                {moss, moss, sporeMoss, darksand}
        });

        nuclear = new GenerationType("[white]\uF7A9 [scarlet]Nuclear", serpulo, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.6f);

            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.suspendParticles) {{
                intensity = 2.5f;
                always = true;
            }});
        }, new Block[][] {
                {stone, shale, moss, darksand},
                {craters, stone, taintedWater, sand},
                {shale, sand, craters, sand},
                {slag, moss, cryofluid, snow},
                {shale, hotrock, dacite, darksand}
        }, new ScatterFilter() {{
            flooronto = snow;
            floor = ice;
            block = whiteTreeDead;
        }});

        wasteland = new GenerationType("[white]\uF75C [#b8510d]Erekir", erekir, new Block[][] {
                {rhyolite, beryllicStone, arkyicStone, rhyolite},
                {crystallineStone, rhyolite, arkyicStone, carbonStone},
                {beryllicStone, carbonStone, rhyoliteCrater, carbonVent},
                {ferricStone, arkyicStone, crystalFloor, ferricStone},
                {beryllicStone, redIce, ferricStone, carbonStone}
        });
    }
}
