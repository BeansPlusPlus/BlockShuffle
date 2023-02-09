package beansplusplus.blockshuffle;

import beansplusplus.beansgameplugin.BeansGamePlugin;
import beansplusplus.beansgameplugin.GameConfiguration;
import beansplusplus.beansgameplugin.GameCreator;
import beansplusplus.beansgameplugin.GameState;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.List;

public class BlockShufflePlugin extends JavaPlugin implements GameCreator {

  public void onEnable() {
    BeansGamePlugin beansGamePlugin = (BeansGamePlugin) getServer().getPluginManager().getPlugin("BeansGamePlugin");
    beansGamePlugin.registerGame(this);
    BlockShuffler.loadAllBlockTypes(getResource("blocks.yml"));
  }

  @Override
  public beansplusplus.beansgameplugin.Game createGame(GameConfiguration config, GameState state) {
    return new BlockShuffleGame(this, config, state);
  }

  @Override
  public boolean isValidSetup(CommandSender commandSender, GameConfiguration gameConfiguration) {
    return true;
  }

  @Override
  public InputStream config() {
    return getResource("config.yml");
  }

  @Override
  public List<String> rulePages() {
    return List.of(
        "Block shuffle is a game where you race to find randomly generated blocks as fast as possible.",
        "Each round, every person is given a randomly generated block. The first person to find the block gets a point for that round. First person to 10 blocks wins.",
        "Default settings will make the round last forever until the block is found by someone. You can force rounds to end earlier if you configure the " + ChatColor.RED + "minutes_per_block" + ChatColor.BLACK + " setting.",
        "By default, PVP and hunger is off and keep inventory is on. These settings can be changed in the config if you like.",
        "Using the config, you can also alter the number of blocks to win, or turn off " + ChatColor.RED + "share_blocks" + ChatColor.BLACK + " such that every player gets a different block per round."
    );
  }

  @Override
  public String name() {
    return "Block Shuffle";
  }
}
