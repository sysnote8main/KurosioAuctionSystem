package kurosio.kurosioauctionsystem.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChatUtil {

    public static final String PREFIX =
            "&6&l[ＫＡＣオークション] ";

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void send(CommandSender sender, String text) {
        sender.sendMessage(color(text));
    }

    public static TextComponent createItemHoverText(ItemStack item) {

        ItemMeta meta = item.getItemMeta();

        // 🎨 表示名
        String name = (meta != null && meta.hasDisplayName())
                ? meta.getDisplayName()
                : item.getType().name();

        TextComponent text = new TextComponent("§e§l" + name);

        // 📜 Lore作成
        StringBuilder lore = new StringBuilder();

        if (meta != null && meta.hasLore()) {
            for (String line : meta.getLore()) {
                lore.append(line).append("\n");
            }
        } else {
            lore.append("No lore");
        }

        // 🖱 ホバー設定
        text.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(lore.toString()).create()
        ));

        return text;
    }
}