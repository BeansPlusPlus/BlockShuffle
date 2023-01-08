package beansplusplus.blockshuffle;

import beansplusplus.gameconfig.ConfigLoader;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.Yaml;

public class BlockShuffler {
  private Random random = new Random();

  private static NavigableMap<Double, Material> blockMap;
  private static double materialTotal;

  public static void loadAllBlockTypes(InputStream is) {
    blockMap = new TreeMap<>();
    materialTotal = 0;
    Yaml yaml = new Yaml();

    Map<String, Object> data = yaml.load(is);

    for (String mat : data.keySet()) {
      Material material = Material.getMaterial(mat);
      double weight = (double) data.get(mat);

      materialTotal += weight;
      blockMap.put(materialTotal, material);
    }
  }

  public Material nextBlockType() {
    double v = random.nextDouble() * materialTotal;
    return blockMap.higherEntry(v).getValue();
  }
}
