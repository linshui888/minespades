package me.nologic.minespades.util;

import lombok.Getter;
import me.nologic.minespades.Minespades;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

public class VaultEconomyProvider {

    @Getter @Nullable
    private Economy economy;

    public VaultEconomyProvider() {
        final Minespades plugin = Minespades.getInstance();

        RegisteredServiceProvider<Economy> rsp;
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null || (rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class)) == null) {
            plugin.getLogger().info("Vault not found.");
            return;
        }

        economy = rsp.getProvider();
    }

}