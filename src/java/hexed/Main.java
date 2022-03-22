package hexed;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.Seq.SeqIterable;
import arc.util.*;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import hexed.HexData.HexCaptureEvent;
import hexed.HexData.HexMoveEvent;
import hexed.HexData.HexTeam;
import hexed.HexData.ProgressIncreaseEvent;
import hexed.comp.Bundle;
import hexed.comp.NoPauseRules;
import hexed.models.UserStatistics;
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
import mindustry.net.Administration.Config;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import org.bson.BsonInt32;
import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static arc.util.Log.err;
import static arc.util.Log.info;
import static hexed.comp.Bundle.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final float spawnDelay = 60 * 4f;
    public static final float baseKillDelay = 60 * 75f;

    public static final int roundTime = 60 * 60 * 90;
    public static final int leaderboardTime = 60 * 60 * 2;
    public static final int updateTime = 60 * 2;
    public static final int winCheckTime = 60 * 2;
    public static final int winCondition = 25;

    public static final int itemRequirement = 2500;

    public static final int leaderboardTimer = 0, updateTimer = 1, winCheckTimer = 2;

    public static final String connectionStringUrl = "mongodb://manager:QULIoZBckRlLkZXn@127.0.0.1:27017/?authSource=darkdustry";
    public static final String databaseName = "darkdustry";
    public static final String collectionName = "hexed";
    public static final Rules rules = new NoPauseRules();
    public static final Interval interval = new Interval(3);
    public static final ObjectMap<String, Team> leftPlayers = new ObjectMap<>();
    public static HexedGenerator.Mode mode;
    public static HexData data;
    public static Schematic start;
    public static boolean restarting = false;
    public static float counter = 0f;
    public static int lastMin;

    public Main() {
        ConnectionString connectionString = new ConnectionString(connectionStringUrl);
        MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(connectionString).retryWrites(true).build();

        MongoClient mongodb = MongoClients.create(settings);
        MongoCollection<Document> hexedCollection = mongodb.getDatabase(databaseName).getCollection(collectionName);

        UserStatistics.setSourceCollection(hexedCollection);
    }

    @Override
    public void init() {
        rules.loadout = ItemStack.list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 100, Items.silicon, 250, Items.titanium, 30);
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        rules.enemyCoreBuildRadius = Hex.diameter * tilesize / 2f;
        rules.unitDamageMultiplier = 1.25f;
        rules.pvp = false;
        rules.canGameOver = false;
        rules.coreCapture = true;
        rules.reactorExplosions = true;
        rules.fire = false;

        rules.revealedBlocks.addAll(Blocks.duct, Blocks.ductRouter, Blocks.ductBridge, Blocks.thruster, Blocks.scrapWall, Blocks.scrapWallLarge, Blocks.scrapWallHuge, Blocks.scrapWallGigantic);
        rules.bannedBlocks.addAll(Blocks.ripple);
        rules.modeName = "Hexed";

        start = Schematics.readBase64("bXNjaAF4nE2SX3LbIBDGFyQh/sh2fINcQCfK5IHItPWMIjSS3DRvuUqu0Jnew71OX5JdPs80wuYDdvmxu0CBjhXVU3xOFH6kX+l0v25x2Sic0jos53k754mIzBif0rjS/uH6fv3z9+36W/rHHYUhz3Na+pc4jnT8MunHuHxPZIc8/UyveaF2HeK2pYXCmtnWz3FKI1VxGah9KpZXOn4x3QDmOU0n3mUv05ijjLohL6mfLsOYLiv5Ob/wkVM+cQbxvPTf4rBlZhEl/pMqP9Lc+KshDcSQFm2pTC3EUfk8JEA6UHaYHcRRYaxkUHFXY7EwFZgKTAWmEmbNEiAdFm+wO9Lqf3DcGMTcEnphajA1mBpMLcyW/TrSsm8vKC1My4vsVpE07bhrGjZqz3wryVbsrCXsUogSvWVpMNvLvEZwtQRnEJc4VBDeElgaK5UwZRxk/PGvmDt47bC1BNaAZ1A5I5UzkhzplpOoJUxDQcLk3S3t1K2+LZXracXTsYiLK+sHSdvidi3qVPxELMTBVmpvcZ+3K3Z4HA55OQlApDwOB5gDzAHmAHOAOVykw0U6SVHkAJc7EY9X4lFeD7QH2gPtgfZAe7w7jzg90B7vzuMELyd8Ao5MVAI=");

        Config.startCommands.set("hexed");

        Events.run(Trigger.update, () -> {
            data.updateStats();

            for (Player player : Groups.player) {
                if (player.team() != Team.derelict && player.team().data().noCores()) {
                    killTeam(player.team());
                    player.clearUnit();
                    player.team(Team.derelict);
                    Call.hideHudText(player.con);
                    sendToChat("events.player-lost", player.coloredName());
                    Call.infoMessage(player.con, Bundle.format("events.you-lost", findLocale(player)));
                }

                if (player.team() == Team.derelict) player.clearUnit();

                if (data.getControlled(player).size == data.hexes().size) {
                    endGame();
                    break;
                }
            }

            state.serverPaused = false;
            lastMin = (int) (roundTime - counter) / 60 / 60;

            if (interval.get(leaderboardTimer, leaderboardTime)) {
                Groups.player.each(player -> Call.infoToast(player.con, getLeaderboard(player), 12f));
            }

            if (interval.get(updateTimer, updateTime)) {
                data.updateControl();
            }

            if (interval.get(winCheckTimer, winCheckTime)) {
                Seq<Player> players = data.getLeaderboard();
                if (players.size > 1 && data.getControlled(players.first()).size >= winCondition && data.getControlled(players.get(1)).size <= 1) {
                    endGame();
                }
            }

            counter += Time.delta;

            if (counter > roundTime) endGame();
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if (event.tile.block() instanceof CoreBlock coreBlock) {
                Hex hex = data.getHex(event.tile.pos());
                if (hex != null) {
                    Groups.unit.each(unit -> unit.team != hex.controller && hex.contains(unit), unit -> unit.damagePierce(coreBlock.health * unit.health / unit.maxHealth));
                    Call.effect(Fx.reactorExplosion, hex.wx, hex.wy, Mathf.random(360f), hex.controller == null ? Color.white : hex.controller.color);

                    hex.updateController();
                    hex.spawnTime.reset();
                }
            }
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if (event.breaking) return;

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

                Seq<Hex> copy = data.hexes().copy();
                copy.shuffle();
                Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());

                if (hex != null) {
                    loadout(event.player, hex.x, hex.y);
                    Core.app.post(() -> data.data(event.player).chosen = false);
                    hex.findController();
                } else {
                    Call.infoMessage(event.player.con, Bundle.format("events.no-empty-hex", findLocale(event.player)));
                    event.player.clearUnit();
                    event.player.team(Team.derelict);
                }
            }

            UserStatistics.find(event.player, userStatistic -> {
                userStatistic.name = event.player.coloredName();
                userStatistic.save();
            });
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
            if (leftPlayers.containsKey(player.uuid())) {
                return leftPlayers.get(player.uuid());
            }

            for (Team team : Seq.with(Team.all).shuffle()) {
                if (team.id > 5 && !team.active() && !Seq.with(players).contains(p -> p.team() == team) && !data.data(team).dying && !data.data(team).chosen && !leftPlayers.containsValue(team, true)) {
                    data.data(team).chosen = true;
                    return team;
                }
            }

            Call.infoMessage(player.con, Bundle.format("events.no-empty-hex", findLocale(player)));
            return Team.derelict;
        };

        netServer.chatFormatter = (player, message) -> {
            if (player != null) {
                int[] wins = {0};
                UserStatistics.find(player, userStatistic -> wins[0] = userStatistic.wins);

                return ("[coral][[[cyan]" + wins[0] + " [sky]#[white] " + player.coloredName() + "[coral]]: [white]" + message);
            }

            return message;
        };
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("lb", "Показать лучших игроков сервера.", (args, player) -> {
            StringBuilder players = new StringBuilder();
            int[] cycle = {1};

            UserStatistics.getSourceCollection().find().sort(new BasicDBObject("wins", new BsonInt32(-1))).limit(10).subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(10);
                }

                @Override
                public void onNext(Document document) {
                    if (document != null)
                        players.append("[accent]").append(cycle[0]++).append(". ").append(document.getString("name")).append("[accent]: [cyan]").append(document.getInteger("wins")).append("\n");
                    else players.append(Bundle.format("commands.lb.none", findLocale(player)));
                }

                @Override
                public void onError(Throwable t) {
                    err(t);
                }

                @Override
                public void onComplete() {
                    Call.infoMessage(player.con, Bundle.format("commands.lb.list", findLocale(player), players.toString()));
                }
            });
        });

        handler.<Player>register("spectator", "Перейти в режим наблюдателя.", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.spectator.already");
                return;
            }

            killTeam(player.team());
            player.clearUnit();
            player.team(Team.derelict);
            Call.hideHudText(player.con);
            bundled(player, "commands.spectator.success");
        });

        handler.<Player>register("captured", "Посмотреть количество захваченных хексов.", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.captured.spectator");
                return;
            }

            bundled(player, "commands.captured.hexes", data.getControlled(player).size);
        });

        handler.<Player>register("leaderboard", "Посмотреть текущий список лидеров.", (args, player) -> player.sendMessage(getLeaderboard(player)));

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

    public void endGame() {
        if (restarting) return;

        state.teams.active.each(team -> team.core().items().clear());
        restarting = true;
        Seq<Player> players = data.getLeaderboard();
        StringBuilder scores = new StringBuilder();

        int index = 1;
        for (Player player : players) {
            if (data.getControlled(player).size > 0) {
                scores.append("[yellow]").append(index).append(".[accent] ").append(player.coloredName()).append("[lightgray] (x").append(data.getControlled(player).size).append(")[]\n");
                index++;
            }
        }

        if (players.any()) {
            Player winner = players.first();
            boolean dominated = data.getControlled(winner).size == data.hexes().size;

            for (Player player : Groups.player) {
                StringBuilder endGameMessage = new StringBuilder(Bundle.format("round-over", findLocale(player)));

                if (player == winner) {
                    endGameMessage.append(Bundle.format("you-won", findLocale(player), data.getControlled(player).size));
                } else {
                    endGameMessage.append(Bundle.format("player-won", findLocale(player), winner.coloredName(), data.getControlled(winner).size));
                }

                if (!dominated)
                    endGameMessage.append(Bundle.format("final-score", findLocale(player), scores.toString()));

                Call.infoMessage(player.con, endGameMessage.toString());
            }

            UserStatistics.find(winner, userStatistic -> {
                userStatistic.name = winner.coloredName();
                userStatistic.wins++;
                userStatistic.save();
            });
        }

        Time.runTask(60f * 15f, this::reload);
    }

    public void updateText(Player player) {
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

    public void reload() {
        Events.fire("HexedGameOver");

        Seq<Player> players = new Seq<>();
        Groups.player.each(p -> {
            players.add(p);
            p.clearUnit();
        });

        logic.reset();

        Call.worldDataBegin();

        mode = Structs.random(HexedGenerator.Mode.values());
        data = new HexData();

        info("Пересоздание локации по сценарию @...", mode);

        HexedGenerator generator = new HexedGenerator();
        world.loadGenerator(Hex.size, Hex.size, generator);
        data.initHexes(generator.getHex());
        info("Локация сгенерирована.");
        state.rules = mode.applyRules(rules.copy());

        logic.play();

        for (Player player : players) {
            boolean admin = player.admin;
            player.reset();
            player.admin = admin;
            player.team(netServer.assignTeam(player, new SeqIterable<>(players)));

            if (player.team() != Team.derelict) {
                Seq<Hex> copy = data.hexes().copy();
                copy.shuffle();
                Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());
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

            netServer.sendWorldData(player);
        }

        for (int i = 0; i < interval.getTimes().length; i++) interval.reset(i, 0f);

        counter = 0f;
        restarting = false;
    }

    public String getLeaderboard(Player player) {
        Seq<Player> players = data.getLeaderboard();
        StringBuilder leaders = new StringBuilder(Bundle.format("leaderboard.header", findLocale(player), lastMin));
        for (int i = 0; i < 3 && i < players.size; i++) {
            Player p = players.get(i);
            leaders.append("[yellow]").append(i + 1).append(".[white] ").append(p.coloredName()).append(Bundle.format("leaderboard.hexes", findLocale(player), data.getControlled(p).size));
        }
        return leaders.toString();
    }

    public void killTeam(Team team) {
        if (team != Team.derelict) {
            data.data(team).dying = true;
            Time.runTask(8f, () -> data.data(team).dying = false);
            world.tiles.eachTile(tile -> {
                if (tile.build != null && tile.block() != Blocks.air && tile.team() == team) {
                    Time.run(Mathf.random(360f), tile::removeNet);
                }
            });

            Groups.unit.each(u -> u.team == team, unit -> Time.run(Mathf.random(360f), () -> Call.unitDespawn(unit)));
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
