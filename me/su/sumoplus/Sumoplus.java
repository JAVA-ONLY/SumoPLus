package me.su.sumoplus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Sumoplus extends JavaPlugin implements Listener {
  private static class Arena {
    Location pos1;
    
    Location pos2;
    
    Location spawn1;
    
    Location spawn2;
    
    private Arena() {}
    
    boolean active = false;
  }
  
  private final Map<String, Arena> arenas = new HashMap<>();
  
  private final Set<UUID> queuedPlayers = new HashSet<>();
  
  private final Map<UUID, String> playerArena = new HashMap<>();
  
  private final Set<UUID> inGamePlayers = new HashSet<>();
  
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, (Plugin)this);
    getLogger().info("SumoPlugin has been enabled.");
  }
  
  public void onDisable() {
    getLogger().info("SumoPlugin has been disabled.");
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("This command can only be run by a player.");
      return true;
    } 
    Player player = (Player)sender;
    if (cmd.getName().equalsIgnoreCase("sumo")) {
      if (args.length == 0) {
        player.sendMessage(ChatColor.RED + "Usage: /sumo <create|set|delete|join|leave>");
        return true;
      } 
      String subcommand = args[0].toLowerCase();
      if (subcommand.equals("create") && args.length == 2) {
        String arenaName = args[1];
        if (this.arenas.containsKey(arenaName)) {
          player.sendMessage(ChatColor.RED + "Arena already exists.");
        } else {
          this.arenas.put(arenaName, new Arena());
          player.sendMessage(ChatColor.GREEN + "Arena " + arenaName + " created.");
        } 
      } else if (subcommand.equals("set") && args.length == 3) {
        String option = args[1].toLowerCase();
        String arenaName = args[2];
        Arena arena = this.arenas.get(arenaName);
        if (arena == null) {
          player.sendMessage(ChatColor.RED + "Arena not found.");
          return true;
        } 
        if (option.equals("pos1")) {
          arena.pos1 = player.getLocation();
          player.sendMessage(ChatColor.GREEN + "Position 1 set for arena " + arenaName);
        } else if (option.equals("pos2")) {
          arena.pos2 = player.getLocation();
          player.sendMessage(ChatColor.GREEN + "Position 2 set for arena " + arenaName);
        } else if (option.equals("playerspawn1")) {
          arena.spawn1 = player.getLocation();
          player.sendMessage(ChatColor.GREEN + "Player spawn 1 set for arena " + arenaName);
        } else if (option.equals("playerspawn2")) {
          arena.spawn2 = player.getLocation();
          player.sendMessage(ChatColor.GREEN + "Player spawn 2 set for arena " + arenaName);
        } else {
          player.sendMessage(ChatColor.RED + "Unknown set option.");
        } 
      } else if (subcommand.equals("delete") && args.length == 2) {
        String arenaName = args[1];
        if (this.arenas.remove(arenaName) != null) {
          player.sendMessage(ChatColor.GREEN + "Arena " + arenaName + " deleted.");
        } else {
          player.sendMessage(ChatColor.RED + "Arena not found.");
        } 
      } else if (subcommand.equals("join")) {
        if (this.playerArena.containsKey(player.getUniqueId())) {
          player.sendMessage(ChatColor.RED + "You are already in a game.");
          return true;
        } 
        this.queuedPlayers.add(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "You have joined the Sumo queue.");
        if (this.queuedPlayers.size() >= 2)
          startGame(); 
      } else if (subcommand.equals("leave")) {
        if (this.queuedPlayers.remove(player.getUniqueId())) {
          player.sendMessage(ChatColor.GREEN + "You have left the Sumo queue.");
        } else if (this.playerArena.containsKey(player.getUniqueId())) {
          player.sendMessage(ChatColor.RED + "You cannot leave the game.");
        } else {
          player.sendMessage(ChatColor.RED + "You are not in a queue or game.");
        } 
      } else {
        player.sendMessage(ChatColor.RED + "Unknown subcommand.");
      } 
      return true;
    } 
    return false;
  }
  
  private void startGame() {
    UUID[] players = this.queuedPlayers.<UUID>toArray(new UUID[0]);
    final UUID player1 = players[0];
    final UUID player2 = players[1];
    this.queuedPlayers.clear();
    final Player p1 = Bukkit.getPlayer(player1);
    final Player p2 = Bukkit.getPlayer(player2);
    if (p1 == null || p2 == null) {
      if (p1 != null)
        this.queuedPlayers.add(player1); 
      if (p2 != null)
        this.queuedPlayers.add(player2); 
      return;
    } 
    Arena arena = findAvailableArena();
    if (arena == null || arena.spawn1 == null || arena.spawn2 == null || arena.pos1 == null || arena.pos2 == null) {
      p1.sendMessage(ChatColor.RED + "No available arenas or arena not fully configured.");
      p2.sendMessage(ChatColor.RED + "No available arenas or arena not fully configured.");
      return;
    } 
    arena.active = true;
    this.playerArena.put(player1, arena.toString());
    this.playerArena.put(player2, arena.toString());
    this.inGamePlayers.add(player1);
    this.inGamePlayers.add(player2);
    p1.teleport(arena.spawn1);
    p2.teleport(arena.spawn2);
    (new BukkitRunnable() {
        int countdown = 3;
        
        public void run() {
          if (this.countdown > 0) {
            p1.sendMessage(ChatColor.YELLOW + "Game starts in " + this.countdown + "...");
            p2.sendMessage(ChatColor.YELLOW + "Game starts in " + this.countdown + "...");
            this.countdown--;
          } else {
            Sumoplus.this.inGamePlayers.add(player1);
            Sumoplus.this.inGamePlayers.add(player2);
            p1.sendMessage(ChatColor.GREEN + "Go!");
            p2.sendMessage(ChatColor.GREEN + "Go!");
            cancel();
          } 
        }
      }).runTaskTimer((Plugin)this, 0L, 20L);
  }
  
  private Arena findAvailableArena() {
    return this.arenas.values().stream().filter(arena -> !arena.active).findFirst().orElse(null);
  }
  
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if (!this.inGamePlayers.contains(player.getUniqueId()))
      return; 
    Arena arena = this.arenas.get(this.playerArena.get(player.getUniqueId()));
    if (arena == null || arena.pos1 == null || arena.pos2 == null)
      return; 
    Location to = event.getTo();
    if (to == null)
      return; 
    World world = player.getWorld();
    if (!isInsideArena(to, arena.pos1, arena.pos2))
      handleWin(player); 
  }
  
  private boolean isInsideArena(Location loc, Location pos1, Location pos2) {
    return (loc.getX() >= Math.min(pos1.getX(), pos2.getX()) && loc
      .getX() <= Math.max(pos1.getX(), pos2.getX()) && loc
      .getZ() >= Math.min(pos1.getZ(), pos2.getZ()) && loc
      .getZ() <= Math.max(pos1.getZ(), pos2.getZ()) && loc
      .getY() >= Math.min(pos1.getY(), pos2.getY()) && loc
      .getY() <= Math.max(pos1.getY(), pos2.getY()));
  }
  
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (this.inGamePlayers.remove(player.getUniqueId()))
      handleWin(player); 
  }
  
  private void handleWin(Player loser) {
    UUID winnerUUID = this.inGamePlayers.stream().filter(uuid -> !uuid.equals(loser.getUniqueId())).findFirst().orElse(null);
    if (winnerUUID == null)
      return; 
    Player winner = Bukkit.getPlayer(winnerUUID);
    if (winner != null) {
      winner.sendMessage(ChatColor.GREEN + "You won the Sumo game!");
      loser.sendMessage(ChatColor.RED + "You lost the Sumo game.");
    } 
    this.inGamePlayers.clear();
    this.playerArena.remove(loser.getUniqueId());
    this.playerArena.remove(winnerUUID);
  }
}
