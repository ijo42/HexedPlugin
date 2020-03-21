package hexed;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import hexed.HexData.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.NetServer.*;
import mindustry.entities.Damage;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.net.Administration;
import mindustry.net.Packets.*;
import mindustry.plugin.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;

import static arc.util.Log.info;
import static mindustry.Vars.*;

public class HexedMod extends Plugin{
    //in seconds
    public static final float spawnDelay = 60 * 4;

    //item requirement to captured a hex
    public static final int itemRequirement = 1310;

    public static final int messageTime = 1;
    //in ticks: 60 minutes
    private final static int roundTime = 60 * 60 * 180;
    //in ticks: 3 minutes
    private final static int leaderboardTime = 60 * 60 * 2;

    private final static int updateTime = 60 * 2;

    private final static int winCondition = 100;

    private final static int timerBoard = 0, timerUpdate = 1, timerWinCheck = 2;

    private final Rules rules = new Rules();
    private Interval interval = new Interval(5);

    private HexData data;
    private boolean restarting = false, registered = false;

    private Schematic start;
    private double counter = 0f;
    private int lastMin;

    public boolean started = false;

    @Override
    public void init(){
        rules.pvp = true;
        rules.tags.put("hexed", "true");
        rules.loadout = ItemStack.list(Items.copper, 1000, Items.lead, 800);
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 1f;
        rules.blockHealthMultiplier = 1.2f;
        rules.unitBuildSpeedMultiplier = 1f;
        rules.enemyCoreBuildRadius = ((Hex.diameter - 1) * tilesize / 2f);
        rules.unitDamageMultiplier = 1f;
        rules.playerHealthMultiplier = 1f;
        rules.playerDamageMultiplier = 0f;
        rules.canGameOver = false;

        //start = Schematics.readBase64("bXNjaAB4nE2SgY7CIAyGC2yDsXkXH2Tvcq+AkzMmc1tQz/j210JpXDL8hu3/lxYY4FtBs4ZbBLvG1ync4wGO87bvMU2vsCzTEtIlwvCxBW7e1r/43hKYkGY4nFN4XqbfMD+29IbhvmHOtIc1LjCmuIcrfm3X9QH2PofHIyYY5y3FaX3OS3ze4fiRwX7dLa5nDHTPddkCkT3l1DcA/OALihZNq4H6NHnV+HZCVshJXA9VYZC9kfVU+VQGKSsbjVT1lOgp1qO4rGIo9yvnquxH1ORIohap6HVIDbtpaNlDi4cWD80eFJdrNhbJc8W61Jzdqi/3wrRIRii7GYdelvWMZDQs1kNbqtYe9/KuGvDX5zD6d5SML66+5dwRqXgQee5GK3Edxw1ITfb3SJ71OomzUAdjuWsWqZyJavd8Issdb5BqVbaoGCVzJqrddaUGTWSFHPs67m6H5HlaTqbqpFc91Kfn+2eQSp9pr96/Xtx6cevZjeKKDuUOklvvXy9uPGdNZFjZi7IXZS/n8Hyf/wFbjj/q");

        start = Schematics.readBase64("bXNjaAB4nE2VYXLiMAyFlZgkYCchSbfH4FBZ6tllBpJOgO3sr56i5+gJ9mysJCuvhWH4GqynJ1l26ZmeM9pM4yVSdb2Nyy0uDe1/LqeXX/FwnKc/8e+80HD7PS+n++XwNp7Ph/O4/IrkxuVI5SVOL3Eh/zq/xeUwzS+R+uvMKw6v4xTXteHbI+pvp9s4iRr0q+txvHFqqpf4Op545XyabrS9T+d5FP1uXC7zEl++Quoj/32Y7sdzvF+J6B9/aEeOP/LyTF4pMAXK9M0vJ98OVIAqpZxJFTIhDwpKsq4GtaAONKiKo9xUNkxJRZ6tKi55yQva8DvlcFSaA5e85CXTVom0rvj4kAWst1JQknXqKpfYBqT+nOTokHcw5Y0pV0ypY0IepE5J1iVl6VRmsQViC8QWiC0QW3yLHcxVmXrPCmWqN9vx96pXQq+EXgm9MulpbGNdK6m3HJX5y5k0R+6Ztlp4YFqVK8xFZcrS79p6X33TW3u1NT2hDagAlaaytRmqOUPK4flZo0Z3TC0i9iDdGSfU206LT57jTCYoVSR7n+NXR+8UM6knTY5QAUo9DUwVSHvl5NcWtAclB6LcPz4en06yaeU6w0+UXjv6ISscaWVe/XnzJ3Oz+vPw5+HPw583f0IVaPXn4c/Dn4c/b/4k2+rPw59nf179Bd1b8RfQvwB/Af4C/AX4C+hfQP8C/AX4C/AX4C+gfwH+AvwF9scrnNwf6kqneZ2rGnNV21ztmNQLT3GtXrJcTnlj81InL6qyR2wHWqe4hpfGsslEpirlrtCT56RGPR98Bps0u7k8C+ag0fORaex68hrkaK2inMmZqzYps/MWyq0pCwWrssXN1Sa9TPSS54LrSXtUco3rHbGH3h56e9OTdekkS+x663XoeAeVDiodVDqodLi5OvSvx/3c437ucT/3tHt80ruSBwUlWVcjdr2fe5sm+Z+RgZx5HmwmhSrbrSF55hMwJM9OngXE1qAv5Q40mIMnUxHyoKRC6ZTTLqd0nsgrBakl/w/ZhnCF");
        Events.on(Trigger.update, () -> {
            if(active()){
                data.updateStats();

                for(Player player : playerGroup.all()){
                    if(player.getTeam() != Team.derelict && player.getTeam().cores().isEmpty()){
                        player.kill();
                        killTiles(player.getTeam());
                        Call.sendMessage("[yellow](!)[] [accent]" + player.name + "[lightgray] has been eliminated![yellow] (!)");
                        Call.onInfoMessage(player.con, "Your cores have been destroyed. You are defeated.");
                        player.setTeam(Team.derelict);
                    }

                    if(player.getTeam() == Team.derelict){
                        player.dead = true;
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
                    Call.onInfoToast(getLeaderboard(), 15f);
                }

                if(interval.get(timerUpdate, updateTime)){
                    data.updateControl();
                }

                if(interval.get(timerWinCheck, 60 * 2)){
                    Array<Player> players = data.getLeaderboard();
                    if(!players.isEmpty() && data.getControlled(players.first()).size >= winCondition && players.size > 1 && data.getControlled(players.get(1)).size <= 1){
                        endGame();
                    }
                }

                counter += Time.delta();

                //kick everyone and restart w/ the script
                if(counter > roundTime && !restarting){
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
            if(active() && event.player.getTeam() != Team.derelict){
                killTiles(event.player.getTeam());
            }
        });

        Events.on(PlayerJoin.class, event -> {
            if(!active() || event.player.getTeam() == Team.derelict) return;
            if(started) {
                event.player.kill();
                event.player.setTeam(Team.derelict);
                event.player.sendMessage("The game has already started. You can't join mid game!\nAssigning into spectator mode.");
                return;
            }

            Array<Hex> copy = data.hexes().copy();
            copy.shuffle();
            Hex hex = copy.find(h -> h.controller == null && h.spawnTime.get());

            if(hex != null){
                loadout(event.player, hex.x, hex.y);
                Core.app.post(() -> data.data(event.player).chosen = false);
                hex.findController();
            }else{
                Call.onInfoMessage(event.player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
                event.player.kill();
                event.player.setTeam(Team.derelict);
            }

            data.data(event.player).lastMessage.reset();
        });

        Events.on(ProgressIncreaseEvent.class, event -> updateText(event.player));

        Events.on(HexCaptureEvent.class, event -> {
            updateText(event.player);
            Tile t = world.tile(event.hex.x, event.hex.y);
            if(t != null) {
                for(int x = 0; x < world.width(); x++){
                    for(int y = 0; y < world.height(); y++){
                        Tile tile = world.tile(x, y);
                        if(t.dst(tile.worldx(), tile.worldy()) < 35f){
                            if(tile.entity != null){
                                tile.entity.kill();
                            }
                        }
                    }
                }
                t.setNet(Blocks.coreShard, event.player.getTeam(), 0);
                Call.onLabel("[accent]hex captured by[] " + event.player.name, 30f, t.worldx(), t.worldy());
                Call.onEffectReliable(Fx.bigShockwave, t.worldx(), t.worldy(), 0, Pal.accent);
            }
        });

        Events.on(HexMoveEvent.class, event -> updateText(event.player));

        TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            Array<Player> arr = Array.with(players);

            if(active()){
                //pick first inactive team
                for(Team team : Team.all()){
                    if(team.id > 5 && !team.active() && !arr.contains(p -> p.getTeam() == team) && !data.data(team).dying && !data.data(team).chosen){
                        data.data(team).chosen = true;
                        return team;
                    }
                }
                Call.onInfoMessage(player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
                return Team.derelict;
            }else{
                return prev.assign(player, players);
            }
        };
    }

    void updateText(Player player){
        HexTeam team = data.data(player);

        StringBuilder message = new StringBuilder("[white]Hex #" + team.location.id + "\n");

        if(!team.lastMessage.get()) return;

        if(team.location.controller == null){
            if(team.progressPercent > 0){
                message.append("[lightgray]Capture progress: [accent]").append((int)(team.progressPercent)).append("%");
            }else{
                message.append("[lightgray][[Empty]");
            }
        }else if(team.location.controller == player.getTeam()){
            message.append("[yellow][[Captured]");
        }else if(team.location != null && team.location.controller != null && data.getPlayer(team.location.controller) != null){
            message.append("[#").append(team.location.controller.color).append("]Captured by ").append(data.getPlayer(team.location.controller).name);
        }else{
            message.append("<Unknown>");
        }

        Call.setHudText(player.con, message.toString());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("hexed", "Begin hosting with the Hexed gamemode.", args -> {
            if(!state.is(State.menu)){
                Log.err("Stop the server first.");
                return;
            }

            data = new HexData();

            logic.reset();
            Log.info("Generating map...");
            HexedGenerator generator = new HexedGenerator();
            world.loadGenerator(generator);
            data.initHexes(generator.getHex());
            info("Map generated.");
            state.rules = rules.copy();
            logic.play();
            netServer.openServer();
        });

        handler.register("countdown", "Get the hexed restart countdown.", args -> {
            Log.info("Time until round ends: &lc{0} minutes", (int)(roundTime - counter) / 60 / 60);
        });

        handler.register("end", "End the game.", args -> endGame());

        handler.register("r", "Restart the server.", args -> System.exit(2));

        handler.register("start", "Start the hexed game.", args -> {
            rules.playerDamageMultiplier = 0.75f;
            Call.onSetRules(rules);
            started = true;
            Log.info("GAME STARTED !");
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        if(registered) return;
        registered = true;

        handler.<Player>register("spectate", "Enter spectator mode. This destroys your base.", (args, player) -> {
             if(player.getTeam() == Team.derelict){
                 player.sendMessage("[scarlet]You're already spectating.");
             }else{
                 killTiles(player.getTeam());
                 player.kill();
                 player.setTeam(Team.derelict);
             }
        });

        handler.<Player>register("captured", "Dispay the number of hexes you have captured.", (args, player) -> {
            if(player.getTeam() == Team.derelict){
                player.sendMessage("[scarlet]You're spectating.");
            }else{
                player.sendMessage("[lightgray]You've captured[accent] " + data.getControlled(player).size + "[] hexes.");
            }
        });

        handler.<Player>register("leaderboard", "Display the leaderboard", (args, player) -> {
            player.sendMessage(getLeaderboard());
        });

        handler.<Player>register("hexstatus", "Get hex status at your position.", (args, player) -> {
            Hex hex = data.data(player).location;
            if(hex != null){
                hex.updateController();
                StringBuilder builder = new StringBuilder();
                builder.append("| [lightgray]Hex #").append(hex.id).append("[]\n");
                builder.append("| [lightgray]Owner:[] ").append(hex.controller != null && data.getPlayer(hex.controller) != null ? data.getPlayer(hex.controller).name : "<none>").append("\n");
                for(TeamData data : state.teams.getActive()){
                    if(hex.getProgressPercent(data.team) > 0){
                        builder.append("|> [accent]").append(this.data.getPlayer(data.team).name).append("[lightgray]: ").append((int)hex.getProgressPercent(data.team)).append("% captured\n");
                    }
                }
                player.sendMessage(builder.toString());
            }else{
                player.sendMessage("[scarlet]No hex found.");
            }
        });


    }

    void endGame(){
        if(restarting) return;

        restarting = true;
        Array<Player> players = data.getLeaderboard();
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < players.size && i < 3; i++){
            if(data.getControlled(players.get(i)).size > 1){
                builder.append("[yellow]").append(i + 1).append(".[accent] ").append(players.get(i).name)
                .append("[lightgray] (x").append(data.getControlled(players.get(i)).size).append(")[]\n");
            }
        }

        if(!players.isEmpty()){
            boolean dominated = data.getControlled(players.first()).size == data.hexes().size;

            for(Player player : playerGroup.all()){
                Call.onInfoMessage(player.con, "[accent]--ROUND OVER--\n\n[lightgray]"
                + (player == players.first() ? "[accent]You[] were" : "[yellow]" + players.first().name + "[lightgray] was") +
                " victorious, with [accent]" + data.getControlled(players.first()).size + "[lightgray] hexes conquered." + (dominated ? "" : "\n\nFinal scores:\n" + builder));
            }
        }

        Log.info("&ly--SERVER RESTARTING--");
        Time.runTask(60f * 180f, () -> {
            netServer.kickAll(KickReason.serverRestarting);
            Time.runTask(5f, () -> System.exit(2));
        });
    }

    String getLeaderboard(){
        StringBuilder builder = new StringBuilder();
        builder.append("[accent]Leaderboard\n[scarlet]").append(lastMin).append("[lightgray] mins. remaining\n\n");
        int count = 0;
        for(Player player : data.getLeaderboard()){
            builder.append("[yellow]").append(++count).append(".[white] ")
            .append(player.name).append("[orange] (").append(data.getControlled(player).size).append(" hexes)\n[white]");

            if(count > 4) break;
        }
        return builder.toString();
    }

    void killTiles(Team team){
        data.data(team).dying = true;
        Time.runTask(4f, () -> data.data(team).dying = false);
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);
                if(tile.entity != null && tile.getTeam() == team){
                    Time.run(Mathf.random(60f * 6), tile.entity::kill);
                }
            }
        }
    }

    void loadout(Player player, int x, int y){
        Stile coreTile = start.tiles.find(s -> s.block instanceof CoreBlock);
        if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        start.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox, st.y + oy);
            if(tile == null) return;

            if(tile.link().block() != Blocks.air){
                tile.link().removeNet();
            }

            tile.setNet(st.block, player.getTeam(), st.rotation);

            if(st.block.posConfig){
                tile.configureAny(Pos.get(tile.x - st.x + Pos.x(st.config), tile.y - st.y + Pos.y(st.config)));
            }else{
                tile.configureAny(st.config);
            }
            if(tile.block() instanceof CoreBlock){
                for(ItemStack stack : state.rules.loadout){
                    Call.transferItemTo(stack.item, stack.amount, tile.drawx(), tile.drawy(), tile);
                }
            }
        });
    }

    public boolean active(){
        return state.rules.tags.getBool("hexed") && !state.is(State.menu);
    }


}
