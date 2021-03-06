/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.papaharni.happychest.events;

import de.papaharni.happychest.HappyChest;
import de.papaharni.happychest.utils.Arena;
import de.papaharni.happychest.utils.Items;
import de.papaharni.happychest.utils.Utils;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryOpen implements Listener {
    
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        Player p = (Player)e.getPlayer();
        if(p == null || !e.getView().getType().equals(InventoryType.CHEST)) {
            return;
        }
        
        Location loc = null;
        if(e.getInventory().getHolder() instanceof DoubleChest)
            loc = ((DoubleChest)e.getInventory().getHolder()).getLocation();
        else if(e.getInventory().getHolder() instanceof Chest)
            loc = ((Chest)e.getInventory().getHolder()).getLocation();
        
        if(loc != null) {
            //ArenaName, Chest Location der laufenden Arena
            for(Map.Entry<String, Location> en: HappyChest.getInstance().getCurChests().entrySet()) {
                //Hole Arena
                Arena a = HappyChest.getInstance().getArena(en.getKey());
                //Ist Location nicht in der Arena dann nächstes Object
                if(!a.isInside(loc))
                    continue;

                //Ist die Instance der Truhen Location zur Runden Location grösser 1 dann abbrechen
                if(en.getValue().distance(loc) > 1.0) {
                    Utils.sendMessage(p, "&eSchade , das ist leider nicht die Schatztruhe.");
                    return;
                }
                
                //Ist der Truhen Inhalt nur einmal verfügbar?
                if(a.getOneForAll()) {
                    //ja
                    Utils.sendMessage(p, "&eGlückwunsch , du hast die richtige Truhe gefunden.");
                    if(Utils.countItems(e.getInventory().getContents()) < 1) {
                        Utils.sendMessage(p, "&eLeider wurde die Kiste in dieser Runde schon geplündert.");
                    }
                    return;
                }
                //Nein und weiter

                //War die Person noch nicht an der Truhe?
                if(!HappyChest.getInstance().getUsedPlayersList(en.getKey()).contains(p.getName())) {
                    //Hat die Person dabei reste hinterlassen?
                    if(HappyChest.getInstance().hasReste(en.getKey(), p.getName())) {
                        //Ja gebe Reste in die Truhe
                        Utils.sendMessage(p, "&eHier ist das was du noch in der Truhe vergessen hast. Bitte nimm es raus bevor es weg ist.");
                        e.getInventory().setContents(HappyChest.getInstance().getReste(en.getKey(), p.getName()));
                        HappyChest.getInstance().delResteByPlayer(en.getKey(), p.getName());
                        return;
                    } else {
                        //Nein , lege Runden belohnung ins Inventar.
                        List<String> list = HappyChest.getInstance().getRoundRewards(en.getKey());
                        ItemStack[] items = new ItemStack[list.size()];
                        int i = 0;
                        for(String str: list) {
                            ItemStack item = Items.getItem(str);
                            if(item == null)
                                continue;
                            items[i] = item;
                            i++;
                        }
                        Utils.sendMessage(p, "&eGlückwunsch , du hast die richtige Truhe gefunden.");
                        e.getInventory().setContents(items);
                        return;
                    }
                }
                //Person war schon dran, zeige eine Leere Truhe
                Utils.sendMessage(p, "&eOh du hast die Truhe bereits geplündert. Versuchs in der nächste Runde noch einmal.");
                e.setCancelled(true);
                ItemStack[] items = new ItemStack[e.getInventory().getSize()];
                e.getInventory().setContents(items);
                return;
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player)e.getPlayer();
        if(p == null || !e.getView().getType().equals(InventoryType.CHEST)) {
            return;
        }
        
        Location loc = null;

        if(e.getInventory().getHolder() instanceof DoubleChest)
            loc = ((DoubleChest)e.getInventory().getHolder()).getLocation();
        else if(e.getInventory().getHolder() instanceof Chest)
            loc = ((Chest)e.getInventory().getHolder()).getLocation();
            
        for(Map.Entry<String, Location> b: HappyChest.getInstance().getCurChests().entrySet()) {
            Arena a = HappyChest.getInstance().getArena(b.getKey());
            if(!a.isInside(loc))
                continue;
            
            if(b.getValue().distance(loc) > 1.0)
                return;
            
            if(a.getOneForAll())
                return;
            
            if(Utils.countItems(e.getInventory().getContents()) > 0) {
                HappyChest.getInstance().setReste(b.getKey(), p.getName(), e.getInventory().getContents());
                HappyChest.getInstance().getUsedPlayersList(b.getKey()).remove(p.getName());
            } else {
                HappyChest.getInstance().getUsedPlayersList(b.getKey()).add(p.getName());
            }
            return;
        }
    }
}
