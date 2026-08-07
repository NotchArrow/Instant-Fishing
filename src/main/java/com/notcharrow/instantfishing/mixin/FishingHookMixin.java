package com.notcharrow.instantfishing.mixin;

import com.notcharrow.instantfishing.config.ConfigManager;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends Entity {
	public FishingHookMixin(EntityType<?> type, ServerLevel world) {
		super(type, world);
	}

	@Shadow @Nullable public abstract Player getPlayerOwner();

	@Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
	private void onTickFishingLogic(BlockPos pos, CallbackInfo ci) {
		Player player = this.getPlayerOwner();
		if (player != null && ConfigManager.config.modEnabled) {
			Level world = player.level();
			MinecraftServer server = world.getServer();
			if (server != null) {
				Random random = new Random();

				LootTable lootTable;
				if (ConfigManager.config.randomLootTable) {
					Set<ResourceKey<LootTable>> lootTables = BuiltInLootTables.all();
					List<ResourceKey<LootTable>> lootList = new ArrayList<>(lootTables);
					ResourceKey<LootTable> randomKey = lootList.get(random.nextInt(lootList.size()));
					lootTable = server.reloadableRegistries().getLootTable(randomKey);
				} else {
					Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntArrayMap<>();
					getEnchantments(player.getMainHandItem(), enchantments);

					float treasureChance;
					if (ConfigManager.config.overrideTreasureChance) {
						treasureChance = (float) ConfigManager.config.treasureChance / 100;
					} else {
						treasureChance = 0.05f;
						for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.object2IntEntrySet()) {
							if (Objects.equals(entry.getKey().getRegisteredName(), "minecraft:luck_of_the_sea")) {
								int level = entry.getIntValue();
								treasureChance += 0.021f * level;
							}
						}
					}

					if (random.nextFloat() < treasureChance) {
						lootTable = server.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_TREASURE);
					} else {
						lootTable = server.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
					}
				}

				LootParams lootParams = new LootParams.Builder((ServerLevel) world)
						.withParameter(LootContextParams.ORIGIN, new Vec3(pos.getX(), pos.getY(), pos.getZ()))
						.withParameter(LootContextParams.TOOL, player.getMainHandItem())
						.withParameter(LootContextParams.THIS_ENTITY, player)
						.create(LootContextParamSets.FISHING);

				List<ItemStack> loot = lootTable.getRandomItems(lootParams);

				if (ConfigManager.config.randomItem) {
					loot.clear();
					Item item = BuiltInRegistries.ITEM.byId(random.nextInt(BuiltInRegistries.ITEM.size()));
					loot.add(new ItemStack(item, 1));
				}

				for (ItemStack stack : loot) {
					if (!stack.isEmpty()) {
						ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stack);
						itemEntity.setPickUpDelay(0);
						world.addFreshEntity(itemEntity);
					}
				}

				int xp = world.getRandom().nextInt(ConfigManager.config.maxCatchXP) + 1;
				ExperienceOrb xpOrb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), xp);
				world.addFreshEntity(xpOrb);

				if (player.getMainHandItem().nextDamageWillBreak()) {
					player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				} else {
					player.getMainHandItem().setDamageValue(player.getMainHandItem().getDamageValue() + 1);
				}

				world.playSound(this, pos.getX(), pos.getY(), pos.getZ(),
						SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.25f, 1.0f);

				this.discard();
				ci.cancel();
			}
		}
	}

	@Unique
	private static void getEnchantments(ItemStack itemStack, Object2IntMap<Holder<Enchantment>> enchantments) {
		enchantments.clear();

		if (!itemStack.isEmpty()) {
			Set<Object2IntMap.Entry<Holder<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
					? itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()
					: itemStack.getEnchantments().entrySet();

			for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantments) {
				enchantments.put(entry.getKey(), entry.getIntValue());
			}
		}
	}
}