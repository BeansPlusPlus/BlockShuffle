package beansplusplus.blockshuffle;

import beansplusplus.beansgameplugin.Game;
import beansplusplus.beansgameplugin.GameConfiguration;
import beansplusplus.beansgameplugin.GameState;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockShuffleGame implements Listener, Game {
  private int blocksToWin;

  private boolean hunger;
  private boolean shareBlocks;

  private int ticksPerBlock;

  private int currentTask;
  private int timer;

  private int gracePeriodSecs;

  private boolean keepInventory;

  private boolean pvp;

  private Map<String, Material> blocks = new HashMap<>();

  private Map<String, Integer> scores = new HashMap<>();

  private Plugin plugin;
  private BlockShuffler shuffler;
  private GameState state;

  public BlockShuffleGame(BlockShufflePlugin plugin, GameConfiguration config, GameState state) {
    this.plugin = plugin;
    this.state = state;

    blocksToWin = config.getValue("blocks_to_win");
    hunger = config.getValue("hunger");
    shareBlocks = config.getValue("share_blocks");
    ticksPerBlock = (int) ((double) config.getValue("minutes_per_block") * 60.0 * 20.0);
    gracePeriodSecs = config.getValue("grace_period_seconds");
    keepInventory = config.getValue("keep_inventory");
    pvp = config.getValue("pvp");
  }

  public void start() {
    Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

    World world = Bukkit.getWorld("world");
    world.setTime(1000);

    for (Player player : Bukkit.getOnlinePlayers()) {
      scores.put(player.getName(), 0);

      player.setHealth(20);
      player.setLevel(0);
      player.setFoodLevel(20);
      player.getInventory().clear();
      player.setGameMode(GameMode.SURVIVAL);
    }

    for (World w : Bukkit.getWorlds()) {
      w.setGameRule(GameRule.KEEP_INVENTORY, keepInventory);
      w.setPVP(pvp);
    }

    setupShuffler();

    if (gracePeriodSecs == 0) {
      pickNextBlock();
    } else {
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.sendMessage(ChatColor.YELLOW + "Beginning grace period of " + ChatColor.RED + gracePeriodSecs + " seconds");
      }
      Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::pickNextBlock, gracePeriodSecs * 20);
    }
  }

  @Override
  public void cleanUp() {
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().cancelTasks(plugin);
  }

  private void setupShuffler() {
    shuffler = new BlockShuffler();
    World world = Bukkit.getWorld("world");
    int border = (int) world.getWorldBorder().getMaxSize();
    if (border > 2500) border = 2500; // anything outside this range tasks ages to get to anyway

    shuffler.calculateFrequencies(Bukkit.getWorld("world"), Bukkit.getWorld("world_nether"), border);
  }

  private void runTimer() {
    if (state.isPaused()) return;

    if (timer > 0) {
      timer--;

      if (timer == 200) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.YELLOW + "10 seconds remaining...");
        }
      }

      if (timer == 1200) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.YELLOW + "1 minute remaining...");
        }
      }

      if (timer == 0) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.YELLOW + "No one found their block this time!");
        }

        Bukkit.getScheduler().cancelTask(currentTask);
        nextRound();
        return;
      }
    }
    List<String> winningPlayers = playersTouchingBlock();
    if (!winningPlayers.isEmpty()) winRound(winningPlayers);
  }

  private void nextRound() {
    if (checkForWinner()) {
      state.stopGame();
      return;
    }

    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::pickNextBlock, 100);
  }

  private void pickNextBlock() {
    Material block = shuffler.nextBlockType();

    for (String name : scores.keySet()) {
      Player player = Bukkit.getPlayer(name);

      if (player != null) player.sendMessage(ChatColor.BLUE + "Next block: " + ChatColor.GREEN + blockName(block));

      blocks.put(name, block);

      if (!shareBlocks) block = shuffler.nextBlockType();
    }

    refreshScoreboard();

    timer = ticksPerBlock;
    currentTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::runTimer, 0, 1);
  }

  private boolean checkForWinner() {
    for (String name : scores.keySet()) {
      if (scores.get(name) >= blocksToWin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.GREEN + name + ChatColor.BLUE + " has won the game!");
        }

        return true;
      }
    }

    return false;
  }

  private void winRound(List<String> winningPlayers) {
    for (String p : winningPlayers) {
      scores.put(p, scores.get(p) + 1);
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.sendMessage(ChatColor.GREEN + p + ChatColor.YELLOW + " found the block and got a point!");
      }
    }
    refreshScoreboard();
    Bukkit.getScheduler().cancelTask(currentTask);
    nextRound();
  }

  private void refreshScoreboard() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      p.setScoreboard(getScoreboard(p));
    }
  }

  public Scoreboard getScoreboard(Player player) {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    Objective obj = scoreboard.registerNewObjective("scoreboard", "scoreboard",
        ChatColor.GOLD + "Block Shuffle - Blocks to win: " + ChatColor.RED + blocksToWin);
    obj.setDisplaySlot(DisplaySlot.SIDEBAR);

    for (String name : scores.keySet()) {
      obj.getScore(name).setScore(scores.get(name));
    }

    if (blocks.containsKey(player.getName())) {
      obj.getScore(ChatColor.BLUE + "Block: " + ChatColor.GREEN + blockName(blocks.get(player.getName()))).setScore(-1);
    }

    return scoreboard;
  }

  private List<String> playersTouchingBlock() {
    List<String> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      List<Material> materials = List.of(
          player.getLocation().add(0, 0, 0).getBlock().getType(),
          player.getLocation().add(0, -1, 0).getBlock().getType(),
          player.getLocation().add(0, 1, 0).getBlock().getType(),
          player.getLocation().add(0, 2, 0).getBlock().getType(),
          player.getLocation().add(1, 0, 0).getBlock().getType(),
          player.getLocation().add(-1, 0, 0).getBlock().getType(),
          player.getLocation().add(0, 0, 1).getBlock().getType(),
          player.getLocation().add(0, 0, -1).getBlock().getType(),
          player.getLocation().add(1, 1, 0).getBlock().getType(),
          player.getLocation().add(-1, 1, 0).getBlock().getType(),
          player.getLocation().add(0, 1, 1).getBlock().getType(),
          player.getLocation().add(0, 1, -1).getBlock().getType()
      );

      if (!blocks.containsKey(player.getName())) continue;

      if (materials.contains(blocks.get(player.getName()))) {
        players.add(player.getName());
      }
    }
    return players;
  }


  private String blockName(Material block) {
    return WordUtils.capitalize(block.name().toLowerCase().replace("_", " "));
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    if (blocks.containsKey(e.getPlayer().getName())) {
      e.getPlayer().sendMessage(ChatColor.BLUE + "Next block: " + ChatColor.GREEN + blockName(blocks.get(e.getPlayer().getName())));
    }

    refreshScoreboard();
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    refreshScoreboard();
  }

  @EventHandler
  public void onFoodLevelChange(FoodLevelChangeEvent e) {
    if (!hunger) e.setCancelled(true);
  }
}