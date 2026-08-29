package com.gamearoosdevelopment.realistictrafficcontrol.scanner;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class ScanRequest {
    private final UUID requestID;
    private final BlockPos startingPos;
    private final List<BlockPos> endingPositions;
    private final Direction startDirection;

    public ScanRequest(UUID requestID, BlockPos startingPos, List<BlockPos> endingPositions, Direction startDirection) {
        this.requestID = requestID;
        this.startingPos = startingPos;
        this.endingPositions = endingPositions;
        this.startDirection = startDirection;
    }

    public UUID getRequestID() {
        return requestID;
    }

    public BlockPos getStartingPos() {
        return startingPos;
    }

    public List<BlockPos> getEndingPositions() {
        return endingPositions;
    }

    public Direction getStartDirection() {
        return startDirection;
    }

    @Override
    public int hashCode() {
        int hashCode = 19;
        hashCode = 487 * hashCode + requestID.hashCode();
        hashCode = 487 * hashCode + startingPos.hashCode();
        hashCode = 487 * hashCode + endingPositions.hashCode();
        hashCode = 487 * hashCode + startDirection.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        ScanRequest scanRequest = (ScanRequest) obj;
        if (scanRequest.endingPositions.size() != endingPositions.size()) {
            return false;
        }
        for (int i = 0; i < scanRequest.endingPositions.size(); i++) {
            if (!endingPositions.get(i).equals(scanRequest.endingPositions.get(i))) {
                return false;
            }
        }
        return requestID.equals(scanRequest.requestID)
                && startingPos.equals(scanRequest.startingPos)
                && startDirection == scanRequest.startDirection;
    }
}
