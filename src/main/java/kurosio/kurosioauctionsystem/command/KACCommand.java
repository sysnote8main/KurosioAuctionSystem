package kurosio.kurosioauctionsystem.command;

import kurosio.kurosioauctionsystem.KurosioAuctionSystem;
import kurosio.kurosioauctionsystem.data.AuctionData;
import kurosio.kurosioauctionsystem.manager.AuctionManager;
import kurosio.kurosioauctionsystem.manager.VaultManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import kurosio.kurosioauctionsystem.manager.VaultManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import kurosio.kurosioauctionsystem.util.ChatUtil;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class KACCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (args.length == 1 && args[0].equalsIgnoreCase("test")) {

            String id = KurosioAuctionSystem
                    .getInstance()
                    .getAuctionManager()
                    .generateAuctionId();

            sender.sendMessage("Generated ID: " + id);

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("money")) {

            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("Player only");
                return true;
            }

            org.bukkit.entity.Player player =
                    (org.bukkit.entity.Player) sender;

            double money = VaultManager
                    .getEconomy()
                    .getBalance(player);

            player.sendMessage("所持金: " + money + "円");

            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("start")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーのみ実行可能");
                return true;
            }

            Player player = (Player) sender;

            long startPrice;
            long bidUnit = 1000;
            double radius = 15;

            // =====================
            // 開始価格
            // =====================
            try {
                startPrice = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("価格が不正です");
                return true;
            }

            if (startPrice <= 0) {
                player.sendMessage("開始価格は1円以上にしてください");
                return true;
            }

            // =====================
            // 入札単位
            // =====================
            if (args.length >= 3) {
                try {
                    bidUnit = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("入札単位が不正です");
                    return true;
                }
            }

            // =====================
            // 半径
            // =====================
            if (args.length >= 4) {
                try {
                    radius = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage("半径が不正です");
                    return true;
                }

                // =====================
                // 半径制限（ここ追加）
                // =====================
                if (radius > 30) {
                    player.sendMessage("半径は最大30mまでです");
                    radius = 30;
                }

                if (radius < 0) {
                    player.sendMessage("半径は0以上にしてください");
                    return true;
                }
            }

            // =====================
            // アイテム取得
            // =====================
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage("アイテムを持ってください");
                return true;
            }

            ItemMeta meta = item.getItemMeta();

            String displayName = (meta != null && meta.hasDisplayName())
                    ? meta.getDisplayName()
                    : item.getType().name();

            // =====================
            // オークション作成
            // =====================
            AuctionManager manager = KurosioAuctionSystem.getInstance().getAuctionManager();

            if (manager.hasAuction(player.getUniqueId())) {
                player.sendMessage("既に出品中です");
                return true;
            }

            String auctionId = manager.generateAuctionId();

            AuctionData auction = new AuctionData(
                    auctionId,
                    player.getUniqueId(),
                    item.clone(),
                    startPrice,
                    bidUnit
            );

            manager.addAuction(auction);
            manager.registerSeller(player.getUniqueId(), auctionId);

            player.getInventory().setItemInMainHand(null);
            player.updateInventory();

            // =====================
            // 通知対象
            // =====================
            List<Player> targets = new ArrayList<>();

            if (radius <= 0) {
                targets.addAll(Bukkit.getOnlinePlayers());
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(player.getWorld())
                            && p.getLocation().distance(player.getLocation()) <= radius) {
                        targets.add(p);
                    }
                }
            }

            // =====================
            // 通知
            // =====================
            for (Player p : targets) {

                ChatUtil.send(p, "&7=======================");
                ChatUtil.send(p, "&e[KACオークション]");
                ChatUtil.send(p, "&a" + player.getName() + "&fさんがオークションを開始しました！");
                ChatUtil.send(p, "&eアイテム名&f: &f" + displayName);
                ChatUtil.send(p, "&e開始価格&f: &6" + startPrice + "円");
                ChatUtil.send(p, "&e入札単位&f: &6" + bidUnit + "円");

                // =====================
                // クリック参加UI
                // =====================
                TextComponent join = new TextComponent("§3§l参加§fするにはこちらをクリック！");

                join.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/kac join " + auctionId
                ));

                join.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§aクリックしてオークションに参加！").create()
                ));

                p.spigot().sendMessage(join);

                ChatUtil.send(p, "&7=======================");
            }

            player.sendMessage("オークションを作成しました ID:" + auctionId);

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("exlist")) {

            if (!(sender instanceof Player)) {
                return true;
            }

            Player player = (Player) sender;

            String auctionId = KurosioAuctionSystem
                    .getInstance()
                    .getAuctionManager()
                    .getSellerAuction(player.getUniqueId());

            if (auctionId == null) {
                player.sendMessage("出品中のオークションはありません");
                return true;
            }

            AuctionData auction = KurosioAuctionSystem
                    .getInstance()
                    .getAuctionManager()
                    .getAuction(auctionId);

            if (auction == null) {
                player.sendMessage("オークション情報が見つかりません");
                return true;
            }

            ChatUtil.send(player, ChatUtil.PREFIX);
            ChatUtil.send(player, "&eID: &f" + auction.getAuctionId());
            ChatUtil.send(player, "&e開始価格: &6&l" + auction.getStartPrice() + "円");
            ChatUtil.send(player, "&e現在価格: &6&l" + auction.getCurrentPrice() + "円");
            ItemStack item = auction.getItem();
            ItemMeta meta = item.getItemMeta();

            String displayName = (meta != null && meta.hasDisplayName())
                    ? meta.getDisplayName()
                    : item.getType().name();

            ChatUtil.send(player, "&eアイテム: &f" + displayName);

            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("bid")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーのみ実行可能");
                return true;
            }

            Player player = (Player) sender;

            // 現在のオークション取得（とりあえず1つ運用）
            AuctionManager manager = KurosioAuctionSystem.getInstance()
                    .getAuctionManager();

            String auctionId = manager.getJoinedAuction(player.getUniqueId());

            if (auctionId == null) {
                player.sendMessage("参加中のオークションがありません");
                return true;
            }

            AuctionData auction = manager.getAuction(auctionId);

            if (auction == null || !auction.isActive()) {
                player.sendMessage("オークションが存在しません");
                return true;
            }

            long newPrice;

// 金額指定あり
            if (args.length >= 2) {

                try {
                    newPrice = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("金額が不正です");
                    return true;
                }

                // 現在価格より低いのはNG
                if (newPrice <= auction.getCurrentPrice()) {
                    player.sendMessage("現在価格より高くしてください");
                    return true;
                }

            } else {
                // 通常入札（+bidUnit）
                newPrice = auction.getCurrentPrice() + auction.getBidUnit();
            }

            double money = VaultManager.getEconomy().getBalance(player);

            if (money < newPrice) {
                player.sendMessage("お金が足りません");
                return true;
            }

            VaultManager.getEconomy().withdrawPlayer(player, newPrice);

            auction.setCurrentPrice(newPrice);
            auction.setLastBidTime(System.currentTimeMillis());

            auction.setHighestBidder(player.getUniqueId());

            ChatUtil.send(
                    player,
                    ChatUtil.PREFIX + "&a入札しました！ &6&l" + newPrice + "円"
            );

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatUtil.color(
                        "&c現在の入札額: &6" + newPrice + "円"
                ));
            }

            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーのみ実行可能");
                return true;
            }

            Player player = (Player) sender;

            String auctionId = args[1];

            AuctionManager manager = KurosioAuctionSystem.getInstance()
                    .getAuctionManager();

            AuctionData auction = manager.getAuction(auctionId);

            if (auction == null) {
                player.sendMessage("そのオークションは存在しません");
                return true;
            }

            if (!auction.isActive()) {
                player.sendMessage("このオークションは終了しています");
                return true;
            }

            // 1人1参加制限
            if (manager.hasJoined(player.getUniqueId())) {
                player.sendMessage("既に別のオークションに参加しています");
                return true;
            }

            manager.registerJoin(player.getUniqueId(), auctionId);

            player.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX + "&aオークションに参加しました！ &7ID:&f" + auctionId
            ));

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {

            Player player = (Player) sender;

            AuctionManager manager = KurosioAuctionSystem.getInstance().getAuctionManager();

            manager.leaveAuction(player.getUniqueId());

            player.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX + "&cオークションから退出しました"
            ));

            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("autobid")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーのみ実行可能");
                return true;
            }

            Player player = (Player) sender;

            long limit;

            try {
                limit = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("金額が不正です");
                return true;
            }

            AuctionManager manager = KurosioAuctionSystem.getInstance().getAuctionManager();

            String auctionId = manager.getJoinedAuction(player.getUniqueId());

            if (auctionId == null) {
                player.sendMessage("参加中のオークションがありません");
                return true;
            }

            manager.setAutoBid(player.getUniqueId(), limit);

            player.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX + "&a自動入札を設定しました！ &6上限:" + limit + "円"
            ));

            return true;
        }
        return true;
    }
}