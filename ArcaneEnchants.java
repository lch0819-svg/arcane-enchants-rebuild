package tw.plantuniverse.enchant;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Initial gameplay implementation for the Vein Miner enchantment.
 * Target: Minecraft 26.3 / Fabric API. Build and in-game validation are still required.
 */
public final class ArcaneEnchants implements ModInitializer {
    public static final String MOD_ID = "arcane_enchants";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final ThreadLocal<Boolean> BREAKING_EXTRA_BLOCKS = ThreadLocal.withInitial(() -> false);

    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register(ArcaneEnchants::afterBlockBreak);
        LOGGER.info("Arcane Enchants initialized: Vein Miner callback registered.");
    }

    private static void afterBlockBreak(Level world, Player player, BlockPos brokenPos,
                                        BlockState brokenState, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel serverLevel) || player.isSpectator()
                || BREAKING_EXTRA_BLOCKS.get()) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        int level = getVeinMinerLevel(serverLevel, tool);
        if (level <= 0 || !tool.isCorrectToolForDrops(brokenState)) return;

        // Level 1 mines a 3x3 plane; higher levels expand depth only up to three layers.
        int radius = 1;
        int depth = Math.min(level, 3);
        Direction normal = dominantLookDirection(player);
        Direction horizontal = normal.getAxis() == Direction.Axis.Y ? Direction.EAST : normal.getClockWise();
        Direction vertical = normal.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
        int depthCount = Math.max(1, depth);

        BREAKING_EXTRA_BLOCKS.set(true);
        try {
            for (int d = 0; d < depthCount; d++) {
                for (int a = -radius; a <= radius; a++) {
                    for (int b = -radius; b <= radius; b++) {
                        if (d == 0 && a == 0 && b == 0) continue;
                        BlockPos target = brokenPos.relative(normal, d)
                                .relative(horizontal, a).relative(vertical, b);
                        if (!serverLevel.isLoaded(target)) continue;
                        BlockState targetState = serverLevel.getBlockState(target);
                        // Only chain-break the same block type as the original block. This avoids
                        // unexpectedly mining mixed materials in the 3x3 area.
                        if (targetState.isAir() || targetState.getBlock() != brokenState.getBlock()
                                || targetState.getDestroySpeed(serverLevel, target) < 0
                                || targetState.getBlock() == Blocks.BEDROCK
                                || serverLevel.getBlockEntity(target) != null
                                || !tool.isCorrectToolForDrops(targetState)) continue;
                        // Drops are preserved. Durability handling for extra blocks will be tightened
                        // after validating the exact 26.3 interaction-manager API.
                        serverLevel.destroyBlock(target, true, player);
                    }
                }
            }
        } finally {
            BREAKING_EXTRA_BLOCKS.set(false);
        }
    }

    private static int getVeinMinerLevel(ServerLevel level, ItemStack stack) {
        ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "vein_miner"));
        Optional<Holder.Reference<Enchantment>> holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).get(key);
        return holder.map(value -> EnchantmentHelper.getItemEnchantmentLevel(value, stack)).orElse(0);
    }

    private static Direction dominantLookDirection(Player player) {
        return Direction.getNearest(player.getLookAngle().x, player.getLookAngle().y,
                player.getLookAngle().z);
    }
}
