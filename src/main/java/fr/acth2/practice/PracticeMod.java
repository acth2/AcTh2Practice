package fr.acth2.practice;

import com.mojang.logging.LogUtils;
import fr.acth2.practice.gameplay.Arena;
import fr.acth2.practice.gameplay.CustomPayloads;
import fr.acth2.practice.misc.PotionFiller;
import fr.acth2.practice.utils.References;
import fr.acth2.practice.misc.PlayerLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.*;

// POUR CONFIGURER LE MOD ALLEZ DANS LA CLASSE REFERENCES !!

@Mod(References.MODID)
public class PracticeMod {
    private static final Queue<ServerPlayer> diamondQueue = new ArrayDeque<>();
    private static final Queue<ServerPlayer> noDebuffQueue = new ArrayDeque<>();

    private static final List<String> toxicList = new ArrayList<>();
    private static final List<String> mutedList = new ArrayList<>();

    private static final Queue<ServerPlayer> inFightList = new ArrayDeque<>();
    private static final Queue<String> disconnectedPlayers = new ArrayDeque<>();
    private static final List<Arena> arenas = new ArrayList<>();
    private static final Logger LOGGER = LogUtils.getLogger();    public PracticeMod() {

        NeoForge.EVENT_BUS.register(this);
        // LES ARENES A ENREGISTRER
        arenas.add(References.kh3sa);
        arenas.add(References.blue0);
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
        PlayerLogger.plog("Bienvenue sur ", player, References.SERVER_NAME);

        if(disconnectedPlayers.contains(player.getName().getString())) {
            PlayerLogger.perr("Veuillez évité de vous déconnecté", player);
            disconnectedPlayers.remove(player.getName().getString());
        }
        resetPlayer(player, null, false);
    }

    @SubscribeEvent
    public void onLivingKnockBack(LivingKnockBackEvent event) {
        if (References.ENABLE_LEGACY_MECHANISM) {
            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                double multipler = 1.2d;

                double originalStrength = event.getStrength();
                double originalRatioX = event.getRatioX();
                double originalRatioZ = event.getRatioZ();
                double legacyStrength = originalStrength * multipler;

                double legacyRatioX = originalRatioX * multipler;
                double legacyRatioZ = originalRatioZ * multipler;

                event.setStrength((float) legacyStrength);
                event.setRatioX(legacyRatioX);
                event.setRatioZ(legacyRatioZ);
            }
        }
    }

    @SubscribeEvent
    public void onPotionThrown(EntityJoinLevelEvent event) {
        if (References.ENABLE_LEGACY_MECHANISM) {
            Entity entity = event.getEntity();
            if (entity instanceof ThrownPotion thrownPotion) {
                Vec3 originalMotion = thrownPotion.getDeltaMovement();

                double scaleFactor = 0.6;
                Vec3 adjustedMotion = originalMotion.scale(scaleFactor);
                thrownPotion.setDeltaMovement(adjustedMotion);
            }
        }
    }

    @SubscribeEvent
    private void onServerTick(ServerTickEvent.Post event) {
        CustomPayloads.clearItemsOnGround(event.getServer().overworld());

        event.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (!inFightList.contains(player)) {
                player.setHealth(20.0F);
                player.clearFire();
            }

            if (!player.getInventory().contains(new ItemStack(Items.DIAMOND)) && !player.getInventory().contains(new ItemStack(Items.CLOCK))) {
                if (!inFightList.contains(player)) {
                    player.getInventory().clearContent();
                    CustomPayloads.giveNodebuffClock(player);
                    player.getInventory().add(new ItemStack(Items.DIAMOND));
                    player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 4));
                }
            }

            NeoForge.EVENT_BUS.addListener((ServerChatEvent chatEvent) -> {
                if (!mutedList.contains(chatEvent.getPlayer().getName().getString())) {
                    String message = chatEvent.getMessage().getString().toLowerCase();
                    ServerPlayer sender = chatEvent.getPlayer();

                    boolean containsBannedWord = References.BANNED_WORDS.stream().anyMatch(message::contains);

                    if (containsBannedWord) {
                        chatEvent.setCanceled(true);

                        if (!toxicList.contains(sender.getName().getString())) {
                            PlayerLogger.perr("Votre message est toxic le prochain avertissement sera suivi d'un ", sender, "mute!");
                            toxicList.add(player.getName().getString());
                        } else {
                            PlayerLogger.perr("Vous etes", sender, " mute!");
                            mutedList.add(sender.getName().getString());
                        }
                    }
                } else {
                    chatEvent.setCanceled(true);
                    PlayerLogger.perr("Vous ne pouvez plus envoyé de messages..", chatEvent.getPlayer());
                }
            });
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

                        // gagnat msg

                        PlayerLogger.plog("============== ", winner, References.PREFIX_SUFFIX);
                        PlayerLogger.plog("Gagnant: ", winner, winner.getName().getString() + " (vous)");
                        PlayerLogger.plog("Perdant: ", winner, loser.getName().getString());
                        PlayerLogger.plog("-------------------", winner);
                        PlayerLogger.plog("Le gagnant n'avais plus que: ", winner, (int) winner.getHealth() + "HP / 20HP");
                        PlayerLogger.plog("============== ", winner, References.PREFIX_SUFFIX);

                        // perdant msg

                        PlayerLogger.perr("============== ", loser, References.PREFIX_SUFFIX);
                        PlayerLogger.perr("Gagnant: ", loser, winner.getName().getString());
                        PlayerLogger.perr("Perdant: ", loser, loser.getName().getString() + " (vous)");
                        PlayerLogger.perr("-------------------", loser);
                        PlayerLogger.perr("Le gagnant n'avais plus que: ", loser, (int) winner.getHealth() + "HP / 20HP");
                        PlayerLogger.perr("============== ", loser, References.PREFIX_SUFFIX);
                        resetPlayers(loser, winner, arena);
                        resetPlayer(loser, null, false);
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
                    arena.getPlayer2().getInventory().add(new ItemStack(Items.CLOCK));
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

    private void giveEquipment(ServerPlayer player, int id) {
        //DIAMOND
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        CustomPayloads.applyEnchant(player, boots, "protection", 4);

        ItemStack leggings = new ItemStack(Items.DIAMOND_LEGGINGS);
        CustomPayloads.applyEnchant(player, leggings, "protection", 4);

        ItemStack chestplate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        CustomPayloads.applyEnchant(player, chestplate, "protection", 4);

        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        CustomPayloads.applyEnchant(player, helmet, "protection", 4);

        player.getInventory().armor.set(0, boots);
        player.getInventory().armor.set(1, leggings);
        player.getInventory().armor.set(2, chestplate);
        player.getInventory().armor.set(3, helmet);

        //NoDebuff & Common
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        CustomPayloads.applyEnchant(player, sword, "sharpness", 5);
        if(id == 1) {
            player.getInventory().add(new ItemStack(Items.ENDER_PEARL, 16));
        }
        player.getInventory().add(new ItemStack(Items.COOKED_BEEF, 64));
        player.getInventory().add(sword);
        if (id == 0) {
            player.getInventory().add(new ItemStack(Items.GOLDEN_APPLE, 5));
        } else if (id == 1) {
            CustomPayloads.givePotions(player, "strong_swiftness", 0);
            CustomPayloads.givePotions(player, "strong_swiftness", 0);
            PotionFiller.fill(player);
        }
    }

    private void resetPlayers(ServerPlayer loser, ServerPlayer winner, Arena arena) {
        resetPlayer(loser, arena, true);
        resetPlayer(winner, arena, true);
    }

    private void resetPlayer(ServerPlayer player2reset, Arena arena, boolean doClean) {
        player2reset.removeAllEffects();
        player2reset.teleportTo(References.SPAWN_POS.getX(), References.SPAWN_POS.getY(), References.SPAWN_POS.getZ());
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