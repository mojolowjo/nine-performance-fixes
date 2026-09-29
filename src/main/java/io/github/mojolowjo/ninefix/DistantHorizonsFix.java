package io.github.mojolowjo.ninefix;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;

/**
 * Distant Horizons 3.x: cap its worker threads at half the CPU's threads - DH's own default. The NINE
 * config sets 8, i.e. every core on most PCs, and DH then keeps the whole CPU busy while it loads LODs
 * (measured: CPU at 100% for a whole 10-minute session), which starves the game's render thread.
 *
 * <p>Uses DH's public API, so it's a temporary override: nothing in DH's config file changes, and
 * switching this fix off gives back whatever DH is configured to. Only loaded when DH is installed.
 */
final class DistantHorizonsFix {

    static void register() {
        DhApiResult<Void> result = DhApiEventRegister.on(DhApiAfterDhInitEvent.class, new DhApiAfterDhInitEvent() {
            @Override
            public void afterDistantHorizonsInit(DhApiEventParam<Void> param) {
                try {
                    apply();
                } catch (Throwable t) {
                    NineFix.LOG.warn("Couldn't cap Distant Horizons' threads", t);
                }
            }
        });
        if (result != null && !result.success) {
            NineFix.LOG.warn("Couldn't hook Distant Horizons startup: {}", result.message);
        }
    }

    static int cap(int hardwareThreads) {
        return Math.max(1, (int) Math.ceil(hardwareThreads * 0.5));
    }

    private static void apply() {
        IDhApiConfigValue<Integer> threads = DhApi.Delayed.configs.multiThreading().threadCount();
        int cap = cap(Runtime.getRuntime().availableProcessors());
        Integer current = threads.getValue();
        if (current == null || current <= cap) {
            NineFix.LOG.info("Distant Horizons uses {} threads - at or below the cap of {}, left alone", current, cap);
        } else if (threads.setValue(cap, "NINE Performance Fixes")) {
            NineFix.LOG.info("Distant Horizons threads capped at {} (configured: {})", cap, current);
        } else {
            NineFix.LOG.warn("Distant Horizons refused the thread cap of {} (configured: {})", cap, current);
        }
    }

    private DistantHorizonsFix() {}
}
