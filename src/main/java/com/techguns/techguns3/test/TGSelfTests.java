package com.techguns.techguns3.test;

import com.techguns.techguns3.TechGuns3;
import net.minecraft.server.MinecraftServer;

/**
 * Headless self-test entry points. Selected from outside via the
 * {@code TECHGUNS3_SELFTEST} environment variable ({@code autofire} or
 * {@code mobs}); production servers simply never set it. The env contract
 * lives here instead of the mod class so the suites own their own wiring.
 */
public final class TGSelfTests {
    private TGSelfTests() {}

    /** Run the requested self-test suite on server start, if any was requested. */
    public static void maybeRun(MinecraftServer server) {
        String suite = System.getenv("TECHGUNS3_SELFTEST");
        if ("autofire".equals(suite)) {
            TechGuns3.LOGGER.info("[TechGuns3] TECHGUNS3_SELFTEST=autofire, running headless gun self-test");
            GunSelfTestCommand.runHeadless(server);
        } else if ("mobs".equals(suite)) {
            TechGuns3.LOGGER.info("[TechGuns3] TECHGUNS3_SELFTEST=mobs, running headless armed-mob self-test");
            MobSelfTestCommand.runHeadless(server);
        }
    }
}
