package me.nologic.minespades;

import me.nologic.minespades.battleground.Battleground;
import me.nologic.minespades.battleground.editor.BattlegroundEditor;
import me.nologic.minespades.battleground.BattlegroundLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class BattlegroundManager {

    private final HashMap<String, Battleground> enabledBattlegrounds;

    private final BattlegroundEditor editor;
    private final BattlegroundLoader loader;

    private final Minespades plugin;

    public BattlegroundManager (Minespades plugin) {
        this.plugin = plugin;
        this.editor = new BattlegroundEditor(plugin);
        this.loader = new BattlegroundLoader(plugin);
        this.enabledBattlegrounds = new HashMap<>();
    }

    public BattlegroundEditor getEditor() {
        return editor;
    }

    public List<String> getEnabledBattlegrounds() {
        return enabledBattlegrounds.keySet().stream().toList();
    }

    public void enable(String name) {
        Battleground battleground = loader.load(name);
        this.enabledBattlegrounds.put(battleground.getBattlegroundName(), battleground);
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 1F));
        Bukkit.broadcast(Component.text("На арене " + StringUtils.capitalize(name) + " начинается новая битва!").color(TextColor.color(18, 123, 15)));
        Bukkit.broadcast(Component.text("Кликни, чтобы подключиться: ").color(TextColor.color(18, 123, 15))
                .append(Component.text("/ms join " + StringUtils.capitalize(name)).clickEvent(ClickEvent.suggestCommand("/ms join " + name)).color(TextColor.color(94, 167, 61)).decorate(TextDecoration.UNDERLINED)));
        List<String> saved = plugin.getConfig().getStringList("Battlegrounds");
        saved.add(name);
        plugin.getConfig().set("Battlegrounds", name);
        plugin.saveConfig();
    }

    public boolean isBattlegroundExist(String battlegroundName) {
        return new File(plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db").exists();
    }

    public Battleground getBattlegroundByName(String name) {
        return this.enabledBattlegrounds.get(name);
    }

}