package com.simibubi.create.foundation.utility;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlockEntityTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
public final class NBTProcessors {
	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> processors = new HashMap<>();
	private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> survivalProcessors = new HashMap<>();
	// Triggered by block tag, not BE type
	private static final UnaryOperator<CompoundTag> signProcessor = data -> {
		for (String key : List.of("front_text", "back_text")) {
			CompoundTag textTag = data.getCompound(key);
			if (!textTag.contains("messages", Tag.TAG_LIST)) continue;
			for (Tag tag : textTag.getList("messages", Tag.TAG_STRING))
				if (tag instanceof StringTag stringTag)
					if (textComponentHasClickEvent(stringTag.getAsString())) return null;
		}
		if (data.contains("front_item") || data.contains("back_item"))
			return null; // "Amendments" compat: sign data contains itemstacks
		return data;
	};
	static {
		addProcessor(
				BlockEntityType.LECTERN, data -> {
					if (!data.contains("Book", Tag.TAG_COMPOUND)) return data;
					CompoundTag book = data.getCompound("Book");
					// Writable books can't have click events, so they're safe to keep
					ResourceLocation writableBookResource = ForgeRegistries.ITEMS.getKey(Items.WRITABLE_BOOK);
					if (writableBookResource != null && book.getString("id").equals(writableBookResource.toString()))
						return data;
					if (!book.contains("tag", Tag.TAG_COMPOUND)) return data;
					CompoundTag tag = book.getCompound("tag");
					if (!tag.contains("pages", Tag.TAG_LIST)) return data;
					ListTag pages = tag.getList("pages", Tag.TAG_STRING);
					for (Tag inbt : pages) {
						if (textComponentHasClickEvent(inbt.getAsString())) return null;
					}
					return data;
				}
		);
		addProcessor(AllBlockEntityTypes.CREATIVE_CRATE.get(), itemProcessor("Filter"));
		addProcessor(AllBlockEntityTypes.PLACARD.get(), itemProcessor("Item"));
		addProcessor(
				AllBlockEntityTypes.CLIPBOARD.get(), data -> {
					if (!data.contains("Item", Tag.TAG_COMPOUND)) return data;
					CompoundTag book = data.getCompound("Item");
					if (!book.contains("tag", Tag.TAG_COMPOUND)) return data;
					CompoundTag itemData = book.getCompound("tag");
					for (List<String> entries : NBTHelper.readCompoundList(
							itemData.getList("Pages", Tag.TAG_COMPOUND),
							pageTag -> NBTHelper.readCompoundList(
									pageTag.getList("Entries", Tag.TAG_COMPOUND),
									tag -> tag.getString("Text")
							)
					)) {
						for (String entry : entries)
							if (textComponentHasClickEvent(entry)) return null;
					}
					return data;
				}
		);
	}
	private NBTProcessors() {
	}
	public static synchronized void addProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
		processors.put(type, processor);
	}
	public static synchronized void addSurvivalProcessor(
			BlockEntityType<?> type,
			UnaryOperator<CompoundTag> processor
	) {
		survivalProcessors.put(type, processor);
	}
	public static UnaryOperator<CompoundTag> itemProcessor(String tagKey) {
		return data -> {
			CompoundTag compound = data.getCompound(tagKey);
			if (!compound.contains("tag", 10)) return data;
			CompoundTag itemTag = compound.getCompound("tag");
			HashSet<String> keys = new HashSet<>(itemTag.getAllKeys());
			for (String key : keys)
				if (isUnsafeItemNBTKey(key)) itemTag.remove(key);
			if (itemTag.isEmpty()) compound.remove("tag");
			return data;
		};
	}
	public static boolean isUnsafeItemNBTKey(String name) {
		return !name.equals(EnchantedBookItem.TAG_STORED_ENCHANTMENTS)
				&& !name.equals("Enchantments")
				&& !name.contains("Potion")
				&& !name.contains("Damage")
				&& !name.equals("display");
	}
	public static ItemStack withUnsafeNBTDiscarded(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null) return stack;
		ItemStack copy = stack.copy();
		copy.setTag(withUnsafeNBTDiscarded(tag));
		return copy;
	}
	public static CompoundTag withUnsafeNBTDiscarded(CompoundTag tag) {
		if (tag == null) return null;
		CompoundTag copy = tag.copy();
		tag.getAllKeys().stream().filter(NBTProcessors::isUnsafeItemNBTKey).forEach(copy::remove);
		return copy;
	}
	public static boolean textComponentHasClickEvent(String json) {
		return textComponentHasClickEvent(Objects.requireNonNull(Component.Serializer.fromJson(json.isEmpty()
				? "\"\""
				: json)));
	}
	public static boolean textComponentHasClickEvent(Component component) {
		for (Component sibling : component.getSiblings())
			if (textComponentHasClickEvent(sibling)) return true;
		component.getStyle();
		return component.getStyle().getClickEvent() != null;
	}
	@Nullable
	public static CompoundTag process(
			BlockState blockState,
			BlockEntity blockEntity,
			CompoundTag compound,
			boolean survival
	) {
		if (compound == null) return null;
		BlockEntityType<?> type = blockEntity.getType();
		if (survival && survivalProcessors.containsKey(type)) compound = survivalProcessors.get(type).apply(compound);
		if (compound != null && processors.containsKey(type)) return processors.get(type).apply(compound);
		if (blockEntity instanceof SpawnerBlockEntity) return compound;
		if (blockState.is(BlockTags.ALL_SIGNS)) return signProcessor.apply(compound);
		if (blockEntity.onlyOpCanSetNbt()) return null;
		return compound;
	}
}
