package com.gamearoosdevelopment.realistictrafficcontrol.blocks.model;

import java.util.Collection;
import java.util.function.Function;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.google.common.collect.ImmutableSet;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;

public class ModelStreetLightDouble implements IModel {

	@Override
	public IBakedModel bake(IModelState state, VertexFormat format,
			Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
		return new BakedModelStreetLightDouble(format);
	}
	
	@Override
	public Collection<ResourceLocation> getTextures() {
		return ImmutableSet.<ResourceLocation>builder()
				.add(new ResourceLocation(ModRealisticTrafficControl.MODID + ":blocks/generic"))
				.add(new ResourceLocation(ModRealisticTrafficControl.MODID + ":blocks/yellow"))
				.build();
	}
}
