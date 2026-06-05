package kurosio.kurosioauctionsystem;

import kurosio.kurosioauctionsystem.command.KACCommand;
import kurosio.kurosioauctionsystem.data.AuctionData;
import kurosio.kurosioauctionsystem.manager.AuctionManager;
import kurosio.kurosioauctionsystem.manager.VaultManager;
import kurosio.kurosioauctionsystem.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public final class KurosioAuctionSystem extends JavaPlugin {

    private static KurosioAuctionSystem instance;

    private AuctionManager auctionManager;

    private File dataFile;
    private YamlConfiguration dataConfig;


    @Override
    public void onEnable() {

        instance = this;

        if (!VaultManager.setupEconomy()) {
            getLogger().severe("Vaultが見つかりません！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(this);

        saveDefaultConfig();

        setupDataFile();
        loadAuctions();

        getCommand("kac").setExecutor(new KACCommand());

        //  20秒
        getServer().getScheduler().runTaskTimer(this, () -> {

            long now = System.currentTimeMillis();

            for (AuctionData auction : auctionManager.getAuctions()) {

                if (!auction.isActive()) continue;

                if (System.currentTimeMillis() - auction.getLastBidTime() >= 20000) {
                    finishAuction(auction);
                }
            }

        }, 20L, 20L);

        getLogger().info("KAC Enabled");
    }

    @Override
    public void onDisable() {
        saveAuctions();
    }

    public static KurosioAuctionSystem getInstance() {
        return instance;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    // =========================
    //  終了処理
    // =========================

    private void finishAuction(AuctionData auction) {

        if (!auction.isActive()) return;

        auction.setActive(false);

        AuctionManager manager = auctionManager;

        //  出品者に返却
        Player seller = Bukkit.getPlayer(auction.getSellerUUID());
        if (seller != null) {
            seller.getInventory().addItem(auction.getItem());
        }

        //  全参加者解除
        for (UUID uuid : manager.getAllJoinedPlayers(auction.getAuctionId())) {
            manager.leaveAuction(uuid);
        }

        //  出品者解除
        manager.unregisterSeller(auction.getSellerUUID());

        //  オークション削除
        manager.removeAuction(auction.getAuctionId());


        UUID winner = auction.getHighestBidder();

        String winnerName = (winner != null)
                ? Bukkit.getOfflinePlayer(winner).getName()
                : "なし";

        Bukkit.broadcastMessage(ChatUtil.color(
                ChatUtil.PREFIX + "\n&e===== オークション結果 ====="
        ));

        Bukkit.broadcastMessage(ChatUtil.color(
                "&eID: &f" + auction.getAuctionId()
        ));

        Bukkit.broadcastMessage(ChatUtil.color(
                "&e落札者&f: &a" + winnerName
        ));

        Bukkit.broadcastMessage(ChatUtil.color(
                "&e落札価格: &6&l" + auction.getCurrentPrice() + "円"
        ));

        ItemStack item = auction.getItem();
        ItemMeta meta = item.getItemMeta();

        String displayName = (meta != null && meta.hasDisplayName())
                ? meta.getDisplayName()
                : item.getType().name();

        Bukkit.broadcastMessage(ChatUtil.color(
                "&eアイテム名: &f" + displayName
        ));

        Bukkit.broadcastMessage(ChatUtil.color(
                "&e======================="
        ));

        if (winner != null && Bukkit.getPlayer(winner) != null) {
            Bukkit.getPlayer(winner).sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX + "&aあなたが落札しました！"
            ));
        }

        if (seller != null) {
            seller.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX + "&aオークションが終了しました"
            ));
        }
    }

    // =========================
    //  YAML保存・読み込み
    // =========================

    private void setupDataFile() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataFile = new File(getDataFolder(), "kac-auctions.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveAuctions() {

        dataConfig.set("auctions", null);

        for (AuctionData auction : auctionManager.getAuctions()) {

            String path = "auctions." + auction.getAuctionId();

            dataConfig.set(path + ".seller", auction.getSellerUUID().toString());
            dataConfig.set(path + ".item", auction.getItem());
            dataConfig.set(path + ".startPrice", auction.getStartPrice());
            dataConfig.set(path + ".currentPrice", auction.getCurrentPrice());
            dataConfig.set(path + ".bidUnit", auction.getBidUnit());
            dataConfig.set(path + ".lastBidTime", auction.getLastBidTime());
            dataConfig.set(path + ".active", auction.isActive());
        }

        try {
            dataConfig.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAuctions() {

        if (dataConfig.getConfigurationSection("auctions") == null) return;

        for (String id : dataConfig.getConfigurationSection("auctions").getKeys(false)) {

            String path = "auctions." + id;

            AuctionData auction = new AuctionData(
                    id,
                    java.util.UUID.fromString(dataConfig.getString(path + ".seller")),
                    dataConfig.getItemStack(path + ".item"),
                    dataConfig.getLong(path + ".startPrice"),
                    dataConfig.getLong(path + ".bidUnit")
            );

            auction.setCurrentPrice(dataConfig.getLong(path + ".currentPrice"));
            auction.setLastBidTime(dataConfig.getLong(path + ".lastBidTime"));
            auction.setActive(dataConfig.getBoolean(path + ".active"));

            auctionManager.addAuction(auction);
        }
    }
}