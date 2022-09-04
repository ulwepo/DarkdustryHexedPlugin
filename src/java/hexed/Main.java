package hexed;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.Seq.SeqIterable;
import arc.util.*;
import arc.util.Timer.Task;
import hexed.comp.Bundle;
import hexed.comp.Statistics;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Locale;

import static hexed.comp.Bundle.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final float spawnDelay = 60 * 6f;
    public static final float winCapturePercent = 0.75f;

    public static final float roundTime = 60 * 60 * 90f;
    public static final float leaderboardTime = 60 * 60 * 3f;
    public static final float updateTime = 60 * 1f;
    public static final float leftTeamDestroyTime = 90f;

    public static final int itemRequirement = 2560;

    public static final int leaderboardTimer = 0, updateTimer = 1;

    public static final Rules rules = new Rules() {
        @Override
        public Gamemode mode() {
            return Gamemode.pvp;
        }
    };

    public static final Interval interval = new Interval(2);
    public static final ObjectMap<String, Team> leftPlayers = new ObjectMap<>();
    public static final ObjectMap<Team, Task> leftPlayerTeams = new ObjectMap<>();

    public static Seq<Block> serpuloOres, erekirOres;

    public static Schematic serpuloStart, erekirStart;
    public static HexedGenerator.Mode mode;

    public static boolean restarting = false;
    public static float counter = roundTime;

    @Override
    public void init() {
        rules.loadout = ItemStack.list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 100, Items.silicon, 250, Items.titanium, 30);
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        rules.enemyCoreBuildRadius = Hex.diameter * tilesize / 2f;
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

        Blocks.moss.asFloor().decoration = Blocks.sporeMoss.asFloor().decoration = Blocks.sporeCluster;

        Bundle.load();
        Statistics.load();

        Events.run(Trigger.update, () -> {
            if (!state.isPlaying()) return;

            HexData.updateStats();

            Groups.player.each(player -> {
                updateText(player);

                if (player.team() == Team.derelict) player.clearUnit();

                if (HexData.getControlledSize(player) >= HexData.hexesAmount() * winCapturePercent) endGame();
            });

            if (interval.get(updateTimer, updateTime)) {
                HexData.updateControl();
            }

            if (interval.get(leaderboardTimer, leaderboardTime)) {
                Groups.player.each(player -> Call.infoToast(player.con, getLeaderboard(player), 10f));
            }

            counter -= Time.delta;

            if (counter <= 0) endGame();
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if (event.tile.block() instanceof CoreBlock) {
                Hex hex = HexData.getHex(event.tile);
                if (hex != null) {
                    hex.updateController();
                    hex.spawnTime.reset();

                    Call.effect(Fx.reactorExplosion, hex.wx, hex.wy, Mathf.random(360f), Tmp.c1.rand());
                }

                Team team = event.tile.team();
                Player player = HexData.getPlayer(team);

                if (team.cores().size <= 1) {
                    if (player != null) {
                        killPlayer(player);
                        sendToChat("events.player-lost", player.name);
                        Call.infoMessage(player.con, format("events.you-lost", findLocale(player)));
                    } else {
                        killTeam(team);
                        sendToChat("events.team-lost", team.color, team.name);
                    }
                }
            }
        });

        Events.on(BuildSelectEvent.class, event -> {
            Hex hex = HexData.getHex(event.tile);
            if (hex != null) {
                hex.updateController();
            }
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            Hex hex = HexData.getHex(event.tile);
            if (hex != null) {
                hex.updateController();
            }
        });

        Events.on(PlayerJoin.class, event -> {
            var statistic = Statistics.getData(event.player.uuid());
            statistic.name = event.player.name;
            Statistics.save();

            if (event.player.team() != Team.derelict) {
                Team team = leftPlayers.get(event.player.uuid());
                if (team != null && !team.data().noCores()) {
                    leftPlayers.remove(event.player.uuid());
                    leftPlayerTeams.remove(team).cancel();
                    return;
                }

                spawn(event.player);
            }
        });

        Events.on(PlayerLeave.class, event -> {
            if (event.player.team() != Team.derelict && !restarting) {
                leftPlayers.put(event.player.uuid(), event.player.team());
                leftPlayerTeams.put(event.player.team(), Timer.schedule(() -> {
                    killTeam(event.player.team());
                    leftPlayers.remove(event.player.uuid());
                    leftPlayerTeams.remove(event.player.team());
                }, leftTeamDestroyTime));
            }
        });

        netServer.assigner = (player, players) -> {
            if (leftPlayers.containsKey(player.uuid())) return leftPlayers.get(player.uuid());

            var teams = Seq.with(Team.all).filter(team -> team != Team.derelict && !team.active() && !leftPlayerTeams.containsKey(team));

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

            for (int i = 0; i < leaders.size;) {
                var statistic = leaders.get(i);

                players.append("[orange]").append(++i).append(". ")
                        .append(statistic.name).append("[accent]: [cyan]")
                        .append(getForm("decl.wins", locale, statistic.wins))
                        .append("\n");
            }

            if (leaders.isEmpty()) {
                players.append(format("commands.top.none", locale));
            }

            Call.infoMessage(player.con, format("commands.top.list", locale, players.toString()));
        });

        handler.<Player>register("spectator", "Перейти в режим наблюдателя.", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.spectator.already");
                return;
            }

            killPlayer(player);
            bundled(player, "commands.spectator.success");
        });

        handler.<Player>register("lb", "Посмотреть текущий список лидеров.", (args, player) -> Call.infoMessage(player.con, getLeaderboard(player)));

        handler.<Player>register("time", "Посмотреть время, оставшееся до конца раунда.", (args, player) -> bundled(player, "commands.time", (int) counter / 60 / 60));

        handler.<Player>register("hexstatus", "Посмотреть статус хекса на своем местоположении.", (args, player) -> {
            var hex = HexData.getHex(player);
            if (hex == null) {
                bundled(player, "commands.hexstatus.not-found");
                return;
            }

            hex.updateController();

            var locale = findLocale(player);
            var status = new StringBuilder(format("commands.hexstatus.hex", locale, hex.id)).append("\n");

            if (hex.controller != null && HexData.getPlayer(hex.controller) != null) {
                status.append(format("commands.hexstatus.owner", locale, HexData.getPlayer(hex.controller).name)).append("\n");
            } else {
                status.append(format("commands.hexstatus.owner.none", locale)).append("\n");

                for (var teamData : state.teams.getActive()) {
                    if (hex.getProgressPercent(teamData.team) > 0 && HexData.getPlayer(teamData.team) != null) {
                        status.append("[white]|> [accent]")
                                .append(HexData.getPlayer(teamData.team).name)
                                .append("[lightgray]: [accent]")
                                .append(format("commands.hexstatus.progress", locale, Strings.autoFixed(hex.getProgressPercent(teamData.team), 4)))
                                .append("\n");
                    }
                }
            }
            player.sendMessage(status.toString());
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");
        handler.removeCommand("gameover");

        handler.register("hexed", "[режим_генерации]", "Запустить сервер в режиме Hexed.", args -> {
            if (!state.isMenu()) {
                Log.err("Сервер уже запущен. Используй 'stop', чтобы остановить его.");
                return;
            }

            var custom = args.length > 0 ? Structs.find(HexedGenerator.Mode.values(), m -> m.name().equalsIgnoreCase(args[0])) : Seq.with(HexedGenerator.Mode.values()).random(mode);
            if (custom == null) {
                Log.err("Режим генерации с таким названием не найден!");
                return;
            }

            mode = custom;
            startGame();
            netServer.openServer();
        });

        handler.register("end", "Завершить раунд.", args -> {
            if (state.isMenu()) {
                Log.err("Сервер не запущен. Используй 'hexed', чтобы запустить его.");
                return;
            }

            endGame();
        });

        handler.register("time", "Посмотреть время, оставшееся до конца раунда.", args -> Log.info("Время до конца раунда: '@' минут", (int) counter / 60 / 60));
    }

    public void updateText(Player player) {
        if (player.team() == Team.derelict) return;

        var hex = HexData.getHex(player);
        if (hex == null) return;

        var locale = findLocale(player);
        var message = new StringBuilder(format("hud.hex", locale, hex.id)).append("\n");

        if (hex.controller == null) {
            if (hex.getProgressPercent(player.team()) > 0) {
                message.append(format("hud.hex.capture-progress", locale, Strings.autoFixed(hex.getProgressPercent(player.team()), 4)));
            } else {
                message.append(format("hud.hex.empty", locale));
            }
        } else if (hex.controller == player.team()) {
            message.append(format("hud.hex.captured", locale));
        } else if (hex.controller != null && HexData.getPlayer(hex.controller) != null) {
            message.append("[#").append(hex.controller.color).append("]").append(format("hud.hex.captured-by-player", locale, HexData.getPlayer(hex.controller).name));
        } else {
            message.append(format("hud.hex.unknown", locale));
        }

        Call.setHudText(player.con, message.toString());
    }

    public void startGame() {
        logic.reset();
        Call.worldDataBegin();

        Log.info("Создание локации по сценарию @...", mode);

        HexedGenerator generator = new HexedGenerator();
        world.loadGenerator(Hex.size, Hex.size, generator);
        HexData.initHexes(generator.getHexes());

        Log.info("Локация сгенерирована.");

        state.rules = mode.applyRules(rules.copy());
        logic.play();

        Call.sendMessage(mode.displayName);
    }

    public void endGame() {
        if (restarting) return;
        restarting = true;

        Events.fire("HexedGameOver");
        Log.info("Раунд окончен.");

        var players = HexData.getLeaderboard();
        var scores = new StringBuilder();

        if (players.any()) {
            for (int i = 0; i < players.size;) {
                var player = players.get(i);
                scores.append("[orange]").append(++i).append(". ")
                        .append(player.name)
                        .append("[lightgray] (").append(getForm("decl.hexes", findLocale(player), HexData.getControlledSize(player))).append(")")
                        .append("\n");
            }

            var winner = players.first();
            var statistic = Statistics.getData(winner.uuid());

            Groups.player.each(player -> {
                var locale = findLocale(player);
                var endGameMessage = new StringBuilder(format("restart.header", locale));

                if (player == winner) {
                    endGameMessage.append(format("restart.you-won", locale, getForm("decl.hexes", locale, HexData.getControlledSize(player))));
                } else {
                    endGameMessage.append(format("restart.player-won", locale, winner.name, getForm("decl.hexes", locale, HexData.getControlledSize(winner))));
                }

                endGameMessage.append("\n\n");

                endGameMessage.append(winner.name).append("[white]: [accent]")
                        .append(getForm("decl.wins", locale, statistic.wins))
                        .append(" [lime]\uE803[accent] ")
                        .append(getForm("decl.wins", locale, statistic.wins + 1));

                endGameMessage.append(format("restart.final-score", locale, scores.toString()));

                Call.infoMessage(player.con, endGameMessage.toString());
            });

            statistic.wins++;
            Statistics.save();
        }

        Time.runTask(60f * 15f, this::reload);
    }

    public void reload() {
        var players = Groups.player.copy(new Seq<>());

        mode = Seq.with(HexedGenerator.Mode.values()).random(mode);
        startGame();

        players.each(player -> {
            boolean admin = player.admin;
            player.reset();
            player.admin = admin;
            player.team(netServer.assignTeam(player, new SeqIterable<>(players)));

            if (player.team() != Team.derelict) {
                spawn(player);
            }

            netServer.sendWorldData(player);
        });

        for (int i = 0; i < interval.getTimes().length; i++) interval.reset(i, 0f);

        leftPlayers.clear();
        leftPlayerTeams.clear();

        counter = roundTime;
        restarting = false;
    }

    public String getLeaderboard(Player player) {
        var players = HexData.getLeaderboard();
        players.setSize(Math.min(4, players.size));

        var locale = findLocale(player);
        var leaders = new StringBuilder(format("leaderboard.header", locale, (int) counter / 60 / 60));

        for (int i = 0; i < players.size;) {
            var p = players.get(i);
            leaders.append("[orange]").append(++i).append(". ")
                    .append(p.name)
                    .append("[orange] (").append(getForm("decl.hexes", locale, HexData.getControlledSize(p))).append(")")
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

        world.tiles.eachTile(tile -> {
            if (tile.build != null && tile.block() != Blocks.air && tile.team() == team) {
                Time.run(Mathf.random(360f), tile::removeNet);
            }
        });

        Groups.unit.each(u -> u.team == team, unit -> Time.run(Mathf.random(360f), () -> Call.unitDespawn(unit)));
    }

    public void spawn(Player player) {
        Hex hex = HexData.getSpawnHex();
        if (hex != null) {
            loadout(player, hex.x, hex.y);
            hex.findController();
        } else {
            Call.infoMessage(player.con, format("events.no-empty-hex", findLocale(player)));
            player.clearUnit();
            player.team(Team.derelict);
        }
    }

    public void loadout(Player player, int x, int y) {
        var start = mode.startScheme;
        var coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        int sx = x - coreTile.x, sy = y - coreTile.y;

        start.tiles.each(stile -> {
            var tile = world.tile(stile.x + sx, stile.y + sy);
            if (tile == null) return;
            tile.setNet(stile.block, player.team(), stile.rotation);
            tile.getLinkedTiles(new Seq<>()).each(t -> t.floor().isDeep(), t -> t.setFloorNet(Blocks.darkPanel3));

            if (stile.config != null) tile.build.configureAny(stile.config);

            if (stile == coreTile) {
                for (ItemStack stack : state.rules.loadout) {
                    Call.setItem(tile.build, stack.item, stack.amount);
                }
            }
        });
    }

    public static String getForm(String key, Locale locale, int value) {
        var words = Bundle.get(key, locale).split("\\|");
        return value + " " + words[(value % 10 == 1 && value % 100 != 11) ? 0 : value % 10 >= 2 && value % 10 <= 4 && (value % 100 < 10 || value % 100 >= 20) ? 1 : 2];
    }
}
