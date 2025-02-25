package com.simibubi.create.content.schematics.client.tools;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
public interface ISchematicTool {
	void init();
	void updateSelection();
	boolean handleRightClick();
	boolean handleMouseWheel(double delta);
	void renderTool(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera);
	void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height);
	void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer);
}
