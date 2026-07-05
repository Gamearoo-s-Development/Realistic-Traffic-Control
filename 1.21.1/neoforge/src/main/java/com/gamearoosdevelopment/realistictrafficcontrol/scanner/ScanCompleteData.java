package com.gamearoosdevelopment.realistictrafficcontrol.scanner;

public class ScanCompleteData {
    private final ScanRequest scanRequest;
    private final boolean timedOut;
    private final boolean trainFound;
    private final boolean trainMovingTowardsDestination;
    private boolean continueScanningForTileEntity = true;

    public ScanCompleteData(ScanRequest scanRequest, boolean timedOut, boolean trainFound,
            boolean trainMovingTowardsDestination) {
        this.scanRequest = scanRequest;
        this.timedOut = timedOut;
        this.trainFound = trainFound;
        this.trainMovingTowardsDestination = trainMovingTowardsDestination;
    }

    public ScanRequest getScanRequest() {
        return scanRequest;
    }

    public boolean getTimedOut() {
        return timedOut;
    }

    public boolean getTrainFound() {
        return trainFound;
    }

    public boolean getTrainMovingTowardsDestination() {
        return trainMovingTowardsDestination;
    }

    public boolean getContinueScanningForTileEntity() {
        return continueScanningForTileEntity;
    }

    public void cancelScanningForTileEntity() {
        continueScanningForTileEntity = false;
    }
}
