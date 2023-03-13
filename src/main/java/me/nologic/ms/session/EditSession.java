package me.nologic.ms.session;

import me.nologic.ms.battleground.Battleground;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class EditSession extends AbstractSession {

    private boolean interrupted;

    // friendly fire, true
    // lifepool, 100
    private HashMap<String, String> actions;

    // Старый инвентарь игрока
    private PlayerInventory inventory;

    public EditSession(Player player, Battleground bg) {
        this.actions = new HashMap<>();
        super.chatInfoMessage = "Режим редактирования арены. Настоящий инвентарь был сохранён, для настройки используй содержимое инвентаря." + "";
        super.actionBarName = "РЕЖИМ РЕДАКТИРОВАНИЯ";
        super.player = player;
        super.battleground = bg;
        inventory = player.getInventory();
    }

    @Override
    protected void pulse() {
        while (!interrupted) {
            player.sendActionBar(Component.text(actionBarName));
            player.sendMessage(chatInfoMessage);
        }
    }

    public void save() {
        this.interrupted = true;
        // Применение настроек из сессии, редактирование файла арены (а где этот файл будет? либо отдельный умл либо в дб)
    }

    public void cancel() {
        this.interrupted = true;
        // Отмена редактирования
    }

}
