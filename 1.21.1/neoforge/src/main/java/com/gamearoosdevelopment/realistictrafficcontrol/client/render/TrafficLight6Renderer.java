package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight6Renderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(9.3, 10.5, 1),
                new BulbRenderer(1.1, 10.5, 0),
                new BulbRenderer(5, 3.5, 2),
                new BulbRenderer(5, -3.5, 3));
    }
}
