package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight5Renderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(5.2, 22.5, 0),
                new BulbRenderer(5.2, 15.5, 1),
                new BulbRenderer(5.2, 9.5, 2),
                new BulbRenderer(5.2, 3.5, 3),
                new BulbRenderer(5.2, -3.5, 4));
    }
}
