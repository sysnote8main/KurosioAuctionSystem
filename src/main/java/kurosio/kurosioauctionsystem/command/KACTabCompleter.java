package kurosio.kurosioauctionsystem.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class KACTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        List<String> list = new ArrayList<>();

        if (args.length == 1) {

            list.add("start");
            list.add("join");
            list.add("leave");
            list.add("bid");
            list.add("autobid");
            list.add("exlist");
            list.add("cancel");

            return list;
        }

        return list;
    }
}