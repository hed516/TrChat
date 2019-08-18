package me.arasple.mc.litechat.formats;

import com.google.common.collect.Lists;
import io.izzel.taboolib.module.locale.TLocale;
import io.izzel.taboolib.module.tellraw.TellrawJson;
import io.izzel.taboolib.util.Strings;
import io.izzel.taboolib.util.Variables;
import io.izzel.taboolib.util.item.Items;
import me.arasple.mc.litechat.LiteChat;
import me.arasple.mc.litechat.utils.MessageColors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @author Arasple
 * @date 2019/8/4 15:25
 */
public class Format {

    private List<ChatPart> parts;
    private MessagePart msgPart;

    public Format(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        parts = Lists.newArrayList();

        section.getKeys(false).forEach(x -> {
            ConfigurationSection s = section.getConfigurationSection(x);
            if (s != null) {
                if (!"message".equalsIgnoreCase(x)) {
                    parts.add(new ChatPart(s));
                } else {
                    msgPart = new MessagePart(section.getString("default-color", "7"), s);
                }
            } else {
                LiteChat.getTLogger().warn("&7加载聊天格式中发生错误. 请检查此节点: " + x);
            }
        });
    }

    public TellrawJson replaceFor(Player player, String message) {
        TellrawJson result = TellrawJson.create();
        parts.forEach(p -> result.append(p.toTellrawJson(player, player.getName())));
        result.append(msgPart.toTellrawJson(player, message));
        return result;
    }

    public List<ChatPart> getParts() {
        return parts;
    }

    public MessagePart getMsgPart() {
        return msgPart;
    }

    /**
     * 聊天组件
     */
    public static class ChatPart {

        private String text;
        private String hover;
        private String command;
        private String suggest;
        private String url;

        public ChatPart(ConfigurationSection section) {
            this(
                    section.getString(".text", null),
                    section.getString(".hover", null),
                    section.getString(".command", null),
                    section.getString(".suggest", null),
                    section.getString(".url", null)
            );
        }

        public ChatPart(String text, String hover, String command, String suggest, String url) {
            this.text = text;
            this.hover = hover;
            this.command = command;
            this.suggest = suggest;
            this.url = url;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getHover() {
            return hover;
        }

        public void setHover(String hover) {
            this.hover = hover;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getSuggest() {
            return suggest;
        }

        public void setSuggest(String suggest) {
            this.suggest = suggest;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String process(CommandSender sender, String str) {
            return process(sender, str, null);
        }

        public String process(CommandSender sender, String str, String value) {
            return TLocale.Translate.setPlaceholders(sender, str != null ? str
                    .replace('&', ChatColor.COLOR_CHAR)
                    .replace("{PLAYER}", sender.getName())
                    .replace("{SENDER}", sender.getName())
                    .replace("{RECEIVER}", value == null ? "" : value)
                    : null);
        }

        public TellrawJson applyJson(Player player, TellrawJson tellraw) {
            return applyJson(player, tellraw, null);
        }

        public TellrawJson applyJson(Player player, TellrawJson tellraw, String value) {
            if (hover != null) {
                tellraw.hoverText(process(player, hover, value));
            }
            if (command != null) {
                tellraw.clickCommand(process(player, command, value));
            }
            if (suggest != null) {
                tellraw.clickSuggest(process(player, suggest, value));
            }
            if (url != null) {
                tellraw.clickOpenURL(process(player, url, value));
            }

            return tellraw;
        }

        public TellrawJson toTellrawJson(Player player, String value) {
            return applyJson(player, TellrawJson.create().append(process(player, getText() == null ? "§8[§cERROR-NULL§8]" : getText(), value)), value);
        }

    }

    /**
     * 组件 - 消息部分
     */
    public static class MessagePart extends ChatPart {

        private ChatColor defaultColor;

        public MessagePart(String defaultColor, ConfigurationSection section) {
            super(section);

            this.defaultColor = ChatColor.getByChar(defaultColor);
        }

        @Override
        public TellrawJson toTellrawJson(Player player, String message) {
            if (LiteChat.getSettings().getBoolean("CHAT-CONTROL.COLOR-CODE.CHAT")) {
                message = MessageColors.processWithPermission(player, message);
            }
            // 取得配置信息
            List<String> itemKeys = LiteChat.getSettings().getStringList("CHAT-CONTROL.ITEM-SHOW.KEYS");
            String itemFormat = LiteChat.getSettings().getStringColored("CHAT-CONTROL.ITEM-SHOW.FORMAT", "§8[§3{0} §bx{1}§8]");
            // 替换物品变量
            for (String key : itemKeys) {
                message = message.replace(key, "<ITEM>");
            }

            TellrawJson format = TellrawJson.create();
            ItemStack item = player.getInventory().getItemInMainHand();

            for (Variables.Variable variable : new Variables(message).find().getVariableList()) {
                if (variable.isVariable()) {
                    format.append(Strings.replaceWithOrder(itemFormat, Items.getName(item), item.getType() != Material.AIR ? item.getAmount() : 1)).hoverItem(item);
                } else {
                    format.append(applyJson(player, TellrawJson.create().append(defaultColor + variable.getText())));
                }
            }

            return format;
        }

    }

}
