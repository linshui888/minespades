package me.nologic.minespades.battleground.editor.task;

import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AddFlagTask extends BaseEditorTask implements Runnable {

    @Override @SneakyThrows
    public void run() {
        try (Connection connection = connect()) {
            PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM teams WHERE name = ?;");
            selectStatement.setString(1, editor.editSession(player).getTargetTeam());
            ResultSet result = selectStatement.executeQuery(); result.next();

            if (result.getString("flag") != null) {
                player.sendMessage("§4Ошибка. У команды %s уже есть флаг.");
                return;
            }

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
            player.sendMessage(String.format("§2Теперь у команды %s есть флаг.", editor.editSession(player).getTargetTeam()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public AddFlagTask(Player player) {
        super(player);
    }

}
