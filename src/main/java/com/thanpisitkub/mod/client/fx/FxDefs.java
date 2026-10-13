package com.thanpisitkub.mod.client.fx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.fx.EmitterDef;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the particle behaviour definitions (assets/thanpisitkub/bedrock_particles/NAME.json) and the
 * sprite sets that Minecraft builds from assets/thanpisitkub/particles/NAME.json.
 * The definitions reload with F3+T, so a resource pack can tweak them.
 */
public final class FxDefs extends SimplePreparableReloadListener<Map<String, EmitterDef>> {

    private static volatile Map<String, EmitterDef> DEFS = Map.of();
    private static final Map<String, SpriteSet> SPRITES = new ConcurrentHashMap<>();

    public static EmitterDef get(String name) {
        return DEFS.get(name);
    }

    public static SpriteSet sprites(String name) {
        return SPRITES.get(name);
    }

    public static void putSprites(String name, SpriteSet set) {
        SPRITES.put(name, set);
    }

    @Override
    protected Map<String, EmitterDef> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, EmitterDef> out = new HashMap<>();
        Map<ResourceLocation, Resource> found = manager.listResources("bedrock_particles", rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
            ResourceLocation rl = e.getKey();
            if (!rl.getNamespace().equals(ThanpisitkubMod.MOD_ID)) continue;
            String path = rl.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                out.put(name, EmitterDef.fromJson(name, json));
            } catch (Exception ex) {
                ThanpisitkubMod.LOGGER.error("Could not load particle definition {}", rl, ex);
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<String, EmitterDef> loaded, ResourceManager manager, ProfilerFiller profiler) {
        DEFS = loaded;
        ThanpisitkubMod.LOGGER.info("Loaded {} thanpisitkub particle definitions", loaded.size());
    }
}
