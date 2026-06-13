package kurosio.kurosioauctionsystem.manager;

import kurosio.kurosioauctionsystem.KurosioAuctionSystem;
import kurosio.kurosioauctionsystem.data.AuctionData;

import java.util.*;

public class AuctionManager {

    private final KurosioAuctionSystem plugin;

    // オークション本体
    private final Map<String, AuctionData> auctions = new HashMap<>();

    // 出品者 → auctionId
    private final Map<UUID, String> sellerAuctions = new HashMap<>();

    // 参加者 → auctionId
    private final Map<UUID, String> joinedAuctions = new HashMap<>();

    // 自動入札上限
    private final Map<UUID, Long> autoBidTime = new HashMap<>();

    public AuctionManager(KurosioAuctionSystem plugin) {
        this.plugin = plugin;
    }

    // =========================
    // Auction本体
    // =========================

    public void addAuction(AuctionData auction) {
        auctions.put(auction.getAuctionId(), auction);
    }

    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    public AuctionData getAuction(String auctionId) {
        return auctions.get(auctionId);
    }


    public Collection<AuctionData> getAuctions() {
        return auctions.values();
    }

    // =========================
    // Seller管理
    // =========================

    public void registerSeller(UUID uuid, String auctionId) {
        sellerAuctions.put(uuid, auctionId);
    }

    public void unregisterSeller(UUID uuid) {
        sellerAuctions.remove(uuid);
    }

    public boolean hasAuction(UUID uuid) {
        return sellerAuctions.containsKey(uuid);
    }

    public String getSellerAuction(UUID uuid) {
        return sellerAuctions.get(uuid);
    }

    // =========================
    // Join管理
    // =========================

    public void registerJoin(UUID uuid, String auctionId) {
        joinedAuctions.put(uuid, auctionId);
    }


    public String getJoinedAuction(UUID uuid) {
        return joinedAuctions.get(uuid);
    }

    public boolean hasJoined(UUID uuid) {
        return joinedAuctions.containsKey(uuid);
    }


    public List<UUID> getAllJoinedPlayers(String auctionId) {

        List<UUID> list = new ArrayList<>();

        for (Map.Entry<UUID, String> entry : joinedAuctions.entrySet()) {
            if (entry.getValue().equals(auctionId)) {
                list.add(entry.getKey());
            }
        }

        return list;
    }

    // =========================
    // AutoBid
    // =========================

    // 自動入札上限
    private final Map<UUID, Long> autoBids = new HashMap<>();


    public void setAutoBid(UUID uuid, long limit) {

        autoBids.put(uuid, limit);

        autoBidTime.put(
                uuid,
                System.currentTimeMillis()
        );
    }


    public Long getAutoBidTime(UUID uuid) {
        return autoBidTime.get(uuid);
    }

    public Map<UUID, Long> getAutoBids() {
        return autoBids;
    }

    public void removeAutoBid(UUID uuid) {

        autoBids.remove(uuid);
        autoBidTime.remove(uuid);
    }

    // =========================
    // 退出処理
    // =========================

    public void leaveAuction(UUID uuid) {

        joinedAuctions.remove(uuid);
        sellerAuctions.remove(uuid);

        autoBids.remove(uuid);
        autoBidTime.remove(uuid);
    }

    // =========================
    // 終了処理
    // =========================

    public void cleanupAuction(String auctionId) {

        AuctionData auction = getAuction(auctionId);
        if (auction == null) return;

        for (UUID uuid : getAllJoinedPlayers(auctionId)) {
            leaveAuction(uuid);
        }

        unregisterSeller(auction.getSellerUUID());

        removeAuction(auctionId);


        notifyUpdate();
    }

    // =========================
    // ID生成
    // =========================

    public String generateAuctionId() {

        int id = plugin.getConfig().getInt("last-auction-id");

        id++;

        plugin.getConfig().set("last-auction-id", id);
        plugin.saveConfig();

        return String.format("%05d", id);
    }

    private Runnable saveHook;

    public void setSaveHook(Runnable saveHook) {
        this.saveHook = saveHook;
    }

    public void notifyUpdate() {
        if (saveHook != null) {
            saveHook.run();
        }
    }

    public Set<UUID> getReceivers(AuctionData auction) {

        Set<UUID> receivers = new HashSet<>(
                getAllJoinedPlayers(
                        auction.getAuctionId()
                )
        );

        receivers.add(
                auction.getSellerUUID()
        );

        if (auction.getHighestBidder() != null) {
            receivers.add(
                    auction.getHighestBidder()
            );
        }

        return receivers;
    }

}