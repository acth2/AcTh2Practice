package fr.acth2.practice.misc;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

public class PlayerLogger {
    public static void plog(String log, ServerPlayer sp, Object... vars) {
        MutableComponent baseMessage = Component.literal(log)
                .withStyle(Style.EMPTY.withColor(0xFFFF55));

        for (Object var : vars) {
            MutableComponent variableComponent = Component.literal(String.valueOf(var))
                    .withStyle(Style.EMPTY.withColor(0xFFAA00));
            baseMessage.append(variableComponent);
        }

        sp.sendSystemMessage(baseMessage);
    }

    public static void perr(String log, ServerPlayer sp, Object... vars) {
        MutableComponent baseMessage = Component.literal(log)
                .withStyle(Style.EMPTY.withColor(0xFF5555));

        for (Object var : vars) {
            MutableComponent variableComponent = Component.literal(String.valueOf(var))
                    .withStyle(Style.EMPTY.withColor(0xAA0000));
            baseMessage.append(variableComponent);
        }
        sp.sendSystemMessage(baseMessage);
    }
}
