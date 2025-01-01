package fr.acth2.practice;

import com.mojang.logging.LogUtils;
import fr.acth2.practice.gameplay.Arena;
import fr.acth2.practice.utils.References;
import fr.acth2.practice.misc.PlayerLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

@Mod(References.MODID)
public class PracticeMod {
    private static final Queue<ServerPlayer> queue = new ArrayDeque<>();
    private static final List<Arena> arenas = new ArrayList<>();
    // METTEZ VOTRE SPAWN
    private static final BlockPos SPAWN_POS = new BlockPos(0, 100, 0);

    private static final Logger LOGGER = LogUtils.getLogger();

    public PracticeMod() {
        NeoForge.EVENT_BUS.register(this);
        // LES ARENES QUE LE MOD VA RECONNAITRE EN TANT QUE TEL
        arenas.add(new Arena("Arena1", new BlockPos(1000, 100, 1000), new BlockPos(1010, 100, 1010)));
        arenas.add(new Arena("Arena2", new BlockPos(1100, 100, 1100), new BlockPos(1110, 100, 1110)));
    }

    @SubscribeEvent
    private void onPlayerQuit(ServerTickEvent.Post event) {
        for (ServerPlayer players : queue) {
            if (players.hasDisconnected()) {
                queue.remove(players);
                LOGGER.info(players.getName() + " disconnected and removed from the queue");
            }
        }
    }

    @SubscribeEvent
    private void onServerStarting(ServerStartingEvent server) {
        LOGGER.info("acth2practice is ON");
    }

    @SubscribeEvent
    private void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        Item item = event.getItemStack().getItem();

        if (item == Items.CLOCK) {
            if (!queue.contains(event.getEntity())) {
                queue.add((ServerPlayer) event.getEntity());
                PlayerLogger.plog("Vous avez rejoint la file d'attente !", (ServerPlayer) event.getEntity());
            } else {
                PlayerLogger.perr("Vous n'êtes plus dans la file d'attente !", (ServerPlayer) event.getEntity());
                queue.remove((ServerPlayer) event.getEntity());
            }
        }
    }

    @SubscribeEvent
    private void onServerTick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (!player.getInventory().contains(new ItemStack(Items.CLOCK))) {
                player.getInventory().add(new ItemStack(Items.CLOCK));
            }
        });

        if (queue.size() >= 2) {
            Arena availableArena = getAvailableArena();
            if (availableArena != null) {
                ServerPlayer player1 = queue.poll();
                ServerPlayer player2 = queue.poll();

                if (player1 != null && player2 != null) {
                    startDuel(player1, player2, availableArena);
                }
            }
        }

        arenas.forEach(arena -> {
            if (arena.isOccupied()) {
                ServerPlayer loser = arena.checkLoser();
                if (loser != null) {
                    ServerPlayer winner = arena.getOpponent(loser);
                    if (winner != null) {
                        winner.sendSystemMessage(Component.nullToEmpty("Vous avez gagné le duel !"));
                    }
                    resetPlayers(loser, winner, arena);
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

    private void startDuel(ServerPlayer player1, ServerPlayer player2, Arena arena) {
        arena.setOccupied(true);
        arena.addPlayers(player1, player2);

        player1.getInventory().clearContent();
        player2.getInventory().clearContent();

        giveEquipment(player1);
        giveEquipment(player2);

        player1.teleportTo(arena.getPosition1().getX(), arena.getPosition1().getY(), arena.getPosition1().getZ());
        player2.teleportTo(arena.getPosition2().getX(), arena.getPosition2().getY(), arena.getPosition2().getZ());

        player1.sendSystemMessage(Component.nullToEmpty("Combat commencé contre " + player2.getName().getString() + " !"));
        player2.sendSystemMessage(Component.nullToEmpty("Combat commencé contre " + player1.getName().getString() + " !"));
    }

    private void giveEquipment(ServerPlayer player) {
        player.getInventory().add(new ItemStack(Items.DIAMOND_SWORD));
        player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 5));
        player.getInventory().add(new ItemStack(Items.DIAMOND_HELMET));
        player.getInventory().add(new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.getInventory().add(new ItemStack(Items.DIAMOND_LEGGINGS));
        player.getInventory().add(new ItemStack(Items.DIAMOND_BOOTS));
    }

    private void resetPlayers(ServerPlayer loser, ServerPlayer winner, Arena arena) {
        loser.teleportTo(SPAWN_POS.getX(), SPAWN_POS.getY(), SPAWN_POS.getZ());
        winner.teleportTo(SPAWN_POS.getX(), SPAWN_POS.getY(), SPAWN_POS.getZ());

        loser.getInventory().clearContent();
        winner.getInventory().clearContent();

        arena.clearPlayers();
        arena.setOccupied(false);
    }
}
