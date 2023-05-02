package me.nologic.minespades.battleground.editor.loadout;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;

@RequiredArgsConstructor
@Getter
public class LoadoutSupplyRule {

    private final transient BattlegroundLoadout loadout;

    @SerializedName("name")
    private final String supplyName;

    @SerializedName("item")
    private final String serializedItemStack;

    private final String permission;
    private final int    interval, amount, maximum;

    // Десериализованный ItemStack, который инициализируется лениво
    private transient ItemStack deserializedItemStack;

    public ItemStack getItemStack() {
        return deserializedItemStack == null ? (deserializedItemStack = deserializeItemStack(serializedItemStack)) : deserializedItemStack;
    }

    @SneakyThrows
    private ItemStack deserializeItemStack(String base64) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        item.setAmount(amount);
        dataInput.close();
        return item;
    }

}