package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight5Renderer extends BaseTrafficLightRenderer {

	@Override
	protected double getBulbZLocation() {
		return 10.3;
	}

	@Override
	protected List<BulbRenderer> getBulbRenderers() {
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(5.2, 22.5, 0))
				.add(new BulbRenderer(5.2, 15.5, 1))
				.add(new BulbRenderer(5.2, 9.5, 2))
				.add(new BulbRenderer(5.2, 3.5, 3))
				.add(new BulbRenderer(5.2, -3.5, 4))
				.build();
	}

}
