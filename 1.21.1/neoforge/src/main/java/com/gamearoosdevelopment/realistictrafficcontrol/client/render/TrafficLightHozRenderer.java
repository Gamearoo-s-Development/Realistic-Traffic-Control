package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLightHozRenderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(0, 5.2, 0),
                new BulbRenderer(5.6, 5.2, 1),
                new BulbRenderer(11.5, 5.2, 2));
    }
}
