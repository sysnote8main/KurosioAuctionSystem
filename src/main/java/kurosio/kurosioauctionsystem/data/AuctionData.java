package kurosio.kurosioauctionsystem.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionData {

    private final String auctionId;
    private final UUID sellerUUID;
    private final ItemStack item;

    private long startPrice;
    private long currentPrice;
    private long bidUnit;
    private long startTime;

    private long lastBidTime;
    private boolean active = true;

    private long endTime;

    private String mythicItemId;

    public AuctionData(String auctionId,
                       UUID sellerUUID,
                       ItemStack item,
                       long startPrice,
                       long bidUnit) {

        this.auctionId = auctionId;
        this.sellerUUID = sellerUUID;
        this.item = item;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.highestOfferPrice = startPrice;
        this.bidUnit = bidUnit;
        this.active = true;
        this.lastBidTime = System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
    }

    // =====================
    // 基本情報
    // =====================

    public String getAuctionId() {
        return auctionId;
    }

    public UUID getSellerUUID() {
        return sellerUUID;
    }

    public ItemStack getItem() {
        return item;
    }

    // =====================
    // 価格情報
    // =====================

    public long getStartPrice() {
        return startPrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(long currentPrice) {
        this.currentPrice = currentPrice;
    }

    public long getBidUnit() {
        return bidUnit;
    }

    // =====================
    // タイマー系
    // =====================

    public long getLastBidTime() {
        return lastBidTime;
    }

    public void setLastBidTime(long lastBidTime) {
        this.lastBidTime = lastBidTime;
    }

    // =====================
    // 状態管理
    // =====================

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private UUID highestBidder;

    public UUID getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(UUID highestBidder) {
        this.highestBidder = highestBidder;
    }

    private long highestOfferPrice;

    public long getHighestOfferPrice() {
        return highestOfferPrice;
    }

    public void setHighestOfferPrice(long highestOfferPrice) {
        this.highestOfferPrice = highestOfferPrice;
    }


    public long getStartTime() {
        return startTime;
    }

    public String getMythicItemId() {
        return mythicItemId;
    }

    public void setMythicItemId(String mythicItemId) {
        this.mythicItemId = mythicItemId;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

}