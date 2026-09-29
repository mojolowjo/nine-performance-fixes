package io.github.mojolowjo.ninefix;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Obsessed (1.5.x, an MCreator mod) sends a player's whole variable set - including long text
 * fields such as stored chat lines - every time any of its procedures touches a variable. Its tick
 * procedures do that dozens of times per tick, so each player got thousands of packets per second
 * (measured: ~19 MB/s of game data per player, nearly all of it this one packet).
 *
 * <p>Server side: all sync requests for a player within one server tick become one packet, sent at the
 * end of that tick with the variables as they are then. Every tick in which the mod asked for a sync
 * still ends with the client holding the server's values, as before; only the in-between copies that
 * the next one overwrote anyway are gone. Until the end-of-tick hook has proven it runs, nothing is
 * held back.
 */
public final class ObsessedSync {

    /** Added to The Obsessed's PlayerVariables by mixin/TheObsessedSyncMixin. */
    public interface Vars {
        /** Runs The Obsessed's own sync (which sends the packet). */
        void ninefix$sendNow(ServerPlayer player);
    }

    private record Pending(ServerPlayer player, Vars vars) {}

    private static final Object LOCK = new Object();
    /** In order of each player's first request this tick; players matched by identity, not equals(). */
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final ThreadLocal<Boolean> SENDING = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static volatile boolean endOfTickHookRuns;
    private static volatile boolean warned;

    /** Called at the start of PlayerVariables.syncPlayerVariables. @return true to skip the send for now. */
    public static boolean intercept(Vars vars, ServerPlayer player) {
        if (SENDING.get() || !endOfTickHookRuns) return false;
        synchronized (LOCK) {
            for (int i = 0; i < PENDING.size(); i++) {
                if (PENDING.get(i).player() == player) {
                    PENDING.set(i, new Pending(player, vars));
                    return true;
                }
            }
            PENDING.add(new Pending(player, vars));
        }
        return true;
    }

    /** Called at the end of every server tick (mixin/ServerTickFlushMixin). */
    public static void flush() {
        endOfTickHookRuns = true;
        List<Pending> work;
        synchronized (LOCK) {
            if (PENDING.isEmpty()) return;
            work = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        SENDING.set(Boolean.TRUE);
        try {
            for (Pending p : work) {
                try {
                    p.vars().ninefix$sendNow(p.player());
                } catch (Throwable t) {
                    if (!warned) {
                        warned = true;
                        NineFix.LOG.warn("The Obsessed network fix: sending a player's variables failed", t);
                    }
                }
            }
        } finally {
            SENDING.set(Boolean.FALSE);
        }
    }

    private ObsessedSync() {}
}
