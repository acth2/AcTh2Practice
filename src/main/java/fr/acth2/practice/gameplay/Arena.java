package fr.acth2.practice.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/*
 * AIDE :
 * Vous devez construire l'arene sur votre monde et la définir en utilisant cette classe dans la classe Principal (PracticeMod)
 * Tout simplement copiez la ligne code 26 et répétez la en méttant VOS informations !
 * 
 */

public class Arena {
    private final String name;
    private final BlockPos position1;
    private final BlockPos position2;
    private final Set<ServerPlayer> players;
    private ServerPlayer classPlayer1;
    private ServerPlayer classPlayer2;
    private boolean isOccupied;

    public Arena(String name, BlockPos position1, BlockPos position2) {
        this.name = name;
        this.position1 = position1;
        this.position2 = position2;
        this.players = new HashSet<>();
        this.isOccupied = false;
    }

    public String getName() {
        return name;
    }

    public BlockPos getPosition1() {
        return position1;
    }

    public BlockPos getPosition2() {
        return position2;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public void setOccupied(boolean occupied) {
        isOccupied = occupied;
    }

    public void addPlayers(ServerPlayer player1, ServerPlayer player2) {
        classPlayer1 = player1;
        classPlayer2 = player2;

        players.add(player1);
        players.add(player2);
    }

    public ServerPlayer getPlayer1() {
        return classPlayer1;
    }

    public ServerPlayer getPlayer2() {
        return classPlayer2;
    }

    public void clearPlayers() {
        players.clear();
    }

    public boolean hasPlayer(ServerPlayer player) {
        return players.contains(player);
    }

    public ServerPlayer getOpponent(ServerPlayer player) {
        for (ServerPlayer p : players) {
            if (!p.equals(player)) {
                return p;
            }
        }
        return null;
    }

    public ServerPlayer checkLoser() {
        for (ServerPlayer player : players) {
            if (player.isDeadOrDying()) {
                return player;
            }
        }
        return null;
    }
}
