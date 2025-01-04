package fr.acth2.practice.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KitSelectionMenu extends ChestMenu {

    public KitSelectionMenu(MenuType<?> type, int containerId, Inventory playerInventory) {
        super(type, containerId, playerInventory, new SimpleContainer(9), 1);

        this.slots.get(0).set(new ItemStack(Items.POTION));
        this.slots.get(1).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
    }
}
