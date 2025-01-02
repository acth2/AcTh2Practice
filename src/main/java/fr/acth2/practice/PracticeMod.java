package fr.acth2.practice;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import fr.acth2.practice.gameplay.Arena;
import fr.acth2.practice.gui.KitSelectionMenu;
import fr.acth2.practice.misc.PotionFiller;
import fr.acth2.practice.utils.References;
import fr.acth2.practice.misc.PlayerLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.*;

@Mod(References.MODID)
public class PracticeMod {
    private static final Queue<ServerPlayer> diamondQueue = new ArrayDeque<>();
    private static final Queue<ServerPlayer> noDebuffQueue = new ArrayDeque<>();

    private static final Queue<ServerPlayer> inFightList = new ArrayDeque<>();
    private static final Queue<String> disconnectedPlayers = new ArrayDeque<>();
    private static final List<Arena> arenas = new ArrayList<>();
    // METTEZ VOTRE SPAWN
    private static final BlockPos SPAWN_POS = new BlockPos(0, 101, 0);

    private static final Logger LOGGER = LogUtils.getLogger();

    public PracticeMod() {
        NeoForge.EVENT_BUS.register(this);
        // LES ARENES QUE LE MOD VA RECONNAITRE EN TANT QUE TEL
        arenas.add(new Arena("kh3sa", new BlockPos(2140, 101, 2103), new BlockPos(2089, 100, 2103)));
        arenas.add(new Arena("blue0", new BlockPos(1063, 101, 1025), new BlockPos(985, 101, 1025)));
    }

    public void clearItemsOnGround(ServerLevel world) {
        for (Entity entity : world.getEntities().getAll()) {
            if (entity instanceof ItemEntity) {
                entity.discard();
            }
        }
    }

    @SubscribeEvent
    private void onPlayerQuit(ServerTickEvent.Post event) {
        for (ServerPlayer players : noDebuffQueue) {
            if (players.hasDisconnected()) {
                noDebuffQueue.remove(players);
                LOGGER.info(players.getName() + " disconnected and removed from the queue");
            }
        }

        for (ServerPlayer players : diamondQueue) {
            if (players.hasDisconnected()) {
                diamondQueue.remove(players);
                LOGGER.info(players.getName() + " disconnected and removed from the queue");
            }
        }
    }

    @SubscribeEvent
    private void onServerStarting(ServerStartingEvent server) {
        LOGGER.info("------------ " + References.NAME);
        LOGGER.info("- The mod is ON");
        LOGGER.info("- Version: " + References.VERSION);
        LOGGER.info("--------------------------");
    }

    @SubscribeEvent
    private void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Item item = event.getItemStack().getItem();

        if (item == Items.DIAMOND) {
            if (!diamondQueue.contains(event.getEntity())) {
                if (!noDebuffQueue.contains(event.getEntity())) {
                    diamondQueue.add((ServerPlayer) event.getEntity());
                    PlayerLogger.plog("Vous avez rejoint la file d'attente ", (ServerPlayer) event.getEntity(), "[DIAMOND] !");
                } else {
                    PlayerLogger.perr("Vous êtes deja dans une file d'attente..", (ServerPlayer) event.getEntity());
                }
            } else {
                PlayerLogger.perr("Vous n'êtes plus dans la file d'attente ", (ServerPlayer) event.getEntity(), "[DIAMOND] !");
                diamondQueue.remove((ServerPlayer) event.getEntity());
            }
        }

        if (item == Items.CLOCK) {
            if (!noDebuffQueue.contains(event.getEntity())) {
                if (!diamondQueue.contains(event.getEntity())) {
                    noDebuffQueue.add((ServerPlayer) event.getEntity());
                    PlayerLogger.plog("Vous avez rejoint la file d'attente ", (ServerPlayer) event.getEntity(), "[NODEBUFF] !");
                } else {
                    PlayerLogger.perr("Vous êtes deja dans une file d'attente..", (ServerPlayer) event.getEntity());
                }
            } else {
                PlayerLogger.perr("Vous n'êtes plus dans la file d'attente ", (ServerPlayer) event.getEntity(), "[NODEBUFF] !");
                noDebuffQueue.remove((ServerPlayer) event.getEntity());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        PlayerLogger.plog("Bienvenue sur ", player, "Arch-JspFrr");

        if(disconnectedPlayers.contains(player.getName().getString())) {
            PlayerLogger.perr("Veuillez évité de vous déconnecté", player);
            disconnectedPlayers.remove(player.getName().getString());
        }
        resetPlayer(player, null, false);
    }

    @SubscribeEvent
    private void onServerTick(ServerTickEvent.Post event) {
        clearItemsOnGround(event.getServer().overworld());

        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (!player.getInventory().contains(new ItemStack(Items.DIAMOND)) && !player.getInventory().contains(new ItemStack(Items.CLOCK))) {
                if (!inFightList.contains(player)) {
                    player.getInventory().add(new ItemStack(Items.CLOCK));
                    player.getInventory().add(new ItemStack(Items.DIAMOND));
                }
            }
        });

        if (diamondQueue.size() >= 2) {
            Arena availableArena = getAvailableArena();
            if (availableArena != null) {
                ServerPlayer player1 = diamondQueue.poll();
                ServerPlayer player2 = diamondQueue.poll();

                if (player1 != null && player2 != null) {
                    startDuel(player1, player2, availableArena, 0);
                }
            }
        }

        if (noDebuffQueue.size() >= 2) {
            Arena availableArena = getAvailableArena();
            if (availableArena != null) {
                ServerPlayer player1 = noDebuffQueue.poll();
                ServerPlayer player2 = noDebuffQueue.poll();

                if (player1 != null && player2 != null) {
                    startDuel(player1, player2, availableArena, 1);
                }
            }
        }

        arenas.forEach(arena -> {
            if (arena.isOccupied()) {
                ServerPlayer loser = arena.checkLoser();
                if (loser != null) {
                    ServerPlayer winner = arena.getOpponent(loser);
                    if (winner != null) {
                        PlayerLogger.plog("Vous avez gagné le duel !", winner);
                        PlayerLogger.perr("Vous avez perdu le duel !", loser);
                        resetPlayers(loser, winner, arena);
                    }
                }

                if(arena.getPlayer1().hasDisconnected()) {
                    resetPlayer(arena.getPlayer2(), arena, true);
                    disconnectedPlayers.add(arena.getPlayer1().getName().getString());
                    PlayerLogger.plog("Vous avez gagné par abandon", arena.getPlayer2());
                    disconnectedPlayers.remove(arena.getPlayer2().getName().getString());
                    arena.getPlayer2().getInventory().add(new ItemStack(Items.CLOCK));
                    arena.getPlayer2().getInventory().add(new ItemStack(Items.DIAMOND));
                }

                if(arena.getPlayer2().hasDisconnected()) {
                    resetPlayer(arena.getPlayer1(), arena, true);
                    disconnectedPlayers.add(arena.getPlayer1().getName().getString());
                    PlayerLogger.plog("Vous avez gagné par abandon", arena.getPlayer1());
                    disconnectedPlayers.remove(arena.getPlayer1().getName().getString());
                    arena.getPlayer1().getInventory().add(new ItemStack(Items.DIAMOND));
                }
            }
        });
    }

    private Arena getAvailableArena() {
        for (Arena arena : arenas) {
            if (!arena.isOccupied()) {
                return arena;
            }
        }
        return null;
    }

    private void startDuel(ServerPlayer player1, ServerPlayer player2, Arena arena, int id) {
        arena.setOccupied(true);
        arena.addPlayers(player1, player2);

        player1.getInventory().clearContent();
        player2.getInventory().clearContent();

        giveEquipment(player1, id);
        giveEquipment(player2, id);

        player1.teleportTo(arena.getPosition1().getX(), arena.getPosition1().getY(), arena.getPosition1().getZ());
        player2.teleportTo(arena.getPosition2().getX(), arena.getPosition2().getY(), arena.getPosition2().getZ());

        PlayerLogger.plog("Combat commencé contre ", player1, player2.getName().getString());
        PlayerLogger.plog("Combat commencé contre ", player2, player1.getName().getString());

        inFightList.add(player1);
        inFightList.add(player2);
    }

    private static void applyEnchant(ServerPlayer player, ItemStack itemStack, String enchantment, int level) {
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

    private void giveEquipment(ServerPlayer player, int id) {
        //DIAMOND
        if (id == 0) {
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            applyEnchant(player, boots, "protection", 4);

            ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
            applyEnchant(player, leggings, "protection", 4);

            ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            applyEnchant(player, chestplate, "protection", 4);

            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            applyEnchant(player, helmet, "protection", 4);

            player.getInventory().armor.set(0, boots);
            player.getInventory().armor.set(1, leggings);
            player.getInventory().armor.set(2, chestplate);
            player.getInventory().armor.set(3, helmet);

            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            applyEnchant(player, sword, "sharpness", 5);

            player.getInventory().add(sword);
            player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 5));
            player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 64));
        }

        if (id == 1) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            applyEnchant(player, sword, "sharpness", 5);
            applyEnchant(player, sword, "fire_aspect", 2);

            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            applyEnchant(player, boots, "protection", 4);
            applyEnchant(player, boots, "unbreaking", 3);

            ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
            applyEnchant(player, leggings, "protection", 4);
            applyEnchant(player, leggings, "unbreaking", 3);

            ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
            applyEnchant(player, chestplate, "protection", 4);
            applyEnchant(player, chestplate, "unbreaking", 3);

            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            applyEnchant(player, helmet, "protection", 4);
            applyEnchant(player, helmet, "unbreaking", 3);

            player.getInventory().armor.set(0, boots);
            player.getInventory().armor.set(1, leggings);
            player.getInventory().armor.set(2, chestplate);
            player.getInventory().armor.set(3, helmet);

            player.getInventory().add(sword);
            player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 64));

            givePotions(player, "swiftness", 0);
            givePotions(player, "swiftness", 0);
            givePotions(player, "fire_resistance", 0);
            PotionFiller.fill(player);
        }
    }

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


    private void resetPlayers(ServerPlayer loser, ServerPlayer winner, Arena arena) {
        resetPlayer(loser, arena, true);
        resetPlayer(winner, arena, true);
    }

    private void resetPlayer(ServerPlayer player2reset, Arena arena, boolean doClean) {
        player2reset.removeAllEffects();
        player2reset.teleportTo(SPAWN_POS.getX(), SPAWN_POS.getY(), SPAWN_POS.getZ());
        player2reset.getInventory().clearContent();
        noDebuffQueue.remove(player2reset);
        diamondQueue.remove(player2reset);

        inFightList.remove(player2reset);
        if(doClean) {
            arena.clearPlayers();
            arena.setOccupied(false);
        }
    }
}
