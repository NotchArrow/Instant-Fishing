package com.notcharrow.instantfishing.mixin;

import com.notcharrow.instantfishing.config.ConfigManager;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.LootCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin extends Entity {
	public FishingBobberEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Shadow @Nullable public abstract PlayerEntity getPlayerOwner();

	@Inject(method = "tickFishingLogic", at = @At("HEAD"), cancellable = true)
	private void onTickFishingLogic(BlockPos pos, CallbackInfo ci) {
		PlayerEntity player = this.getPlayerOwner();
		if (player != null && ConfigManager.config.modEnabled) {
			MinecraftServer server = player.getServer();
			if (server != null) {
				ServerWorld world = (ServerWorld) player.getWorld();
				Random random = new Random();

				LootTable lootTable;
				if (ConfigManager.config.randomLootTable) {
					Set<RegistryKey<LootTable>> lootTables = LootTables.getAll();
					List<RegistryKey<LootTable>> lootList = new ArrayList<>(lootTables);
					RegistryKey<LootTable> randomKey = lootList.get(random.nextInt(lootList.size()));
					lootTable = server.getReloadableRegistries().getLootTable(randomKey);
				} else {
					Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntArrayMap<>();
					getEnchantments(player.getMainHandStack(), enchantments);

					float treasureChance;
					if (ConfigManager.config.overrideTreasureChance) {
						treasureChance = (float) ConfigManager.config.treasureChance / 100;
					} else {
						treasureChance = 0.05f;
						for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry: enchantments.object2IntEntrySet()) {
							if (Objects.equals(entry.getKey().getIdAsString(), "minecraft:luck_of_the_sea")) {
								int level = entry.getIntValue();
								treasureChance += 0.021f * level;
							}
						}
					}

					if (random.nextFloat() < treasureChance) {
						lootTable = server.getReloadableRegistries().getLootTable(LootTables.FISHING_TREASURE_GAMEPLAY);
					} else {
						lootTable = server.getReloadableRegistries().getLootTable(LootTables.FISHING_GAMEPLAY);
					}
				}

				LootWorldContext lootWorldContext = (new LootWorldContext.Builder(((ServerWorld) player.getWorld()).toServerWorld()))
						.add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
						.add(LootContextParameters.TOOL, player.getMainHandStack())
						.add(LootContextParameters.THIS_ENTITY, player)
						.build(LootContextTypes.FISHING);

				List<ItemStack> loot = lootTable.generateLoot(lootWorldContext);

				for (ItemStack stack : loot) {
					if (!stack.isEmpty()) {
						ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stack);
						itemEntity.setPickupDelay(0);
						world.spawnEntity(itemEntity);
					}
				}

				int xp = player.getWorld().getRandom().nextInt(ConfigManager.config.maxCatchXP) + 1;
				ExperienceOrbEntity xpOrb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), xp);
				world.spawnEntity(xpOrb);

				if (player.getMainHandStack().willBreakNextUse()) {
					player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
				} else {
					player.getMainHandStack().damage(1, player);
				}

				world.playSound(this, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS);

				this.discard();
				ci.cancel();
			}
		}
	}

	@Unique
	private static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
		enchantments.clear();

		if (!itemStack.isEmpty()) {
			Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
					? itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).getEnchantmentEntries()
					: itemStack.getEnchantments().getEnchantmentEntries();

			for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantments) {
				enchantments.put(entry.getKey(), entry.getIntValue());
			}
		}
	}
}