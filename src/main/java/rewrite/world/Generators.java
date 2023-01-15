package rewrite.world;

import mindustry.content.Blocks;
import mindustry.world.Block;

public class Generators {

    public static Generator tarFields, winter, rivers, erekir;

    public static void load() {
        tarFields = new SerpuloGenerator("Tar Fields", new Block[][] {
                {Blocks.sand, Blocks.shale, Blocks.darksandWater, Blocks.shale, Blocks.darksand},
                {Blocks.darksand, Blocks.darksand, Blocks.basalt, Blocks.darksand, Blocks.darksand},
                {Blocks.tar, Blocks.basalt, Blocks.darksand, Blocks.darksand, Blocks.darksand},
                {Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.basalt, Blocks.darksand},
                {Blocks.darksand, Blocks.shale, Blocks.darksandWater, Blocks.shale, Blocks.sand}
        }, rules -> {

        });

        winter = new SerpuloGenerator("Winter", new Block[][] {
                {Blocks.darksand, Blocks.dacite, Blocks.moss, Blocks.dacite, Blocks.ice},
                {Blocks.darksandTaintedWater, Blocks.dacite, Blocks.ice, Blocks.dacite, Blocks.ice},
                {Blocks.taintedWater, Blocks.ice, Blocks.iceSnow, Blocks.taintedWater, Blocks.iceSnow},
                {Blocks.moss, Blocks.darksandTaintedWater, Blocks.moss, Blocks.dacite, Blocks.moss},
                {Blocks.snow, Blocks.ice, Blocks.iceSnow, Blocks.dacite, Blocks.darksand}
        }, rules -> {

        });

        rivers = new SerpuloGenerator("Rivers", new Block[][] {
                {Blocks.sand, Blocks.dacite, Blocks.sand, Blocks.stone, Blocks.grass},
                {Blocks.sandWater, Blocks.mud, Blocks.grass, Blocks.stone, Blocks.grass},
                {Blocks.water, Blocks.grass, Blocks.darksand, Blocks.water, Blocks.darksand},
                {Blocks.darksandWater, Blocks.darksandTaintedWater, Blocks.craters, Blocks.stone, Blocks.grass},
                {Blocks.darksand, Blocks.grass, Blocks.dirt, Blocks.mud, Blocks.dirt}
        }, rules -> {

        }) {{
            riverFloor2 = Blocks.water;
        }};

        erekir = new ErekirGenerator("Erekir", new Block[][] {
                {Blocks.carbonStone, Blocks.arkyicStone, Blocks.crystallineStone, Blocks.denseRedStone, Blocks.rhyolite},
                {Blocks.beryllicStone, Blocks.rhyolite, Blocks.redmat, Blocks.regolith, Blocks.redmat},
                {Blocks.arkyicStone, Blocks.beryllicStone, Blocks.beryllicStone, Blocks.beryllicStone, Blocks.bluemat},
                {Blocks.regolith, Blocks.beryllicStone, Blocks.crystalFloor, Blocks.ferricStone, Blocks.roughRhyolite},
                {Blocks.ferricStone, Blocks.ferricCraters, Blocks.regolith, Blocks.ferricStone, Blocks.regolith}
        }, rules -> {

        });
    }
}