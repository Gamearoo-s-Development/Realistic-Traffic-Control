package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight4HozRenderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(-5.5, 5.3, 0),
                new BulbRenderer(1, 5.3, 1),
                new BulbRenderer(7, 5.3, 2),
                new BulbRenderer(14, 5.3, 3));
    }
}
