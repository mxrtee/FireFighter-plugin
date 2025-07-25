package com.github.tommyt0mmy.firefighter;

import com.github.tommyt0mmy.firefighter.commands.Fireset;
import com.github.tommyt0mmy.firefighter.commands.Firetool;
import com.github.tommyt0mmy.firefighter.commands.Help;
import com.github.tommyt0mmy.firefighter.events.*;
import com.github.tommyt0mmy.firefighter.tabcompleters.FiresetTabCompleter;
import com.github.tommyt0mmy.firefighter.tabcompleters.HelpTabCompleter;
import com.github.tommyt0mmy.firefighter.utility.Configs;
import com.github.tommyt0mmy.firefighter.utility.Messages;
import com.github.tommyt0mmy.firefighter.utility.UpdateChecker;
import com.github.tommyt0mmy.firefighter.utility.XMaterial;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FireFighter extends JavaPlugin
{

    private static FireFighter instance;

    //UPDATE CHECKER
    private final int spigotResourceId = 68772;
    private final String spigotResourceUrl = "https://www.spigotmc.org/resources/firefighter.68772/";

    public File datafolder = getDataFolder();
    public String prefix = "[" + this.getDescription().getPrefix() + "] ";
    public final String version = this.getDescription().getVersion();
    public boolean startedMission = false;
    public boolean missionsIntervalState = false;
    public boolean programmedStart = false;
    public long nextMissionStart;
    public HashMap<UUID, Integer> PlayerContribution = new HashMap<>();
    public String missionName = "";
    public HashMap<UUID, Location> fireset_first_position = new HashMap<>();
    public HashMap<UUID, Location> fireset_second_position = new HashMap<>();
    public Logger console = getLogger();
    public Messages messages = null;
    public Configs configs = null;

    public static FireFighter getInstance()
    {
        return instance;
    }

    private void setInstance(FireFighter instance)
    {
        FireFighter.instance = instance;
    }

    public static <T> T getByRandomClass(Set<T> set) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException("The Set cannot be empty.");
        }
        int randomIndex = new Random().nextInt(set.size());
        int i = 0;
        for (T element : set) {
            if (i == randomIndex) {
                return element;
            }
            i++;
        }
        throw new IllegalStateException("Something went wrong while picking a random element.");
    }

    public void onEnable()
    {
        //priority 1
        setInstance(this);

        //priority 2
        configs = new Configs();
        messages = new Messages();
        nextMissionStart = configs.getConfig().getLong("missions_interval") * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                List<Player> playerList = new ArrayList<>();
                for(Player player : Bukkit.getOnlinePlayers()) {
                    User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
                    Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
                    if(inheritedGroups.contains(LuckPermsProvider.get().getGroupManager().getGroup("pompieri"))){
                        playerList.add(player);
                    }

                    if(playerList.size() >= 2) {
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), "fireset startmission " + getByRandomClass(configs.getConfig().getConfigurationSection("missions").getKeys(false)));
                    }
                }
            }
        }.runTaskTimer(this,0,3600 * 20);

        //priority 3
        loadEvents();
        loadCommands();

        //priority 4
        @SuppressWarnings("unused")
        BukkitTask task = new MissionsHandler(this).runTaskTimer(this, 0, 20);

        //checking for updates
        UpdateChecker updateChecker = new UpdateChecker();
        if (updateChecker.needsUpdate())
        {
            console.info("An update for FireFighter is available at:");
            console.info(spigotResourceUrl);
            console.info(String.format("Installed version: %s Lastest version: %s", updateChecker.getCurrent_version(), updateChecker.getLastest_version()));
        }

        console.info("FireFighter v" + version + " enabled succesfully");
    }

    public void onDisable()
    {
        console.info("FireFighter v" + version + " disabled succesfully");
    }

    private void loadEvents()
    {
        this.getServer().getPluginManager().registerEvents(new FireExtinguisherActivation(), this);
        this.getServer().getPluginManager().registerEvents(new FiresetWand(), this);
        this.getServer().getPluginManager().registerEvents(new RewardsetGUI(), this);
        this.getServer().getPluginManager().registerEvents(new onPlayerJoin(), this);
        this.getServer().getPluginManager().registerEvents(new FireBootsActivation(), this);
        this.getServer().getPluginManager().registerEvents(new FireMaskActivation(), this);
        this.getServer().getPluginManager().registerEvents(new ArmorListener(), this);
    }

    private void loadCommands() {
        getCommand("firefighter").setExecutor(new Help());
        getCommand("fireset").setExecutor(new Fireset());
        getCommand("firetool").setExecutor(new Firetool());
        getCommand("firefighter").setTabCompleter(new HelpTabCompleter());
        getCommand("fireset").setTabCompleter(new FiresetTabCompleter());
    }

    public ItemStack getFireExtinguisher()
    {
        ItemStack fire_extinguisher = new ItemStack(Material.STICK);
        //getting meta
        ItemMeta meta = fire_extinguisher.getItemMeta();
        meta.setCustomModelData(78);
        //modifying meta
        meta.setDisplayName("§fLancia Antincendio");
        List<String> lore = new ArrayList<>();
        lore.add("§7Usala per spegnere gli incendi");
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setLore(lore);
        fire_extinguisher.setItemMeta(meta);
        return fire_extinguisher;
    }

    public ItemStack getFireBoots(){
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        meta.setColor(Color.RED);
        meta.setDisplayName("§fStivali");
        List<String> lore = new ArrayList<>();
        lore.add("§7Usali per saltare più in alto");
        meta.setLore(lore);
        boots.setItemMeta(meta);

        return boots;
    }

    public ItemStack getFireMask(){
        ItemStack mask = new ItemStack(Material.FEATHER);
        ItemMeta meta = mask.getItemMeta();
        meta.setDisplayName("§fMaschera Antigas");
        meta.setCustomModelData(98);
        List<String> lore = new ArrayList<>();
        lore.add("§7Usali per proteggerti dalle fiamme");
        meta.setLore(lore);
        mask.setItemMeta(meta);

        return mask;
    }

    public int getSpigotResourceId()
    {
        return spigotResourceId;
    }

    public String getSpigotResourceUrl()
    {
        return spigotResourceUrl;
    }
}
