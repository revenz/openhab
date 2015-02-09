package org.openhab.action.imperihab.internal;

import java.net.URLEncoder;

import org.openhab.io.net.http.HttpUtil;

public class imperiHabBackgroundWorker implements Runnable {
	
	public final static int MODE_SAY = 1;
	public final static int MODE_GOTOPAGE = 2;
	
	private String serverAddress;
	private String text;
	private int mode;
	private int dashboardPageIndex;

    public imperiHabBackgroundWorker(String serverAddress, String text) {
        this.serverAddress = serverAddress;
        this.text = text;
        this.mode = MODE_SAY;        
    }
    public imperiHabBackgroundWorker(String serverAddress, int dashboardPageIndex) {
        this.serverAddress = serverAddress;
        this.dashboardPageIndex = dashboardPageIndex;
        this.mode = MODE_GOTOPAGE;
    }

    @SuppressWarnings("deprecation")
	public void run() {	
		String url = null;
		if(mode == MODE_SAY){
			url = String.format("http://%s/api/rest/speech/tts?text=%s", serverAddress,URLEncoder.encode(text));
		}
		else if(mode == MODE_GOTOPAGE)
			url = String.format("http://%s/api/rest/dashboard/gotopage?pageIdx=%s", serverAddress,String.valueOf(dashboardPageIndex));
		else
			return;		
		HttpUtil.executeUrl("GET", url, 5000);	
    }
}
