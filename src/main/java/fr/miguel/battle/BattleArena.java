package fr.miguel.battle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

/**
 * 
 */
public final class BattleArena {

    private static final int MIN = -16, MAX = 31, FLOOR_Y = 64, HEIGHT = 14;

    public static final Vec3 SPAWN_A = new Vec3(0.5, FLOOR_Y + 1, 7.5);
    public static final Vec3 SPAWN_B = new Vec3(14.5, FLOOR_Y + 1, 7.5);

    public static final float YAW_A = -90f;
    public static final float YAW_B = 90f;

    private final RuntimeLevelHandle handle;
    private final ServerLevel level;

    private BattleArena(RuntimeLevelHandle handle) {
        this.handle = handle;
        this.level = handle.asLevel();
    }

    public static BattleArena create(MinecraftServer server) {
        Holder<Biome> biome = server.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS);

        RuntimeLevelConfig config = new RuntimeLevelConfig()
                .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                .setDifficulty(Difficulty.NORMAL)
                .setGenerator(new VoidChunkGenerator(biome))
                .setSeed(0L);

        BattleArena arena = new BattleArena(Fantasy.get(server).openTemporaryLevel(config));

        arena.build();

        return arena;

    }

    private void build() {

        for (int cx = MIN >> 4; cx <= MAX >> 4; cx++)
            for (int cz = MIN >> 4; cz <= MAX >> 4; cz++)
                level.setChunkForced(cx, cz, true);

        BlockState lantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState dark = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = MIN; x <= MAX; x++) {
            for (int z = MIN; z <= MAX; z++) {
                set(pos.set(x, FLOOR_Y, z), ((x + z) & 1) == 0 ? lantern : dark);
                set(pos.set(x, FLOOR_Y + HEIGHT, z), barrier);                      // plafond
                if (x == MIN || x == MAX || z == MIN || z == MAX)
                    for (int y = FLOOR_Y + 1; y < FLOOR_Y + HEIGHT; y++)
                        set(pos.set(x, y, z), barrier);                             // murs
            }
        }
    }

    private void set(BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    public ServerLevel level() { return level; }

    public void close() { handle.delete(); }
}
