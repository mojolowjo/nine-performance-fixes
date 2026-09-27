// Test double for GeckoLib's cache: records which namespaces the mod asks GeckoLib to skip.
package software.bernie.geckolib.cache;

import java.util.Set;
import java.util.TreeSet;

public final class GeckoLibCache {
    public static final Set<String> EXCLUDED = new TreeSet<>();

    public static synchronized void registerNamespaceExclusion(String namespace) {
        EXCLUDED.add(namespace);
    }
}
