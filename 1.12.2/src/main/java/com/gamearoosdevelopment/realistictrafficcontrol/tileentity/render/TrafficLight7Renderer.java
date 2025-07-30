package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.BaseTrafficLightRenderer.BulbRenderer;
import com.google.common.collect.ImmutableList;

public class TrafficLight7Renderer extends BaseTrafficLightRenderer {

	@Override
	protected double getBulbZLocation() {
		return -5.6;
	}

	@Override
	protected List<BulbRenderer> getBulbRenderers() {
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(9.3, 10.5, 1))
				.add(new BulbRenderer(1.1, 10.5, 0))
				.add(new BulbRenderer(5, 3.5, 2))
				
				.build();
	}

}