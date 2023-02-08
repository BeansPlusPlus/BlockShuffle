package beansplusplus.blockshuffle;

import beansplusplus.beansgameplugin.BeansGamePlugin;
import beansplusplus.beansgameplugin.GameConfiguration;
import beansplusplus.beansgameplugin.GameCreator;
import beansplusplus.beansgameplugin.GameState;
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
    return List.of("TODO");
  }

  @Override
  public String name() {
    return "Block Shuffle";
  }
}
