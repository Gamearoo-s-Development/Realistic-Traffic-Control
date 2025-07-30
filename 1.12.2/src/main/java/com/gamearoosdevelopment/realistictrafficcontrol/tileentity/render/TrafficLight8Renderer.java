package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render.BaseTrafficLightRenderer.BulbRenderer;
import com.google.common.collect.ImmutableList;

public class TrafficLight8Renderer extends BaseTrafficLightRenderer {

	@Override
	protected double getBulbZLocation() {
		return -5.6;
	}

	@Override
	protected List<BulbRenderer> getBulbRenderers() {
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(5, 6.5, 1))
				.add(new BulbRenderer(5, 13.5, 0))
				.add(new BulbRenderer(0.5, -0.5, 2))
				.add(new BulbRenderer(9.3, -0.5, 3))
				.build();
	}

}
