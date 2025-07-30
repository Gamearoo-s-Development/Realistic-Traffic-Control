package com.gamearoosdevelopment.realistictrafficcontrol.tileentity.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLightDoghouseRenderer extends BaseTrafficLightRenderer {

	@Override
	protected double getBulbZLocation() {
		return -5.6;
	}

	@Override
	protected List<BulbRenderer> getBulbRenderers() {
		return ImmutableList
				.<BulbRenderer>builder()
				.add(new BulbRenderer(5.2, 10.5, 0))
				.add(new BulbRenderer(1, 2.5, 1))
				.add(new BulbRenderer(1, -4, 2))
				.add(new BulbRenderer(10.5, 2.5, 3))
				.add(new BulbRenderer(10.5, -4, 4))
				.build();
	}

}
