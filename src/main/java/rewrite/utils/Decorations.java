package rewrite.utils;

import arc.struct.ObjectMap;
import mindustry.content.Blocks;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;

public class Decorations {

    public static ObjectMap<Floor, Block> walls;
    public static ObjectMap<Floor, Block> props;

    public static ObjectMap<Floor, Block> trees;
    public static ObjectMap<Floor, Block> deadTrees;

    public static ObjectMap<Floor, Block> vents;
    public static ObjectMap<Floor, Block> crystals;

    public static void load() {
        walls = ObjectMap.of(
                Blocks.grass, Blocks.shrubs,
                Blocks.moss, Blocks.sporeWall,
                Blocks.mud, Blocks.dirtWall
        );

        props = ObjectMap.of(
                Blocks.moss, Blocks.sporeCluster,
                Blocks.sporeMoss, Blocks.sporeCluster
        );

        trees = ObjectMap.of(
                Blocks.grass, Blocks.pine,
                Blocks.snow, Blocks.snowPine,
                Blocks.ice, Blocks.snowPine,
                Blocks.iceSnow, Blocks.snowPine,
                Blocks.moss, Blocks.sporePine,
                Blocks.sporeMoss, Blocks.sporePine
        );

        deadTrees = ObjectMap.of(
                Blocks.snow, Blocks.whiteTree,
                Blocks.ice, Blocks.whiteTree,
                Blocks.iceSnow, Blocks.whiteTree,
                Blocks.moss, Blocks.whiteTreeDead,
                Blocks.sporeMoss, Blocks.whiteTreeDead
        );

        vents = ObjectMap.of(
                Blocks.rhyolite, Blocks.rhyoliteVent,
                Blocks.beryllicStone, Blocks.arkyicVent,
                Blocks.arkyicStone, Blocks.arkyicVent,
                Blocks.yellowStone, Blocks.yellowStoneVent,
                Blocks.yellowStonePlates, Blocks.yellowStoneVent,
                Blocks.regolith, Blocks.yellowStoneVent,
                Blocks.redStone, Blocks.redStoneVent,
                Blocks.denseRedStone, Blocks.redStoneVent,
                Blocks.carbonStone, Blocks.carbonVent
        );

        crystals = ObjectMap.of(
                Blocks.crystallineStone, Blocks.crystalCluster,
                Blocks.crystalFloor, Blocks.vibrantCrystalCluster,
                Blocks.regolith, Blocks.crystalBlocks,
                Blocks.arkyicStone, Blocks.crystalOrbs
        );
    }
}