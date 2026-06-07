package kurosio.kurosioauctionsystem.manager;

import kurosio.kurosioauctionsystem.KurosioAuctionSystem;
import kurosio.kurosioauctionsystem.data.AuctionData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class HistoryManager {

    private final KurosioAuctionSystem plugin;

    private File file;
    private FileConfiguration config;

    public HistoryManager(KurosioAuctionSystem plugin) {

        this.plugin = plugin;

        file = new File(
                plugin.getDataFolder(),
                "history.yml"
        );

        if (!file.exists()) {

            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveHistory(AuctionData auction) {

        String path =
                "history." + auction.getAuctionId();

        // =========================
        // 時刻
        // =========================

        config.set(
                path + ".start-time",
                auction.getStartTime()
        );

        config.set(
                path + ".end-time",
                System.currentTimeMillis()
        );

        // =========================
        // 出品者
        // =========================

        config.set(
                path + ".seller-uuid",
                auction.getSellerUUID().toString()
        );

        config.set(
                path + ".seller-name",
                Bukkit.getOfflinePlayer(
                        auction.getSellerUUID()
                ).getName()
        );

        // =========================
        // 落札者
        // =========================

        UUID winner =
                auction.getHighestBidder();

        if (winner != null) {

            config.set(
                    path + ".winner-uuid",
                    winner.toString()
            );

            config.set(
                    path + ".winner-name",
                    Bukkit.getOfflinePlayer(winner)
                            .getName()
            );

        } else {

            config.set(
                    path + ".winner-uuid",
                    "NONE"
            );

            config.set(
                    path + ".winner-name",
                    "NONE"
            );
        }

        // =========================
        // アイテム情報
        // =========================

        ItemStack item =
                auction.getItem();

        config.set(
                path + ".item-type",
                item.getType().name()
        );

        config.set(
                path + ".amount",
                item.getAmount()
        );

        ItemMeta meta =
                item.getItemMeta();

        String displayName =
                (meta != null && meta.hasDisplayName())
                        ? meta.getDisplayName()
                        : item.getType().name();

        config.set(
                path + ".display-name",
                displayName
        );

        // Mythic Item ID
        config.set(
                path + ".mythic-item-id",
                auction.getMythicItemId()
        );

        // =========================
        // 価格
        // =========================

        config.set(
                path + ".price",
                auction.getCurrentPrice()
        );

        save();
    }
}