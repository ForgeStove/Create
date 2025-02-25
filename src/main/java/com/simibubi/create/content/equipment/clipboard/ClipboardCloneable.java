package com.simibubi.create.content.equipment.clipboard;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
public interface ClipboardCloneable {
	String getClipboardKey();
	boolean writeToClipboard(CompoundTag tag, Direction side);
	boolean readFromClipboard(CompoundTag tag, Player player, Direction side, boolean simulate);
}
