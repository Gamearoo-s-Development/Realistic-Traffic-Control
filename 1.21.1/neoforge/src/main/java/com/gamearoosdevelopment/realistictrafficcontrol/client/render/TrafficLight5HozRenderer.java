package com.gamearoosdevelopment.realistictrafficcontrol.client.render;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TrafficLight5HozRenderer extends BaseTrafficLightRenderer {

    @Override
    protected double getBulbZLocation() {
        return -5.6;
    }

    @Override
    protected List<BulbRenderer> getBulbRenderers() {
        return ImmutableList.of(
                new BulbRenderer(-8, 5.3, 0),
                new BulbRenderer(-1, 5.3, 1),
                new BulbRenderer(5.2, 5.3, 2),
                new BulbRenderer(11, 5.3, 3),
                new BulbRenderer(18, 5.3, 4));
    }
}
