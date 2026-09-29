// Test double.
package net.minecraft.world.level.chunk;
import java.util.HashMap;
import java.util.Map;
public class LevelChunk {
    public final Map<Object, Object> blockEntities = new HashMap<>();
    public Map<?, ?> getBlockEntities() { return blockEntities; }
}
