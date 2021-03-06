package me.tedwoodworth.diplomacy.nations;

import me.tedwoodworth.diplomacy.Diplomacy;
import me.tedwoodworth.diplomacy.players.DiplomacyPlayer;
import me.tedwoodworth.diplomacy.players.DiplomacyPlayers;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getOfflinePlayer;

public class Nations {

    private static Nations instance = null;
    private final File nationConfigFile = new File(Diplomacy.getInstance().getDataFolder(), "nations.yml");
    private List<Nation> nations = new ArrayList<>();
    private YamlConfiguration nationConfig;

    public List<Nation> getNations() {
        return nations;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(new EventListener(), Diplomacy.getInstance());
    }

    public Nation createNation(String name, DiplomacyPlayer leader) {
        var founder = getOfflinePlayer(leader.getUUID());
        var nextNationID = nationConfig.getString("NextNationID");
        if (nextNationID == null) {
            nationConfig.set("NextNationID", "0");
            nextNationID = "0";
        }

        var nationID = nextNationID;
        nextNationID = String.valueOf(Integer.parseInt(nextNationID) + 1);

        Reader reader = new InputStreamReader(Objects.requireNonNull(Diplomacy.getInstance().getResource("Default/Nation.yml")));
        ConfigurationSection nationSection = YamlConfiguration.loadConfiguration(reader);

        nationConfig.set("Nations." + nationID, nationSection);
        nationConfig.set("NextNationID", nextNationID);
        var initializedNationSection = Nation.initializeNation(nationSection, founder, name);
        var nation = new Nation(nationID, initializedNationSection);
        nations.add(nation);
        return nation;

    }

    public static Nations getInstance() {
        if (instance == null) {
            instance = new Nations();
        }
        return instance;
    }

    private Nations() {
        nationConfig = YamlConfiguration.loadConfiguration(nationConfigFile);
        var nationsSection = nationConfig.getConfigurationSection("Nations");
        if (nationsSection == null) {
            nationsSection = nationConfig.createSection("Nations");
        }
        for (var nationID : nationsSection.getKeys(false)) {
            var nationSection = nationConfig.getConfigurationSection("Nations." + nationID);
            if (nationSection == null) {
                nationConfig.set("Nations." + nationID, null);
            } else {
                var nation = new Nation(nationID, nationSection);
                nations.add(nation);
            }
        }

    }

    @Nullable
    public Nation get(DiplomacyPlayer player) {
        for (var nation : nations) {
            for (var member : nation.getMembers()) {
                var testPlayer = DiplomacyPlayers.getInstance().get(UUID.fromString(member));
                if (player.equals(testPlayer)) {
                    return nation;
                }
            }
        }
        return null;
    }

    @Nullable
    public Nation get(String name) {
        for (var nation : nations) {
            if (name.equalsIgnoreCase(nation.getName())) {
                return nation;
            }
        }
        return null;
    }

    @Nullable
    public Nation getFromID(String id) {
        for (var nation : nations) {
            if (id.equalsIgnoreCase(nation.getNationID())) {
                return nation;
            }
        }
        return null;
    }

    public void rename(String name, Nation nation) {
        nationConfig.set("Nations." + nation.getNationID() + ".Name", name);
        nation.setName(name);
    }

    public void removeNation(Nation nation) {
        nationConfig.set("Nations." + nation.getNationID(), null);
        nations.remove(nation);
    }

    public static boolean isWilderness(Nation nation) {
        return nation == null;
    }

    public void save() {
        try {
            nationConfig.save(nationConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class EventListener implements Listener {
        @EventHandler
        void onWorldSave(WorldSaveEvent event) {
            save();
        }
    }
}
