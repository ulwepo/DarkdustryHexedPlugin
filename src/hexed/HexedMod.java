package hexed;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import hexed.HexData.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.NetServer.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.io.TypeIO;
import mindustry.maps.Map;
import mindustry.mod.*;
import mindustry.net.Packets.*;
import mindustry.net.WorldReloader;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static arc.util.Log.*;
import static mindustry.Vars.*;

import java.util.HashSet;

public class HexedMod extends Plugin{
    //in seconds
    public static final float spawnDelay = 60 * 4;
    //health requirement needed to capture a hex; no longer used
    public static final float healthRequirement = 35000;
    //item requirement to captured a hex
    public static final int itemRequirement = 2500;

    public static final int messageTime = 1;
    //in ticks: 60 minutes
    private final static int roundTime = 60 * 60 * 90;
    //in ticks: 3 minutes
    private final static int leaderboardTime = 60 * 60 * 2;

    private final static int updateTime = 60 * 2;

    private final static int winCondition = 25;

    private final static int timerBoard = 0, timerUpdate = 1, timerWinCheck = 2;

    protected static HexedGenerator.Mode mode;

    private final Rules rules = new Rules();
    private Interval interval = new Interval(5);

    private HexData data;
    private boolean restarting = false, registered = false;

    private Schematic start;
    private double counter = 0f;
    private int lastMin;

    private final HashSet<Team> timerTeams = new HashSet<>();

    @Override
    public void init(){
        rules.pvp = true;
        rules.tags.put("hexed", "true");
        rules.loadout = ItemStack.list(Items.copper, 500, Items.lead, 500, Items.graphite, 250, Items.metaglass, 200, Items.silicon, 250, Items.titanium, 100);
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 1f / 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 3.6f;
        rules.enemyCoreBuildRadius = (Hex.diameter) * tilesize / 2f;
        rules.unitDamageMultiplier = 1.4f;
        rules.canGameOver = false;
        //Если включить это, то после выхода игрока с
        //сервера на его месте появится дереликтовое ядро
        //Смотрите баг #3
        rules.coreCapture = false;

        start = Schematics.readBase64("bXNjaAB4nE2SgY7CIAyGC2yDsXkXH2Tvcq+AkzMmc1tQz/j210JpXDL8hu3/lxYY4FtBs4ZbBLvG1ync4wGO87bvMU2vsCzTEtIlwvCxBW7e1r/43hKYkGY4nFN4XqbfMD+29IbhvmHOtIc1LjCmuIcrfm3X9QH2PofHIyYY5y3FaX3OS3ze4fiRwX7dLa5nDHTPddkCkT3l1DcA/OALihZNq4H6NHnV+HZCVshJXA9VYZC9kfVU+VQGKSsbjVT1lOgp1qO4rGIo9yvnquxH1ORIohap6HVIDbtpaNlDi4cWD80eFJdrNhbJc8W61Jzdqi/3wrRIRii7GYdelvWMZDQs1kNbqtYe9/KuGvDX5zD6d5SML66+5dwRqXgQee5GK3Edxw1ITfb3SJ71OomzUAdjuWsWqZyJavd8Issdb5BqVbaoGCVzJqrddaUGTWSFHPs67m6H5HlaTqbqpFc91Kfn+2eQSp9pr96/Xtx6cevZjeKKDuUOklvvXy9uPGdNZFjZi7IXZS/n8Hyf/wFbjj/q");

        Events.run(Trigger.update, () -> {
            if(active()){
                data.updateStats();

                for(Player player : Groups.player){
                    if(player.team() != Team.derelict && player.team().cores().isEmpty()){
                        player.clearUnit();
                        killTiles(player.team());
                        Call.sendMessage("[yellow](!)[] [accent]" + player.name + "[lightgray] был уничтожен![yellow] (!)");
                        Call.infoMessage(player.con, "Твои ядра уничтожены. Ты проиграл.");
                        player.team(Team.derelict);
                    }

                    if(player.team() == Team.derelict){
                        player.clearUnit();
                    }else if(data.getControlled(player).size == data.hexes().size){
                        endGame();
                        break;
                    }
                }

                int minsToGo = (int)(roundTime - counter) / 60 / 60;
                if(minsToGo != lastMin){
                    lastMin = minsToGo;
                }

                if(interval.get(timerBoard, leaderboardTime)){
                    Call.infoToast(getLeaderboard(), 15f);
                    rules.loadout.each(e -> e.amount *= 1.033);
                }

                if(interval.get(timerUpdate, updateTime)){
                    data.updateControl();
                }

                if(interval.get(timerWinCheck, 60 * 2)){
                    Seq<Player> players = data.getLeaderboard();
                    if(!players.isEmpty() && data.getControlled(players.first()).size >= winCondition && players.size > 1 && data.getControlled(players.get(1)).size <= 1){
                        endGame();
                    }
                }

                counter += Time.delta;

                //kick everyone and restart the script
                if(counter > roundTime){
                    endGame();
                }
            }else{
                counter = 0;
            }
        });

        Events.on(BlockDestroyEvent.class, event -> {
            //reset last spawn times so this hex becomes vacant for a while.
            if(event.tile.block() instanceof CoreBlock){
                Hex hex = data.getHex(event.tile.pos());
                
                if(hex != null){
                    //update state
                    hex.spawnTime.reset();
                    hex.updateController();
                }
            }
        });

        Events.on(PlayerLeave.class, event -> {
            if(active() && event.player.team() != Team.derelict) {
                timerTeams.add(event.player.team());
                Timer.schedule(() -> {
                    if (!event.player.team().active()) {
                        killTiles(event.player.team());
                    }
                    timerTeams.remove(event.player.team());
                }, 75f);
            }
        });

        Events.on(PlayerJoin.class, event -> {
            if(!active() || event.player.team() == Team.derelict) return;

            Seq<Hex> copy = data.hexes().copy();
            copy.shuffle();
            Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());

            if(hex != null){
                loadout(event.player, hex.x, hex.y);
                Core.app.post(() -> data.data(event.player).chosen = false);
                hex.findController();
            }else{
                Call.infoMessage(event.player.con, "Не найдено свободных хексов для спавна.\nПереключение в режим наблюдателя...");
                event.player.unit().kill();
                event.player.team(Team.derelict);
            }

            data.data(event.player).lastMessage.reset();
        });

        Events.on(ProgressIncreaseEvent.class, event -> updateText(event.player));

        Events.on(HexCaptureEvent.class, event -> updateText(event.player));

        Events.on(HexMoveEvent.class, event -> updateText(event.player));

        TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Seq<Player> arr = Seq.with(players);

            if(active()){
                //pick first inactive team
                for(Team team : Team.all){
                    if(team.id > 5 && !team.active() && !arr.contains(p -> p.team() == team) && !data.data(team).dying && !data.data(team).chosen && !timerTeams.contains(team)) {
                        data.data(team).chosen = true;
                        return team;
                    }
                }
                Call.infoMessage(player.con, "Свободных хексов для спавна не найдено.\nПереключение в режим наблюдателя...");
                return Team.derelict;
            }
            return prev.assign(player, players);
        };
    }

    void updateText(Player player){
        HexTeam team = data.data(player);

        StringBuilder message = new StringBuilder("[white]Хекс #" + team.location.id + "\n");

        if(!team.lastMessage.get()) return;

        if(team.location.controller == null){
            if(team.progressPercent > 0){
                message.append("[lightgray]Прогресс захвата: [accent]").append((int)(team.progressPercent)).append("%");
            }else{
                message.append("[lightgray][[Пусто]");
            }
        }else if(team.location.controller == player.team()){
            message.append("[yellow][[Захвачен]");
        }else if(team.location != null && team.location.controller != null && data.getPlayer(team.location.controller) != null){
            message.append("[#").append(team.location.controller.color).append("]Захвачен игроком ").append(data.getPlayer(team.location.controller).name);
        }else{
            message.append("<Неизвестно>");
        }

        Call.setHudText(player.con, message.toString());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("hexed", "[mode/list]", "Начать хостить в режиме Хексов.", args -> {
            if(args.length > 0 && args[0].equalsIgnoreCase("list")){
                Log.info("Доступные режимы:");
                for(HexedGenerator.Mode value : HexedGenerator.Mode.values()){
                    Log.info("- @", value);
                }
                return;
            }

            if(!state.is(State.menu)){
                Log.err("Сначала выключи сервер, клоун.");
                return;
            }

            HexedGenerator.Mode custom = null;
            if(args.length > 0){
                try{
                    custom = HexedGenerator.Mode.valueOf(args[0]);
                }catch(Throwable t){
                    Log.err("Неверное название режима.");
                }
            }
            mode = custom == null ? Structs.random(HexedGenerator.Mode.values()) : custom;

            data = new HexData();

            logic.reset();
            info("Генерирую карту по сценарию @...", mode);
            HexedGenerator generator = new HexedGenerator();
            world.loadGenerator(Hex.size, Hex.size, generator);
            data.initHexes(generator.getHex());
            info("Карта сгенерирована.");
            state.rules = rules.copy();
            logic.play();
            netServer.openServer();
        });

        handler.register("countdown", "Get the hexed restart countdown.", args -> {
            info("Время до конца раунда: &lc@ минут", (int)(roundTime - counter) / 60 / 60);
        });

        handler.register("end", "End the game.", args -> endGame());
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        if(registered) return;
        registered = true;

        handler.<Player>register("spectate", "Режим наблюдателя. При использовании уничтожает твою базу.", (args, player) -> {
            if(player.team() == Team.derelict){
                player.sendMessage("[scarlet]Ты уже наблюдатель.");
            }else{
                killTiles(player.team());
                player.unit().kill();
                player.team(Team.derelict);
            }
        });

        handler.<Player>register("captured", "Узнать количество захваченных хексов.", (args, player) -> {
            if(player.team() == Team.derelict){
                player.sendMessage("[scarlet]Ты наблюдатель. Откуда у тебя могу быть хексы, клоун?");
            }else{
                player.sendMessage("[lightgray]Ты захватил[accent] " + data.getControlled(player).size + "[] хексов.");
            }
        });

        handler.<Player>register("leaderboard", "Показать таблицу лидеров.", (args, player) -> {
            player.sendMessage(getLeaderboard());
        });

        handler.<Player>register("hexstatus", "Узнать статус хекса.", (args, player) -> {
            Hex hex = data.data(player).location;
            if(hex != null){
                hex.updateController();
                StringBuilder builder = new StringBuilder();
                builder.append("| [lightgray]Хекс #").append(hex.id).append("[]\n");
                String name = hex.controller != null && data.getPlayer(hex.controller) != null ? data.getPlayer(hex.controller).name : "<никто>";
                builder.append("| [lightgray]Владелец:[] ").append(name).append("\n");
                for(TeamData data : state.teams.getActive()){
                    if(hex.getProgressPercent(data.team) > 0){
                        builder.append("|> [accent]").append(this.data.getPlayer(data.team).name).append("[lightgray]: ")
                                .append((int)hex.getProgressPercent(data.team)).append("% захвата\n");
                    }
                }
                player.sendMessage(builder.toString());
            }else{
                player.sendMessage("[scarlet]Хекс не найден.");
            }
        });
    }

    void endGame(){
        if(restarting) return;

        restarting = true;
        Seq<Player> players = data.getLeaderboard();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < players.size && i < 3; i++){
            if(data.getControlled(players.get(i)).size > 1){
                builder.append("[yellow]").append(i + 1).append(".[accent] ").append(players.get(i).name)
                        .append("[lightgray] (x").append(data.getControlled(players.get(i)).size).append(")[]\n");
            }
        }

        if(!players.isEmpty()){
            boolean dominated = data.getControlled(players.first()).size == data.hexes().size;

            for(Player player : Groups.player){
                Call.infoMessage(player.con, "[accent]--ROUND OVER--\n\n[lightgray]"
                        + (player == players.first() ? "[accent]Ты[] оказался" : "[yellow]" + players.first().name + "[lightgray] оказался") +
                        " победителем, захватив [accent]" + data.getControlled(players.first()).size + "[lightgray] хексов."
                        + (dominated ? "" : "\n\nФинальные очки:\n" + builder));
            }
        }

        Time.runTask(60f * 10f, this::reload);
    }

    void reload(){
        Seq<Player> players = new Seq<>();
        for(Player p : Groups.player){
            if(p.isLocal()) continue;
            players.add(p);
            p.clearUnit();
        }

        logic.reset();

        Call.worldDataBegin();

        mode = Structs.random(HexedGenerator.Mode.values());
        data = new HexData();

        info("Пересоздание карты по сценарию @...", mode);

        HexedGenerator generator = new HexedGenerator();
        world.loadGenerator(Hex.size, Hex.size, generator);
        data.initHexes(generator.getHex());
        info("Карта сгенерирована.");
        state.rules = rules.copy();

        logic.play();

        for(Player p : players){
            if(p.con == null) continue;

            boolean wasAdmin = p.admin;
            p.reset();
            p.admin = wasAdmin;
            p.team(netServer.assignTeam(p, new Seq.SeqIterable<>(players)));

            if(p.team() != Team.derelict){
                Seq<Hex> copy = data.hexes().copy();
                copy.shuffle();
                Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());
                if(hex != null){
                    loadout(p, hex.x, hex.y);
                    Core.app.post(() -> data.data(p).chosen = false);
                    hex.findController();
                }else{
                    Call.infoMessage(p.con, "Не найдено свободных хексов для спавна.\nПереключение в режим наблюдателя...");
                    p.unit().kill();
                    p.team(Team.derelict);
                }
                data.data(p).lastMessage.reset();
            }

            netServer.sendWorldData(p);
        }

        for(int i = 0; i < 5; i++){
            interval.reset(i, 0f);
        }

        counter = 0f;

        restarting = false;
    }

    String getLeaderboard(){
        StringBuilder builder = new StringBuilder();
        builder.append("[accent]Список лидеров\n[scarlet]").append(lastMin).append("[lightgray] мин. до конца раунда\n\n");
        int count = 0;
        for(Player player : data.getLeaderboard()){
            builder.append("[yellow]").append(++count).append(".[white] ")
                    .append(player.name).append("[orange] (").append(data.getControlled(player).size).append(" хексов)\n[white]");

            if(count > 4) break;
        }
        return builder.toString();
    }

    void killTiles(Team team){
        data.data(team).dying = true;
        Time.runTask(8f, () -> data.data(team).dying = false);
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);
                if(tile.build != null && tile.team() == team){
                    Time.run(Mathf.random(60f * 6), tile.build::kill);
                }
            }
        }
        for(Unit unit : Groups.unit){
            if(unit.team() == team) unit.kill();
        }
    }

    void loadout(Player player, int x, int y){
        Stile coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        start.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox, st.y + oy);
            if(tile == null) return;

            if(tile.block() != Blocks.air){
                tile.removeNet();
            }

            tile.setNet(st.block, player.team(), st.rotation);

            if(st.config != null){
                tile.build.configureAny(st.config);
            }
            if(tile.block() instanceof CoreBlock){
                for(ItemStack stack : state.rules.loadout){
                    Call.setItem(tile.build, stack.item, stack.amount);
                }
            }
        });
    }

    public boolean active(){
        return state.rules.tags.getBool("hexed") && !state.is(State.menu);
    }
}
