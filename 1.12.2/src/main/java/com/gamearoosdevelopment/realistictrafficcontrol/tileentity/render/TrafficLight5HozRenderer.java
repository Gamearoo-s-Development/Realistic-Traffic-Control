package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight5HozRenderer extends BaseTrafficLightRenderer {

	@Override
	protected double getBulbZLocation() {
		return -5.6;
	}

	@Override
	protected List<BulbRenderer> getBulbRenderers() {
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(-8, 5.3, 0))
				.add(new BulbRenderer(-1, 5.3, 1))
				.add(new BulbRenderer(5.2, 5.3, 2))
				.add(new BulbRenderer(11, 5.3, 3))
				.add(new BulbRenderer(18, 5.3, 4))
				.build();
	}

}
