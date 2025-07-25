package com.github.tommyt0mmy.firefighter.events;

import com.github.tommyt0mmy.firefighter.FireFighter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FireMaskActivation implements Listener {

    @EventHandler
    public void onClick(PlayerInteractEvent e){
        Player player = e.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if(hand.isSimilar(FireFighter.getInstance().getFireMask())) {
                hand.setAmount(hand.getAmount() - 1);
                player.getInventory().setHelmet(FireFighter.getInstance().getFireMask());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();

        if(e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK){
            if(player.getInventory().getHelmet().isSimilar(FireFighter.getInstance().getFireMask())){
                e.setCancelled(true);
            }
        }
    }
}
