package kurosio.kurosioauctionsystem.listener;

import kurosio.kurosioauctionsystem.KurosioAuctionSystem;
import kurosio.kurosioauctionsystem.data.AuctionData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

public class AuctionQuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        KurosioAuctionSystem plugin =
                KurosioAuctionSystem.getInstance();

        for (AuctionData auction :
                new ArrayList<>(plugin.getAuctionManager().getAuctions())) {

            if (!auction.isActive()) continue;

            if (auction.getSellerUUID().equals(
                    event.getPlayer().getUniqueId()
            )) {

                plugin.cancelAuction(
                        auction,
                        "出品者がログアウトしたため"
                );

                return;
            }
        }
    }
}