package com.gamearoosdevelopment.realistictrafficcontrol.crossing;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

/** Hook for crossing-lamp block entities paired to a relay. */
public interface ICrossingLampBlockEntity {

    void setState(CrossingLampState state);
}
