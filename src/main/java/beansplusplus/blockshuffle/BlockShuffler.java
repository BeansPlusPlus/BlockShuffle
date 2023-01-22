package beansplusplus.blockshuffle;

import org.bukkit.*;

import java.io.InputStream;
import java.util.*;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.yaml.snakeyaml.Yaml;

public class BlockShuffler {
  private Random random = new Random();

  private static Map<String, Object> data;

  public static void loadAllBlockTypes(InputStream is) {
    Yaml yaml = new Yaml();
    data = yaml.load(is);
  }

  private NavigableMap<Double, Material> blockMap;
  private double materialTotal;

  public void calculateFrequencies(World overworld, World nether, int border) {
    Map<String, Double> weights = new HashMap<>();

    Map<Biome, Double> biomeDistribution = biomeDistribution(overworld, border, 0.6);
    biomeDistribution.putAll(biomeDistribution(nether, border, 0.4));

    for (String block : data.keySet()) {
      Map<String, Object> map = (Map) data.get(block);

      List<Biome> biomes = new ArrayList<>();
      List<StructureType> structures = new ArrayList<>();

      if (map.containsKey("biome")) {
        for (String b : (List<String>) map.get("biome")) {
          biomes.add(Biome.valueOf(b));
        }
      }

      if (map.containsKey("structure")) {
        for (String s : (List<String>) map.get("structure")) {
          structures.add(Registry.STRUCTURE_TYPE.get(NamespacedKey.minecraft(s.toLowerCase())));
        }
      }

      double value = (double) map.get("frequency") * biomeRarityMultiplier(biomes, biomeDistribution);

      if (value > 0) weights.put(block, value);
    }

    createMapFromWeights(weights);
  }

  private Map<Biome, Double> biomeDistribution(World world, int border, double multiplier) {
    Map<Biome, Double> map = new HashMap<>();

    double total = 0;

    for (int z = -border; z < border; z += border / 50) {
      for (int x = -border; x < border; x += border / 50) {
        for (int y = -50; y <= 50; y += 20) {
          double value = getDistanceMultipler(x, y, z) * multiplier;
          Biome b = world.getBiome(x, y, z);
          map.put(b, map.getOrDefault(b, 0.0) + value);
          total += value;
        }
      }
    }
    for (Biome b : map.keySet()) {
      map.put(b, map.get(b) / total);

    }

    return map;
  }

  private double getDistanceMultipler(int x, int y, int z) {
    int dy = (50 - y) * 100;

    double d = x * x + dy * dy + z * z;

    if (d == 0) return 1;

    return Math.pow(1.0 / d, 0.8);
  }

  private double biomeRarityMultiplier(List<Biome> biomes, Map<Biome, Double> distribution) {
    if (biomes.size() == 0) return 1;

    double count = 0;
    for (Biome b : biomes) {
      count += distribution.getOrDefault(b, 0.0);
    }

    return 1.0 - Math.pow(1.0 - count, 40.0);
  }

  private void createMapFromWeights(Map<String, Double> weights) {
    blockMap = new TreeMap<>();
    materialTotal = 0;

    for (String mat : weights.keySet()) {
      Material material = Material.getMaterial(mat);
      double weight = weights.get(mat);

      materialTotal += weight;
      blockMap.put(materialTotal, material);
    }
  }

  public Material nextBlockType() {
    double v = random.nextDouble() * materialTotal;
    return blockMap.higherEntry(v).getValue();
  }
}
