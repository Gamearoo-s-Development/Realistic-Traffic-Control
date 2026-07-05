package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight2Renderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(5.2, 9, 0),
                new BulbRenderer(5.2, 2.5, 1));
    }
}
