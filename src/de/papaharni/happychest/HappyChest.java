package de.papaharni.happychest;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.papaharni.happychest.commands.hch;
import de.papaharni.happychest.events.InventoryOpen;
import de.papaharni.happychest.events.PlayerInteract;
import de.papaharni.happychest.utils.Arena;
import de.papaharni.happychest.utils.ArenaWorks;
import de.papaharni.happychest.utils.Blocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Pappi
 */
public class HappyChest extends JavaPlugin {
    
    private static HappyChest _instance;
    private static boolean _debugMode;
    
    //Runden Tasks Speicherung - Speichert den Task jeder Runde Map<Arenaname, Task>
    private final Map<String, BukkitTask> _areaTask = new HashMap<>();
    
    //Reste von Spielern in den Kisten Map<ArenaName,Map<Spielername, ItemStack Array>
    private final Map<String, HashMap<String, ItemStack[]>> _reste = new HashMap<>();
    
    //Arena / Aktuelle suchende Truhe Map<Arenaname, Location der Truhe>
    private final Map<String, Location> _curchests = new HashMap<>();
    
    //Arena / Liste der bereits gefundenen Spielernamen Map<Arenaname, List<Spielername>>
    private final Map<String, Collection<String>> _usedPlayers = new HashMap<>();
    
    //Arena / Liste der zu erhaltenden Items Map<Arenaname, List<Item String>>
    private final Map<String, List<String>> _roundRewards = new HashMap<>();
    
    
    //Zur Markierung falls kein WE vorhanden ist.
    private final List<String> _allowMark = new ArrayList<>();
    private final Map<String, Location> _playerMarksLeft = new HashMap<>();
    private final Map<String, Location> _playerMarksRight = new HashMap<>();
    
    //Arena Saver
    private final Map<String, Arena> _areas = new HashMap<>();
    
    //Löschungs Maps - Spielername / Arena - Spielername / Zeit
    private final Map<String, String> _remrequest = new HashMap<>();
    private final Map<String, Long> _remrequesttime = new HashMap<>();

    //Sonstiges - Externe Plugins
    private WorldGuardPlugin _wg;
    private static Economy economy = null;
    
    private Map<String, Enchantment> _ench = new HashMap<>();
    private static language _lang;
    
    @Override
    public void onEnable() {
        if(!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        _instance = this;
        _lang = new language();
        _wg = getWorldGuard();
        this.getCommand("hch").setExecutor(new hch(this));
        getServer().getPluginManager().registerEvents(new InventoryOpen(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteract(this), this);
        ArenaWorks.loadAreas();
        
        fillEnchantments();
    }
    
    @Override
    public void onDisable() {
        ArenaWorks.saveAreas();
    }
    
    public static HappyChest getInstance() {
        return _instance;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    private boolean setupEconomy() {
        RegisteredServiceProvider economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if(economyProvider != null)
            economy = (Economy)economyProvider.getProvider();
        return economy != null;
    }
    
    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }
        return (WorldGuardPlugin) plugin;
    }
    
    public WorldGuardPlugin getWG() {
        return _wg;
    }
    
    public static language getLang() {
        return _lang;
    }
         
    
    public boolean isAllowMarking(String p) {
        return _allowMark.contains(p)?true:false;
    }
    
    public void allowMarking(String p) {
        _allowMark.add(p);
    }
    
    public void denyMarking(String p) {
        _allowMark.remove(p);
    }
    
    public void setMarking(String p, String size, Location loc) {
        switch(size) {
            case "left":
                _playerMarksLeft.put(p, loc);
                break;
            case "right":
                _playerMarksRight.put(p, loc);
                break;
            default:
                break;
        }
    }
    
    public void delMarking(String p, String size) {
        switch(size) {
            case "left":
                _playerMarksLeft.remove(p);
                break;
            case "right":
                _playerMarksRight.remove(p);
                break;
            default:
                _playerMarksLeft.remove(p);
                _playerMarksRight.remove(p);
                break;
        }
    }
    
    public boolean isMarking(String p, String size) {
        switch(size) {
            case "left":
                return _playerMarksLeft.containsKey(p);
            case "right":
                return _playerMarksRight.containsKey(p);
            default:
                return false;
        }
    }
    
    public Location getMarking(String p, String size) {
        switch(size) {
            case "left":
                return _playerMarksLeft.get(p);
            case "right":
                return _playerMarksRight.get(p);
            default:
                return null;
        }
    }
    
    public void addArena(Arena a) {
        _areas.put(a.getName().toLowerCase(), a);
    }
    
    public boolean isArena(String a) {
        return _areas.containsKey(a.toLowerCase());
    }
    
    public Arena getArena(String a) {
        return isArena(a)?_areas.get(a.toLowerCase()):null;
    }
    
    public Map<String, Arena> getArenas() {
        return _areas;
    }
    
    public void delArena(String a) {
        _areas.remove(a.toLowerCase());
    }
    
    public void addRemRequest(String p, String a) {
        _remrequest.put(p.toLowerCase(), a.toLowerCase());
        _remrequesttime.put(p.toLowerCase(), System.currentTimeMillis());
    }
    
    public String getRemRequest(String p) {
        return (isRemRequest(p.toLowerCase()))?_remrequest.get(p.toLowerCase()):"";
    }
    
    public boolean isRemRequest(String p) {
        return _remrequest.containsKey(p.toLowerCase());
    }
    
    public void delRemRequest(String p) {
        _remrequest.remove(p.toLowerCase());
        _remrequesttime.remove(p.toLowerCase());
    }
    
    public Long getRemRequestTime(String p) {
        return (_remrequesttime.containsKey(p.toLowerCase()))?_remrequesttime.get(p.toLowerCase()):0;
    }
    
    public Map<String, Location> getCurChests() {
        return _curchests;
    }
    
    public Collection<String> getUsedPlayersList(String a) {
        if(!_usedPlayers.containsKey(a.toLowerCase()))
            addUsedPlayersList(a.toLowerCase());
        return _usedPlayers.get(a.toLowerCase());
    }
    
    public void addUsedPlayersList(String a) {
        List<String> list = new ArrayList<>();
        _usedPlayers.put(a.toLowerCase(), list);
    }
    
    public void delUsedPlayersList(String a) {
        _usedPlayers.remove(a.toLowerCase());
    }
    
    public List<String> getRoundRewards(String a) {
        if(!_roundRewards.containsKey(a.toLowerCase())) {
            List<String> list = new ArrayList<>();
            addRoundRewards(a.toLowerCase(), list);
        }
        return _roundRewards.get(a.toLowerCase());
    }
    
    public void addRoundRewards(String a, List<String> list) {
        _roundRewards.put(a.toLowerCase(), list);
    }
    
    public void delRoundRewards(String a) {
        _roundRewards.remove(a.toLowerCase());
    }
    
    public boolean hasReste(String a, String p) {
        if(!_reste.containsKey(a))
            return false;
        if(!_reste.get(a).containsKey(p))
            return false;
        return true;
    }
    
    public ItemStack[] getReste(String a, String p) {
        ItemStack[] item = new ItemStack[1];
        if(!hasReste(a, p))
            return item;
        return _reste.get(a).get(p);
    }
    
    public void setReste(String a, String p, ItemStack[] items) {
        if(!_reste.containsKey(a)) {
            HashMap<String, ItemStack[]> rest = new HashMap<>();
            _reste.put(a, rest);
        }
        _reste.get(a).put(p, items);
    }
    
    public void delResteByPlayer(String a, String p) {
        if(!_reste.containsKey(a))
            return;
        if(!_reste.get(a).containsKey(p))
            return;
        _reste.get(a).remove(p);
    }
    
    public void cancelArenaTask(String a) {
        if(_areaTask.containsKey(a)) {
            if(_areaTask.get(a) != null) {
                _areaTask.get(a).cancel();
            }
            _areaTask.remove(a);
        }
    }
    
    public boolean isArenaTask(String a) {
        return _areaTask.containsKey(a);
    }
    
    public void setArenaTask(String a, BukkitTask t) {
        cancelArenaTask(a);
        _areaTask.put(a, t);
    }
    
    public void clearOnRoundEnd(String a) {
        delUsedPlayersList(a);
        if(_reste.containsKey(a))
            _reste.remove(a);
    }
    
    public void clearOnEventEnd(String a) {
        clearOnRoundEnd(a);
        if(_roundRewards.containsKey(a))
            _roundRewards.remove(a);
        if(_curchests.containsKey(a)) {
            Blocks.clearChest(_curchests.get(a));
            _curchests.remove(a);
        }
        if(_areaTask.containsKey(a))
            cancelArenaTask(a);
    }
    
    public Map<String, Enchantment> getEnchants() {
        return _ench;
    }
    
    public void fillEnchantments() {
        _ench.put("power", Enchantment.ARROW_DAMAGE);
        _ench.put("flame", Enchantment.ARROW_FIRE);
        _ench.put("infinity", Enchantment.ARROW_INFINITE);
        _ench.put("punch", Enchantment.ARROW_KNOCKBACK);
        _ench.put("bane_of_arthropods", Enchantment.DAMAGE_ARTHROPODS);
        _ench.put("smite", Enchantment.DAMAGE_UNDEAD);
        _ench.put("efficiency", Enchantment.DIG_SPEED);
        _ench.put("unbreaking", Enchantment.DURABILITY);
        _ench.put("fire_aspect", Enchantment.FIRE_ASPECT);
        _ench.put("knockback", Enchantment.KNOCKBACK);
        _ench.put("fortune", Enchantment.LOOT_BONUS_BLOCKS);
        _ench.put("looting", Enchantment.LOOT_BONUS_MOBS);
        _ench.put("respiration", Enchantment.OXYGEN);
        _ench.put("protection", Enchantment.PROTECTION_ENVIRONMENTAL);
        _ench.put("blast_protection", Enchantment.PROTECTION_EXPLOSIONS);
        _ench.put("feather_falling", Enchantment.PROTECTION_FALL);
        _ench.put("fire_protection", Enchantment.PROTECTION_FIRE);
        _ench.put("projectile_protection", Enchantment.PROTECTION_PROJECTILE);
        _ench.put("silk_touch", Enchantment.SILK_TOUCH);
        _ench.put("thorns", Enchantment.THORNS);
        _ench.put("aqua_affinity", Enchantment.WATER_WORKER);
        _ench.put("sharpness", Enchantment.DAMAGE_ALL);
    }
}
