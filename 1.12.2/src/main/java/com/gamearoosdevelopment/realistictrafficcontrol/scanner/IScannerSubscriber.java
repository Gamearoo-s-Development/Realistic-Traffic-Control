package com.gamearoosdevelopment.realistictrafficcontrol.scanner;

import java.util.List;

public interface IScannerSubscriber {
	List<ScanRequest> getScanRequests();
	void onScanComplete(ScanCompleteData scanCompleteData);
	default void onScanRequestsCompleted() { };
}
