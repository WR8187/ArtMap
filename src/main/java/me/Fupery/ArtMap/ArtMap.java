package me.Fupery.ArtMap;

import me.Fupery.ArtMap.Command.CommandHandler;
import me.Fupery.ArtMap.Compatability.CompatibilityManager;
import me.Fupery.ArtMap.IO.*;
import me.Fupery.ArtMap.Listeners.*;
import me.Fupery.ArtMap.Menu.Handler.MenuHandler;
import me.Fupery.ArtMap.Protocol.ArtistHandler;
import me.Fupery.ArtMap.Protocol.Channel.ChannelCacheManager;
import me.Fupery.ArtMap.Recipe.ArtMaterial;
import me.Fupery.ArtMap.Recipe.RecipeLoader;
import me.Fupery.ArtMap.Utils.Lang;
import me.Fupery.ArtMap.Utils.Preview;
import me.Fupery.ArtMap.Utils.TaskManager;
import me.Fupery.ArtMap.Utils.VersionHandler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.persistence.PersistenceException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ArtMap extends JavaPlugin {

    private static SoftReference<ArtMap> pluginInstance = null;
    private final int mapResolutionFactor = 4;// TODO: 20/07/2016 consider adding other resolutions
    private MenuHandler menuHandler;
    private ArtistHandler artistHandler;
    private ConcurrentHashMap<Player, Preview> previewing;
    private VersionHandler bukkitVersion;
    private TaskManager taskManager;
    private ArtDatabase artDatabase;
    private ChannelCacheManager cacheManager;
    private MapManager mapManager;
    private RecipeLoader recipeLoader;
    private CompatibilityManager compatManager;
    private Lang lang;
    private List<String> titleFilter;
    private PixelTableManager pixelTable;
    private boolean hasRegisteredListeners = false;

    public static ArtDatabase getArtDatabase() {
        return instance().artDatabase;
    }

    public static ArtMap instance() {
        if (pluginInstance == null || pluginInstance.get() == null) {
            pluginInstance = new SoftReference<>((ArtMap) Bukkit.getPluginManager().getPlugin("ArtMap"));
        }
        return pluginInstance.get();
    }

    public static TaskManager getTaskManager() {
        return instance().taskManager;
    }

    public static ArtistHandler getArtistHandler() {
        return instance().artistHandler;
    }

    public static ConcurrentHashMap<Player, Preview> getPreviewing() {
        return instance().previewing;
    }

    public static VersionHandler getBukkitVersion() {
        return instance().bukkitVersion;
    }

    public static ChannelCacheManager getCacheManager() {
        return instance().cacheManager;
    }

    public static Lang getLang() {
        return instance().lang;
    }

    public static RecipeLoader getRecipeLoader() {
        return instance().recipeLoader;
    }

    public static CompatibilityManager getCompatManager() {
        return instance().compatManager;
    }

    public static MenuHandler getMenuHandler() {
        return instance().menuHandler;
    }

    public static MapManager getMapManager() {
        return instance().mapManager;
    }

    @Override
    public void onEnable() {
        pluginInstance = new SoftReference<>(this);
        saveDefaultConfig();

        taskManager = new TaskManager(this);
        previewing = new ConcurrentHashMap<>();
        artistHandler = new ArtistHandler(this);
        bukkitVersion = new VersionHandler();
        cacheManager = new ChannelCacheManager();
        menuHandler = new MenuHandler(this);
        compatManager = new CompatibilityManager();
        mapManager = new MapManager(this);
        FileConfiguration langFile = loadOptionalYAML("customLang", "lang.yml");
        boolean disableActionBar = getConfig().getBoolean("disableActionBar");
        boolean hidePrefix = getConfig().getBoolean("hidePrefix");
        lang = new Lang(getConfig().getString("language"), langFile, disableActionBar, hidePrefix);
        artDatabase = new SQLiteDatabase(this);
//        setupDatabase();
//        if (artDatabase == null) {
//            getPluginLoader().disablePlugin(this);
//            getLogger().warning(lang.getMsg("CANNOT_BUILD_DATABASE"));
//            return;
//        }
        if (!loadTables()) {
            getLogger().warning(lang.getMsg("INVALID_DATA_TABLES"));
            getPluginLoader().disablePlugin(this);
            return;
        }
        FileConfiguration filter = YamlConfiguration.loadConfiguration(getTextResource("titleFilter.yml"));
        titleFilter = filter.getStringList("blacklisted");

        getCommand("artmap").setExecutor(new CommandHandler());

        if (!hasRegisteredListeners) {
            PluginManager manager = getServer().getPluginManager();
            manager.registerEvents(new PlayerInteractListener(), this);
            manager.registerEvents(new PlayerInteractEaselListener(), this);
            manager.registerEvents(new PlayerQuitListener(), this);
            manager.registerEvents(new ChunkUnloadListener(), this);
            manager.registerEvents(new PlayerCraftListener(), this);
            manager.registerEvents(new InventoryInteractListener(), this);
            manager.registerEvents(new EaselInteractListener(), this);
            if (bukkitVersion.getVersion() != VersionHandler.BukkitVersion.v1_8) {
                manager.registerEvents(new PlayerSwapHandListener(), this);
                manager.registerEvents(new PlayerDismountListener(), this);
            }
            hasRegisteredListeners = true;
        }
        recipeLoader = new RecipeLoader(loadOptionalYAML("customRecipes", "recipe.yml"));
        ArtMaterial.setupRecipes();
    }

    @Override
    public void onDisable() {
        artistHandler.stop();
        menuHandler.closeAll();
        mapManager.saveKeys();


        if (previewing.size() > 0) {
            for (Player player : previewing.keySet()) {
                Preview.stop(player);
            }
        }
        recipeLoader.unloadRecipes();
        reloadConfig();
        pluginInstance = null;
    }

    private FileConfiguration loadOptionalYAML(String configOption, String fileName) {
        FileConfiguration defaultValues = YamlConfiguration.loadConfiguration(getTextResource(fileName));
        if (!getConfig().getBoolean(configOption)) {
            return defaultValues;
        } else {
            File file = new File(getDataFolder(), fileName);
            if (!file.exists()) {
                try {
                    if (!file.createNewFile()) return defaultValues;
                    Files.copy(getResource(fileName), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    getLogger().info(String.format("Failed to build %s file", fileName));
                    return defaultValues;
                }
            }
            return YamlConfiguration.loadConfiguration(file);
        }
    }

    private void setupDatabase() {
        try {
            getDatabase().find(MapArt.class).findRowCount();
        } catch (PersistenceException ex) {
            getLogger().info("Building MapArt database");
            installDDL();
        }
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> list = new ArrayList<>();
        list.add(MapArt.class);
        return list;
    }

    private boolean loadTables() {
        return ((pixelTable = PixelTableManager.buildTables(mapResolutionFactor)) != null);
    }

    public int getMapResolutionFactor() {
        return mapResolutionFactor;
    }

    public List<String> getTitleFilter() {
        return titleFilter;
    }

    public PixelTableManager getPixelTable() {
        return pixelTable;
    }

    public Reader getTextResourceFile(String fileName) {
        return getTextResource(fileName);
    }
}