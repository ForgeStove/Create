package com.simibubi.create.content.contraptions.behaviour.dispenser;
public record DispenseItemLocation(boolean internal, int slot) {
	public static final DispenseItemLocation NONE = new DispenseItemLocation(false, -1);
	public boolean isEmpty() {
		return slot < 0;
	}
}
