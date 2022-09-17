package hexed.generation;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Planets;
import mindustry.content.Weathers;
import mindustry.maps.filters.*;
import mindustry.type.Weather.WeatherEntry;

import static hexed.Main.*;

public class GenerationTypes {

    public static GenerationType beta;

    public static void load() {
        beta = new GenerationType("[white]\uF7EA [accent]Beta", Planets.serpulo, Blocks.moss, Blocks.sporeWall, rules -> {
            rules.lighting = true;
            rules.ambientLight = Color.black.cpy().a(.7f);

            rules.weather.add(new WeatherEntry(Weathers.sporestorm, 5f, 10f, 1f, 2f) {{
                intensity = .25f;
            }});
        },
                new NoiseFilter() {{
                    scl = 39.92f;
                    octaves = 3.015f;
                    floor = Blocks.rhyolite;
                    block = Blocks.rhyoliteWall;
                }},

                new NoiseFilter() {{
                    scl = 39.92f;
                    octaves = 3.015f;
                    floor = Blocks.regolith;
                    block = Blocks.regolithWall;
                }},

                new NoiseFilter() {{
                    scl = 39.92f;
                    octaves = 3.015f;
                    floor = Blocks.arkyicStone;
                    block = Blocks.arkyicWall;
                }},

                new NoiseFilter() {{
                    scl = 498.99997f;
                    threshold = 0f;
                    octaves = 1f;
                    falloff = 0f;
                    tilt = -4f;
                    floor = Blocks.carbonStone;
                    block = Blocks.carbonWall;
                    target = Blocks.stone;
                }},

                new OreFilter() {{
                    scl = 24.949999f;
                    threshold = 0.84499997f;
                    octaves = 1.98f;
                    falloff = 0.29999998f;
                    ore = Blocks.wallOreTungsten;
                }},

                new OreFilter() {{
                    scl = 24.949999f;
                    threshold = 0.88f;
                    octaves = 1.98f;
                    falloff = 0.29999998f;
                    ore = Blocks.wallOreThorium;
                }},

                new OreFilter() {{
                    scl = 34.93f;
                    octaves = 1.98f;
                    falloff = 0.29999998f;
                    ore = Blocks.wallOreBeryllium;
                }},

                new NoiseFilter() {{
                    scl = 79.84f;
                    threshold = 0.72499996f;
                    octaves = 3.015f;
                    floor = Blocks.slag;
                    target = Blocks.carbonStone;
                }},

                new NoiseFilter() {{
                    scl = 29.939999f;
                    threshold = 0.695f;
                    octaves = 3.015f;
                    falloff = 0.69f;
                    floor = Blocks.arkyciteFloor;
                    target = Blocks.arkyicStone;
                }},

                new ScatterFilter() {{
                    chance = 0.5f;
                    flooronto = Blocks.arkyicStone;
                    floor = Blocks.arkyicVent;
                }},

                new ScatterFilter() {{
                    chance = 0.5f;
                    flooronto = Blocks.carbonStone;
                    floor = Blocks.carbonVent;
                }},

                new ScatterFilter() {{
                    chance = 0.5f;
                    flooronto = Blocks.rhyolite;
                    floor = Blocks.rhyoliteVent;
                }}
        );
    }

    public static Seq<GenerationType> all() {
        return Seq.with(beta);
    }

    public static GenerationType random() {
        return all().random(type);
    }
}
