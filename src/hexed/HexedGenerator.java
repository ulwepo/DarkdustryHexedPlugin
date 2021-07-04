package hexed;

import arc.func.Cons;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.Simplex;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.maps.Map;
import mindustry.maps.filters.*;
import mindustry.maps.filters.GenerateFilter.GenerateInput;
import mindustry.world.*;

import java.sql.Ref;

import static hexed.HexedMod.mode;
import static mindustry.Vars.*;

public class HexedGenerator implements Cons<Tiles>{
    public int width = Hex.size, height = Hex.size;

    enum Mode{
        // elevation --->
        // temperature
        // |
        // v
        def(new Block[][]{
                {Blocks.sand, Blocks.sand, Blocks.darksand, Blocks.sand, Blocks.darksand, Blocks.grass},
                {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.moss, Blocks.sand},
                {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.grass, Blocks.shale},
                {Blocks.darksandTaintedWater, Blocks.darksandTaintedWater, Blocks.moss, Blocks.moss, Blocks.sporeMoss, Blocks.stone},
                {Blocks.ice, Blocks.iceSnow, Blocks.snow, Blocks.dacite, Blocks.hotrock, Blocks.darksand}
        }, new Block[][]{
                {Blocks.stoneWall, Blocks.duneWall, Blocks.sandWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.sporeWall, Blocks.sandWall},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.sporeWall, Blocks.stoneWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowWall, Blocks.stoneWall, Blocks.duneWall}
        }),

        oilFlats(new Block[][]{
                {Blocks.sand, Blocks.shale, Blocks.sand, Blocks.shale},
                {Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand},
                {Blocks.shale, Blocks.sand, Blocks.sand, Blocks.sand},
                {Blocks.tar, Blocks.sand, Blocks.tar, Blocks.darksand},
                {Blocks.darksand, Blocks.shale, Blocks.darksand, Blocks.shale}
        }, new Block[][]{
                {Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall},
                {Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall}
        }),

        ice(new Block[][]{
                {Blocks.snow, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.snow},
                {Blocks.snow, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.snow},
                {Blocks.iceSnow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
                {Blocks.water, Blocks.iceSnow, Blocks.snow, Blocks.iceSnow, Blocks.snow},
                {Blocks.snow, Blocks.iceSnow, Blocks.ice, Blocks.snow, Blocks.water}
        }, new Block[][]{
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowPine, Blocks.iceWall, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.iceWall, Blocks.snowWall},
                {Blocks.snowPine, Blocks.snowWall, Blocks.snowPine, Blocks.iceWall, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowWall, Blocks.snowWall, Blocks.snowPine, Blocks.snowWall},
                {Blocks.iceWall, Blocks.snowPine, Blocks.snowWall, Blocks.iceWall, Blocks.snowPine}
        }),

        rived(new Block[][]{
                {Blocks.sand, Blocks.sand, Blocks.stone, Blocks.dirt, Blocks.sand, Blocks.grass},
                {Blocks.darksandWater, Blocks.dirt, Blocks.darksand, Blocks.taintedWater, Blocks.grass, Blocks.grass},
                {Blocks.water, Blocks.darksand, Blocks.darksand, Blocks.water, Blocks.grass, Blocks.grass},
                {Blocks.darksandTaintedWater, Blocks.taintedWater, Blocks.stone, Blocks.stone, Blocks.grass, Blocks.stone},
                {Blocks.sand, Blocks.sand, Blocks.stone, Blocks.dirt, Blocks.dirt, Blocks.grass}
        }, new Block[][]{
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.sandWall, Blocks.sandWall, Blocks.pine, Blocks.pine},
                {Blocks.dirtWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.stoneWall, Blocks.duneWall, Blocks.duneWall, Blocks.pine, Blocks.pine},
                {Blocks.stoneWall, Blocks.dirtWall, Blocks.duneWall, Blocks.dirtWall, Blocks.duneWall, Blocks.stoneWall},
                {Blocks.stoneWall, Blocks.pine, Blocks.sandWall, Blocks.sandWall, Blocks.pine, Blocks.pine}
        }),

        lavaLand(new Block[][]{
                {Blocks.sand, Blocks.shale, Blocks.sand, Blocks.shale},
                {Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand},
                {Blocks.shale, Blocks.sand, Blocks.sand, Blocks.sand},
                {Blocks.tar, Blocks.sand, Blocks.tar, Blocks.darksand},
                {Blocks.darksand, Blocks.shale, Blocks.darksand, Blocks.shale}
        }, new Block[][]{
                {Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.sandWall, Blocks.sandWall},
                {Blocks.sandWall, Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall},
                {Blocks.sandWall, Blocks.shaleWall, Blocks.sandWall, Blocks.sandWall}
        });

        final Block[][] floors;
        final Block[][] blocks;

        Mode(Block[][] floors, Block[][] blocks){
            this.floors = floors;
            this.blocks = blocks;
        }
    }

    @Override
    public void get(Tiles tiles){
        Seq<GenerateFilter> ores = new Seq<>();
        maps.addDefaultOres(ores);
        ores.each(o -> ((OreFilter)o).threshold -= 0.05f);
        ores.insert(0, new OreFilter(){{
            ore = Blocks.oreScrap;
            scl += 2 / 2.1F;
        }});
        GenerateInput in = new GenerateInput();
        IntSeq hex = getHex();

        int s1 = Mathf.random(0, 10000);
        int s2 = Mathf.random(0, 10000);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int temp = Mathf.clamp((int)((Simplex.noise2d(s1, 12, 0.6, 1.0 / 400, x, y) - 0.5) * 10 * mode.blocks.length), 0, mode.blocks.length - 1);
                int elev = Mathf.clamp((int)(((Simplex.noise2d(s2, 12, 0.6, 1.0 / 700, x, y) - 0.5) * 10 + 0.15f) * mode.blocks[0].length), 0, mode.blocks[0].length - 1);

                Block floor = mode.floors[temp][elev];
                Block wall = mode.blocks[temp][elev];
                Block ore = Blocks.air;

                for(GenerateFilter f : ores){
                    in.floor = Blocks.stone;
                    in.block = wall;
                    in.overlay = ore;
                    in.x = x;
                    in.y = y;
                    in.width = in.height = Hex.size;
                    f.apply(in);
                    if(in.overlay != Blocks.air){
                        ore = in.overlay;
                    }
                }

                tiles.set(x, y, new Tile(x, y, floor.id, ore.id, wall.id));
            }
        }

        for(int i = 0; i < hex.size; i++){
            int x = Point2.x(hex.get(i));
            int y = Point2.y(hex.get(i));
            Geometry.circle(x, y, width, height, Hex.diameter, (cx, cy) -> {
                if(Intersector.isInsideHexagon(x, y, Hex.diameter, cx, cy)){
                    Tile tile = tiles.getn(cx, cy);
                    tile.setBlock(Blocks.air);
                }
            });
            Angles.circle(3, 360f / 3 / 2f - 90, f -> {
                Tmp.v1.trnsExact(f, Hex.spacing + 12);
                if(Structs.inBounds(x + (int)Tmp.v1.x, y + (int)Tmp.v1.y, width, height)){
                    Tmp.v1.trnsExact(f, Hex.spacing / 2 + 7);
                    Bresenham2.line(x, y, x + (int)Tmp.v1.x, y + (int)Tmp.v1.y, (cx, cy) -> {
                        Geometry.circle(cx, cy, width, height, 3, (c2x, c2y) -> tiles.getn(c2x, c2y).setBlock(Blocks.air));
                    });
                }
            });
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = tiles.getn(x, y);
                Block wall = tile.block();
                Block floor = tile.floor();

                if(wall == Blocks.air){
                    if(Mathf.chance(0.03)){
                        if(floor == Blocks.sand) wall = Blocks.sandBoulder;
                        else if(floor == Blocks.stone) wall = Blocks.boulder;
                        else if(floor == Blocks.shale) wall = Blocks.shaleBoulder;
                        else if(floor == Blocks.darksand) wall = Blocks.boulder;
                        else if(floor == Blocks.moss) wall = Blocks.sporeCluster;
                        else if(floor == Blocks.ice) wall = Blocks.snowBoulder;
                        else if(floor == Blocks.snow) wall = Blocks.snowBoulder;
                    }
                }
                tile.setBlock(wall);
            }
        }

        if(mode == Mode.rived){
            RiverNoiseFilter rivernoise = new RiverNoiseFilter();
            Reflect.set(rivernoise, "floor", Blocks.sand);
            Reflect.set(rivernoise, "floor2", Blocks.water);

            rivernoise.randomize();
            in.begin(rivernoise, width, height, tiles::getn);
            rivernoise.apply(tiles, in);
        }

        for(int i = 0; i < hex.size; i++){
            int x = Point2.x(hex.get(i));
            int y = Point2.y(hex.get(i));

            int offsetX = x - 2;
            int offsetY = y - 2;
            for(int x5 = offsetX; x5 < offsetX + 5; x5++){
                for(int y5 = offsetY; y5 < offsetY + 5; y5++){
                    Tile t = tiles.get(x5, y5);
                    t.setFloor(Blocks.metalFloor.asFloor());
                }
            }
        }

        state.map = new Map(StringMap.of("name", "Hex"));
    }

    public IntSeq getHex(){
        IntSeq array = new IntSeq();
        double h = Math.sqrt(3) * Hex.spacing / 2;
        //base horizontal spacing=1.5w
        //offset = 3/4w
        for(int x = 0; x < width / Hex.spacing - 2; x++){
            for(int y = 0; y < height / (h / 2) - 2; y++){
                int cx = (int)(x * Hex.spacing * 1.5 + (y % 2) * Hex.spacing * 3.0 / 4) + Hex.spacing / 2;
                int cy = (int)(y * h / 2) + Hex.spacing / 2;
                array.add(Point2.pack(cx, cy));
            }
        }
        return array;
    }
}
