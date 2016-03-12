package org.openhab.binding.imperihab.internal;

import java.util.ArrayList;
import java.util.Date;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.types.State;

public class imperiHabBindingConfig implements BindingConfig {		
	
	String room;
	String type;
	String unit;
	String label;
	String name;
	State state;
	String persist;
	String wattsId; // used to include power readings on devices
	String accumulationId; // used for the rain sensor
	String curmodeId;
	String currentTempId;
	String login;
	String password;
	String localjpegurl;
	String localmjpegurl;
	String remotejpegurl;
	String remotemjpegurl;
	String proxyjpegurl;
	float step;
	float minVal;
	float maxVal;
	String[] availableModes;
	boolean invert;
	ArrayList<String> parameters = new ArrayList<String>();		
	ArrayList<imperiHabBindingConfigChildItem> childItems = new ArrayList<imperiHabBindingConfigChildItem>();
	
	
	boolean isTripped = false;
	Date lastTripped;
	
	imperiHabBindingConfig(){
		
	}

	String getDeviceString(){
		synchronized (this.parameters) {
			return "{\"id\":\"" + this.name + "\",\"name\":\"" + (this.label != null ? this.label : this.name.replace("_", " ")) + "\",\"type\":\"" + 
					this.type + "\",\"room\":\"" + this.room + "\",\"params\":[\n\t\t" + 
					imperiHabUtils.join(this.parameters, ",\n\t\t") + 
					"\n\t]}";
		}	
    }
	
	String generateParameters(){
		String parameters = "[";
		return parameters;
	}
	
	static String getParameterString(Object[]... values){
		if(values == null || values.length == 0)
			return "";
		String result = "{";
		for(Object[] v : values){
			if(v[1] == null)
				continue;
			result += "\"" + v[0] +"\":" + (v[1] instanceof String ? "\"" + v[1] + "\"" : v[1].toString()) + ",";
		}
		return result.substring(0, result.length() - 1) + "}";
	}
	
	class imperiHabBindingConfigChildItem {
		String key;
		String value;
	}
}
