package com.gamearoosdevelopment.realistictrafficcontrol.crossing;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

/**
 * Hook for crossing-gate block entities paired to a relay. Implementations are added when the gate
 * subsystem is ported; the relay orchestration calls these methods when present.
 */
public interface ICrossingGateBlockEntity {

    enum GateStatus {
        Open,
        Closing,
        Closed,
        Opening
    }

    GateStatus getStatus();

    void setStatus(GateStatus status);

    void setFlashState(CrossingLampState state);
}
