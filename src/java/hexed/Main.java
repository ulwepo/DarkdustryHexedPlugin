package hexed;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Timer;
import arc.util.Time;
import arc.util.CommandHandler;
import arc.util.Structs;
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
import hexed.comp.ConfigurationManager;
import hexed.comp.NoPauseRules;
import hexed.models.UserStatistics;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.core.GameState.State;
import mindustry.core.NetServer.ChatFormatter;
import mindustry.core.NetServer.TeamAssigner;
import mindustry.game.*;
import mindustry.game.EventType.BlockDestroyEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.game.EventType.Trigger;
import mindustry.game.Schematic.Stile;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.io.JsonIO;
import mindustry.mod.Plugin;
import mindustry.type.ItemStack;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import org.bson.BsonInt32;
import org.bson.Document;
import org.json.JSONObject;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

import static arc.util.Log.err;
import static arc.util.Log.info;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static final float spawnDelay = 60 * 4;
    public static final int itemRequirement = 3000;
    public static final int messageTime = 1;
    private static final int roundTime = 60 * 60 * 90;
    private static final int leaderboardTime = 60 * 60 * 2;
    private static final int updateTime = 60 * 2;
    private static final int winCondition = 20;
    private static final int timerBoard = 0, timerUpdate = 1, timerWinCheck = 2;

    protected static HexedGenerator.Mode mode;

    private Rules rules = new Rules();
    private NoPauseRules hexRules;
    private final Interval interval = new Interval(5);

    private HexData data;
    private boolean restarting = false;

    private Schematic start;
    private double counter = 0f;
    private int lastMin;

    private final ObjectMap<String, Team> teamTimers = new ObjectMap<>();

    public Main() throws IOException {
        ConfigurationManager config = new ConfigurationManager();
        JSONObject jsonData = config.getJsonData();

        ConnectionString connString = new ConnectionString(jsonData.getString("mongoURI"));

        MongoClientSettings settings = MongoClientSettings
                .builder()
                .applyConnectionString(connString)
                .retryWrites(true)
                .build();

        MongoClient mongodb = MongoClients.create(settings);
        MongoCollection<Document> hexedCollection = mongodb.getDatabase(jsonData.getString("dbName")).getCollection(jsonData.getString("dbCollection"));
        UserStatistics.setSourceCollection(hexedCollection);
    }

    @Override
    public void init() {
        rules.tags.put("hexed", "true");
        rules.loadout = ItemStack.list(Items.copper, 350, Items.lead, 250, Items.graphite, 150, Items.metaglass, 100, Items.silicon, 250, Items.titanium, 30);
        rules.buildCostMultiplier = 0.8f;
        rules.buildSpeedMultiplier = 2f;
        rules.blockHealthMultiplier = 1.5f;
        rules.unitBuildSpeedMultiplier = 1.25f;
        rules.enemyCoreBuildRadius = Hex.diameter * tilesize / 2f;
        rules.unitDamageMultiplier = 1.25f;
        rules.fire = false;
        rules.reactorExplosions = true;
        rules.canGameOver = false;
        rules.coreCapture = false;
        rules.revealedBlocks.addAll(
            Blocks.duct,
            Blocks.ductRouter,
            Blocks.ductBridge,
            Blocks.thruster,
            Blocks.scrapWall,
            Blocks.scrapWallLarge,
            Blocks.scrapWallHuge,
            Blocks.scrapWallGigantic
        );
        rules.bannedBlocks.addAll(
            Blocks.ripple,
            Blocks.microProcessor,
            Blocks.logicProcessor,
            Blocks.hyperProcessor
        );

        start = Schematics.readBase64("bXNjaAF4nE2SX3LbIBDGFyQh/sh2fINcQCfK5IHItPWMIjSS3DRvuUqu0Jnew71OX5JdPs80wuYDdvmxu0CBjhXVU3xOFH6kX+l0v25x2Sic0jos53k754mIzBif0rjS/uH6fv3z9+36W/rHHYUhz3Na+pc4jnT8MunHuHxPZIc8/UyveaF2HeK2pYXCmtnWz3FKI1VxGah9KpZXOn4x3QDmOU0n3mUv05ijjLohL6mfLsOYLiv5Ob/wkVM+cQbxvPTf4rBlZhEl/pMqP9Lc+KshDcSQFm2pTC3EUfk8JEA6UHaYHcRRYaxkUHFXY7EwFZgKTAWmEmbNEiAdFm+wO9Lqf3DcGMTcEnphajA1mBpMLcyW/TrSsm8vKC1My4vsVpE07bhrGjZqz3wryVbsrCXsUogSvWVpMNvLvEZwtQRnEJc4VBDeElgaK5UwZRxk/PGvmDt47bC1BNaAZ1A5I5UzkhzplpOoJUxDQcLk3S3t1K2+LZXracXTsYiLK+sHSdvidi3qVPxELMTBVmpvcZ+3K3Z4HA55OQlApDwOB5gDzAHmAHOAOVykw0U6SVHkAJc7EY9X4lFeD7QH2gPtgfZAe7w7jzg90B7vzuMELyd8Ao5MVAI=");

        Events.run(Trigger.update, () -> {
            if (active()) {
                data.updateStats();

                for (Player player : Groups.player) {
                    if (player.team() != Team.derelict && player.team().cores().isEmpty()) {
                        player.clearUnit();
                        killTeam(player.team());
                        Groups.player.each(p -> bundled(p, "server.player-lost", player.coloredName()));
                        Call.infoMessage(player.con, Bundle.format("server.you-lost", findLocale(player)));
                        player.team(Team.derelict);
                    }

                    if (player.team() == Team.derelict) {
                        player.clearUnit();
                    } else if (data.getControlled(player).size == data.hexes().size) {
                        endGame();
                        break;
                    }
                }

                state.serverPaused = false;
                rules = state.rules;
                if (rules.pvp && rules instanceof NoPauseRules) {
                    rules.pvp = false;
                }

                int minsToGo = (int) (roundTime - counter) / 60 / 60;
                if (minsToGo != lastMin) {
                    lastMin = minsToGo;
                }

                if (interval.get(timerBoard, leaderboardTime)) {
                    Groups.player.each(player -> Call.infoToast(player.con, getLeaderboard(player), 12.5f));
                }

                if (interval.get(timerUpdate, updateTime)) {
                    data.updateControl();
                }

                if (interval.get(timerWinCheck, 60 * 2)) {
                    Seq<Player> players = data.getLeaderboard();
                    if (!players.isEmpty() && data.getControlled(players.first()).size >= winCondition && players.size > 1 && data.getControlled(players.get(1)).size <= 1) {
                        endGame();
                    }
                }

                counter += Time.delta;

                if (counter > roundTime) endGame();
            } else counter = 0;
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if (active() && event.tile.block() instanceof CoreBlock) {
                Hex hex = data.getHex(event.tile.pos());
                 if (hex != null) {
                     hex.spawnTime.reset();
                     hex.updateController();
                 }
            }
        });

        Events.on(PlayerLeave.class, event -> {
            if (active() && event.player.team() != Team.derelict) {
                teamTimers.put(event.player.uuid(), event.player.team());
                Timer.schedule(() -> {
                    int count = Groups.player.count(p -> p.team() == event.player.team());
                    if (count > 0) return;
                    killTeam(event.player.team());
                    teamTimers.remove(event.player.uuid());
                }, 60f);
            }
        });

        Events.on(PlayerJoin.class, event -> {
            if (active() && event.player.team() != Team.derelict) {
                if (teamTimers.containsKey(event.player.uuid())) {
                    teamTimers.remove(event.player.uuid());
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
                    Call.infoMessage(event.player.con, Bundle.format("server.no-empty-hex", findLocale(event.player)));
                    event.player.clearUnit();
                    event.player.team(Team.derelict);
                }
                data.data(event.player).lastMessage.reset();
            }
        });

        Events.on(EventType.WorldLoadEvent.class, event -> Time.runTask(5f, () -> {
            rules = state.rules;
            if (rules.pvp && !(rules instanceof NoPauseRules)) {
                rules.pvp = false;
                hexRules = new NoPauseRules();
                JsonIO.copy(rules, hexRules);
                state.rules = hexRules;
            } else if (rules.pvp) rules.pvp = false;
        }));

        Events.on(ProgressIncreaseEvent.class, event -> updateText(event.player));

        Events.on(HexCaptureEvent.class, event -> updateText(event.player));

        Events.on(HexMoveEvent.class, event -> updateText(event.player));

        TeamAssigner prevAssigner = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Seq<Player> arr = Seq.with(players);
            if (active()) {
                if (teamTimers.containsKey(player.uuid())) return teamTimers.get(player.uuid());
                for (Team team : Team.all) {
                    if (team.id > 5 && !team.active() && !arr.contains(p -> p.team() == team) && !data.data(team).dying && !data.data(team).chosen && !teamTimers.containsValue(team, true)) {
                        data.data(team).chosen = true;
                        return team;
                    }
                }
                Call.infoMessage(player.con, Bundle.format("server.no-empty-hex", findLocale(player)));
                return Team.derelict;
            }

            return prevAssigner.assign(player, players);
        };

        ChatFormatter prevFormat = netServer.chatFormatter;
        netServer.chatFormatter = (player, message) -> {
            if (player == null) return message;
            if (active()) {
                int[] wins = {0};

                UserStatistics.find(new BasicDBObject("UUID", player.uuid()), userStatistic -> {
                    userStatistic.name = player.name;
                    userStatistic.save();
                    wins[0] = userStatistic.wins;
                });

                return ("[coral][[[cyan]" + wins[0] + " [sky]#[white] " + player.coloredName() + "[coral]]:[white] " + message);
            }

            return prevFormat.format(player, message);
        };
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("lb", "Показать текущих лучших игроков сервера.", (args, player) -> {
            StringBuilder players = new StringBuilder();
            int[] cycle = {1};

            UserStatistics.getSourceCollection().find().sort(new BasicDBObject("wins", new BsonInt32(-1))).limit(10).subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.request(10);
                }

                @Override
                public void onNext(Document t) {
                    if (!Objects.isNull(t)) players.append("[accent]").append(cycle[0]++).append(". ").append(t.getString("name")).append("[accent]: [cyan]").append(t.getInteger("wins")).append("\n");
                    else players.append(Bundle.format("commands.lb.none", findLocale(player)));
                }

                @Override
                public void onError(Throwable t) {
                    if (!Objects.isNull(t)) err(t);
                }

                @Override
                public void onComplete() {
                    Call.infoMessage(player.con, Bundle.format("commands.lb.list", findLocale(player), players.toString()));
                }
            });
        });

        handler.<Player>register("spectator", "Режим наблюдателя. Уничтожает твою базу", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.spectator.already");
                return;
            }
            killTeam(player.team());
            player.clearUnit();
            player.team(Team.derelict);
            bundled(player, "commands.spectator.success");
        });

        handler.<Player>register("captured", "Узнать количество захваченных хексов.", (args, player) -> {
            if (player.team() == Team.derelict) {
                bundled(player, "commands.captured.spectator");
                return;
            }
            bundled(player, "commands.captured.hexes", data.getControlled(player).size);
        });

        handler.<Player>register("leaderboard", "Показать таблицу лидеров.", (args, player) -> player.sendMessage(getLeaderboard(player)));

        handler.<Player>register("hexstatus", "Узнать статус хекса на своем местоположении.", (args, player) -> {
            Hex hex = data.data(player).location;
            if (hex != null) {
                hex.updateController();
                StringBuilder status = new StringBuilder();
                status.append(Bundle.format("commands.hexstatus.hex", findLocale(player))).append(hex.id).append("[]\n");
                status.append(Bundle.format("commands.hexstatus.owner", findLocale(player))).append(hex.controller != null && data.getPlayer(hex.controller) != null ? data.getPlayer(hex.controller).coloredName() : Bundle.format("commands.hexstatus.owner.none", findLocale(player))).append("\n");
                for (Teams.TeamData data : state.teams.getActive()) {
                    if (hex.getProgressPercent(data.team) > 0 && hex.getProgressPercent(data.team) <= 100) {
                        status.append("[white]|> [accent]").append(this.data.getPlayer(data.team).coloredName()).append("[lightgray]: [accent]").append(Bundle.format("commands.hexstatus.captured", findLocale(player), (int) hex.getProgressPercent(data.team))).append("\n");
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

        handler.register("hexed", "[mode/list]", "Запустить сервер в режиме HexPvp.", args -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                info("Доступные режимы:");
                for (HexedGenerator.Mode value : HexedGenerator.Mode.values()) {
                    info("- @", value);
                }
                return;
            }

            if (!state.is(State.menu)) {
                err("Сначала необходимо остановить сервер.");
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
            info("Генерирую локацию по сценарию @...", mode);
            HexedGenerator generator = new HexedGenerator();
            world.loadGenerator(Hex.size, Hex.size, generator);
            data.initHexes(generator.getHex());
            info("Локация сгенерирована.");
            state.rules = rules.copy();
            logic.play();
            netServer.openServer();
        });

        handler.register("time", "Узнать время до конца раунда.", args -> info("Время до конца раунда: &lc@ минут", (int) (roundTime - counter) / 60 / 60));

        handler.register("end", "Принудительно закончить раунд.", args -> endGame());
    }

    public void endGame() {
        if (restarting) return;

        state.teams.active.each(team -> team.core().items().clear());
        restarting = true;
        Seq<Player> players = data.getLeaderboard();
        StringBuilder scores = new StringBuilder();
        for (int i = 0; i < players.size && i < 4; i++) {
            if (data.getControlled(players.get(i)).size > 1) {
                scores.append("[yellow]").append(i + 1).append(".[accent] ").append(players.get(i).name).append("[lightgray] (x").append(data.getControlled(players.get(i)).size).append(")[]\n");
            }
        }

        if (!players.isEmpty()) {
            boolean dominated = data.getControlled(players.first()).size == data.hexes().size;

            for (Player player : Groups.player) {
                StringBuilder endGameMessage = new StringBuilder(Bundle.format("round-over", findLocale(player)));

                if (player == players.first()) endGameMessage.append(Bundle.format("you-won", findLocale(player), data.getControlled(players.first()).size));
                else endGameMessage.append(Bundle.format("player-won", findLocale(player), players.first().coloredName(), data.getControlled(players.first()).size));

                if (!dominated) endGameMessage.append(Bundle.format("final-score", findLocale(player), scores.toString()));

                Call.infoMessage(player.con, endGameMessage.toString());
            }

            UserStatistics.find(new BasicDBObject("UUID", players.first().uuid()), userStatistic -> {
                userStatistic.name = players.first().name;
                userStatistic.wins++;
                userStatistic.save();
            });
        }

        Time.runTask(60f * 15f, this::reload);
    }

    public void updateText(Player player) {
        HexTeam team = data.data(player);

        StringBuilder message = new StringBuilder(Bundle.format("hex", findLocale(player)) + team.location.id + "\n");

        if (!team.lastMessage.get()) return;

        if (team.location.controller == null) {
            if (team.progressPercent > 0) {
                message.append(Bundle.format("capture-progress", findLocale(player))).append((int) (team.progressPercent)).append("%");
            } else {
                message.append(Bundle.format("hex-empty", findLocale(player)));
            }
        } else if (team.location.controller == player.team()) {
            message.append(Bundle.format("hex-captured", findLocale(player)));
        } else if (team.location != null && team.location.controller != null && data.getPlayer(team.location.controller) != null) {
            message.append("[#").append(team.location.controller.color).append("]").append(Bundle.format("hex-captured-by-player", findLocale(player))).append(data.getPlayer(team.location.controller).coloredName());
        } else {
            message.append(Bundle.format("hex-unknown", findLocale(player)));
        }

        Call.setHudText(player.con, message.toString());
    }

    public void reload() {
        Seq<Player> players = new Seq<>();
        Groups.player.each(p -> {
            players.add(p);
            p.clearUnit();
        });

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

        for (Player p : players) {
            if (p.con == null) continue;

            boolean admin = p.admin;
            p.reset();
            p.admin = admin;
            p.team(netServer.assignTeam(p, new Seq.SeqIterable<>(players)));

            if (p.team() != Team.derelict) {
                Seq<Hex> copy = data.hexes().copy();
                copy.shuffle();
                Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());
                if (hex != null) {
                    loadout(p, hex.x, hex.y);
                    Core.app.post(() -> data.data(p).chosen = false);
                    hex.findController();
                } else {
                    Call.infoMessage(p.con, Bundle.format("server.no-empty-hex", findLocale(p)));
                    p.clearUnit();
                    p.team(Team.derelict);
                }
                data.data(p).lastMessage.reset();
            }

            netServer.sendWorldData(p);
        }

        for (int i = 0; i < 5; i++) interval.reset(i, 0f);

        counter = 0f;
        restarting = false;
    }

    public String getLeaderboard(Player p) {
        StringBuilder builder = new StringBuilder(Bundle.format("leaderboard.header", findLocale(p), lastMin));
        int count = 0;
        for (Player player : data.getLeaderboard()) {
            builder.append("[yellow]").append(count++).append(".[white] ").append(player.coloredName()).append(Bundle.format("leaderboard.hexes", findLocale(p), data.getControlled(player).size));
            if (count > 4) break;
        }
        return builder.toString();
    }

    public void killTeam(Team team) {
        data.data(team).dying = true;
        Time.runTask(8f, () -> data.data(team).dying = false);
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                Tile tile = world.tile(x, y);
                if (tile.build != null && tile.block() != Blocks.air && tile.team() == team) {
                    Time.run(Mathf.random(60f * 6), tile::removeNet);
                }
            }
        }
        Groups.unit.each(u -> u.team == team, unit -> Time.run(Mathf.random(360), unit::kill));
    }

    public void loadout(Player player, int x, int y) {
        Stile coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        if (coreTile == null) throw new IllegalArgumentException("Загружена схема без ядра. Выключаю сервер...");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        start.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox, st.y + oy);
            if (tile == null) return;

            if (tile.block() != Blocks.air) tile.removeNet();
            if (tile.floor().isLiquid) tile.setFloorNet(Blocks.darkPanel3);

            tile.setNet(st.block, player.team(), st.rotation);

            if (st.config != null) tile.build.configureAny(st.config);

            if (tile.block() instanceof CoreBlock) {
                for (ItemStack stack : state.rules.loadout) {
                    Call.setItem(tile.build, stack.item, stack.amount);
                }
            }
        });
    }

    public boolean active() {
        return (state.rules.tags.getBool("hexed") && !state.is(State.menu));
    }

    public static void bundled(Player player, String key, Object... values) {
        player.sendMessage(Bundle.format(key, findLocale(player), values));
    }

    private static Locale findLocale(Player player) {
        Locale locale = Structs.find(Bundle.supportedLocales, l -> l.toString().equals(player.locale) || player.locale.startsWith(l.toString()));
        return (locale != null) ? locale : Bundle.defaultLocale();
    }
}
