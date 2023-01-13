package hexed;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import hexed.HexData.PlayerData;
import hexed.components.PlanetData;
import hexed.components.Statistics;
import hexed.generation.GenerationType;
import hexed.generation.GenerationTypes;
import mindustry.content.Items;
import mindustry.game.EventType.*;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.net.WorldReloader;
import mindustry.type.Planet;
import mindustry.world.Block;
import mindustry.world.blocks.environment.SteamVent;
import mindustry.world.blocks.storage.CoreBlock;
import useful.Bundle;

import static arc.struct.Seq.with;
import static arc.util.Strings.autoFixed;
import static hexed.Hex.radius;
import static hexed.generation.GenerationType.*;
import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;
import static mindustry.content.Planets.*;
import static mindustry.game.Schematics.readBase64;
import static mindustry.type.ItemStack.list;

public class Main extends Plugin {

    public static final float winCapturePercent = 0.75f;

    public static final float roundTime = 60 * 60 * 90f;
    public static final float leftTeamDestroyTime = 90f;

    public static final int itemRequirement = 2560;

    public static final Rules rules = new Rules();

    public static final ObjectMap<Planet, PlanetData> planets = new ObjectMap<>();
    public static final ObjectMap<Block, Block> vents = new ObjectMap<>();

    public static boolean restarting = false;
    public static float counter = roundTime;

    public static GenerationType type;

    public static String getForm(String key, Player player, int value) {
        var words = Bundle.get(key, player).split("\\|");
        return value + " " + words[value % 10 == 1 && value % 100 != 11 ? 0 : value % 10 >= 2 && value % 10 <= 4 && (value % 100 < 10 || value % 100 >= 20) ? 1 : 2];
    }

    @Override
    public void init() {
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        rules.enemyCoreBuildRadius = radius * tilesize;
        rules.logicUnitBuild = true;
        rules.pvp = false;
        rules.canGameOver = false;
        rules.coreCapture = true;
        rules.reactorExplosions = true;
        rules.damageExplosions = true;
        rules.fire = false;

        rules.bannedBlocks.addAll(ripple, swarmer);
        rules.modeName = "Hexed";

        planets.put(serpulo, new PlanetData(
                with(oreCopper, oreLead, oreScrap, oreCoal, oreTitanium, oreThorium),
                list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 150, Items.silicon, 250, Items.titanium, 50),
                readBase64("bXNjaAF4nE2SW1LDMAxFZSd2/EgpXUhWxPBhUgOdSeNM0vLYOj8g+ZaBpOm1LOlYlk2RDg21czpnMq/5Ix8pHvM2rqflciozEdkpPeVpI/3wuKM4lmXJ6/CepokO/4xhSutLJjeW+S1/lpW6bUyXS14pboV9w5LmPFGT1pG6p+r5pMM/1y/gOk8lHTmvH8uah/k6Tvm6kT3nWWbDUt55ybkcM8V0WofnNF4Ks4gyf6TqjzS//LQQA7GkRTuqpoN4qk+AREgPyg7WXgIVxkoGDf+1mKxMBaYCU4GphNmyREiPyRvsnrT6K45fBjG3ll6ZGkwNpgZTC7PjuJ605N0JSgvT8qSWyuTlnIaMYaf2zHey9QbBLRpRqw8sBtad2C2Ka6U4i7oCS0M1jlMii3HSCVvHUcbfX1rcPYJ3wjNYy4Bn0TkrnbOyOdIdb4J5jq0oZXJ2Rzt162+H8E6SHYuE+Dq/l207nK5DnySgioN4+GrvHc7zdsQel0MCuGIvBYjUy+EB84B5wDxgHjCPg/Q4SC9bFNkj5B4NVbhLt/YaSEUHoAPQAeiAexdQZwA64N4FrBBkhR8RWUj7")
        ));

        planets.put(erekir, new PlanetData(
                with(wallOreBeryllium, wallOreTungsten, wallOreThorium),
                list(Items.beryllium, 500, Items.tungsten, 300, Items.graphite, 350, Items.silicon, 250),
                readBase64("bXNjaAF4nGNgZWBlZmDJS8xNZWC72HCx+WI7A3dKanFyUWZBSWZ+HgMDA1tOYlJqTjEDU3QsIwNPcn5Rqm5yZkliSmoOUJKRgYEJCBkA4IsSVg==")
        ));

        // Добавляем кастомные стены блокам
        grass.asFloor().wall = pine;

        // Добавляем кастомные декорации блокам
        grass.asFloor().decoration = pine;
        moss.asFloor().decoration = sporeCluster;
        sporeMoss.asFloor().decoration = sporePine;

        content.blocks().each(block -> {
            if (block instanceof SteamVent vent)
                vents.put(vent.parent, vent);
        });

        vents.put(regolith, yellowStoneVent);
        vents.put(ferricStone, carbonVent);

        Bundle.load(Main.class);
        Statistics.load();
        GenerationTypes.load();

        Timer.schedule(HexData::updateControl, 0f, 1f);
        Timer.schedule(() -> Groups.player.each(player -> Call.infoPopup(player.con, getLeaderboard(player, false), 12f, 8, 0, 2, 50, 0)), 0f, 180f);

        Events.run(Trigger.update, () -> {
            if (!state.isPlaying()) return;

            HexData.datas.each(PlayerData::active, data -> {
                updateText(data.player);
                if (data.controlled() >= HexData.hexes.size * winCapturePercent) endGame();
            });

            counter -= Time.delta;
            if (counter <= 0) endGame();
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if (!(event.tile.block() instanceof CoreBlock)) return;

            var hex = HexData.getClosestHex(event.tile);
            if (hex == null) return;

            hex.updateController();

            var team = event.tile.team();
            var player = HexData.getPlayer(team);

            if (team.cores().size > 1) return;

            if (player != null) {
                killPlayer(player);
                Bundle.sendToChat("events.player-lost", player.coloredName());
                Call.infoMessage(player.con, Bundle.format("events.you-lost", player));
            } else {
                killTeam(team);
                Bundle.sendToChat("events.team-lost", team.color, team.name);
            }
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            var hex = HexData.getClosestHex(event.tile);
            if (hex != null) hex.updateController();
        });

        Events.on(PlayerJoin.class, event -> {
            Statistics.getData(event.player.uuid()).name = event.player.name;
            Statistics.save();

            if (event.player.team() == Team.derelict || restarting) return;

            var data = HexData.getData(event.player.uuid());
            if (data != null && !data.player.team().data().noCores()) {
                data.player = event.player;
                data.left.cancel();
            } else spawn(event.player);
        });

        Events.on(PlayerLeave.class, event -> {
            if (event.player.team() == Team.derelict || restarting) return;

            var data = HexData.getData(event.player.uuid());
            if (data != null)
                data.left = Timer.schedule(() -> killTeam(event.player.team()), leftTeamDestroyTime);
        });

        netServer.assigner = (player, players) -> {
            var data = HexData.getData(player.uuid());
            if (data != null) return data.player.team();

            var teams = with(Team.all).filter(team -> team != Team.derelict && !team.active() && HexData.getData(team) == null);
            return teams.any() ? teams.random() : Team.derelict;
        };

        netServer.chatFormatter = (player, message) -> player != null ? "[coral][[[cyan]" + Statistics.getData(player.uuid()).wins + " [sky]#[white] " + player.coloredName() + "[coral]]: [white]" + message : message;
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("top", "Show the best 10 players of the server.", (args, player) -> {
            var leaders = Statistics.getLeaders();
            leaders.truncate(10);

            var builder = new StringBuilder();

            if (leaders.isEmpty())
                builder.append(Bundle.format("commands.top.none", player));
            else for (int i = 0; i < leaders.size; i++) {
                var data = leaders.get(i);
                builder.append("[orange]").append(i + 1).append(". ")
                        .append(data.name).append("[accent]: [cyan]")
                        .append(getForm("wins", player, data.wins)).append("\n");
            }

            Call.infoMessage(player.con, Bundle.format("commands.top.list", player, builder.toString()));
        });

        handler.<Player>register("spectator", "Switch to spectator mode.", (args, player) -> {
            if (player.team() == Team.derelict)
                Bundle.bundled(player, "commands.spectator.already"); // почему оно не возвращает в игру?
            else {
                killPlayer(player);
                Bundle.bundled(player, "commands.spectator.success");
            }
        });

        handler.<Player>register("lb", "View the current leaderboard.", (args, player) -> Call.infoMessage(player.con, getLeaderboard(player, false)));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");

        handler.register("hexed", "[generation_mode...]", "Open the server in hexed mode.", args -> {
            if (!state.isMenu()) {
                Log.err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            var custom = args.length > 0 ? all.find(type -> type.name.toLowerCase().contains(args[0].toLowerCase())) : next();
            if (custom == null) {
                Log.err("No generation mode with this name found!");
                return;
            }

            startGame(custom);
            netServer.openServer();
        });

        handler.register("gameover", "End the round.", args -> {
            if (state.isMenu()) {
                Log.err("The server is not running. Use 'hexed' to run it.");
                return;
            }

            endGame();
        });
    }

    public void updateText(Player player) {
        var hex = HexData.getClosestHex(player);
        if (hex == null) return;

        var builder = new StringBuilder(Bundle.format("hex", player, hex.id)).append("\n");

        if (hex.controller == null)
            if (hex.getProgressPercent(player.team()) > 0)
                builder.append(Bundle.format("hex.capture-progress", player, autoFixed(hex.getProgressPercent(player.team()), 4)));
            else
                builder.append(Bundle.format("hex.empty", player));
        else if (hex.controller.player == player)
            builder.append(Bundle.format("hex.captured", player));
        else if (hex.controller.player != null)
            builder.append(Bundle.format("hex.captured-by-player", player, hex.controller.color(), hex.controller.name()));
        else
            builder.append(Bundle.format("hex.unknown", player));

        Call.setHudText(player.con, builder.toString());
    }

    public void startGame(GenerationType next) {
        type = next;

        var reloader = new WorldReloader();
        reloader.begin();

        Log.info("Generating location for scenario @...", type.name);

        world.loadGenerator(Hex.size, Hex.size, HexedGenerator::generate);
        HexData.init();

        Log.info("Location generated.");

        logic.play();
        state.rules = type.applyRules(rules.copy());

        reloader.end();
    }

    public void endGame() {
        if (restarting) return;
        restarting = true;

        Events.fire("Gameover");
        Log.info("The round is over.");

        Time.runTask(60f * 15f, this::reload);

        var datas = HexData.getLeaderboard();
        if (datas.isEmpty()) return;

        var winner = datas.first();
        var statistic = Statistics.getData(winner.player.uuid());

        Groups.player.each(player -> {
            var builder = new StringBuilder(Bundle.format("restart.header", player));

            if (player == winner.player)
                builder.append(Bundle.format("restart.you-won", player, getForm("hexes", player, winner.controlled())));
            else
                builder.append(Bundle.format("restart.player-won", player, winner.name(), getForm("hexes", player, winner.controlled())));

            builder.append("\n\n");

            builder.append(winner.name()).append("[white]: [accent]")
                    .append(getForm("wins", player, statistic.wins))
                    .append(" [lime]\uE803[accent] ")
                    .append(getForm("wins", player, statistic.wins + 1));

            builder.append(Bundle.format("restart.final-score", player, getLeaderboard(player, true)));

            Call.infoMessage(player.con, builder.toString());
        });

        statistic.wins++;
        Statistics.save();
    }

    public void reload() {
        var players = Groups.player.copy(new Seq<>());

        startGame(next());

        players.each(player -> {
            player.team(netServer.assignTeam(player, players));
            spawn(player);
        });

        counter = roundTime;
        restarting = false;
    }

    public String getLeaderboard(Player player, boolean endGame) {
        var leaderboard = HexData.getLeaderboard();
        var builder = new StringBuilder();

        if (!endGame) {
            leaderboard.truncate(5);
            builder.append(Bundle.format("leaderboard.header", player, (int) counter / 60 / 60));
        }

        for (int i = 0; i < leaderboard.size; i++) {
            var data = leaderboard.get(i);
            builder.append("[orange]").append(i + 1).append(". ").append(data.name())
                    .append("[orange] (").append(getForm("hexes", player, data.controlled())).append(")")
                    .append("\n");
        }

        return builder.toString();
    }

    public void killPlayer(Player player) {
        killTeam(player.team());

        player.clearUnit();
        player.team(Team.derelict);
        Call.hideHudText(player.con);
    }

    public void killTeam(Team team) {
        if (team == Team.derelict || !team.data().active()) return;

        var data = HexData.getData(team);
        if (data != null && data.left != null) data.left.cancel();

        HexData.removeData(team);

        world.tiles.eachTile(tile -> {
            if (tile.build != null && tile.block() != air && tile.team() == team)
                Time.run(Mathf.random(360f), tile::removeNet);
        });

        Groups.unit.each(unit -> unit.team == team, unit -> Time.run(Mathf.random(360f), () -> Call.unitEnvDeath(unit)));

        team.data().plans.clear();
    }

    public void spawn(Player player) {
        var hex = HexData.getSpawnHex();
        if (hex != null) {
            HexedGenerator.loadout(player, hex);
            hex.findController();
            HexData.datas.add(new PlayerData(player));
        } else {
            Call.infoMessage(player.con, Bundle.format("events.no-empty-hex", player));
            player.clearUnit();
            player.team(Team.derelict);
        }
    }
}