package hexed.generation;

import arc.graphics.Color;
import mindustry.content.Planets;
import mindustry.content.Weathers;
import mindustry.maps.filters.*;
import mindustry.type.Weather.WeatherEntry;
import mindustry.world.Block;

import static mindustry.content.Blocks.*;

public class GenerationTypes {

    public static GenerationType oilFlats, winter, rivers, lavaLand, spores, erekir;

    public static void load() {
        oilFlats = new GenerationType("[white]\uF826 [accent]Oil Flats", Planets.serpulo, rules -> {
            rules.fire = false;

            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.sandstorm, 14f, 42f, 3.5f, 10.5f) {{
                intensity = 0.5f;
            }});
        }, new Block[][] {
                {sand, darksand, sand, shale, sand},
                {shale, dacite, tar, shale, tar},
                {darksand, shale, shale, tar, darksand},
                {tar, sand, tar, darksand, sand},
                {darksand, shale, darksand, shale, darksand}
        });

        winter = new GenerationType("[white]\uF825 [cyan]Winter", Planets.serpulo, rules -> {
            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.snow, 20f, 60f, 5f, 15f) {{
                intensity = 0.5f;
            }});
        }, new Block[][] {
                {darksand, dacite, darksand, snow, darksand, darksand},
                {snow, cryofluid, ice, iceSnow, darksand, dacite},
                {darksand, darksandTaintedWater, dacite, cryofluid, darksand, snow},
                {ice, darksandTaintedWater, snow, darksand, ice, darksand},
                {snow, iceSnow, darksand, grass, iceSnow, darksand},
                {darksand, dacite, snow, darksand, ice, darksand}
        }, new RiverNoiseFilter() {{
            floor = air;
            floor2 = darksandWater;
            block = air;
        }});

        rivers = new GenerationType("[white]\uF828 [accent]Rivers", Planets.serpulo, rules -> {
            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.rain, 20f, 60f, 5f, 15f) {{
                intensity = 0.7f;
            }});
        }, new Block[][] {
                {sand, stone, sand, dirt, sand, darksand},
                {darksandWater, mud, grass, mud, grass, sand},
                {water, grass, darksand, water, sand, grass},
                {darksandTaintedWater, taintedWater, stone, sand, grass, stone},
                {darksand, sand, stone, sand, dirt, sand}
        }, new RiverNoiseFilter() {{
            floor = air;
            floor2 = water;
            block = air;
        }});

        lavaLand = new GenerationType("[white]\uF827 [orange]Lava Land", Planets.serpulo, new Block[][] {
                {darksand, stone, sand, shale, sand},
                {shale, basalt, slag, stone, basalt},
                {darksand, hotrock, hotrock, magmarock, darksand},
                {shale, craters, slag, hotrock, sand},
                {dacite, shale, basalt, shale, shale}
        });

        spores = new GenerationType("[white]\uF82B [purple]Spores", Planets.serpulo, rules -> {
            rules.lighting = true;
            rules.ambientLight = new Color(0.01f, 0.01f, 0.04f, 0.3f);

            rules.weather.clear();
            rules.weather.add(new WeatherEntry(Weathers.sporestorm, 14f, 42f, 3.5f, 10.5f) {{
                intensity = 0.5f;
            }});
        }, new Block[][] {
                {moss, sporeMoss, grass, sand, darksand},
                {sporeMoss, dacite, taintedWater, sporeMoss, moss},
                {darksandTaintedWater, taintedWater, basalt, hotrock, sand},
                {darksand, sand, darksandWater, grass, dacite},
                {sand, moss, sporeMoss, darksand, moss}
        });

        erekir = new GenerationType("[white]\uF6C9 [orange]Erekir", Planets.erekir, rules -> {
            rules.buildCostMultiplier = 0.75f;
            rules.buildSpeedMultiplier = 2.5f;
            rules.blockHealthMultiplier = 1.5f;
            rules.unitBuildSpeedMultiplier = 0.8f;
        }, new Block[][] {
                    {regolith, beryllicStone, crystallineStone, denseRedStone, rhyolite},
                    {arkyicStone, rhyolite, redmat, carbonStone, redmat},
                    {beryllicStone, arkyicStone, arkyicStone, arkyicStone, bluemat},
                    {carbonStone, arkyicStone, crystalFloor, ferricStone, roughRhyolite},
                    {ferricStone, ferricCraters, carbonStone, ferricStone, carbonStone}
        }, new NoiseFilter() {{
            threshold = 0.75f;
            floor = arkyciteFloor;
        }}, new NoiseFilter() {{
            threshold = 0.825f;
            floor = slag;
            target = regolith;
        }}, new NoiseFilter() {{
            scl = 10f;
            floor = air;
            block = graphiticWall;
            target = carbonWall;
        }}, new NoiseFilter() {{
            scl = 10f;
            floor = air;
            block = graphiticWall;
            target = ferricStoneWall;
        }}, new NoiseFilter() {{
            scl = 10f;
            floor = air;
            block = graphiticWall;
            target = crystallineStoneWall;
        }});
    }
}