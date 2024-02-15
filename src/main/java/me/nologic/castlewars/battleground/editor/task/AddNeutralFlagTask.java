package me.nologic.castlewars.battleground.editor.task;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import me.nologic.castlewars.battleground.util.BattlegroundDataDriver;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Translatable
public class AddNeutralFlagTask extends BaseEditorTask implements MinorityFeature, Runnable {

    @TranslationKey(section = "editor-info-messages", name = "neutral-flag-created", value = "&2Success&r. Added a neutral flag at %s.")
    private String neutralFlagCreatedMessage;

    @Override @SneakyThrows
    public void run() {

        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(editor.editSession(player).getTargetBattleground());
        final int x = player.getLocation().getBlockX(), y = player.getLocation().getBlockY(), z = player.getLocation().getBlockZ();
        final ItemStack item = player.getInventory().getItemInMainHand();

        JsonObject jsonFlag = new JsonObject();
        jsonFlag.addProperty("item", super.serializeItemStack(item));
        String data = jsonFlag.toString();

        driver.executeUpdate("INSERT INTO objects(x, y, z, type, data) VALUES(?,?,?,?,?)", x, y, z, "NEUTRAL_FLAG", data);
        player.sendMessage(String.format(neutralFlagCreatedMessage, this.stringifyLocation(player.getLocation())));

    }

    public AddNeutralFlagTask(Player player) {
        super(player);
        plugin.getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), plugin);
    }

    private String stringifyLocation(final @Nullable Location location) {
        if (location == null) return null;
        return String.format("&ex&b%s&f, &ey&b%s&f, &ez&b%s&f", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

}