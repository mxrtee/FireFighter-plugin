package com.github.tommyt0mmy.firefighter.events;

import com.github.tommyt0mmy.firefighter.FireFighter;
import com.github.tommyt0mmy.firefighter.utility.Permissions;
import com.github.tommyt0mmy.firefighter.utility.XMaterial;
import com.github.tommyt0mmy.firefighter.utility.XSound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireExtinguisherActivation implements Listener
{

    private FireFighter FireFighterClass = FireFighter.getInstance();

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        try
        {
            Player p = e.getPlayer();
            Action action = e.getAction();
            ItemStack item = e.getItem();
            if (isFireExtinguisher(item))
            {
                e.setCancelled(true);
            } else
            {
                return;
            }
            if (!(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) return;

            if (!p.hasPermission(Permissions.USE_EXTINGUISHER.getNode()))
            {
                p.sendMessage(FireFighterClass.messages.formattedMessage("§c", "invalid_permissions"));
                return;
            }

            //durability
            if (!p.hasPermission(Permissions.FREEZE_EXTINGUISHER.getNode()))
            {
                item.setDurability((short) (item.getDurability() + 1));
                if (item.getDurability() > 249)
                {
                    e.setCancelled(true);
                    p.getInventory().remove(item);
                    XSound.ENTITY_ITEM_BREAK.playSound(p, 5, 0);
                    return;
                }
            }

            //particle effects and turning off fire
            new BukkitRunnable() {
                Location loc = p.getLocation();
                Vector direction = loc.getDirection().normalize();
                double timer = 0;

                @Override
                public void run() {
                    timer++;
                    boolean playExtinguishingSound = false;

                    // Calcolo il punto di emissione lungo la direzione di tiro
                    double x = direction.getX() * timer;
                    double y = direction.getY() * timer + 1.4;
                    double z = direction.getZ() * timer;
                    loc.add(x, y, z);

                    // Particelle di "fumo"
                    showParticle(loc, Particle.CLOUD, (int)(timer * 3.0), 0);

                    // --- ESTINZIONE IN AREE 5x5 ---
                    // Scorro dx e dz da -2 a +2 per coprire 5 blocchi per lato
                    for (int dx = -3; dx <= 3; dx++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            Location loc2 = loc.clone().add(dx, 0, dz);
                            Block block = loc2.getBlock();
                            if (block.getType() == XMaterial.FIRE.parseMaterial()) {
                                block.setType(Material.AIR);
                                playExtinguishingSound = true;
                            }
                        }
                    }

                    // Suono di estinzione (una sola volta per tick)
                    if (playExtinguishingSound) {
                        XSound.BLOCK_FIRE_EXTINGUISH.playSound(p, 1, 0);
                    }

                    // Ripristino posizione di loc per il prossimo tick
                    loc.subtract(x, y, z);

                    // Durata massima del getto: 10 tick (~0.5s)
                    if (timer > 9) {
                        this.cancel();
                    }
                }
            }.runTaskTimer(FireFighterClass, 0, 1);
            //sound
            new BukkitRunnable()
            {
                int t = 0;

                public void run()
                {
                    t++;
                    XSound.BLOCK_WOOL_STEP.playSound(p, 3, 0);
                    XSound.BLOCK_SAND_PLACE.playSound(p, 3, 0);
                    if (t > 3)
                    {
                        this.cancel();
                    }
                }
            }.runTaskTimer(FireFighterClass, 0, 1);

        } catch (Exception E)
        {
            E.printStackTrace();
        }
    }

    private boolean isFireExtinguisher(ItemStack item)
    {
        if (item == null)
        {
            return false;
        }
        if (!item.hasItemMeta())
        {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (item.getType() != Material.STICK)
        {
            return false;
        }
        if (!meta.hasLore() && !meta.hasCustomModelData())
        {
            return false;
        }
        return meta.getCustomModelData() == 78;
    }

    private void showParticle(Location loc, Particle particle, int count, int offsetXZ)
    {
        World w = loc.getWorld();
        w.spawnParticle(particle, loc, count, offsetXZ, 0, offsetXZ, 0);
    }
}