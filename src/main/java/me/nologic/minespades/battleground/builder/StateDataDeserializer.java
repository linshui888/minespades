package me.nologic.minespades.battleground.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;

// TODO: Возможно, стоит придумать более подходящее название этому классу.
public class StateDataDeserializer {

    private String data;

    /* Универсальная десериализация. */
    public void deserialize(BlockState state, String data) {
        this.data = data;
        if (state instanceof Sign sign)           deserialize(sign);
        if (state instanceof Container container) deserialize(container);
        // TODO: Добавить поддержку других тайл-энтитей.
    }

    /* Десериализация табличек. */
    private void deserialize(Sign sign) {
        JsonObject obj = JsonParser.parseString(data).getAsJsonObject();
        sign.setGlowingText(obj.get("glow").getAsBoolean());
        String[] lines = obj.get("content").getAsString().split("\n");
        for (int i = 0; i < lines.length; i++)
            sign.setLine(i, lines[i]);
        sign.setColor(DyeColor.valueOf(obj.get("color").getAsString()));
        sign.update(true, false);
    }

    /* Десериализация контейнеров (блоки, имеющие инвентарь). */
    private void deserialize(Container container) {
        container.getInventory().setContents(this.readInventory(data).getContents());
    }

    private Inventory readInventory(String inventoryJson) {
        JsonObject json = JsonParser.parseString(inventoryJson).getAsJsonObject();
        Inventory inventory = Bukkit.createInventory(null, InventoryType.valueOf(json.get("type").getAsString()));

        JsonArray items = json.get("items").getAsJsonArray();
        for (JsonElement element : items) {
            JsonObject jsonItem = element.getAsJsonObject();
            ItemStack item = this.getItemStackFromBase64String(jsonItem.get("data").getAsString());
            inventory.setItem(jsonItem.get("slot").getAsInt(), item);
        }

        return inventory;
    }

    @SneakyThrows
    private ItemStack getItemStackFromBase64String(String base64) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

}