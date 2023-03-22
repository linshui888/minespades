package me.nologic.minespades.game;

import me.nologic.minespades.battleground.Battleground;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class GameMaster {

    private final HashMap<String, Battleground> enabledBattlegrounds;

    public GameMaster() {
        this.enabledBattlegrounds = new HashMap<>();
    }

    public void addBattleground(Battleground battleground) {
        this.enabledBattlegrounds.put(battleground.getName(), battleground);
        battleground.setLaunched(true);
    }

    public List<String> getEnabledBattlegrounds() {
        return enabledBattlegrounds.keySet().stream().toList();
    }

    public void connect(Player player, String name) {
        try {
            Battleground battleground = this.enabledBattlegrounds.get(name);
            if (battleground.isConnectable()) {
                battleground.connect(player);
            } else player.sendMessage("Не удалось подключиться к арене.");
        } catch (NullPointerException ex) {
            player.sendMessage("§4Арены с названием " + name + " не существует!");
            Bukkit.getLogger().warning(String.format("Игрок %s попытался подключиться к несуществующей арене: %s.", player.getName(), name));
        }
    }

}