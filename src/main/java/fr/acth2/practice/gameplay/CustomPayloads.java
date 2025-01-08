package fr.acth2.practice.gameplay;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class CustomPayloads {
    public static void givePotions(ServerPlayer player, String effect, int splash) {
        boolean splash2bool = splash != 0;
        String command = String.format("give %s %s[minecraft:potion_contents=%s]",
                player.getName().getString(),
                "minecraft:" + (splash2bool ? "splash_potion" : "potion"),
                effect);

        System.out.println(command);
        MinecraftServer server = player.getServer();
        if (server != null) {
            CommandSourceStack serverSource = server.createCommandSourceStack();
            try {
                var parseResults = server.getCommands().getDispatcher().parse(new StringReader(command), serverSource);
                server.getCommands().getDispatcher().execute(parseResults);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void giveMenuClock(ServerPlayer player) {
        String command = String.format("give %s minecraft:clock[minecraft:item_name=Menu]",
                player.getName().getString());

        System.out.println(command);
        MinecraftServer server = player.getServer();
        if (server != null) {
            CommandSourceStack serverSource = server.createCommandSourceStack();
            try {
                var parseResults = server.getCommands().getDispatcher().parse(new StringReader(command), serverSource);
                server.getCommands().getDispatcher().execute(parseResults);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void applyEnchant(ServerPlayer player, ItemStack itemStack, String enchantment, int level) {
        player.setItemInHand(player.getUsedItemHand(), itemStack);
        String command = String.format("enchant %s %s %d", player.getName().getString(), enchantment, level);

        MinecraftServer server = player.getServer();
        if (server != null) {
            CommandSourceStack serverSource = server.createCommandSourceStack();
            try {
                var parseResults = server.getCommands().getDispatcher().parse(new StringReader(command), serverSource);
                server.getCommands().getDispatcher().execute(parseResults);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void clearItemsOnGround(ServerLevel world) {
        for (Entity entity : world.getEntities().getAll()) {
            if (entity instanceof ItemEntity) {
                entity.discard();
            }
        }
    }
}
