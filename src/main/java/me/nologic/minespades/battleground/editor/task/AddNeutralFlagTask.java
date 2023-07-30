package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import me.nologic.minespades.battleground.util.BattlegroundDataDriver;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Translatable
public class AddNeutralFlagTask extends BaseEditorTask implements MinorityFeature, Runnable {

    @TranslationKey(section = "editor-info-messages", name = "neutral-flag-created", value = "§2Success§r. Created a neutral flag at %s.")
    private String flagCreatedMessage;

    @Override @SneakyThrows
    public void run() {

        final BattlegroundDataDriver driver = new BattlegroundDataDriver().connect(editor.editSession(player).getTargetBattleground());
        final String sql = "INSERT INTO objects(x, y, z, type, data) VALUES(?,?,?,?,?)";
        final Location location = player.getLocation();

        ItemStack item = player.getInventory().getItemInMainHand();
        JsonObject jsonFlag = new JsonObject();
        jsonFlag.addProperty("item", super.serializeItemStack(item));
        final String data = jsonFlag.toString();

        driver.executeUpdate(sql, location.getBlockX(), location.getBlockY(), location.getBlockZ(), "NEUTRAL_FLAG", data).closeConnection();
        player.sendMessage(String.format(flagCreatedMessage, editor.editSession(player).getTargetTeam()));

    }

    public AddNeutralFlagTask(Player player) {
        super(player);
        plugin.getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), plugin);
    }

}