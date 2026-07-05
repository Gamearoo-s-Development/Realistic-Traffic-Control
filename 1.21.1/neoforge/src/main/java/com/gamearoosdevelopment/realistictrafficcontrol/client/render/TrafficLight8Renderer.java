package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight8Renderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(5, 6.5, 1),
                new BulbRenderer(5, 13.5, 0),
                new BulbRenderer(0.5, -0.5, 2),
                new BulbRenderer(9.3, -0.5, 3));
    }
}
