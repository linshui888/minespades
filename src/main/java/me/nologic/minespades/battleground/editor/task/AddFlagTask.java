package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import me.nologic.minority.MinorityFeature;
import me.nologic.minority.annotations.Translatable;
import me.nologic.minority.annotations.TranslationKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Translatable
public class AddFlagTask extends BaseEditorTask implements MinorityFeature, Runnable {

    @TranslationKey(section = "editor-info-messages", name = "team-flag-created", value = "§2Success§r. Now team §3%s §rhave a flag!")
    private String flagCreatedMessage;

    @Override @SneakyThrows
    public void run() {
        try (Connection connection = connect()) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            selectStatement.setString(1, editor.editSession(player).getTargetTeam());
            ResultSet result = selectStatement.executeQuery(); result.next();

            ItemStack item = player.getInventory().getItemInMainHand();
            JsonObject jsonFlag = new JsonObject();
            jsonFlag.addProperty("x", player.getLocation().getBlockX());
            jsonFlag.addProperty("y", player.getLocation().getBlockY());
            jsonFlag.addProperty("z", player.getLocation().getBlockZ());
            jsonFlag.addProperty("item", super.serializeItemStack(item));
            String data = jsonFlag.toString();

            PreparedStatement insertStatement = connection.prepareStatement("UPDATE teams SET flag = ? WHERE name = ?;");
            insertStatement.setString(1, data);
            insertStatement.setString(2, editor.editSession(player).getTargetTeam());
            insertStatement.executeUpdate();
            player.sendMessage(String.format(flagCreatedMessage, editor.editSession(player).getTargetTeam()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public AddFlagTask(Player player) {
        super(player);
        plugin.getConfigurationWizard().generate(this.getClass());
        this.init(this, this.getClass(), plugin);
    }

}