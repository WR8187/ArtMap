package me.Fupery.ArtMap.Legacy;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.IO.MapArt;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlatDatabaseConverter {

    private JavaPlugin plugin;

    public FlatDatabaseConverter(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean convertDatabase() {
        String dbFileName = "mapList.yml";
        File databaseFile = new File(plugin.getDataFolder(), dbFileName);
        if (!databaseFile.exists()) return false;
        plugin.getLogger().info("Old 'mapList.yml' database found! Converting to new format ...");
        plugin.getLogger().info("(This may take a while, but will only need to run once)");
        List<MapArt> artworks = readArtworks(databaseFile);
        ArtMap.getArtDatabase().addArtworks(artworks.toArray(new MapArt[artworks.size()]));
        File disabledDatabaseFile = new File(plugin.getDataFolder(), dbFileName + ".off");
        if (!databaseFile.renameTo(disabledDatabaseFile)) {
            plugin.getLogger().info("Error disabling mapList.yml! Delete this file manually.");
            return false;
        }
        plugin.getLogger().info(String.format("Conversion completed! %s artworks converted. " +
                "mapList.yml has been disabled.", artworks.size()));
        return true;
    }

    public List<MapArt> readArtworks(File databaseFile) {
        ArrayList<MapArt> artworkList = new ArrayList<>();
        FileConfiguration database = YamlConfiguration.loadConfiguration(databaseFile);
        ConfigurationSection artworks = database.getConfigurationSection("artworks");

        for (String title : artworks.getKeys(false)) {
            ConfigurationSection map = artworks.getConfigurationSection(title);
            if (map != null) {
                int mapIDValue = map.getInt("mapID");
                OfflinePlayer player = (map.contains("artist")) ?
                        Bukkit.getOfflinePlayer(UUID.fromString(map.getString("artist"))) : null;
                String date = map.getString("date");
                MapArt artwork = new MapArt(((short) mapIDValue), title, player, date);
                if (ArtMap.getArtDatabase().containsArtwork(artwork, true)) {
                    plugin.getLogger().info(String.format("    Ignoring '%s' (already exists in database) ...", title));
                } else {
                    plugin.getLogger().info(String.format("    Converting '%s' ...", title));
                    artworkList.add(artwork);
                }
            }
        }
        return artworkList;
    }
}
