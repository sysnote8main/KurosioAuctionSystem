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

import java.util.*;

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

        if (args.length == 0) {
            sender.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX + "&a/kac help &fでヘルプを表示できます。"
            ));
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
                // 半径制限
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
            manager.registerSeller(
                    player.getUniqueId(),
                    auctionId
            );

            manager.registerJoin(player.getUniqueId(), auctionId);

// YAMLへ即保存
            manager.notifyUpdate();

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

                ChatUtil.send(p, "&6&l[ＫＡＣオークション]");
                ChatUtil.send(p, "&e=======================");
                ChatUtil.send(p, "&a" + player.getName() + "&fさんがオークションを開始しました！");
                ChatUtil.send(p, "&eID&f: &f" + auction.getAuctionId());
                ChatUtil.send(p, "&eアイテム名&f: &f" + displayName);
                int amount = item.getAmount();
                if (amount > 1) {
                    ChatUtil.send(p, "&e個数&f: &f" + amount);
                }
                ChatUtil.send(p, "&e開始価格&f: &6" + String.format("%,d", startPrice) + "円");
                ChatUtil.send(p, "&e入札単位&f: &6" + String.format("%,d", bidUnit) + "円");

                // =====================
                // クリック参加UI
                // =====================
                TextComponent join = new TextComponent("§a§l参加§fするにはこちらをクリック！");

                join.setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/kac join " + auctionId
                ));

                join.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("§aクリックしてオークションに参加！").create()
                ));

                p.spigot().sendMessage(join);

                ChatUtil.send(p, "&e=======================");
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
            ChatUtil.send(player, "&e開始価格: &6&l" + String.format("%,d", auction.getStartPrice()) + "円");
            ChatUtil.send(player, "&e現在価格: &6&l" + String.format("%,d", auction.getCurrentPrice()) + "円");
            ItemStack item = auction.getItem();
            ItemMeta meta = item.getItemMeta();

            String displayName = (meta != null && meta.hasDisplayName())
                    ? meta.getDisplayName()
                    : item.getType().name();

            ChatUtil.send(player, "&eアイテム: &f" + displayName);
            int amount = item.getAmount();
            if (amount > 1) {
                ChatUtil.send(player, "&e個数&f: &f" + amount);
            }

            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("bid")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーのみ実行可能");
                return true;
            }

            Player player = (Player) sender;

            // 現在のオークション取得
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


            if (player.getUniqueId().equals(auction.getHighestBidder())) {

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&c既に最高入札者です。 &7(現在: &6" +
                                String.format("%,d", auction.getCurrentPrice()) +
                                "円&7)"
                ));

                return true;
            }

// 金額指定あり
            long newPrice;

            boolean firstBid = auction.getHighestBidder() == null;

            if (args.length >= 2) {

                try {
                    newPrice = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("金額が不正です");
                    return true;
                }

            } else {

                // /kac bid
                if (firstBid) {
                    newPrice = auction.getStartPrice();
                } else {
                    newPrice = auction.getCurrentPrice() + auction.getBidUnit();
                }
            }

            long bidUnit = auction.getBidUnit();
            long currentPrice = auction.getCurrentPrice();

// 最低入札額
            long minimumPrice;

            if (firstBid) {
                minimumPrice = auction.getStartPrice();
            } else {
                minimumPrice = currentPrice + bidUnit;
            }

// 最低入札額チェック
            if (newPrice < minimumPrice) {

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&c最低入札額は &6" +
                                String.format("%,d", minimumPrice) +
                                "円&cです。"
                ));

                return true;
            }

// 入札単位チェック
            if (!firstBid) {

                long diff = newPrice - currentPrice;

                if (diff % bidUnit != 0) {

                    player.sendMessage(ChatUtil.color(
                            ChatUtil.PREFIX +
                                    "&c入札額は &6" +
                                    String.format("%,d", bidUnit) +
                                    "円&c単位で入力してください。"
                    ));

                    return true;
                }
            }


            Long autoLimit =
                    manager.getAutoBids()
                            .get(player.getUniqueId());

            if (autoLimit != null) {

                // 自動入札額未満は禁止
                if (newPrice < autoLimit) {

                    player.sendMessage(ChatUtil.color(
                            ChatUtil.PREFIX +
                                    "&c自動入札上限(&6" +
                                    String.format("%,d", autoLimit) +
                                    "円&c)以下では入札できません。"
                    ));

                    return true;
                }

                // 上限以上なら手動入札を優先
                manager.removeAutoBid(
                        player.getUniqueId()
                );

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&e自動入札を解除しました。"
                ));
            }

            double money = VaultManager.getEconomy().getBalance(player);

            if (money < newPrice) {
                player.sendMessage("お金が足りません");
                return true;
            } else {

                // 普通の入札
                auction.setCurrentPrice(newPrice);
                auction.setHighestOfferPrice(newPrice);
                auction.setHighestBidder(player.getUniqueId());
                auction.setLastAutoBid(false);
                KurosioAuctionSystem.getInstance().saveAuctions();
            }

            auction.setLastBidTime(System.currentTimeMillis());

            manager.notifyUpdate();

            boolean autoBidTriggered =
                    processAutoBids(manager, auction);

            ChatUtil.send(
                    player,
                    ChatUtil.PREFIX +
                            "&a入札しました！ &6&l" +
                            String.format("%,d", newPrice) +
                            "円"
            );

            if (!autoBidTriggered) {

                String autoLabel = ""; // ←完全に削除 or 空固定

                for (UUID uuid : manager.getReceivers(auction)) {

                    Player target = Bukkit.getPlayer(uuid);
                    if (target == null) continue;

                    target.sendMessage(ChatUtil.color(
                            "&c現在の入札額: &6" +
                                    String.format("%,d", auction.getCurrentPrice()) +
                                    "円" +
                                    autoLabel
                    ));
                }
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

            Long currentAuto =
                    manager.getAutoBids()
                            .get(player.getUniqueId());

            if (currentAuto != null
                    && limit < currentAuto) {

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&c現在の自動入札上限より低い金額には変更できません。"
                ));

                return true;
            }

            AuctionData auction =
                    manager.getAuction(auctionId);

            if (auction == null) {

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&cオークション情報が見つかりません。"
                ));

                return true;
            }

            if (limit < auction.getStartPrice()) {

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&c開始価格未満は設定できません。"
                ));

                return true;
            }

            if (limit % auction.getBidUnit() != 0) {

                player.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&c入札単位に合わせて入力してください。"
                ));

                return true;
            }

            manager.setAutoBid(
                    player.getUniqueId(),
                    limit
            );


// 自分が最高入札者でなければ即時自動入札を試行
            processAutoBids(
                    manager,
                    auction
            );

            manager.notifyUpdate();

            player.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX +
                            "&a自動入札を設定しました！ &6上限&f:&6" +
                            String.format("%,d", limit) +
                            "円"
            ));

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage("プレイヤーのみ実行可能");
                return true;
            }

            Player player = (Player) sender;

            AuctionManager manager =
                    KurosioAuctionSystem.getInstance().getAuctionManager();

            manager.notifyUpdate();

            String auctionId =
                    manager.getSellerAuction(player.getUniqueId());

            if (auctionId == null) {
                player.sendMessage("出品中のオークションはありません");
                return true;
            }

            AuctionData auction = manager.getAuction(auctionId);

            if (auction == null) {
                player.sendMessage("オークションが見つかりません");
                return true;
            }

            // アイテム返却・送信

            // 参加者退出
            for (UUID uuid : manager.getReceivers(auction)) {

                Player target = Bukkit.getPlayer(uuid);

                if (target == null) continue;

                target.sendMessage(ChatUtil.color(
                        ChatUtil.PREFIX +
                                "&cオークションが出品者によって中止されました &eID&f: &f" +
                                auction.getAuctionId()
                ));
            }

            KurosioAuctionSystem.getInstance()
                    .cancelAuction(
                            auction,
                            "出品者によって中止されました"
                    );

            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {

            ChatUtil.send(sender, ChatUtil.PREFIX);
            ChatUtil.send(sender, "&e=======コマンド一覧=======");
            ChatUtil.send(sender, "&6&lコマンド一覧");
            ChatUtil.send(sender, "&a/kac start <開始価格> [入札単位] [半径]");
            ChatUtil.send(sender, "&6&lオークションを開始");
            ChatUtil.send(sender, "&f※入札単位・半径は任意。任意の半径内にｵｰｸｼｮﾝ開始を通知させます。");
            ChatUtil.send(sender, "&a/kac join <ID>  &f-オークションへ参加");
            ChatUtil.send(sender, "開始通知の&3&lメッセージクリック&fでも参加できます。");
            ChatUtil.send(sender, "&a/kac leave      &f-オークションから退出");
            ChatUtil.send(sender, "&f※最高入札者の場合退出しても落札者となります。");
            ChatUtil.send(sender, "&a/kac bid        &f-最低入札単位分入札します。");
            ChatUtil.send(sender, "&a/kac bid [金額]  &f-入札します。");
            ChatUtil.send(sender, "&f※金額の誤入力に注意");
            ChatUtil.send(sender, "&a/kac autobid <上限額> &f-自動入札を設定します。");
            ChatUtil.send(sender, "&a/kac exlist     &f-出品状況を表示 &c出品者のみ");
            ChatUtil.send(sender, "&a/kac cancel     &f-出品中オークションを中止 &c出品者のみ");
            ChatUtil.send(sender, "&e========================");

            return true;
        }
        return true;
    }

    private boolean processAutoBids(
            AuctionManager manager,
            AuctionData auction
    ) {

        if (auction == null || !auction.isActive()) {
            return false;
        }

        String auctionId = auction.getAuctionId();
        long bidUnit = auction.getBidUnit();

        Map<UUID, Long> limits = new HashMap<>();

// =========================
// 現在の最高入札者
// =========================
        UUID currentWinner = auction.getHighestBidder();

        if (currentWinner != null) {

            Long winnerAuto = manager.getAutoBids().get(currentWinner);

            if (winnerAuto != null) {
                limits.put(currentWinner, winnerAuto);
            } else {
                limits.put(currentWinner, auction.getCurrentPrice());
            }
        }

        // =========================
        // 自動入札者追加
        // =========================
        for (Map.Entry<UUID, Long> entry : manager.getAutoBids().entrySet()) {

            UUID uuid = entry.getKey();

            if (!auctionId.equals(manager.getJoinedAuction(uuid))) {
                continue;
            }

            limits.put(uuid, entry.getValue());
        }

        // =========================
        // 単独処理
        // =========================
        if (limits.size() == 1) {

            UUID only = limits.keySet().iterator().next();

            if (auction.getHighestBidder() == null) {

                auction.setCurrentPrice(
                        auction.getStartPrice()
                );

                auction.setHighestBidder(only);

                auction.setHighestOfferPrice(
                        limits.get(only)
                );

                auction.setLastAutoBid(true);

                auction.setLastBidTime(
                        System.currentTimeMillis()
                );

                notifyAutoUpdate(
                        manager,
                        auction,
                        auction.getStartPrice(),
                        only
                );

                KurosioAuctionSystem.getInstance()
                        .saveAuctions();

                return true;
            }

            return false;
        }

        // =========================
// ソート
// =========================
        List<Map.Entry<UUID, Long>> sorted =
                new ArrayList<>(limits.entrySet());

        sorted.sort((a, b) -> {

            // まず上限額で比較
            int limitCompare =
                    Long.compare(
                            b.getValue(),
                            a.getValue()
                    );

            if (limitCompare != 0) {
                return limitCompare;
            }

            // 同額なら設定時刻が早い方を優先
            Long aTime =
                    manager.getAutoBidTime(
                            a.getKey()
                    );

            Long bTime =
                    manager.getAutoBidTime(
                            b.getKey()
                    );

            if (aTime == null) aTime = Long.MAX_VALUE;
            if (bTime == null) bTime = Long.MAX_VALUE;

            return Long.compare(
                    aTime,
                    bTime
            );
        });

        UUID topUser = sorted.get(0).getKey();
        long topLimit = sorted.get(0).getValue();

        UUID secondUser = sorted.get(1).getKey();
        long secondLimit = sorted.get(1).getValue();

        long currentPrice = auction.getCurrentPrice();

        long newPrice = Math.min(topLimit, secondLimit + bidUnit);

        if (newPrice <= currentPrice) {
            return false;
        }

        UUID priceOwner = topUser;

        boolean autoTriggered = true;

        auction.setHighestBidder(topUser);
        auction.setHighestOfferPrice(topLimit);
        auction.setCurrentPrice(newPrice);
        auction.setLastAutoBid(autoTriggered);
        auction.setLastBidTime(System.currentTimeMillis());

        KurosioAuctionSystem.getInstance().saveAuctions();

        // =========================
        // ④ 通知（全員＋出品者＋自動表示）
        // =========================
        Set<UUID> receivers = new HashSet<>();

        receivers.addAll(manager.getAllJoinedPlayers(auctionId));
        receivers.add(auction.getSellerUUID());

        for (UUID uuid : receivers) {

            Player target = Bukkit.getPlayer(uuid);
            if (target == null) continue;

            target.sendMessage(ChatUtil.color(
                            "&c現在の入札額: &6" +
                            String.format("%,d", newPrice) +
                            "円" +
                            (autoTriggered ? " &7（自動入札）" : "")
            ));
        }

        // 勝者通知
        Player winner = Bukkit.getPlayer(topUser);
        if (winner != null) {
            winner.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX +
                            "&aあなたの入札が更新されました！ &6" +
                            String.format("%,d", newPrice) +
                            "円"
            ));
        }

        return true;
    }

    private void notifyAutoUpdate(
            AuctionManager manager,
            AuctionData auction,
            long price,
            UUID winner
    ) {

        Player p = Bukkit.getPlayer(winner);

        if (p != null) {
            p.sendMessage(ChatUtil.color(
                    ChatUtil.PREFIX +
                            "&aあなたの自動入札が反映されました！ &6" +
                            String.format("%,d", price) + "円"
            ));
        }

        String autoLabel = auction.isLastAutoBid() ? " (自動入札)" : "";

        for (UUID uuid : manager.getAllJoinedPlayers(auction.getAuctionId())) {

            Player target = Bukkit.getPlayer(uuid);
            if (target == null) continue;

            target.sendMessage(ChatUtil.color(
                    "&c現在の入札額: &6" +
                            String.format("%,d", auction.getCurrentPrice()) +
                            "円" +
                            autoLabel
            ));
        }
    }
}