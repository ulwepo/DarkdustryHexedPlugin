package hexed;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import hexed.HexData.PlayerData;
import hexed.components.*;
import hexed.generation.GenerationType;
import hexed.generation.GenerationTypes;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.Planet;
import mindustry.world.Block;
import mindustry.world.blocks.environment.SteamVent;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Locale;

import static arc.struct.Seq.with;
import static arc.util.Align.left;
import static arc.util.Strings.autoFixed;
import static hexed.components.Bundle.*;
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

    public static final Rules rules = new Rules() {
        @Override
        public Gamemode mode() {
            return Gamemode.pvp;
        }
    };

    public static final ObjectMap<Planet, PlanetData> planets = new ObjectMap<>();
    public static final ObjectMap<Block, Block> vents = new ObjectMap<>();

    public static boolean restarting = false;
    public static float counter = roundTime;
    public static GenerationType type;

    public static String getForm(String key, Locale locale, int value) {
        var words = get(key, locale).split("\\|");
        return value + " " + words[value % 10 == 1 && value % 100 != 11 ? 0 : value % 10 >= 2 && value % 10 <= 4 && (value % 100 < 10 || value % 100 >= 20) ? 1 : 2];
    }

    @Override
    public void init() {
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        rules.enemyCoreBuildRadius = Hex.radius * tilesize;
        rules.unitDamageMultiplier = 1.25f;
        rules.logicUnitBuild = true;
        rules.pvp = false;
        rules.attackMode = true;
        rules.canGameOver = false;
        rules.coreCapture = true;
        rules.reactorExplosions = true;
        rules.fire = false;

        rules.bannedBlocks.add(ripple);
        rules.modeName = "Hexed";

        for (var team : Team.all) {
            var teamRule = rules.teams.get(team);
            teamRule.aiCoreSpawn = false;
            teamRule.rtsAi = true;
            teamRule.rtsMinWeight = 24f;
        }

        planets.put(serpulo, new PlanetData(
                with(oreCopper, oreLead, oreScrap, oreCoal, oreTitanium, oreThorium),
                list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 100, Items.silicon, 250, Items.titanium, 30),
                readBase64("bXNjaAF4nE2SW1LDMAxFZSd2/EgpXUhWxPBhUgOdSeNM0vLYOj8g+ZaBpOm1LOlYlk2RDg21czpnMq/5Ix8pHvM2rqflciozEdkpPeVpI/3wuKM4lmXJ6/CepokO/4xhSutLJjeW+S1/lpW6bUyXS14pboV9w5LmPFGT1pG6p+r5pMM/1y/gOk8lHTmvH8uah/k6Tvm6kT3nWWbDUt55ybkcM8V0WofnNF4Ks4gyf6TqjzS//LQQA7GkRTuqpoN4qk+AREgPyg7WXgIVxkoGDf+1mKxMBaYCU4GphNmyREiPyRvsnrT6K45fBjG3ll6ZGkwNpgZTC7PjuJ605N0JSgvT8qSWyuTlnIaMYaf2zHey9QbBLRpRqw8sBtad2C2Ka6U4i7oCS0M1jlMii3HSCVvHUcbfX1rcPYJ3wjNYy4Bn0TkrnbOyOdIdb4J5jq0oZXJ2Rzt162+H8E6SHYuE+Dq/l207nK5DnySgioN4+GrvHc7zdsQel0MCuGIvBYjUy+EB84B5wDxgHjCPg/Q4SC9bFNkj5B4NVbhLt/YaSEUHoAPQAeiAexdQZwA64N4FrBBkhR8RWUj7")
        ));

        planets.put(erekir, new PlanetData(
                with(wallOreBeryllium, wallOreTungsten, wallOreThorium),
                list(Items.beryllium, 500, Items.tungsten, 300, Items.graphite, 350, Items.silicon, 250),
                readBase64("bXNjaAF4nGNgZWBlZmDJS8xNZWC72HCx+WI7A3dKanFyUWZBSWZ+HgMDA1tOYlJqTjEDU3QsIwNPcn5Rqm5yZkliSmoOUJKRgYEJCBkA4IsSVg==")
        ));

        // Добавляем кастомные стены блокам
        grass.asFloor().wall = pine;
        sand.asFloor().wall = sandWall;

        // Добавляем кастомные декорации блокам
        grass.asFloor().decoration = pine;
        moss.asFloor().decoration = sporeCluster;
        sporeMoss.asFloor().decoration = sporePine;

        content.blocks().each(block -> {
            if (block instanceof SteamVent vent) {
                vents.put(vent.parent, vent);
            }
        });

        vents.put(regolith, yellowStoneVent);
        vents.put(ferricStone, carbonVent);

        Bundle.load();
        Statistics.load();
        GenerationTypes.load();

        Timer.schedule(HexData::updateControl, 0f, 1f);
        Timer.schedule(() -> {
            if (state.isPlaying())
                Groups.player.each(player -> Call.infoPopup(player.con, getLeaderboard(findLocale(player), false), 12f, left, 0, 2, 50, 0));
        }, 0f, 180f);

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
            if (hex != null) {
                hex.updateController();
                Call.effect(Fx.reactorExplosion, hex.wx, hex.wy, Mathf.random(360f), Tmp.c1.rand());
            }

            var team = event.tile.team();
            var player = HexData.getPlayer(team);

            if (team.cores().size > 1) return;

            if (player != null) {
                killPlayer(player);
                sendToChat("events.player-lost", player.coloredName());
                Call.infoMessage(player.con, format("events.you-lost", findLocale(player)));
            } else {
                killTeam(team);
                sendToChat("events.team-lost", team.color, team.name);
            }
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            var hex = HexData.getClosestHex(event.tile);
            if (hex != null) hex.updateController();
        }); // чисто для красоты

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

            var locale = findLocale(player);
            var builder = new StringBuilder();

            if (leaders.isEmpty())
                builder.append(format("commands.top.none", locale));
            else for (int i = 0; i < leaders.size; i++) {
                var data = leaders.get(i);
                builder.append("[orange]").append(i + 1).append(". ")
                        .append(data.name).append("[accent]: [cyan]")
                        .append(getForm("wins", locale, data.wins)).append("\n");
            }


            Call.infoMessage(player.con, format("commands.top.list", locale, builder.toString()));
        });

        handler.<Player>register("spectator", "Switch to spectator mode.", (args, player) -> {
            if (player.team() == Team.derelict)
                bundled(player, "commands.spectator.already"); // почему оно не возвращает в игру?
            else {
                killPlayer(player);
                bundled(player, "commands.spectator.success");
            }
        });

        handler.<Player>register("lb", "View the current leaderboard.", (args, player) -> Call.infoMessage(player.con, getLeaderboard(findLocale(player), false)));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");
        handler.removeCommand("gameover");

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

            type = custom;
            startGame();
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

        var locale = findLocale(player);
        var builder = new StringBuilder(format("hex", locale, hex.id)).append("\n");

        if (hex.controller == null)
            if (hex.getProgressPercent(player.team()) > 0)
                builder.append(format("hex.capture-progress", locale, autoFixed(hex.getProgressPercent(player.team()), 4)));
            else
                builder.append(format("hex.empty", locale));
        else if (hex.controller.player == player)
            builder.append(format("hex.captured", locale));
        else if (hex.controller.player != null)
            builder.append(format("hex.captured-by-player", locale, hex.controller.player.team().color, hex.controller.name()));
        else
            builder.append(format("hex.unknown", locale));

        Call.setHudText(player.con, builder.toString());
    }

    public void startGame() {
        logic.reset();
        Call.worldDataBegin();

        Log.info("Generating location for scenario @...", type.name);

        world.loadGenerator(Hex.size, Hex.size, HexedGenerator::generate);
        HexData.init();

        Log.info("Location generated.");

        state.rules = type.applyRules(rules.copy());
        logic.play();
    }

    public void endGame() {
        if (restarting) return;
        restarting = true;

        Events.fire("HexedGameOver");
        Log.info("The round is over.");

        Time.runTask(60f * 15f, this::reload);

        var datas = HexData.getLeaderboard();
        if (datas.isEmpty()) return;

        var winner = datas.first();
        var statistic = Statistics.getData(winner.player.uuid());

        Groups.player.each(player -> {
            var locale = findLocale(player);
            var builder = new StringBuilder(format("restart.header", locale));

            if (player == winner.player)
                builder.append(format("restart.you-won", locale, getForm("hexes", locale, winner.controlled())));
            else
                builder.append(format("restart.player-won", locale, winner.name(), getForm("hexes", locale, winner.controlled())));

            builder.append("\n\n");

            builder.append(winner.name()).append("[white]: [accent]")
                    .append(getForm("wins", locale, statistic.wins))
                    .append(" [lime]\uE803[accent] ")
                    .append(getForm("wins", locale, statistic.wins + 1));

            builder.append(format("restart.final-score", locale, getLeaderboard(locale, true)));

            Call.infoMessage(player.con, builder.toString());
        });

        statistic.wins++;
        Statistics.save();
    }

    public void reload() {
        var players = Groups.player.copy(new Seq<>());

        type = next();
        startGame();

        players.each(player -> {
            boolean admin = player.admin;
            player.reset();
            player.admin(admin);

            player.team(netServer.assignTeam(player, players));

            spawn(player);

            netServer.sendWorldData(player);
        });

        counter = roundTime;
        restarting = false;
    }

    public String getLeaderboard(Locale locale, boolean endGame) {
        var datas = HexData.getLeaderboard();
        var builder = new StringBuilder();

        if (!endGame) {
            datas.truncate(5);
            builder.append(format("leaderboard.header", locale, (int) counter / 60 / 60));
        }

        for (int i = 0; i < datas.size; i++) {
            var data = datas.get(i);
            builder.append("[orange]").append(i + 1).append(". ").append(data.name())
                    .append("[orange] (").append(getForm("hexes", locale, data.controlled())).append(")")
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
            Call.infoMessage(player.con, format("events.no-empty-hex", findLocale(player)));
            player.clearUnit();
            player.team(Team.derelict);
        }
    }
}