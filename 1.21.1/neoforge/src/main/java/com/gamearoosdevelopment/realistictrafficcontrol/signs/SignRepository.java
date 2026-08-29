package com.gamearoosdevelopment.realistictrafficcontrol.signs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign.TextLine;
import com.gamearoosdevelopment.realistictrafficcontrol.util.Tuple;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Port of 1.12.2 {@code SignRepository}. Loads bundled {@code misc/signs.json}; external signpack ZIP
 * loading is deferred to a client-only hook.
 */
public class SignRepository {
    private final HashMap<Tuple<String, Integer>, Sign> signsByTypeVariant = new HashMap<>();
    private final HashMap<UUID, Sign> signsByID = new HashMap<>();
    private final HashMap<String, String> friendlyTypesByName = new HashMap<>();
    private final HashMap<UUID, String> packNamesByID = new HashMap<>();
    private final ArrayList<Sign> allSigns = new ArrayList<>();
    private boolean signsInitialized;

    public void reload() {
        signsByTypeVariant.clear();
        signsByID.clear();
        friendlyTypesByName.clear();
        packNamesByID.clear();
        allSigns.clear();
        signsInitialized = false;
        init(str -> {}, steps -> {});
    }

    public void init(Consumer<String> splashUpdate, IntConsumer maximumUpdate) {
        if (signsInitialized) {
            return;
        }
        try (InputStream jsonStream = getBaseJson()) {
            JsonElement parser = JsonParser.parseReader(new InputStreamReader(jsonStream));
            processSignFile(parser.getAsJsonObject(), null, splashUpdate, maximumUpdate);
        } catch (Exception ex) {
            ModRealisticTrafficControl.LOGGER.error("Could not process base signpack.", ex);
        }
        signsInitialized = true;
    }

    private InputStream getBaseJson() {
        return ModRealisticTrafficControl.class.getClassLoader()
                .getResourceAsStream("assets/realistictrafficcontrol/misc/signs.json");
    }

    private void processSignFile(JsonObject signsFile, Object zipFile, Consumer<String> splashUpdate,
            IntConsumer stepsUpdate) throws Exception {
        UUID packID = UUID.fromString(signsFile.get("pack_id").getAsString());
        String name = signsFile.get("name").getAsString();
        packNamesByID.put(packID, name);

        if (signsFile.has("types") && signsFile.get("types").isJsonObject()) {
            JsonObject typesObject = signsFile.get("types").getAsJsonObject();
            for (var entry : typesObject.entrySet()) {
                friendlyTypesByName.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        JsonElement signsArrayObject = signsFile.get("signs");
        if (signsArrayObject == null || !signsArrayObject.isJsonArray()) {
            return;
        }
        processSignsArray(signsArrayObject.getAsJsonArray(), packID, splashUpdate, stepsUpdate);
    }

    private void processSignsArray(JsonArray signs, UUID packID, Consumer<String> splashUpdate, IntConsumer stepsUpdate) {
        stepsUpdate.accept(signs.size());
        for (JsonElement signElement : signs) {
            if (!signElement.isJsonObject()) {
                continue;
            }
            try {
                Sign sign = getSignFromObject(signElement.getAsJsonObject(), packID, splashUpdate);
                if (sign.getVariant() >= 0) {
                    signsByTypeVariant.put(new Tuple<>(sign.getType(), sign.getVariant()), sign);
                }
                signsByID.put(sign.getID(), sign);
                allSigns.add(sign);
            } catch (Exception ex) {
                ModRealisticTrafficControl.LOGGER.error("A sign failed to load.", ex);
            }
        }
    }

    private Sign getSignFromObject(JsonObject signObject, UUID packID, Consumer<String> splashUpdate) throws Exception {
        UUID id = UUID.fromString(signObject.get("id").getAsString());
        String name = signObject.get("name").getAsString();
        splashUpdate.accept("Reading " + name);
        String type = signObject.get("type").getAsString();
        String frontTextureName = signObject.get("front").getAsString();
        ResourceLocation frontRL = ResourceLocation.fromNamespaceAndPath("realistictrafficcontrol",
                "textures/block/signs/" + packID + "/" + type + "/" + frontTextureName);
        String backTextureName = signObject.has("back") ? signObject.get("back").getAsString() : "back.png";
        ResourceLocation backRL = ResourceLocation.fromNamespaceAndPath("realistictrafficcontrol",
                "textures/block/signs/" + packID + "/" + type + "/" + backTextureName);
        int variant = signObject.has("variant") ? Integer.parseInt(signObject.get("variant").getAsString()) : -1;
        String tooltip = signObject.has("tooltip") ? signObject.get("tooltip").getAsString() : null;
        String note = signObject.has("note") ? signObject.get("note").getAsString() : null;
        boolean halfHeight = signObject.has("halfheight") && signObject.get("halfheight").getAsBoolean();
        ArrayList<TextLine> textLines = new ArrayList<>();
        if (signObject.has("textlines")) {
            for (JsonElement element : signObject.get("textlines").getAsJsonArray()) {
                textLines.add(getTextLine(element.getAsJsonObject()));
            }
        }
        return new Sign(id, frontRL, backRL, name, variant, type, tooltip, note, halfHeight, textLines);
    }

    private TextLine getTextLine(JsonObject textLineObject) throws Exception {
        String label = textLineObject.get("label").getAsString();
        double x = textLineObject.get("x").getAsDouble();
        double y = textLineObject.get("y").getAsDouble();
        double width = textLineObject.get("width").getAsDouble();
        int color = textLineObject.get("color").getAsInt();
        int maxLength = textLineObject.has("maxlength") ? textLineObject.get("maxlength").getAsInt() : -1;
        double xScale = textLineObject.has("xscale") ? textLineObject.get("xscale").getAsDouble() : 1;
        double yScale = textLineObject.has("yscale") ? textLineObject.get("yscale").getAsDouble() : 1;
        SignHorizontalAlignment hAlign = SignHorizontalAlignment.Left;
        if (textLineObject.has("halign")) {
            String str = textLineObject.get("halign").getAsString().toLowerCase();
            for (SignHorizontalAlignment align : SignHorizontalAlignment.values()) {
                if (align.toString().toLowerCase().equals(str)) {
                    hAlign = align;
                    break;
                }
            }
        }
        SignVerticalAlignment vAlign = SignVerticalAlignment.Top;
        if (textLineObject.has("valign")) {
            String str = textLineObject.get("valign").getAsString().toLowerCase();
            for (SignVerticalAlignment align : SignVerticalAlignment.values()) {
                if (align.toString().toLowerCase().equals(str)) {
                    vAlign = align;
                    break;
                }
            }
        }
        return new TextLine(label, x, y, width, xScale, yScale, maxLength, color, hAlign, vAlign);
    }

    public Sign getSignByTypeVariant(String type, int variant) {
        return signsByTypeVariant.get(new Tuple<>(type, variant));
    }

    public Sign getSignByID(UUID id) {
        return signsByID.get(id);
    }

    public String getFriendlyTypeName(String unlocalizedName) {
        return friendlyTypesByName.get(unlocalizedName);
    }

    public ImmutableMap<UUID, String> getPacksByID() {
        return ImmutableMap.copyOf(packNamesByID);
    }

    public ImmutableList<Sign> getAllSigns() {
        return ImmutableList.copyOf(allSigns);
    }

    public void initClientTextures() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        // Dynamic texture registration from signpack ZIPs is handled by SignClientEvents.
    }
}
