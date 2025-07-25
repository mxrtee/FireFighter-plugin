package com.github.tommyt0mmy.firefighter;

import com.github.tommyt0mmy.firefighter.utility.Configs;
import com.github.tommyt0mmy.firefighter.utility.Permissions;
import com.github.tommyt0mmy.firefighter.utility.TitleActionBarUtil;
import com.github.tommyt0mmy.firefighter.utility.XMaterial;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MissionsHandler extends BukkitRunnable
{
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final RegionContainer container;

    private final FireFighter fireFighter = FireFighter.getInstance();
    private boolean firstRun = true;

    // Track initial and current fires per mission
    private final Map<String, Integer> initialFires = new ConcurrentHashMap<>();
    private final Map<String, Set<Block>> currentFires = new ConcurrentHashMap<>();

    public MissionsHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    @Override
    public void run() {
        if (firstRun) { firstRun = false; return; }
        if (!config.contains("missions") || fireFighter.startedMission == false) return;

        World world = Bukkit.getWorld(fireFighter.configs.getConfig().getString("missions." + fireFighter.missionName + ".world"));
        if (world == null) return;
        RegionManager mgr = container.get(BukkitAdapter.adapt(world));
        ProtectedRegion region = mgr.getRegion(fireFighter.missionName);
        if (region == null) return;

        // Spawn fires and collect current fire blocks
        Set<Block> fires = new HashSet<>();
        BlockVector3 min = region.getMinimumPoint(), max = region.getMaximumPoint();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y < max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block below = world.getBlockAt(x, y, z);
                    Block above = world.getBlockAt(x, y + 1, z);
                    if (below.getType().isSolid() && above.getType() == Material.AIR) {
                        above.setType(Material.FIRE);
                        fires.add(above);
                    }
                }
            }
        }

        // Initialize counts
        if (!initialFires.containsKey(fireFighter.missionName)) {
            initialFires.put(fireFighter.missionName, fires.size());
        }
        currentFires.put(fireFighter.missionName, fires);
        int initial = initialFires.get(fireFighter.missionName);
        int remaining = fires.size();
        int extinguished = initial - remaining;

        // Build progress bar
        int totalSlots = 20;
        int filledSlots = (int) ((double) extinguished / initial * totalSlots);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filledSlots; i++) bar.append('█');
        for (int i = filledSlots; i < totalSlots; i++) bar.append('░');
        String hotbar = "§a" + bar + " §7(" + extinguished + "/" + initial + ")";

        // Broadcast to players in region
        for (Player p : world.getPlayers()) {
            BlockVector3 pos = BlockVector3.at(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            if (region.contains(pos)) {
                Broadcast(world, "", "", hotbar, "firefighter.use");
            }
        }

        // Check for mission end
        if (remaining == 0) {
            turnOffInstructions();
        }
    }

    // Existing methods from your code:
    private void Broadcast(World w, String title, String subtitle, String hotbar, String permission) {
        if (w == null) return;
        for (Player dest : w.getPlayers()) {
            if (!dest.hasPermission(permission)) continue;
            if (!title.isEmpty()) TitleActionBarUtil.sendTitle(dest, title, 10, 100, 20);
            if (!subtitle.isEmpty()) TitleActionBarUtil.sendSubTitle(dest, subtitle, 10, 100, 20);
            new BukkitRunnable() {
                int timer = 0;
                public void run() {
                    TitleActionBarUtil.sendActionBarMessage(dest, hotbar);
                    if (++timer >= 4) cancel();
                }
            }.runTaskTimer(fireFighter, 0, 20);
        }
    }

    private void turnOffInstructions() {
        fireFighter.console.info("Mission ended");
        giveRewards();
        fireFighter.startedMission = false;
        fireFighter.missionName = "";
        initialFires.remove(fireFighter.missionName);
        currentFires.remove(fireFighter.missionName);
        fireFighter.PlayerContribution.clear();
    }

    private void giveRewards() {
        // implement reward logic as before
    }

  /*  public void spawnFire(ProtectedRegion r, World world){
        BlockVector3 min = r.getMinimumPoint(), max = r.getMaximumPoint();
        for(int x=min.getBlockX(); x<=max.getBlockX(); x++)
            for(int y=min.getBlockY(); y<max.getBlockY(); y++)
                for(int z=min.getBlockZ(); z<=max.getBlockZ(); z++){
                    Block above = world.getBlockAt(x, y+1, z);
                    Block below = world.getBlockAt(x, y, z);
                    if(above.getType()==Material.AIR && below.getType().isSolid())
                        above.setType(Material.FIRE);
                }
    }

    private FireFighter FireFighterClass = FireFighter.getInstance();

    public MissionsHandler()
    {
        config = FireFighterClass.getConfig();
    }

    private Boolean firstRun = true;
    private FileConfiguration config;
    private List<Block> setOnFire = new ArrayList<>();

    @Override
    public void run()
    {
        if (firstRun)
        {
            firstRun = false;
            return;
        }
        if (!config.contains("missions"))
        {
            FireFighterClass.console.info("There are no missions! Start setting up new missions by typing in-game '/firefighter fireset 2'");
            return;
        }
        if (FireFighterClass.startedMission)
        {
            return;
        }
        if (System.currentTimeMillis() < FireFighterClass.nextMissionStart && !FireFighterClass.programmedStart)
        {
            return;
        }
        if (!FireFighterClass.missionsIntervalState && !FireFighterClass.programmedStart)
        {
            return;
        }

        int fire_lasting_ticks = Integer.parseInt(FireFighterClass.getConfig().get("fire_lasting_seconds").toString()) * 20;
        FireFighterClass.startedMission = true;
        FireFighterClass.configs.loadConfigs();
        //selecting random mission
        Random random = new Random();
        List<String> missions = new ArrayList<>(((MemorySection) config.get("missions")).getKeys(false));
        if (missions.size() < 1)
        {
            FireFighterClass.console.info("There are no missions! Start setting up new missions by typing in-game '/firefighter fireset 2'");
            return;
        }
        String missionName = FireFighterClass.missionName;
        if (!FireFighterClass.programmedStart)
        { //if started randomly
            missionName = missions.get(random.nextInt(missions.size()));
            FireFighterClass.missionName = missionName;
            FireFighterClass.nextMissionStart = System.currentTimeMillis() + ((FireFighterClass.configs.getConfig().getInt("missions_interval")) * 1000) + ((FireFighterClass.configs.getConfig().getInt("fire_lasting_seconds")) * 1000);
        } else
        { //if programmed with /fireset startmission
            FireFighterClass.programmedStart = false;
        }
        String missionPath = "missions." + missionName;
        FireFighterClass.PlayerContribution.clear();
        spawnFire(WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(Bukkit.getWorld("Napoli"))).getRegion(FireFighter.getInstance().configs.getConfig().getString(missionPath + "." + "region")), Bukkit.getWorld("Napoli"));


        //TURNING OFF THE MISSION

        new BukkitRunnable()
        {
            public void run()
            {
                turnOffInstructions();
                cancel();
            }
        }.runTaskTimer(FireFighterClass, fire_lasting_ticks, 1);
    }

    private void Broadcast(World w, String title, String subtitle, String hotbar, String permission)
    {
        if (w == null)
        { //avoids NPE
            return;
        }
        for (Player dest : w.getPlayers())
        {
            if (!dest.hasPermission(permission))
            {
                continue;
            }
            TitleActionBarUtil.sendTitle(dest, title, 10, 100, 20);
            TitleActionBarUtil.sendSubTitle(dest, subtitle, 10, 100, 20);
            try
            {
                new BukkitRunnable()
                {
                    int timer = 0;

                    public void run()
                    {
                        timer++;
                        TitleActionBarUtil.sendActionBarMessage(dest, hotbar);
                        if (timer >= 4)
                        {
                            cancel();
                        }
                    }
                }.runTaskTimer(FireFighterClass, 0, 50);
            } catch (Exception ignored)
            {
            }
        }
    }

    private void Broadcast(World w, String message, String permission)
    {
        if (w == null)
        { //avoids NPE
            return;
        }
        for (Player dest : w.getPlayers())
        {
            if (dest.hasPermission(permission))
            {
                dest.sendMessage(message);
            }
        }
    }

    private String getMediumCoord(String missionName)
    { //returns the medium position of the mission
        String res = "";
        String missionPath = "missions." + missionName;
        res += (((Integer.parseInt(config.get(missionPath + ".first_position.x").toString()) + Integer.parseInt(config.get(missionPath + ".second_position.x").toString())) / 2) + ""); //X
        res += " ";
        res += (config.get(missionPath + ".altitude").toString()); // Y
        res += " ";
        res += (((Integer.parseInt(config.get(missionPath + ".first_position.z").toString()) + Integer.parseInt(config.get(missionPath + ".second_position.z").toString())) / 2) + ""); // Z
        return res;
    }

    private void giveRewards()
    {
        String missionPath = "missions." + FireFighterClass.missionName;
        String rewardsPath = missionPath + ".rewards";
        String worldName = (String) FireFighterClass.getConfig().get(missionPath + ".world");
        if (FireFighterClass.getConfig().get(rewardsPath) == null || FireFighterClass.getConfig().getInt(rewardsPath + ".size") == 0)
        { //no rewards set
            FireFighterClass.getConfig().set(rewardsPath + ".size", 0);
            FireFighterClass.console.info("There aren't rewards set for the mission! Who will complete that mission won't receive a reward :(");
            FireFighterClass.console.info("Begin setting rewards with '/fireset editmission <name> rewards', drag items in and out and then save!");
        } else
        {
            //picking up a random reward from the rewardsList
            Random random = new Random();
            int randomIndex = random.nextInt(FireFighterClass.getConfig().getInt(rewardsPath + ".size"));
            ItemStack reward = FireFighterClass.getConfig().getItemStack(rewardsPath + "." + randomIndex);
            //picking the best player
            UUID bestPlayer = null;
            for (Player p : Bukkit.getWorld(worldName).getPlayers())
            {
                UUID currentUUID = p.getUniqueId();
                if (FireFighterClass.PlayerContribution.get(currentUUID) == null)
                {
                    continue;
                }
                if (FireFighterClass.PlayerContribution.get(currentUUID) == 0)
                {
                    continue;
                }
                if (FireFighterClass.PlayerContribution.get(bestPlayer) != null)
                {
                    if (FireFighterClass.PlayerContribution.get(bestPlayer) < FireFighterClass.PlayerContribution.get(currentUUID))
                    {
                        bestPlayer = currentUUID;
                    }
                } else if (FireFighterClass.PlayerContribution.get(currentUUID) > 0)
                {
                    bestPlayer = currentUUID;
                }
            }
            //giving the best player the selected reward
            if (bestPlayer != null)
            {
                Bukkit.getPlayer(bestPlayer).getInventory().addItem(reward);
                Bukkit.getPlayer(bestPlayer).sendMessage(ChatColor.translateAlternateColorCodes('&', FireFighterClass.messages.formattedMessage("§a", "received_reward")));
            } else
            {
                FireFighterClass.console.info("No one contributed to the mission!");
            }
        }
    }

    private void turnOffInstructions()
    {
        FireFighterClass.console.info("Mission ended");
        giveRewards();
        FireFighterClass.startedMission = false;
        FireFighterClass.missionName = "";
        setOnFire.clear();
        FireFighterClass.PlayerContribution.clear();
    }*/
}