package hexed;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.Seq.SeqIterable;
import arc.util.CommandHandler;
import arc.util.Interval;
import arc.util.Structs;
import arc.util.Time;
import hexed.HexData.HexCaptureEvent;
import hexed.HexData.HexMoveEvent;
import hexed.HexData.HexTeam;
import hexed.HexData.ProgressIncreaseEvent;
import hexed.comp.Bundle;
import hexed.comp.NoPauseRules;
import hexed.comp.PlayerData;
import hexed.comp.Statistics;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.game.EventType.*;
import mindustry.game.Rules;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.game.Schematics;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static arc.util.Log.err;
import static arc.util.Log.info;
import static hexed.comp.Bundle.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final float spawnDelay = 60 * 6f;
    public static final float baseKillDelay = 60 * 75f;

    public static final int roundTime = 60 * 60 * 90;
    public static final int leaderboardTime = 60 * 60 * 2;
    public static final int updateTime = 60 * 2;

    public static final int itemRequirement = 2560;

    public static final int leaderboardTimer = 0, updateTimer = 1;

    public static final Rules rules = new NoPauseRules();
    public static final Interval interval = new Interval(2);
    public static final ObjectMap<String, Team> leftPlayers = new ObjectMap<>();

    public static Schematic start;

    public static HexedGenerator.Mode mode;
    public static HexData data;
    public static boolean restarting = false;
    public static float counter = 0f;

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
        rules.canGameOver = false;
        rules.coreCapture = true;
        rules.reactorExplosions = true;
        rules.fire = false;

        rules.modeName = "Hexed";

        start = Schematics.readBase64("bXNjaAF4nE2SX3LbIBDGFyQh/sh2fINcQCfK5IHItPWMIjSS3DRvuUqu0Jnew71OX5JdPs80wuYDdvmxu0CBjhXVU3xOFH6kX+l0v25x2Sic0jos53k754mIzBif0rjS/uH6fv3z9+36W/rHHYUhz3Na+pc4jnT8MunHuHxPZIc8/UyveaF2HeK2pYXCmtnWz3FKI1VxGah9KpZXOn4x3QDmOU0n3mUv05ijjLohL6mfLsOYLiv5Ob/wkVM+cQbxvPTf4rBlZhEl/pMqP9Lc+KshDcSQFm2pTC3EUfk8JEA6UHaYHcRRYaxkUHFXY7EwFZgKTAWmEmbNEiAdFm+wO9Lqf3DcGMTcEnphajA1mBpMLcyW/TrSsm8vKC1My4vsVpE07bhrGjZqz3wryVbsrCXsUogSvWVpMNvLvEZwtQRnEJc4VBDeElgaK5UwZRxk/PGvmDt47bC1BNaAZ1A5I5UzkhzplpOoJUxDQcLk3S3t1K2+LZXracXTsYiLK+sHSdvidi3qVPxELMTBVmpvcZ+3K3Z4HA55OQlApDwOB5gDzAHmAHOAOVykw0U6SVHkAJc7EY9X4lFeD7QH2gPtgfZAe7w7jzg90B7vzuMELyd8Ao5MVAI=");

        Bundle.load();
        Statistics.load();

        Events.run(Trigger.update, () -> {
            if (!state.isPlaying()) return;

            data.updateStats();

            Groups.player.each(player -> {
                if (player.team() != Team.derelict && player.team().data().noCores()) {
                    killPlayer(player);
                    sendToChat("events.player-lost", player.coloredName());
                    Call.infoMessage(player.con, Bundle.format("events.you-lost", findLocale(player)));
                }

                if (data.getControlled(player).size == data.hexes().size) {
                    endGame();
                }
            });

            state.serverPaused = false;

            if (interval.get(leaderboardTimer, leaderboardTime)) {
                Groups.player.each(player -> Call.infoToast(player.con, getLeaderboard(player), 12f));
            }

            if (interval.get(updateTimer, updateTime)) {
                data.updateControl();
            }

            counter += Time.delta;

            if (counter > roundTime) endGame();
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if (event.tile.block() instanceof CoreBlock) {
                Hex hex = data.getHex(event.tile.pos());
                if (hex != null) {
                    hex.updateController();
                    hex.spawnTime.reset();

                    Call.effect(Fx.reactorExplosion, hex.wx, hex.wy, Mathf.random(360f), Color.white);
                    Groups.player.each(this::updateText);
                }
            }
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            Hex hex = data.hexes().find(h -> h.contains(event.tile));
            if (hex != null) {
                hex.updateController();
            }
        });

        Events.on(PlayerJoin.class, event -> {
            if (event.player.team() != Team.derelict) {
                if (leftPlayers.containsKey(event.player.uuid())) {
                    leftPlayers.remove(event.player.uuid());
                    return;
                }

                spawn(event.player);
            }

            PlayerData data = Statistics.getData(event.player.uuid());
            data.name = event.player.coloredName();
            Statistics.save();
        });

        Events.on(PlayerLeave.class, event -> {
            if (event.player.team() != Team.derelict) {
                leftPlayers.put(event.player.uuid(), event.player.team());

                Time.run(baseKillDelay, () -> {
                    Player player = Groups.player.find(p -> p.team() == event.player.team() && p.uuid().equals(event.player.uuid()));
                    if (player == null) {
                        killTeam(event.player.team());
                        leftPlayers.remove(event.player.uuid());
                    }
                });
            }
        });

        Events.on(ProgressIncreaseEvent.class, event -> updateText(event.player));

        Events.on(HexMoveEvent.class, event -> updateText(event.player));

        Events.on(HexCaptureEvent.class, event -> updateText(event.player));

        netServer.assigner = (player, players) -> {
            if (leftPlayers.containsKey(player.uuid())) return leftPlayers.get(player.uuid());

            Team team = Seq.with(Team.all).shuffle().find(t -> t.id > 5 && !t.active() && !data.data(t).dying && !data.data(t).chosen && !leftPlayers.containsValue(t, true));
            return team != null ? team : Team.derelict;
        };

        netServer.chatFormatter = (player, message) -> {
            if (player != null) {
                int wins = Statistics.getData(player.uuid()).wins;
                return ("[coral][[[cyan]" + wins + " [sky]#[white] " + player.coloredName() + "[coral]]: [white]" + message);
            }

            return message;
        };
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("top", "Показать лучших игроков сервера.", (args, player) -> {
            StringBuilder players = new StringBuilder();
            Seq<PlayerData> leaders = Statistics.getLeaders();

            for (int i = 0; i < Math.min(leaders.size, 10); i++) {
                players.append("[accent]").append(i + 1).append(". ").append(leaders.get(i).name).append("[accent]: [cyan]").append(leaders.get(i).wins).append("\n");
            }

            if (leaders.isEmpty()) {
                players.append(Bundle.format("commands.top.none", findLocale(player)));
            }

            Call.infoMessage(player.con, Bundle.format("commands.top.list", findLocale(player), players.toString()));
        });

        handler.<Player>register("spectator", "Перейти в режим наблюдателя.", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.spectator.already");
                return;
            }

            killPlayer(player);
            bundled(player, "commands.spectator.success");
        });

        handler.<Player>register("captured", "Посмотреть количество захваченных хексов.", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.captured.spectator");
                return;
            }

            bundled(player, "commands.captured.hexes", data.getControlled(player).size);
        });

        handler.<Player>register("lb", "Посмотреть текущий список лидеров.", (args, player) -> player.sendMessage(getLeaderboard(player)));

        handler.<Player>register("hexstatus", "Посмотреть статус хекса на своем местоположении.", (args, player) -> {
            Hex hex = data.data(player).location;
            if (hex != null) {
                hex.updateController();
                StringBuilder status = new StringBuilder(Bundle.format("commands.hexstatus.hex", findLocale(player), hex.id)).append("\n").append(Bundle.format("commands.hexstatus.owner", findLocale(player), hex.controller != null && data.getPlayer(hex.controller) != null ? data.getPlayer(hex.controller).coloredName() : Bundle.format("commands.hexstatus.owner.none", findLocale(player)))).append("\n");
                for (TeamData teamData : state.teams.getActive()) {
                    if (hex.getProgressPercent(teamData.team) > 0 && hex.getProgressPercent(teamData.team) <= 100) {
                        status.append("[white]|> [accent]").append(data.getPlayer(teamData.team).coloredName()).append("[lightgray]: [accent]").append(Bundle.format("commands.hexstatus.captured", findLocale(player), (int) hex.getProgressPercent(teamData.team))).append("\n");
                    }
                }
                player.sendMessage(status.toString());
                return;
            }
            bundled(player, "commands.hexstatus.not-found");
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");
        handler.removeCommand("gameover");

        handler.register("hexed", "[mode/list]", "Start the server in HexPvP mode.", args -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                info("Доступные режимы:");
                for (HexedGenerator.Mode value : HexedGenerator.Mode.values()) {
                    info("- @", value);
                }
                return;
            }

            if (!state.isMenu()) {
                err("Сервер уже запущен. Используй 'stop', чтобы остановить его.");
                return;
            }

            HexedGenerator.Mode custom = null;
            if (args.length > 0) {
                try {
                    custom = HexedGenerator.Mode.valueOf(args[0]);
                } catch (Exception e) {
                    err("Неверное название режима. Будет выбран случайный режим.");
                }
            }

            mode = custom == null ? Structs.random(HexedGenerator.Mode.values()) : custom;
            data = new HexData();

            logic.reset();

            info("Создание локации по сценарию @...", mode);

            HexedGenerator generator = new HexedGenerator();
            world.loadGenerator(Hex.size, Hex.size, generator);
            data.initHexes(generator.getHex());

            info("Локация сгенерирована.");

            state.rules = mode.applyRules(rules.copy());
            logic.play();

            netServer.openServer();
        });

        handler.register("time", "See the time remaining until the end of the round.", args -> info("Время до конца раунда: &lc@ минут", (int) (roundTime - counter) / 60 / 60));

        handler.register("end", "End the round.", args -> endGame());
    }

    public void updateText(Player player) {
        if (player.team() == Team.derelict) return;

        HexTeam team = data.data(player);
        StringBuilder message = new StringBuilder(Bundle.format("hex", findLocale(player), team.location.id)).append("\n");

        if (team.location.controller == null) {
            if (team.progressPercent > 0) {
                message.append(Bundle.format("hex.capture-progress", findLocale(player), (int) (team.progressPercent)));
            } else {
                message.append(Bundle.format("hex.empty", findLocale(player)));
            }
        } else if (team.location.controller == player.team()) {
            message.append(Bundle.format("hex.captured", findLocale(player)));
        } else if (team.location.controller != null && data.getPlayer(team.location.controller) != null) {
            message.append("[#").append(team.location.controller.color).append("]").append(Bundle.format("hex.captured-by-player", findLocale(player), data.getPlayer(team.location.controller).coloredName()));
        } else {
            message.append(Bundle.format("hex.unknown", findLocale(player)));
        }

        Call.setHudText(player.con, message.toString());
    }

    public void endGame() {
        if (restarting) return;

        restarting = true;

        Seq<Player> players = data.getLeaderboard();
        StringBuilder scores = new StringBuilder();

        if (players.any()) {
            for (int i = 0; i < players.size; i++) {
                scores.append("[yellow]").append(i + 1).append(".[white] ").append(players.get(i).coloredName()).append("[lightgray] (x").append(data.getControlled(players.get(i)).size).append(")[]\n");
            }

            Player winner = players.first();
            Groups.player.each(player -> {
                StringBuilder endGameMessage = new StringBuilder(Bundle.format("round-over", findLocale(player)));

                if (player == winner) {
                    endGameMessage.append(Bundle.format("you-won", findLocale(player), data.getControlled(player).size));
                } else {
                    endGameMessage.append(Bundle.format("player-won", findLocale(player), winner.coloredName(), data.getControlled(winner).size));
                }

                endGameMessage.append(Bundle.format("final-score", findLocale(player), scores.toString()));

                Call.infoMessage(player.con, endGameMessage.toString());
            });

            PlayerData data = Statistics.getData(winner.uuid());
            data.wins++;
            Statistics.save();
        }

        Time.runTask(60f * 15f, this::reload);
    }

    public void reload() {
        Events.fire("HexedGameOver");

        Seq<Player> players = Groups.player.copy(new Seq<>());
        mode = Structs.random(HexedGenerator.Mode.values());
        data = new HexData();

        logic.reset();
        Call.worldDataBegin();

        info("Пересоздание локации по сценарию @...", mode);

        HexedGenerator generator = new HexedGenerator();
        world.loadGenerator(Hex.size, Hex.size, generator);
        data.initHexes(generator.getHex());

        info("Локация сгенерирована.");

        state.rules = mode.applyRules(rules.copy());
        logic.play();

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

        counter = 0f;
        restarting = false;
    }

    public String getLeaderboard(Player player) {
        Seq<Player> players = data.getLeaderboard();
        StringBuilder leaders = new StringBuilder(Bundle.format("leaderboard.header", findLocale(player), (int) (roundTime - counter) / 60 / 60));
        for (int i = 0; i < Math.min(3, players.size); i++) {
            leaders.append("[yellow]").append(i + 1).append(".[white] ").append(players.get(i).coloredName()).append(Bundle.format("leaderboard.hexes", findLocale(player), data.getControlled(players.get(i)).size));
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
        if (team == Team.derelict) return;

        data.data(team).dying = true;
        Time.runTask(8f, () -> data.data(team).dying = false);
        world.tiles.eachTile(tile -> {
            if (tile.build != null && tile.block() != Blocks.air && tile.team() == team) {
                Time.run(Mathf.random(360f), tile::removeNet);
            }
        });

        Groups.unit.each(u -> u.team == team, unit -> Time.run(Mathf.random(360f), () -> Call.unitDespawn(unit)));
    }

    public void spawn(Player player) {
        Hex hex = data.getSpawnHex();
        if (hex != null) {
            loadout(player, hex.x, hex.y);
            Core.app.post(() -> data.data(player).chosen = false);
            hex.findController();
        } else {
            Call.infoMessage(player.con, Bundle.format("events.no-empty-hex", findLocale(player)));
            player.clearUnit();
            player.team(Team.derelict);
        }
    }

    public void loadout(Player player, int x, int y) {
        Stile coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        int ox = x - coreTile.x, oy = y - coreTile.y;
        start.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox, st.y + oy);
            if (tile == null) return;
            tile.setNet(st.block, player.team(), st.rotation);
            tile.getLinkedTiles(new Seq<>()).each(t -> t.floor().isDeep(), t -> t.setFloorNet(Blocks.darkPanel3));

            if (st.config != null) tile.build.configureAny(st.config);

            if (st == coreTile) {
                for (ItemStack stack : state.rules.loadout) {
                    Call.setItem(tile.build, stack.item, stack.amount);
                }
            }
        });
    }
}
