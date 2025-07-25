package com.github.tommyt0mmy.firefighter.events;

import com.github.tommyt0mmy.firefighter.FireFighter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class FireBootsActivation implements Listener {


    @EventHandler
    public void onUse(ArmorEquipEvent e){
        Player player = e.getPlayer();
        if(e.getItem().isSimilar(FireFighter.getInstance().getFireBoots())){
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE,14));
        }
    }

    @EventHandler
    public void onUnEquip(ArmorUnequipEvent e){
        Player player = e.getPlayer();
        if(e.getItem().isSimilar(FireFighter.getInstance().getFireBoots())) {
            if (player.hasPotionEffect(PotionEffectType.JUMP)) {
                player.removePotionEffect(PotionEffectType.JUMP);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();

        if(e.getCause() == EntityDamageEvent.DamageCause.FALL){
            if(player.getInventory().getBoots().isSimilar(FireFighter.getInstance().getFireBoots())){
                e.setCancelled(true);
            }
        }
    }

}
