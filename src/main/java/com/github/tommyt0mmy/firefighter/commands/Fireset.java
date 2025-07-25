package com.github.tommyt0mmy.firefighter.commands;

import com.github.tommyt0mmy.firefighter.FireFighter;
import com.github.tommyt0mmy.firefighter.utility.Permissions;
import com.github.tommyt0mmy.firefighter.utility.TitleActionBarUtil;
import com.github.tommyt0mmy.firefighter.utility.XMaterial;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Fireset implements CommandExecutor
{

    private FireFighter FireFighterClass = FireFighter.getInstance();
    private final Map<String, Integer> initialCount = new ConcurrentHashMap<>();
    private final Map<String, Set<Block>> currentFires = new ConcurrentHashMap<>();
    private Map<String, Set<BlockVector3>> firePositions = new ConcurrentHashMap<>();
    private Map<String, BossBar> bossBars = new ConcurrentHashMap<>();
    private BukkitRunnable missionTask;
    private String activeMissionId = null;

    public ProtectedRegion getPlayerRegion(Player player) {
        com.sk89q.worldedit.entity.Player wrappedPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet applicableRegionSet = query.getApplicableRegions(wrappedPlayer.getLocation());
        Iterator iterator = applicableRegionSet.iterator();
        if (iterator.hasNext()) {
            ProtectedRegion region = (ProtectedRegion) iterator.next();
            return region;
        }
        return null;
    }

    private String getUsage()
    { //TODO Change method
        return ((String) FireFighterClass.getDescription().getCommands().get("fireset").get("usage")).replaceAll("<command>", "fireset");
    }

    @Override
    public boolean onCommand(CommandSender Sender, Command cmd, String label, String[] args)
    {


        if (!(Sender.hasPermission(Permissions.FIRESET.getNode()) || Sender.isOp()))
        {
            Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "invalid_permissions"));
            return true;
        }

        FireFighterClass.configs.loadConfigs();

        ItemStack wand = FireFighterClass.configs.getConfig().getItemStack("fireset.wand");

        if (args.length == 0)
        { //giving the wand
            if(!(Sender instanceof Player)) return true;
            Player p = (Player) Sender;
            p.getInventory().addItem(wand);
            p.sendMessage(FireFighterClass.messages.formattedMessage("§e", "fireset_wand_instructions"));
        } else
        {
            command_switch:
            switch (args[0])
            {


                case "startmission":
                    if (!Sender.hasPermission(Permissions.START_MISSION.getNode()))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "invalid_permissions"));
                    }
                    //arguments check
                    if (args.length != 2)
                    {
                        Sender.sendMessage(getUsage());
                        return true;
                    }
                    String missionId = args[1];
                    FileConfiguration cfg = FireFighter.getInstance().configs.getConfig();
                    World world = Bukkit.getWorld("Napoli");
                    ProtectedRegion region = WorldGuard.getInstance()
                            .getPlatform().getRegionContainer()
                            .get(BukkitAdapter.adapt(world))
                            .getRegion(cfg.getString("missions." + missionId + ".region"));
                    if (region == null) {
                        Sender.sendMessage("§cWorld or region not found for mission " + missionId);
                        return true;
                    }

                    // Cancel any existing task and bossbar
                    if (missionTask != null) {
                        missionTask.cancel();
                    }
                    BossBar oldBar = bossBars.remove(missionId);
                    if (oldBar != null) {
                        oldBar.removeAll();
                    }

                    // Create new BossBar
                    BossBar bossBar = Bukkit.createBossBar(
                            "§aStatus incendio",
                            BarColor.GREEN,
                            BarStyle.SOLID
                    );
                    bossBars.put(missionId, bossBar);
                    activeMissionId = missionId;

                    missionTask = new BukkitRunnable() {
                        private boolean firstRun = true;

                        @Override
                        public void run() {
                            if (firstRun) {
                                Set<BlockVector3> positions = new HashSet<>();
                                BlockVector3 min = region.getMinimumPoint();
                                BlockVector3 max = region.getMaximumPoint();
                                for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                                    for (int y = min.getBlockY(); y < max.getBlockY(); y++) {
                                        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                                            Block below = world.getBlockAt(x, y, z);
                                            if (!below.getType().isSolid()) continue;
                                            Block above = world.getBlockAt(x, y + 1, z);
                                            if (above.getType() == Material.AIR) {
                                                above.setType(Material.FIRE);
                                                positions.add(BlockVector3.at(x, y + 1, z));
                                            }
                                        }
                                    }
                                }
                                firePositions.put(missionId, positions);
                                initialCount.put(missionId, positions.size());
                                // Add players in region to bossbar
                                for (Player p : world.getPlayers()) {
                                    BlockVector3 loc = BlockVector3.at(
                                            p.getLocation().getBlockX(),
                                            p.getLocation().getBlockY(),
                                            p.getLocation().getBlockZ()
                                    );
                                    if (region.contains(loc)) {
                                        bossBar.addPlayer(p);
                                    }
                                }
                                firstRun = false;
                            }

                            Set<BlockVector3> positions = firePositions.get(missionId);
                            int initial = initialCount.getOrDefault(missionId, 0);
                            int remaining = 0;
                            for (BlockVector3 vec : positions) {
                                if (world.getBlockAt(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()).getType() == Material.FIRE) {
                                    remaining++;
                                }
                            }

                            // Update bossbar progress
                            double progress = initial > 0 ? (double) remaining / initial : 1.0;
                            bossBar.setProgress(Math.max(0, Math.min(progress, 1)));


                            List<Player> toRemove = new ArrayList<>();
                            for (Player p : bossBar.getPlayers()) {
                                BlockVector3 loc = BlockVector3.at(
                                        p.getLocation().getBlockX(),
                                        p.getLocation().getBlockY(),
                                        p.getLocation().getBlockZ()
                                );
                                if (!region.contains(loc)) {
                                    toRemove.add(p);
                                }
                            }
                            for (Player p : toRemove) {
                                bossBar.removePlayer(p);
                            }
                            // Add new entrants
                            for (Player p : world.getPlayers()) {
                                BlockVector3 loc = BlockVector3.at(
                                        p.getLocation().getBlockX(),
                                        p.getLocation().getBlockY(),
                                        p.getLocation().getBlockZ()
                                );
                                if (region.contains(loc) && !bossBar.getPlayers().contains(p)) {
                                    bossBar.addPlayer(p);
                                }
                            }

                            // End mission when no fires remain
                            if (remaining == 0) {
                                cancel();
                                bossBar.removeAll();
                                firePositions.remove(missionId);
                                initialCount.remove(missionId);
                                bossBars.remove(missionId);
                                giveRewards(missionId, world);
                                Sender.sendMessage("§aMission " + missionId + " completed!");
                            }
                        }
                    };
                    // Schedule task every second
                    missionTask.runTaskTimer(FireFighter.getInstance(), 0L, 20L);
                    Sender.sendMessage("§aMission " + missionId + " started!");
                    break;


                case "missions": ///MISSIONS LIST///
                    if (!(Sender instanceof Player))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("", "only_players_command"));
                        return true;
                    }
                    //Page selection
                    int page = 1, count = 0;
                    if (args.length == 2)
                    {
                        if (args[1].matches("\\d+"))
                        {
                            page = Integer.parseInt(args[1]);
                        } else
                        {
                            Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "page_not_found"));
                            return true;
                        }
                    } else if (args.length > 2)
                    {
                        Sender.sendMessage(getUsage());
                    }
                    //two missions per page
                    Set<String> missions = new TreeSet<>();
                    missions = FireFighterClass.configs.getConfig().getConfigurationSection("missions").getKeys(false);
                    if (missions.size() < (page * 2) - 1 || page * 2 <= 0)
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "page_not_found"));
                        return true;
                    }
                    Sender.sendMessage(ChatColor.translateAlternateColorCodes('&', FireFighterClass.messages.getMessage("fireset_missions_header")));
                    for (String curr : missions)
                    {
                        count++;
                        if (count == (page * 2) - 1 || count == page * 2)
                        {
                            ConfigurationSection missionSection = FireFighterClass.configs.getConfig().getConfigurationSection("missions." + curr);
                            Player p = (Player) Sender;
                            Sender.sendMessage(ChatColor.translateAlternateColorCodes('&', FireFighterClass.messages.getMessage("fireset_missions_name").replaceAll("<mission>", curr)));
                            Sender.sendMessage(ChatColor.translateAlternateColorCodes('&', FireFighterClass.messages.getMessage("fireset_missions_position")
                                    .replaceAll("<id>",WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld())).getRegion(missionSection.getString("region")).getId())));
                        }
                    }
                    Sender.sendMessage(ChatColor.translateAlternateColorCodes('&', FireFighterClass.messages.getMessage("fireset_missions_footer")
                            .replaceAll("<current page>", String.valueOf(page))
                            .replaceAll("<total>", String.valueOf((missions.size() + 1) / 2))));
                    break;


                case "deletemission": ///DELETE MISSION///
                    if (!(Sender instanceof Player))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("", "only_players_command"));
                        return true;
                    }
                    if (args.length != 2)
                    {
                        Sender.sendMessage(getUsage());
                        break;
                    }
                    if (existsMission(args[1]))
                    {
                        FireFighterClass.configs.set("missions." + args[1], null); //removes the path
                        FireFighterClass.configs.saveToFile();
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§a", "fireset_delete"));
                    } else
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "fireset_mission_not_found"));
                    }
                    break;


                case "editmission": ///EDIT MISSION///
                    if (!(Sender instanceof Player))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("", "only_players_command"));
                        return true;
                    }
                    if (args.length < 3)
                    {
                        Sender.sendMessage(getUsage());
                        break;
                    }
                    if (!existsMission(args[1]))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "fireset_mission_not_found"));
                        break;
                    }
                    switch (args[2])
                    {
                        case "name":  //editing mission's name
                            if (args.length < 4)
                            {
                                Sender.sendMessage(getUsage());
                                break command_switch;
                            }
                            String newName = args[3];
                            FireFighterClass.configs.loadConfigs();
                            MemorySection mission = (MemorySection) FireFighterClass.configs.getConfig().get("missions." + args[1]);
                            FireFighterClass.configs.set("missions." + args[1], null); //removes the old mission
                            FireFighterClass.configs.set("missions." + newName, mission); //puts the new mission

                            if (!(FireFighterClass.configs.saveToFile())) return false; //error on saving

                            break;
                        case "description":  //editing mission's description
                            if (args.length < 4)
                            {
                                Sender.sendMessage(getUsage());
                                break command_switch;
                            }

                            StringBuilder newDescription = new StringBuilder();
                            for (int i = 3; i < args.length; i++)
                            {
                                newDescription.append(args[i]).append(" ");
                            }
                            FireFighterClass.configs.set("missions." + args[1] + ".description", newDescription.toString());
                            if (!(FireFighterClass.configs.saveToFile())) return false; //error on saving

                            break;
                        case "rewards":  //editing mission's rewards
                            if (!Sender.hasPermission(Permissions.SET_REWARDS.getNode()))
                            { //invalid permissions
                                Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "invalid_permissions"));
                                return true;
                            }
                            Player player = (Player) Sender;
                            openRewardsGUI(args[1], player);

                            break;
                        default:
                            Sender.sendMessage(getUsage());
                            break command_switch;
                    }
                    break;


                case "addmission": ///ADD MISSION///
                    if (!(Sender instanceof Player))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("", "only_players_command"));
                        return true;
                    }
                    if (args.length < 2)
                    {
                        Sender.sendMessage(getUsage());
                        break;
                    }
                    Player p = (Player) Sender;
                    if(getPlayerRegion(p) == null) return true;
                        FireFighterClass.configs.set("missions." + args[1] + ".region", getPlayerRegion(p).getId());
                        if (args.length >= 3)
                        {
                            StringBuilder description = new StringBuilder();
                            for (int i = 2; i < args.length; i++)
                            {
                                description.append(args[i]).append(" ");
                            }
                            FireFighterClass.configs.set("missions." + args[1] + ".description", description.toString());
                        } else
                        {
                            FireFighterClass.configs.set("missions." + args[1] + ".description", ChatColor.RED + "Fire at: " + args[1]);
                        }
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§a", "fireset_added_mission"));
                        FireFighterClass.configs.saveToFile();

                    break;
                case "setwand":
                    if (!(Sender instanceof Player))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("", "only_players_command"));
                        return true;
                    }
                    if (args.length != 1)
                    {
                        Sender.sendMessage(getUsage());
                        break;
                    }
                    Player player = (Player) Sender;
                    ItemStack newWand = player.getInventory().getItemInMainHand();
                    if (newWand.getType() != Material.AIR)
                    { //checks if the player has something in his hand
                        newWand.setAmount(1);
                        FireFighterClass.configs.set("fireset.wand", newWand);
                        FireFighterClass.configs.saveToFile();
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§a", "fireset_wand_set"));
                    }
                    break;
                case "endmission":
                    if (!Sender.hasPermission(Permissions.START_MISSION.getNode()))
                    {
                        Sender.sendMessage(FireFighterClass.messages.formattedMessage("§c", "invalid_permissions"));
                    }
                    if(args.length == 1){
                        Sender.sendMessage("§cSpecifica il tipo di missione");
                        break;
                    }
                    String mission = args[1];
                    endMission(Sender, mission);
                    break;

                default:
                    Sender.sendMessage(getUsage());
                    break;
            }
        }
        return true;
    }
    private void endMission(CommandSender sender, String missionId) {
        if (activeMissionId == null || !activeMissionId.equals(missionId)) {
            sender.sendMessage("§cNo active mission " + missionId + " to end.");
            return;
        }
        sender.sendMessage("§eForce stopping mission " + missionId + "...");
        stopMission();
    }


    private boolean existsMission(String name)
    {
        if (FireFighterClass.configs.getConfig().contains("missions." + name))
        {
            return true;
        }
        return false;
    }

    private void openRewardsGUI(String missionName, Player inventoryOwner)
    {
        //reading rewards informations from config.yml
        List<ItemStack> inventoryContent = new ArrayList<ItemStack>();
        String rewardsPath = "missions." + missionName + ".rewards";
        int Size = 9;
        String title = ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Rewards - " + missionName;
        if (FireFighterClass.configs.getConfig().get(rewardsPath) != null)
        { //if there are rewards set
            int rewardsCount = FireFighterClass.configs.getConfig().getInt(rewardsPath + ".size");
            Size = (rewardsCount / 9 + 1) * 9;
            for (int i = 0; i < rewardsCount; i++)
            {
                ItemStack tmp = FireFighterClass.configs.getConfig().getItemStack(rewardsPath + "." + i);
                inventoryContent.add(tmp);
            }
        } else
        {
            FireFighterClass.configs.set(rewardsPath + ".size", "0");
        }
        //initializing GUI
        Inventory GUI = Bukkit.createInventory(inventoryOwner, Size + 9, title);
        for (int i = 0; i < inventoryContent.size(); i++)
        {
            ItemStack tmp = inventoryContent.get(i);
            GUI.setItem(i, tmp);
        }
        ItemStack item1 = XMaterial.LIGHT_GRAY_STAINED_GLASS_PANE.parseItem(); //void part of the footer
        ItemMeta im1 = item1.getItemMeta();
        im1.setDisplayName("§r");
        item1.setItemMeta(im1);
        ItemStack item2 = XMaterial.LIME_STAINED_GLASS_PANE.parseItem(); //'add a line' button
        ItemMeta im2 = item2.getItemMeta();
        im2.setDisplayName(ChatColor.GREEN + "Add a line");
        item2.setItemMeta(im2);
        ItemStack item3 = XMaterial.RED_STAINED_GLASS_PANE.parseItem(); //'remove a line' button
        ItemMeta im3 = item3.getItemMeta();
        im3.setDisplayName(ChatColor.RED + "Remove a line");
        item3.setItemMeta(im3);
        ItemStack item4 = XMaterial.LIME_STAINED_GLASS.parseItem(); //'save changes' button
        ItemMeta im4 = item4.getItemMeta();
        im4.setDisplayName(ChatColor.GREEN + "Save changes");
        item4.setItemMeta(im4);
        //placing the footer in the inventory
        GUI.setItem(Size, item1);
        GUI.setItem(Size + 1, item1);
        GUI.setItem(Size + 2, item1);
        GUI.setItem(Size + 3, item2);
        GUI.setItem(Size + 4, item1);
        GUI.setItem(Size + 5, item3);
        GUI.setItem(Size + 6, item1);
        GUI.setItem(Size + 7, item1);
        GUI.setItem(Size + 8, item4);
        //opening GUI
        inventoryOwner.openInventory(GUI);
    }

    private void sendHotbar(Player p, String msg) {
        TitleActionBarUtil.sendActionBarMessage(p, msg);
    }

    private void giveRewards(String missionId, World world) {
        // implement reward logic from original giveRewards()
    }
    private void stopMission() {
        if (missionTask != null) {
            missionTask.cancel();
            missionTask = null;
        }
        if (activeMissionId != null) {
            // Cleanup fires
            Set<BlockVector3> positions = firePositions.remove(activeMissionId);
            if (positions != null) {
                for (BlockVector3 vec : positions) {
                    World world = Bukkit.getWorlds().get(0);
                    Block b = world.getBlockAt(vec.getBlockX(), vec.getBlockY(), vec.getBlockZ());
                    if (b.getType() == Material.FIRE) {
                        b.setType(Material.AIR);
                    }
                }
            }
            // Remove bossbar
            BossBar bar = bossBars.remove(activeMissionId);
            if (bar != null) bar.removeAll();
            initialCount.remove(activeMissionId);
            activeMissionId = null;
        }
    }


}

