package com.techguns.techguns3.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GunDefinition;
import com.techguns.techguns3.registry.GunRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/** Server-side encode + broadcast of gun definitions. */
public final class GunServerSync {
    private GunServerSync() {}

    private static final Gson GSON = new GsonBuilder().create();

    public static GunPackets.GunSync encodeCurrent() {
        Map<Identifier, GunDefinition> defs = GunRegistry.snapshot();
        Map<Identifier, String> json = new HashMap<>();
        for (var entry : defs.entrySet()) {
            try {
                JsonElement el = GunDefinition.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow();
                json.put(entry.getKey(), GSON.toJson(el));
            } catch (Exception e) {
                TechGuns3.LOGGER.error("[TechGuns3] failed to encode gun {}", entry.getKey(), e);
            }
        }
        return new GunPackets.GunSync(json);
    }

    public static void sendTo(ServerPlayer player) {
        try {
            PacketDistributor.sendToPlayer(player, encodeCurrent());
        } catch (Exception e) {
            TechGuns3.LOGGER.error("[TechGuns3] gun sync to {} failed", player.getScoreboardName(), e);
        }
    }

    public static void broadcast() {
        try {
            PacketDistributor.sendToAllPlayers(encodeCurrent());
        } catch (Exception e) {
            TechGuns3.LOGGER.error("[TechGuns3] gun sync broadcast failed", e);
        }
    }
}
