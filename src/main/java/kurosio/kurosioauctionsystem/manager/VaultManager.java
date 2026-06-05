package kurosio.kurosioauctionsystem.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultManager {

    private static Economy economy;

    public static boolean setupEconomy() {

        RegisteredServiceProvider<Economy> rsp =
                org.bukkit.Bukkit.getServicesManager()
                        .getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();

        return economy != null;
    }

    public static Economy getEconomy() {
        return economy;
    }
}