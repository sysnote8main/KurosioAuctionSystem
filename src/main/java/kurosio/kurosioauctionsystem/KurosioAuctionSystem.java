package kurosio.kurosioauctionsystem;

import kurosio.kurosioauctionsystem.command.KACCommand;
import kurosio.kurosioauctionsystem.command.KACTabCompleter;
import kurosio.kurosioauctionsystem.data.AuctionData;
import kurosio.kurosioauctionsystem.listener.AuctionQuitListener;
import kurosio.kurosioauctionsystem.manager.AuctionManager;
import kurosio.kurosioauctionsystem.manager.ReturnManager;
import kurosio.kurosioauctionsystem.manager.VaultManager;
import kurosio.kurosioauctionsystem.util.ChatUtil;
import kurosio.kurosioauctionsystem.manager.HistoryManager;
import kurosio.kurosioauctionsystem.listener.PlayerJoinListener;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static kurosio.kurosioauctionsystem.util.ChatUtil.color;

public final class KurosioAuctionSystem extends JavaPlugin {

    private static KurosioAuctionSystem instance;

    private AuctionManager auctionManager;
    private HistoryManager historyManager;

    private File dataFile;
    private YamlConfiguration dataConfig;

    private ReturnManager returnManager;


    @Override
    public void onEnable() {

        instance = this;

        if (!VaultManager.setupEconomy()) {
            getLogger().severe("Vaultが見つかりません！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(this);

        auctionManager.setSaveHook(() -> saveAuctions());

        historyManager = new HistoryManager(this);
        returnManager = new ReturnManager(this);

        Bukkit.getPluginManager().registerEvents(
                new PlayerJoinListener(),
                this
        );

        Bukkit.getPluginManager().registerEvents(
                new AuctionQuitListener(),
                this
        );

        saveDefaultConfig();

        setupDataFile();
        loadAuctions();

        getCommand("kac").setExecutor(new KACCommand());
        getCommand("kac").setTabCompleter(new KACTabCompleter());

        //  20秒
        getServer().getScheduler().runTaskTimer(this, () -> {

            long now = System.currentTimeMillis();

            for (AuctionData auction : auctionManager.getAuctions()) {

                if (!auction.isActive()) continue;

                long elapsed =
                        System.currentTimeMillis()
                                - auction.getLastBidTime();

                long remaining =
                        35 - (elapsed / 1000);

                if (remaining == 5
                        || remaining == 4
                        || remaining == 3
                        || remaining == 2
                        || remaining == 1) {

                    for (UUID uuid :
                            auctionManager.getReceivers(auction)) {

                        Player target =
                                Bukkit.getPlayer(uuid);

                        if (target == null) continue;

                        target.sendMessage(color(
                                        "&eあと" +
                                        remaining +
                                        "秒"
                        ));
                    }
                }

                if (elapsed >= 35000) {
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

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public ReturnManager getReturnManager() {
        return returnManager;
    }

    // =========================
    //  終了処理
    // =========================

    public void finishAuction(AuctionData auction) {

        if (!auction.isActive()) return;

        auction.setActive(false);

        auction.setEndTime(System.currentTimeMillis());

        AuctionManager manager = auctionManager;

        Player seller = Bukkit.getPlayer(auction.getSellerUUID());
        UUID winner = auction.getHighestBidder();

        // =========================
        // 落札処理
        // =========================
        if (winner != null) {

            Player winnerPlayer = Bukkit.getPlayer(winner);

            if (winnerPlayer != null) {

                // 落札者から徴収
                VaultManager.getEconomy().withdrawPlayer(
                        winnerPlayer,
                        auction.getCurrentPrice()
                );

                // アイテム付与
                Map<Integer, ItemStack> leftOver =
                        winnerPlayer.getInventory().addItem(auction.getItem());

                for (ItemStack item : leftOver.values()) {

                    winnerPlayer.getWorld().dropItemNaturally(
                            winnerPlayer.getLocation(),
                            item
                    );
                }

                // 出品者へ入金
                VaultManager.getEconomy().depositPlayer(
                        Bukkit.getOfflinePlayer(
                                auction.getSellerUUID()
                        ),
                        auction.getCurrentPrice()
                );

                if (seller != null) {
                    seller.sendMessage(color(
                            ChatUtil.PREFIX +
                                    "&a売上として &6&l" +
                                    String.format("%,d", auction.getCurrentPrice()) +
                                    "円&a受け取りました。"
                    ));
                }

            } else {

                cancelAuction(
                        auction,
                        "落札時に最高入札者がオフラインだったため"
                );

                return;
            }
        } else {

            if (seller != null) {
                Map<Integer, ItemStack> leftOver =
                        seller.getInventory().addItem(
                                auction.getItem()
                        );

                for (ItemStack item : leftOver.values()) {

                    seller.getWorld().dropItemNaturally(
                            seller.getLocation(),
                            item
                    );
                }

                seller.sendMessage(color(
                        ChatUtil.PREFIX +
                                "&e入札者がいなかったためアイテムを返却しました。"
                ));
            }
        }

        Set<UUID> participants = new HashSet<>(
                manager.getAllJoinedPlayers(auction.getAuctionId())
        );


        // =========================
        // 後処理
        // =========================
        for (UUID uuid : manager.getAllJoinedPlayers(auction.getAuctionId())) {
            manager.leaveAuction(uuid);
        }

        manager.unregisterSeller(
                auction.getSellerUUID()
        );


        KurosioAuctionSystem.getInstance()
                .getHistoryManager()
                .saveHistory(auction);

        manager.removeAuction(
                auction.getAuctionId()
        );

        manager.notifyUpdate();

        // =========================
        // 結果発表
        // =========================

        String winnerName = (winner != null)
                ? Bukkit.getOfflinePlayer(winner).getName()
                : "なし";

        ItemStack item = auction.getItem();
        ItemMeta meta = item.getItemMeta();

        String displayName = (meta != null && meta.hasDisplayName())
                ? meta.getDisplayName()
                : item.getType().name();

        Set<UUID> receivers = new HashSet<>(
                participants
        );

// 出品者は必ず含める
        receivers.add(
                auction.getSellerUUID()
        );

// 落札者も必ず含める
        if (winner != null) {
            receivers.add(winner);
        }

        for (UUID uuid : receivers) {

            Player target = Bukkit.getPlayer(uuid);

            if (target == null) continue;

            target.sendMessage(color(
                    ChatUtil.PREFIX +
                            "\n&e===== オークション結果 ====="
            ));

            target.sendMessage(color(
                    "&eID&f: &f" +
                            auction.getAuctionId()
            ));

            target.sendMessage(color(
                    "&e落札者&f: &a" +
                            winnerName
            ));

            target.sendMessage(color(
                    "&e落札価格&f: &6&l" +
                            String.format("%,d",
                                    auction.getCurrentPrice()
                            ) +
                            "円"
            ));

            TextComponent itemLine =
                    new TextComponent(
                            color(
                                    "&eアイテム名&f: "
                            )
                    );

            TextComponent itemName =
                    new TextComponent(
                            color(
                                    "&f" + displayName
                            )
                    );

            itemName.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(
                                    buildItemHover(item)
                            ).create()
                    )
            );

            itemLine.addExtra(itemName);

            target.spigot().sendMessage(itemLine);

            int amount = item.getAmount();
            if (amount > 1) {
                target.sendMessage(color(
                        "&e個数&f: &f" +
                                amount
                ));
            }
            target.sendMessage(color(
                    "&e======================="
            ));
        }

        if (winner != null && Bukkit.getPlayer(winner) != null) {
            Bukkit.getPlayer(winner).sendMessage(color(
                    ChatUtil.PREFIX +
                            "&aあなたが落札しました！ &6" +
                            String.format("%,d", auction.getCurrentPrice()) +
                            "円"
            ));
        }

        if (seller != null) {
            seller.sendMessage(color(
                    ChatUtil.PREFIX +
                            "&aオークションが終了しました"
            ));
        }
    }

    public void cancelAuction(AuctionData auction, String reason) {

        if (auction == null) return;

        for (UUID uuid : auctionManager.getReceivers(auction)) {

            Player target = Bukkit.getPlayer(uuid);

            if (target == null) continue;

            target.sendMessage(color(
                    ChatUtil.PREFIX +
                            "&cオークションが中止されました &7(ID:" +
                            auction.getAuctionId() +
                            ")"
            ));
        }
        for (UUID uuid : auctionManager.getReceivers(auction)) {

            Player target = Bukkit.getPlayer(uuid);

            if (target == null) continue;

            target.sendMessage(color(
                    "&7理由: &f" + reason
            ));
        }

        auction.setActive(false);

        // 出品者へ返却
        Player seller =
                Bukkit.getPlayer(
                        auction.getSellerUUID()
                );

        if (seller != null) {

            Map<Integer, ItemStack> leftOver =
                    seller.getInventory().addItem(
                            auction.getItem()
                    );

            for (ItemStack item : leftOver.values()) {

                seller.getWorld().dropItemNaturally(
                        seller.getLocation(),
                        item
                );
            }

            seller.sendMessage(color(
                    ChatUtil.PREFIX +
                            "&e出品アイテムを返却しました。"
            ));

        } else {

            returnManager.addReturn(
                    auction.getSellerUUID(),
                    auction.getItem()
            );
        }
        // 履歴保存
        historyManager.saveHistory(auction);

        // 後処理
        auctionManager.cleanupAuction(
                auction.getAuctionId()
        );

        auctionManager.notifyUpdate();
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

    public void saveAuctions() {

        dataConfig.set("auctions", null);

        for (AuctionData auction : auctionManager.getAuctions()) {

            String path = "auctions." + auction.getAuctionId();

            UUID seller = auction.getSellerUUID();

            dataConfig.set(
                    path + ".seller.uuid",
                    seller.toString()
            );

            dataConfig.set(
                    path + ".seller.name",
                    Bukkit.getOfflinePlayer(seller).getName()
            );
            UUID bidder = auction.getHighestBidder();

            if (bidder != null) {

                dataConfig.set(
                        path + ".bidder.uuid",
                        bidder.toString()
                );

                dataConfig.set(
                        path + ".bidder.name",
                        Bukkit.getOfflinePlayer(bidder).getName()
                );

            } else {

                dataConfig.set(path + ".bidder.uuid", "NONE");
                dataConfig.set(path + ".bidder.name", "NONE");
            }

            ItemStack item = auction.getItem();
            ItemMeta meta = item.getItemMeta();

            String displayName =
                    (meta != null && meta.hasDisplayName())
                            ? meta.getDisplayName()
                            : item.getType().name();


            dataConfig.set(
                    path + ".item",
                    auction.getItem()
            );

            dataConfig.set(
                    path + ".item.mythic-item-id",
                    auction.getMythicItemId()
            );

            dataConfig.set(
                    path + ".start-price",
                    auction.getStartPrice()
            );

            dataConfig.set(
                    path + ".current-price",
                    auction.getCurrentPrice()
            );

            dataConfig.set(
                    path + ".bid-unit",
                    auction.getBidUnit()
            );

            dataConfig.set(
                    path + ".active",
                    auction.isActive()
            );

            dataConfig.set(
                    path + ".start-time",
                    auction.getStartTime()
            );

            dataConfig.set(
                    path + ".last-bid-time",
                    auction.getLastBidTime()
            );


            if (auction.getHighestBidder() != null) {
                dataConfig.set(
                        path + ".highestBidder",
                        auction.getHighestBidder().toString()
                );
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void loadAuctions() {

        if (dataConfig.getConfigurationSection("auctions") == null) {
            return;
        }

        for (String id : dataConfig.getConfigurationSection("auctions").getKeys(false)) {

            String path = "auctions." + id;

            AuctionData auction = new AuctionData(
                    id,
                    UUID.fromString(
                            dataConfig.getString(path + ".seller.uuid")
                    ),
                    dataConfig.getItemStack(path + ".item"),
                    dataConfig.getLong(path + ".start-price"),
                    dataConfig.getLong(path + ".bid-unit")
            );

            auction.setCurrentPrice(
                    dataConfig.getLong(path + ".current-price")
            );

            auction.setLastBidTime(
                    dataConfig.getLong(path + ".last-bid-time")
            );

            String bidder = dataConfig.getString(path + ".highestBidder");

            if (bidder != null && !bidder.equalsIgnoreCase("NONE")) {
                auction.setHighestBidder(
                        UUID.fromString(bidder)
                );
            }

            getHistoryManager().saveHistory(auction);

            returnManager.addReturn(
                    auction.getSellerUUID(),
                    auction.getItem()
            );
        }

        dataConfig.set("auctions", null);

        try {
            dataConfig.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String buildItemHover(
            ItemStack item
    ) {

        StringBuilder sb =
                new StringBuilder();

        ItemMeta meta =
                item.getItemMeta();

        // Lore表示
        if (meta != null && meta.hasLore()) {

            for (String line : meta.getLore()) {

                sb.append(
                        color(line)
                ).append("\n");
            }
        }

        // シュルカー中身表示
        if (meta instanceof BlockStateMeta) {

            BlockStateMeta blockMeta =
                    (BlockStateMeta) meta;

            if (blockMeta.getBlockState() instanceof ShulkerBox) {

                ShulkerBox shulker =
                        (ShulkerBox) blockMeta.getBlockState();

                Inventory inv =
                        shulker.getInventory();

                boolean foundItem = false;

                for (ItemStack content : inv.getContents()) {

                    if (content == null) {
                        continue;
                    }

                    if (!foundItem) {

                        if (sb.length() > 0) {
                            sb.append("\n");
                        }

                        sb.append("§e──── 内容物 ────\n");

                        foundItem = true;
                    }

                    ItemMeta contentMeta =
                            content.getItemMeta();

                    String name;

                    if (contentMeta != null
                            && contentMeta.hasDisplayName()) {

                        name = color(
                                contentMeta.getDisplayName()
                        );

                    } else {

                        name = content.getType().name();
                    }

                    sb.append(name)
                            .append(" §7×")
                            .append(content.getAmount())
                            .append("\n");
                }
            }
        }

        return sb.toString().trim();
    }

}