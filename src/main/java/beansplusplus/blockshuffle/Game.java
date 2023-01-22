package beansplusplus.blockshuffle;

import beansplusplus.gameconfig.GameConfiguration;
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

public class Game implements Listener {
  private int blocksToWin;

  private boolean hunger;
  private boolean shareBlocks;

  private int ticksPerBlock;

  private int currentTask;
  private int timer;

  private Map<String, Material> blocks = new HashMap<>();

  private Map<String, Integer> scores = new HashMap<>();

  private Plugin plugin;
  private BlockShuffler shuffler;

  public Game(BlockShufflePlugin plugin) {
    this.plugin = plugin;
  }

  public void start() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.setHealth(20);
      player.setLevel(0);
      player.setFoodLevel(20);
      player.getInventory().clear();
      player.setGameMode(GameMode.SURVIVAL);
    }

    World world = Bukkit.getWorld("world");
    world.setTime(1000);

    this.blocksToWin = GameConfiguration.getConfig().getValue("blocks_to_win");
    this.hunger = GameConfiguration.getConfig().getValue("hunger");
    this.shareBlocks = GameConfiguration.getConfig().getValue("share_blocks");
    this.ticksPerBlock = (int) ((double) GameConfiguration.getConfig().getValue("minutes_per_block") * 60.0 * 20.0);

    this.shuffler = new BlockShuffler();

    int border = (int) world.getWorldBorder().getMaxSize();
    if (border > 2500) border = 2500; // anything outside this range tasks ages to get to anyway

    shuffler.calculateFrequencies(Bukkit.getWorld("world"), Bukkit.getWorld("world_nether"), border);

    for (Player player : Bukkit.getOnlinePlayers()) {
      scores.put(player.getName(), 0);
    }

    boolean keepInventory = GameConfiguration.getConfig().getValue("keep_inventory");
    boolean pvp = GameConfiguration.getConfig().getValue("pvp");

    for (World w : Bukkit.getWorlds()) {
      w.setGameRule(GameRule.KEEP_INVENTORY, keepInventory);
      w.setPVP(pvp);
    }

    Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    refreshScoreboard();


    int gracePeriodSecs = GameConfiguration.getConfig().getValue("grace_period_seconds");

    if (gracePeriodSecs == 0) {
      pickNextBlock();
    } else {
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.sendMessage(ChatColor.YELLOW + "Beginning grace period of " + ChatColor.RED + gracePeriodSecs + " seconds");
      }
      Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::pickNextBlock, gracePeriodSecs * 20);
    }
  }

  public void end() {
    HandlerList.unregisterAll(this);
    Bukkit.getScheduler().cancelTasks(plugin);
  }

  private void refreshScoreboard() {
    Scoreboard scoreboard = getScoreboard();

    for (Player p : Bukkit.getOnlinePlayers()) {
      p.setScoreboard(scoreboard);
    }
  }

  public Scoreboard getScoreboard() {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    Objective obj = scoreboard.registerNewObjective("scoreboard", "scoreboard",
        ChatColor.GOLD + "Block Shuffle - Blocks to win: " + ChatColor.RED + blocksToWin);
    obj.setDisplaySlot(DisplaySlot.SIDEBAR);

    for (String name : scores.keySet()) {
      obj.getScore(name).setScore(scores.get(name));
    }

    return scoreboard;
  }

  private void nextRound() {
    if (checkForWinner()) {
      end();
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

  private void runTimer() {
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
    List<String> winningPlayers = playersAboveBlock();
    if (!winningPlayers.isEmpty()) {
      for (String p : winningPlayers) {
        scores.put(p, scores.get(p) + 1);
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.sendMessage(ChatColor.GREEN + p + ChatColor.YELLOW + " found the block and got a point!");
        }
      }
      refreshScoreboard();
      Bukkit.getScheduler().cancelTask(currentTask);
      nextRound();
      return;
    }
  }

  private List<String> playersAboveBlock() {
    List<String> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      Material material1 = player.getLocation().add(0, -1, 0).getBlock().getType();
      Material material2 = player.getLocation().getBlock().getType();

      if (!blocks.containsKey(player.getName())) continue;

      if (material1 == blocks.get(player.getName()) || material2 == blocks.get(player.getName())) {
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