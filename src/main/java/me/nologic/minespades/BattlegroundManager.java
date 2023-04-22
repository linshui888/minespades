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

    /**
     * Перезагрузка арены. Восстановит все блоки, удалит всех энтитей (включая предметы и прожектайлы).
     * Перед использованием рекомендуется убедиться, что на арене нет игроков.
     * На время перезагрузки арена помечается как отключенная (enabled==false) и из-за этого к ней
     * нельзя подключиться.
     * */
    public void reset(Battleground battleground) {
        disable(battleground.getBattlegroundName());
        enable(battleground.getBattlegroundName());
    }

    public void enable(String name) {
        Battleground battleground = this.load(name);
        battleground.setEnabled(true);
        this.enabledBattlegrounds.put(battleground.getBattlegroundName(), battleground);
        this.callToArms(name);
        List<String> saved = plugin.getConfig().getStringList("Battlegrounds");
        saved.add(name);
        plugin.getConfig().set("Battlegrounds", name);
        plugin.saveConfig();
    }

    /* Загрузка арены из датабазы. */
    private Battleground load(String name) {
        return loader.load(name);
    }

    private void disable(String name) {
        this.getBattlegroundByName(name).setEnabled(false);
    }

    /**
     * Объявляет в чате о готовности арены, а так же отправляет звук.
     * */
    private void callToArms(String name) {
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_7, 1F, 1F));
        Bukkit.broadcast(Component.text("На арене " + StringUtils.capitalize(name) + " начинается новая битва!").color(TextColor.color(172, 127, 67)));
        Bukkit.broadcast(Component.text("Кликни, чтобы подключиться: ").color(TextColor.color(172, 127, 67))
                .append(Component.text("/ms join " + StringUtils.capitalize(name)).clickEvent(ClickEvent.suggestCommand("/ms join " + name)).color(TextColor.color(187, 151, 60)).decorate(TextDecoration.UNDERLINED)));

    }

    public boolean isBattlegroundExist(String battlegroundName) {
        return new File(plugin.getDataFolder() + "/battlegrounds/" + battlegroundName + ".db").exists();
    }

    public Battleground getBattlegroundByName(String name) {
        return this.enabledBattlegrounds.get(name);
    }

}