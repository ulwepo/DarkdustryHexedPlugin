package hexed;

import arc.Events;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.*;
import hexed.HexData.PlayerData;
import hexed.components.Bundle;
import hexed.components.Statistics;
import hexed.generation.GenerationType;
import hexed.generation.GenerationTypes;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Locale;

import static hexed.components.Bundle.*;
import static hexed.generation.GenerationType.*;
import static mindustry.Vars.*;

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

    public static Seq<Block> serpuloOres, erekirOres;
    public static Schematic serpuloStart, erekirStart;

    public static boolean restarting = false;
    public static float counter = roundTime;
    public static GenerationType type;

    @Override
    public void init() {
        rules.loadout = ItemStack.list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 100, Items.silicon, 250, Items.titanium, 30);
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

        rules.bannedBlocks.add(Blocks.ripple);
        rules.modeName = "Hexed";

        serpuloOres = Seq.with(Blocks.oreCopper, Blocks.oreLead, Blocks.oreScrap, Blocks.oreCoal, Blocks.oreTitanium, Blocks.oreThorium);
        erekirOres = Seq.with(Blocks.wallOreBeryllium, Blocks.wallOreTungsten, Blocks.wallOreThorium);

        serpuloStart = Schematics.readBase64("bXNjaAF4nE2SW1LDMAxFZSd2/EgpXUhWxPBhUgOdSeNM0vLYOj8g+ZaBpOm1LOlYlk2RDg21czpnMq/5Ix8pHvM2rqflciozEdkpPeVpI/3wuKM4lmXJ6/CepokO/4xhSutLJjeW+S1/lpW6bUyXS14pboV9w5LmPFGT1pG6p+r5pMM/1y/gOk8lHTmvH8uah/k6Tvm6kT3nWWbDUt55ybkcM8V0WofnNF4Ks4gyf6TqjzS//LQQA7GkRTuqpoN4qk+AREgPyg7WXgIVxkoGDf+1mKxMBaYCU4GphNmyREiPyRvsnrT6K45fBjG3ll6ZGkwNpgZTC7PjuJ605N0JSgvT8qSWyuTlnIaMYaf2zHey9QbBLRpRqw8sBtad2C2Ka6U4i7oCS0M1jlMii3HSCVvHUcbfX1rcPYJ3wjNYy4Bn0TkrnbOyOdIdb4J5jq0oZXJ2Rzt162+H8E6SHYuE+Dq/l207nK5DnySgioN4+GrvHc7zdsQel0MCuGIvBYjUy+EB84B5wDxgHjCPg/Q4SC9bFNkj5B4NVbhLt/YaSEUHoAPQAeiAexdQZwA64N4FrBBkhR8RWUj7");
        erekirStart = Schematics.readBase64("bXNjaAF4nGNgZWBlZmDJS8xNZWC72HCx+WI7A3dKanFyUWZBSWZ+HgMDA1tOYlJqTjEDU3QsIwNPcn5Rqm5yZkliSmoOUJKRgYEJCBkA4IsSVg==");

        Blocks.grass.asFloor().wall = Blocks.pine;
        Blocks.sand.asFloor().wall = Blocks.sandWall;
        Blocks.moss.asFloor().decoration = Blocks.sporeMoss.asFloor().decoration = Blocks.sporeCluster;

        Bundle.load();
        Statistics.load();
        GenerationTypes.load();

        Timer.schedule(HexData::updateControl, 0f, 1f);
        Timer.schedule(() -> {
            if (state.isPlaying()) Groups.player.each(player -> Call.infoToast(player.con, getLeaderboard(findLocale(player), false), 10f));
        }, 0f, 180f);

        Events.run(Trigger.update, () -> {
            if (!state.isPlaying()) return;

            HexData.datas.each(PlayerData::active, data -> {
                updateText(data.player);
                if (data.controls >= HexData.hexes.size * winCapturePercent) endGame();
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

            var old = HexData.getData(event.player.uuid());
            if (old != null && !old.player.team().data().noCores()) {
                old.player = player;
                old.left.cancel();
            } else spawn(event.player);
        });

        Events.on(PlayerLeave.class, event -> {
            if (event.player.team() == Team.derelict || restarting) return;
            
            HexData.getData(event.player.team()).left = Timer.schedule(() -> killTeam(event.player.team()), leftTeamDestroyTime);
        });

        netServer.assigner = (player, players) -> {
            var data = HexData.getData(player.uuid());
            if (data != null) return data.player.team();

            var teams = Seq.with(Team.all).filter(team -> team != Team.derelict && !team.active());
            return teams.any() ? teams.random() : Team.derelict;
        };

        netServer.chatFormatter = (player, message) -> player != null ? "[coral][[[cyan]" + Statistics.getData(player.uuid()).wins + " [sky]#[white] " + player.coloredName() + "[coral]]: [white]" + message : message;
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("top", "Показать топ-10 игроков сервера.", (args, player) -> {
            var leaders = Statistics.getLeaders();
            leaders.setSize(Math.min(10, leaders.size));

            var locale = findLocale(player);
            var players = new StringBuilder();

            if (leaders.isEmpty())
                players.append(format("commands.top.none", locale));
            else for (int i = 0; i < leaders.size; i++) {
                var statistic = leaders.get(i);
                players.append("[orange]").append(i + 1).append(". ")
                        .append(statistic.name).append("[accent]: [cyan]")
                        .append(getForm("decl.wins", locale, statistic.wins)).append("\n");
            }

            Call.infoMessage(player.con, format("commands.top.list", locale, players.toString()));
        });

        handler.<Player>register("spectator", "Перейти в режим наблюдателя.", (args, player) -> {
            if (player.team() == Team.derelict)
                bundled(player, "commands.spectator.already"); // почему оно не возвращает в игру?
            else {
                killPlayer(player);
                bundled(player, "commands.spectator.success");
            }
        });

        handler.<Player>register("lb", "Посмотреть текущий список лидеров.", (args, player) -> Call.infoMessage(player.con, getLeaderboard(findLocale(player), false)));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");
        handler.removeCommand("gameover");

        handler.register("hexed", "[generation_mode]", "Open the server in hexed mode.", args -> {
            if (!state.isMenu()) {
                Log.err("Already hosting. Type 'stop' to stop hosting first.");
                return;
            }

            var custom = args.length > 0 ? all.find(type -> type.name.equalsIgnoreCase(args[0])) : next();
            if (custom == null) {
                Log.err("No generation mode with this name found!");
                return;
            }

            type = custom;
            startGame();
            netServer.openServer();
        });

        handler.register("end", "Finish the round.", args -> {
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
        var message = new StringBuilder(format("hex", locale, hex.id)).append("\n");

        if (hex.controller == null)
            if (hex.getProgressPercent(player.team()) > 0) {
                message.append(format("hex.capture-progress", locale, Strings.autoFixed(hex.getProgressPercent(player.team()), 4)));
            } else {
                message.append(format("hex.empty", locale));
            }
        else if (hex.controller.player == player)
            message.append(format("hex.captured", locale));
        else if (hex.controller != null && hex.controller.player !=null) 
            message.append("[#").append(hex.controller.player.team().color).append("]").append(format("hex.captured-by-player", locale, hex.controller.player.coloredName()));
        else
            message.append(format("hex.unknown", locale));

        Call.setHudText(player.con, message.toString());
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
            var endGameMessage = new StringBuilder(format("restart.header", locale));

            if (player == winner.player)
                endGameMessage.append(format("restart.you-won", locale, getForm("decl.hexes", locale, winner.controls)));
            else
                endGameMessage.append(format("restart.player-won", locale, winner.name(), getForm("decl.hexes", locale, winner.controls)));

            endGameMessage.append("\n\n");

            endGameMessage.append(winner.name()).append("[white]: [accent]")
                    .append(getForm("decl.wins", locale, statistic.wins))
                    .append(" [lime]\uE803[accent] ")
                    .append(getForm("decl.wins", locale, statistic.wins + 1));

            endGameMessage.append(format("restart.final-score", locale, getLeaderboard(locale, true)));

            Call.infoMessage(player.con, endGameMessage.toString());
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

            var team = netServer.assignTeam(player, players);
            Log.info(team.name);
            player.team(team);

            spawn(player);

            netServer.sendWorldData(player);
        });

        counter = roundTime;
        restarting = false;
    }

    public String getLeaderboard(Locale locale, boolean endGame) {
        var datas = HexData.getLeaderboard();
        var leaders = new StringBuilder();

        if (!endGame) {
            datas.setSize(Math.min(4, datas.size));
            leaders.append(format("leaderboard.header", locale, (int) counter / 60 / 60));
        }

        for (int i = 0; i < datas.size; i++) {
            var data = datas.get(i);
            leaders.append("[orange]").append(i + 1).append(". ").append(data.name())
                    .append("[orange] (").append(getForm("decl.hexes", locale, data.controls)).append(")")
                    .append("\n");
        }
        return leaders.toString();
    }

    public void killPlayer(Player player) {
        killTeam(player.team());

        player.clearUnit();
        player.team(Team.derelict);
        Call.hideHudText(player.con);
    }

    public void killTeam(Team team) {
        if (team == Team.derelict || !team.data().active()) return;

        var data = HexData.datas.find(d -> d.player.team() == team);
        if (data.left != null) data.left.cancel();

        world.tiles.eachTile(tile -> {
            if (tile.build != null && tile.block() != Blocks.air && tile.team() == team)
                Time.run(Mathf.random(360f), tile::removeNet);
        });

        Groups.unit.each(u -> u.team == team, unit -> Time.run(Mathf.random(360f), () -> Call.unitDespawn(unit)));
    }

    public void spawn(Player player) {
        Hex hex = HexData.getSpawnHex();
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

    public static String getForm(String key, Locale locale, int value) {
        var words = Bundle.get(key, locale).split("\\|");
        return value + " " + words[(value % 10 == 1 && value % 100 != 11) ? 0 : value % 10 >= 2 && value % 10 <= 4 && (value % 100 < 10 || value % 100 >= 20) ? 1 : 2];
    }
}
