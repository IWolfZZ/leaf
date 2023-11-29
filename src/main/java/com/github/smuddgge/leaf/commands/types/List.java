package com.github.smuddgge.leaf.commands.types;

import com.github.smuddgge.leaf.Leaf;
import com.github.smuddgge.leaf.MessageManager;
import com.github.smuddgge.leaf.commands.BaseCommandType;
import com.github.smuddgge.leaf.commands.CommandStatus;
import com.github.smuddgge.leaf.commands.CommandSuggestions;
import com.github.smuddgge.leaf.datatype.ProxyServerInterface;
import com.github.smuddgge.leaf.datatype.User;
import com.github.smuddgge.leaf.discord.DiscordBotMessageAdapter;
import com.github.smuddgge.leaf.discord.DiscordBotRemoveMessageHandler;
import com.github.smuddgge.leaf.placeholders.PlaceholderManager;
import com.github.smuddgge.squishyconfiguration.interfaces.ConfigurationSection;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * <h1>List Command Type</h1>
 * Used to show a list of online players.
 */
public class List extends BaseCommandType {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getSyntax() {
        return "/[name]";
    }

    @Override
    public CommandSuggestions getSuggestions(ConfigurationSection section, User user) {
        return null;
    }

    @Override
    public CommandStatus onConsoleRun(ConfigurationSection section, String[] arguments) {
        ArrayList<String> possiblePermissions = new ArrayList<>();

        for (String key : section.getSection("list").getKeys()) {
            String permission = section.getSection("list").getSection(key).getString("permission");
            possiblePermissions.add(permission);
        }

        MessageManager.log(this.getFormatted(section, possiblePermissions, possiblePermissions));
        return new CommandStatus();
    }

    @Override
    public CommandStatus onPlayerRun(ConfigurationSection section, String[] arguments, User user) {
        ArrayList<String> possiblePermissions = new ArrayList<>();
        ArrayList<String> sendersPermissions = new ArrayList<>();

        for (String key : section.getSection("list").getKeys()) {
            String permission = section.getSection("list").getSection(key).getString("permission");

            possiblePermissions.add(permission);

            if (!user.hasPermission(permission)) continue;

            sendersPermissions.add(permission);
        }

        user.sendMessage(this.getFormatted(section, sendersPermissions, possiblePermissions));
        return new CommandStatus();
    }

    @Override
    public CommandStatus onDiscordRun(ConfigurationSection section, SlashCommandInteractionEvent event) {
        ArrayList<String> possiblePermissions = new ArrayList<>();

        for (String key : section.getSection("list").getKeys()) {
            String permission = section.getSection("list").getSection(key).getString("permission");
            possiblePermissions.add(permission);
        }

        // Get list placeholder.
        String list = this.getFormatted(section.getSection("discord_bot"), new ArrayList<>(), possiblePermissions);

        // Create message.
        DiscordBotMessageAdapter message = new DiscordBotMessageAdapter(section, "discord_bot.message", "%list%")
                .setParser(new DiscordBotMessageAdapter.PlaceholderParser() {
                    @Override
                    public @NotNull String parsePlaceholders(@NotNull String string) {
                        return PlaceholderManager.parse(string, null, null)
                                .replace("%list%", list);
                    }
                });

        event.reply(message.buildMessage()).complete();
        new DiscordBotRemoveMessageHandler(section.getSection("discord_bot"), event);
        return new CommandStatus();
    }

    /**
     * Used to get the formatted message.
     *
     * @param section             The section of configuration the command is from.
     * @param permissions         The list of permissions the player has.
     * @param possiblePermissions The list of the commands possible permissions.
     * @return The formatted message.
     */
    private String getFormatted(ConfigurationSection section, java.util.List<String> permissions, java.util.List<String> possiblePermissions) {
        ProxyServerInterface proxyServerInterface = new ProxyServerInterface(Leaf.getServer());

        // Build the message.
        StringBuilder builder = new StringBuilder();

        String header = section.getAdaptedString("header", "\n", null);
        if (header != null) {
            builder.append(header.replace("\\n", "\n")).append("\n");
        }

        // For each rank in the list.
        for (String key : section.getSection("list").getKeys()) {
            ConfigurationSection innerSection = section.getSection("list").getSection(key);

            // Get the permission.
            String permission = innerSection.getString("permission");
            if (permission == null) continue;

            // Get the filtered list of players.
            // If the player's permissions include the permission add vanished players to the list.
            java.util.List<User> players = proxyServerInterface.getFilteredPlayers(
                    permission, possiblePermissions, permissions.contains(permission)
            );

            // Don't include the section if there are 0 players.
            if (players.size() == 0) continue;

            // Append the header
            String innerHeader = innerSection.getAdaptedString("header", "'\n", null);

            if (innerHeader != null) {
                builder.append("\n").append(innerHeader
                        .replace("%amount%", String.valueOf(players.size()))
                        .replace("\\n", "\n"));
            }

            // Append the players
            for (User user : players) {
                String userSection = innerSection.getString("section")
                        .replace("%player%", user.getName())
                        .replace("\\n", "\n");

                builder.append("\n").append(PlaceholderManager.parse(userSection, null, user));
            }
        }

        String footer = section.getAdaptedString("footer", "\n", null);
        if (footer != null) {
            builder.append("\n").append(footer.replace("\\n", "\n"));
        }

        return builder.toString();
    }
}
