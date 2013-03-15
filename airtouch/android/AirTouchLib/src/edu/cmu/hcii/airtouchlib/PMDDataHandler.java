package edu.cmu.hcii.airtouchlib;

import edu.cmu.hcii.airtouchlib.SendReceiveTask.PMDSendData;

public interface PMDDataHandler {
	public void newPMDData(PMDSendData data);
	public void onSendReceiveTaskFailed(String message);
}
