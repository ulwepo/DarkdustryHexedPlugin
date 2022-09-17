package hexed.generation;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.*;
import mindustry.maps.filters.*;
import mindustry.type.Weather.WeatherEntry;

import static hexed.Main.type;

public class GenerationTypes {

    public static GenerationType wasteland, rivers;

    public static void load() {
        wasteland = new GenerationType("[#c58c45]Wasteland", Planets.erekir, rules -> {
            rules.lighting = true;
            rules.ambientLight = Color.black.cpy().a(.7f);

            rules.weather.add(new WeatherEntry(Weathers.sandstorm, 5f, 10f, 1f, 2f) {{
                intensity = .25f;
            }});
        }, new NoiseFilter() {{
            scl = 39.92f;
            octaves = 3.015f;
            floor = Blocks.rhyolite;
            block = Blocks.rhyoliteWall;
        }}, new NoiseFilter() {{
            scl = 39.92f;
            octaves = 3.015f;
            floor = Blocks.regolith;
            block = Blocks.regolithWall;
        }}, new NoiseFilter() {{
            scl = 39.92f;
            octaves = 3.015f;
            floor = Blocks.arkyicStone;
            block = Blocks.arkyicWall;
        }}, new NoiseFilter() {{
            scl = 498.99997f;
            threshold = 0f;
            octaves = 1f;
            falloff = 0f;
            tilt = -4f;
            floor = Blocks.carbonStone;
            block = Blocks.carbonWall;
            target = Blocks.stone;
        }}, new OreFilter() {{
            scl = 30f;
            threshold = 0.78f;
            octaves = 1.98f;
            falloff = 0.29999998f;
            ore = Blocks.wallOreTungsten;
        }}, new OreFilter() {{
            scl = 30f;
            octaves = 1.98f;
            falloff = 0.29999998f;
            ore = Blocks.wallOreThorium;
        }}, new OreFilter() {{
            scl = 38f;
            octaves = 1.98f;
            falloff = 0.29999998f;
            ore = Blocks.wallOreBeryllium;
        }}, new NoiseFilter() {{
            scl = 79.84f;
            threshold = 0.695f;
            octaves = 3.015f;
            floor = Blocks.slag;
            target = Blocks.carbonStone;
        }}, new NoiseFilter() {{
            scl = 29.939999f;
            threshold = 0.695f;
            octaves = 3.015f;
            falloff = 0.69f;
            floor = Blocks.arkyciteFloor;
            target = Blocks.arkyicStone;
        }}, new NoiseFilter() {{
            scl = 2.5f;
            floor = Blocks.carbonStone;
            block = Blocks.graphiticWall;
            target = Blocks.carbonWall;
        }}, new ScatterFilter() {{
            chance = 0.5f;
            flooronto = Blocks.arkyicStone;
            floor = Blocks.arkyicVent;
        }}, new ScatterFilter() {{
            chance = 0.5f;
            flooronto = Blocks.carbonStone;
            floor = Blocks.carbonVent;
        }}, new ScatterFilter() {{
            chance = 0.5f;
            flooronto = Blocks.rhyolite;
            floor = Blocks.rhyoliteVent;
        }});

        rivers = new GenerationType("[white]\uF828 [accent]Rivers", Planets.serpulo, rules -> {

        }, new NoiseFilter() {{
            scl = 39.92f;
            octaves = 3.015f;
            floor = Blocks.sand;
            block = Blocks.sandWall;
        }}, new NoiseFilter() {{
            scl = 39.92f;
            octaves = 3.015f;
            floor = Blocks.darksand;
            block = Blocks.duneWall;
        }}, new NoiseFilter() {{
            scl = 39.92f;
            octaves = 3.015f;
            floor = Blocks.grass;
            block = Blocks.pine;
        }}, new NoiseFilter() {{
            scl = 498.99997f;
            threshold = 0f;
            octaves = 1f;
            falloff = 0f;
            tilt = -4f;
            floor = Blocks.dacite;
            block = Blocks.daciteWall;
            target = Blocks.stone;
        }}, new RiverNoiseFilter() {{
            floor = Blocks.sandWater;
            floor2 = Blocks.water;
            block = Blocks.air;
        }});
    }

    public static Seq<GenerationType> all() {
        return Seq.with(wasteland, rivers);
    }

    public static GenerationType random() {
        return all().random();
    }
}
