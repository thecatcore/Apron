/**
 * This software is provided under the terms of the Minecraft Forge Public
 * License v1.1.
 */
package forge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public interface IDestroyToolHandler {
	void onDestroyCurrentItem(PlayerEntity arg, ItemStack arg2);
}
