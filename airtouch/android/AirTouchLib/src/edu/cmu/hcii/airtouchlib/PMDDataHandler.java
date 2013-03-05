package edu.cmu.hcii.airtouchlib;

import edu.cmu.hcii.airtouchlib.SendReceiveTask.PMDSendData;

public interface PMDDataHandler {
	public void NewPMDData(PMDSendData data);
	public void OnSendReceiveTaskFailed(String message);
}
