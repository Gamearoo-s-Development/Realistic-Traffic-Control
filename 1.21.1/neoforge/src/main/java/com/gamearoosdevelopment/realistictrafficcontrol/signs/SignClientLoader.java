package com.gamearoosdevelopment.realistictrafficcontrol.signs;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Client-only loader for legacy {@code tc_signpacks/*.zip} sign packs. */
final class SignClientLoader {

    static void loadExternalPacks(SignRepository repository) {
        Path packsDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("tc_signpacks");
        if (!Files.isDirectory(packsDirectory)) {
            return;
        }
        try (var paths = Files.list(packsDirectory)) {
            paths.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .forEach(path -> loadPack(repository, path));
        } catch (Exception ex) {
            ModRealisticTrafficControl.LOGGER.error("Could not scan tc_signpacks.", ex);
        }
    }

    private static void loadPack(SignRepository repository, Path path) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry jsonEntry = zip.getEntry("signs.json");
            if (jsonEntry == null) {
                ModRealisticTrafficControl.LOGGER.error("Sign pack {} has no signs.json", path.getFileName());
                return;
            }
            JsonObject json;
            try (InputStream stream = zip.getInputStream(jsonEntry);
                    InputStreamReader reader = new InputStreamReader(stream)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            UUID packId = UUID.fromString(json.get("pack_id").getAsString());
            repository.processExternalSignFile(json);
            String marker = "/signs/" + packId + "/";
            for (Sign sign : repository.getAllSigns()) {
                if (sign.getFrontImageResourceLocation().getPath().contains(marker)) {
                    loadTexture(zip, sign.getFrontImageResourceLocation(), marker);
                    loadTexture(zip, sign.getBackImageResourceLocation(), marker);
                }
            }
        } catch (Exception ex) {
            ModRealisticTrafficControl.LOGGER.error("Could not load sign pack {}", path.getFileName(), ex);
        }
    }

    private static void loadTexture(ZipFile zip, ResourceLocation location, String marker) throws Exception {
        String path = location.getPath();
        int markerIndex = path.indexOf(marker);
        if (markerIndex < 0) {
            return;
        }
        String entryName = path.substring(markerIndex + marker.length());
        ZipEntry imageEntry = zip.getEntry(entryName);
        if (imageEntry == null) {
            ModRealisticTrafficControl.LOGGER.error("Missing sign-pack texture {}", entryName);
            return;
        }
        try (InputStream stream = zip.getInputStream(imageEntry)) {
            NativeImage image = NativeImage.read(stream);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
        }
    }

    private SignClientLoader() {
    }
}
