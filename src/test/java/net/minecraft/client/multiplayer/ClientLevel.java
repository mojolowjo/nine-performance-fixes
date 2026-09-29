// Test double: a level whose chunks are prepared by the test; counts getChunk calls.
package net.minecraft.client.multiplayer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.chunk.LevelChunk;
public class ClientLevel {
    public final Map<Long, LevelChunk> chunks = new HashMap<>();
    public int getChunkCalls;
    public LevelChunk getChunk(int x, int z) { getChunkCalls++; return chunks.getOrDefault(((long) x << 32) ^ (z & 0xFFFFFFFFL), new LevelChunk()); }
    public int calculateBlockTint(net.minecraft.core.BlockPos pos, net.minecraft.world.level.ColorResolver resolver) { return 0; }
}
