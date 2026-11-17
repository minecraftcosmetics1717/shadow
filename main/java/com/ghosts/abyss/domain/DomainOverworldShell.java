
package com.ghosts.abyss.domain;

import com.ghosts.abyss.Abyss;

import com.ghosts.abyss.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = "abyss",
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DomainOverworldShell {

    private static final String SAVE_NAME =
            "abyss_overworld_shells";

    // Protect restored blocks from the older JC barrier
    // cleanup pass for three seconds.
    private static final Map<String, Long> RECENTLY_RESTORED =
            new HashMap<>();
    public static final int INNER_SPHERE_RADIUS = 21;

    // Allows Abyss to place/remove domain_int while
// preventing external attacks from replacing it.
    private static final ThreadLocal<Boolean> EDITING =
            ThreadLocal.withInitial(() -> false);

    public static boolean isEditing() {
        return EDITING.get();
    }

    public static boolean isActive(
            ServerLevel level,
            Vec3 center,
            int radius
    ) {
        if (level == null || center == null) return false;

        return ShellData.get(level).snapshots.containsKey(
                sphereKey(center, radius)
        );
    }
    private DomainOverworldShell() {}

    private static BlockState barrier() {
        var block = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation(
                        "jujutsucraft",
                        "jujutsu_barrier"
                )
        );

        return block != null
                ? block.defaultBlockState()
                : Blocks.BLACK_CONCRETE.defaultBlockState();
    }

    private static String sphereKey(Vec3 center, int radius) {
        return BlockPos.containing(center).asLong()
                + ":" + radius;
    }

    private static String recentKey(
            ServerLevel level,
            Vec3 center,
            int radius
    ) {
        return level.dimension().location()
                + ":" + sphereKey(center, radius);
    }

    private static boolean isChest(BlockState state) {
        return state.is(Blocks.CHEST)
                || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.ENDER_CHEST);
    }

    public static boolean recentlyRestored(
            ServerLevel level,
            Vec3 center,
            int radius
    ) {
        String key = recentKey(level, center, radius);
        Long expiry = RECENTLY_RESTORED.get(key);

        if (expiry == null) return false;

        if (level.getGameTime() > expiry) {
            RECENTLY_RESTORED.remove(key);
            return false;
        }

        return true;
    }

    public static void place(
            ServerLevel level,
            Vec3 center,
            int radius
    ) {
        if (level == null || center == null) return;

        ShellData data = ShellData.get(level);
        String key = sphereKey(center, radius);

        // Avoid saving the temporary barrier as the
        // original terrain if this gets called twice.
        if (data.snapshots.containsKey(key)) return;

        ListTag savedBlocks = new ListTag();
        data.snapshots.put(key, savedBlocks);
        data.setDirty();

        RECENTLY_RESTORED.remove(
                recentKey(level, center, radius)
        );

        BlockState outer = barrier();

        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);

// Outer breakable black sphere.
        double shellMinSq = (radius - 0.5)
                * (radius - 0.5);

        double shellMaxSq = (radius + 0.5)
                * (radius + 0.5);

// Inner glowing, intangible sphere.
        double innerMinSq = (INNER_SPHERE_RADIUS - 0.5)
                * (INNER_SPHERE_RADIUS - 0.5);

        double innerMaxSq = (INNER_SPHERE_RADIUS + 0.5)
                * (INNER_SPHERE_RADIUS + 0.5);

        BlockState inner = ModBlocks.DOMAIN_INT.get()
                .defaultBlockState();

        int changed = 0;
        EDITING.set(true);
        try {
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int y = -radius - 1; y <= radius + 1; y++) {
                for (int z = -radius - 1; z <= radius + 1; z++) {

                    double distanceSq =
                            x * x + y * y + z * z;

                    if (distanceSq > shellMaxSq) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(
                            cx + x,
                            cy + y,
                            cz + z
                    );

                    BlockState original =
                            level.getBlockState(pos);


                    // Only replace air blocks.
                    // Never destroy or overwrite existing terrain.
                    if (!original.isAir()) {
                        continue;
                    }

                    boolean onOuterShell =
                            distanceSq >= shellMinSq;

                    boolean onInnerShell =
                            distanceSq >= innerMinSq
                                    && distanceSq < innerMaxSq;

                    BlockState replacement;

                    if (onOuterShell) {
                        // Breakable black domain barrier.
                        replacement = outer;

                    } else if (onInnerShell) {
                        // Intangible inner domain layer.
                        replacement = inner;

                    } else {
                        // Leave the sphere's interior untouched.
                        continue;
                    }

                    if (original.equals(replacement)) {
                        continue;
                    }


                    // Save the complete original block state.
                    CompoundTag entry = new CompoundTag();
                    entry.putLong("Pos", pos.asLong());
                    entry.put(
                            "State",
                            NbtUtils.writeBlockState(original)
                    );

                    // Preserve BlockEntity data, when present.
                    BlockEntity blockEntity =
                            level.getBlockEntity(pos);

                    if (blockEntity != null) {
                        entry.put(
                                "BlockEntity",
                                blockEntity.saveWithFullMetadata()
                        );
                    }

                    // Record the block BEFORE changing it.
                    savedBlocks.add(entry);
                    data.setDirty();

                    // Prevent a temporary replacement from
                    // intentionally breaking a stored block
                    // entity's inventory/data.
                    if (blockEntity != null) {
                        level.removeBlockEntity(pos);
                    }

                    // 2 = send the block change to clients.
                    // Avoid ordinary neighbor updates while
                    // temporarily constructing the sphere.
                    if (level.setBlock(pos, replacement, 2)) {
                        changed++;
                    }
                }
            }
        }

        Abyss.LOGGER.info(
                "[Abyss Sphere] Created at {} radius={} changed={}",
                center,
                radius,
                changed
        );
        } finally {
            EDITING.remove();
        }
    }

    public static void clear(
            ServerLevel level,
            Vec3 center,
            int radius
    ) {
        if (level == null || center == null) return;

        String key = sphereKey(center, radius);

        restoreSnapshot(level, key);

        RECENTLY_RESTORED.put(
                recentKey(level, center, radius),
                level.getGameTime() + 60
        );
    }

    private static void restoreSnapshot(
            ServerLevel level,
            String key
    ) {
        ShellData data = ShellData.get(level);

        ListTag savedBlocks = data.snapshots.get(key);
        if (savedBlocks == null) return;

        int restored = 0;
        ListTag failures = new ListTag();
        EDITING.set(true);
        try {
        // Reverse order helps keep restoration predictable.
        for (int i = savedBlocks.size() - 1; i >= 0; i--) {
            CompoundTag entry = savedBlocks.getCompound(i);

            BlockPos pos = BlockPos.of(
                    entry.getLong("Pos")
            );

            try {
                BlockState original = NbtUtils.readBlockState(
                        level.registryAccess()
                                .lookupOrThrow(Registries.BLOCK),
                        entry.getCompound("State")
                );

                level.getChunkAt(pos);

                // Don't drop temporary block entity contents.
                if (level.getBlockEntity(pos) != null) {
                    level.removeBlockEntity(pos);
                }

                level.setBlock(pos, original, 2);

                if (entry.contains(
                        "BlockEntity",
                        Tag.TAG_COMPOUND
                )) {
                    CompoundTag beTag =
                            entry.getCompound("BlockEntity");

                    BlockEntity restoredEntity =
                            BlockEntity.loadStatic(
                                    pos,
                                    original,
                                    beTag
                            );

                    if (restoredEntity != null) {
                        level.setBlockEntity(restoredEntity);
                        restoredEntity.setChanged();
                    }
                }

                restored++;

            } catch (Exception exception) {
                failures.add(entry.copy());

                Abyss.LOGGER.error(
                        "[Abyss Sphere] Failed to restore {}",
                        pos,
                        exception
                );
            }
        }

        if (failures.isEmpty()) {
            data.snapshots.remove(key);
        } else {
            // Keep failed entries so their originals
            // aren't permanently forgotten.
            data.snapshots.put(key, failures);
        }

        data.setDirty();

        Abyss.LOGGER.info(
                "[Abyss Sphere] Restored={} failed={}",
                restored,
                failures.size()
        );
        } finally {
            EDITING.remove();
        }
    }

    // Domain instances are not meant to survive a
    // server restart. Recover any unfinished terrain
    // restoration if the server stopped mid-domain.
    @SubscribeEvent
    public static void onServerStarted(
            ServerStartedEvent event
    ) {
        RECENTLY_RESTORED.clear();

        for (ServerLevel level :
                event.getServer().getAllLevels()) {

            ShellData data = ShellData.get(level);

            List<String> unfinished =
                    new ArrayList<>(data.snapshots.keySet());

            for (String key : unfinished) {
                restoreSnapshot(level, key);
            }
        }
    }

    private static final class ShellData extends SavedData {

        private final Map<String, ListTag> snapshots =
                new HashMap<>();

        private static ShellData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    ShellData::load,
                    ShellData::new,
                    SAVE_NAME
            );
        }

        private static ShellData load(CompoundTag tag) {
            ShellData data = new ShellData();

            ListTag shells = tag.getList(
                    "Shells",
                    Tag.TAG_COMPOUND
            );

            for (int i = 0; i < shells.size(); i++) {
                CompoundTag shell = shells.getCompound(i);

                data.snapshots.put(
                        shell.getString("Key"),
                        shell.getList(
                                "Blocks",
                                Tag.TAG_COMPOUND
                        )
                );
            }

            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag shells = new ListTag();

            for (Map.Entry<String, ListTag> entry :
                    snapshots.entrySet()) {

                CompoundTag shell = new CompoundTag();

                shell.putString(
                        "Key",
                        entry.getKey()
                );

                shell.put(
                        "Blocks",
                        entry.getValue()
                );

                shells.add(shell);
            }

            tag.put("Shells", shells);
            return tag;
        }
    }
}
