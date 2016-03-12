/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.imperihab.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




import org.apache.commons.lang.StringUtils;
import org.openhab.binding.imperihab.imperiHabBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author reven
 * @since 1.7.0
 */
public class imperiHabGenericBindingProvider extends AbstractGenericBindingProvider implements imperiHabBindingProvider {

	/** The Constant logger. */
	static final Logger logger = LoggerFactory.getLogger(imperiHabGenericBindingProvider.class);
	static final Pattern rgxBindingConfig = Pattern.compile("\\G(\\w+)[\\s]*[=:][\\s]*(('[^']+')|([^;,:]+))[\\s]*[,;]?");
	
	static HashMap<String, String> ItemLookups = new HashMap<String, String>();
	

	static String DEFAULT_TEMPERATURE_UNIT = "C";
	
	void log(String message){
		logger.debug("imperiHabGenericBindingProvider: " + message);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "imperihab";
	}
	

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		//if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
		//	throw new BindingConfigParseException("item '" + item.getName()
		//			+ "' is of type '" + item.getClass().getSimpleName()
		//			+ "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
		//}
		log(item.getName() + " binding config: " + bindingConfig);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		imperiHabBindingConfig config = new imperiHabBindingConfig();
		config.invert = false;
		
		//parse bindingconfig here ...
		log(item.getName() + " = " + bindingConfig);		
		Matcher matcher = rgxBindingConfig.matcher(bindingConfig);
		int lastMatchPos = 0;
		while (matcher.find()) {
			String key = matcher.group(1).trim();
			String value = matcher.group(2).trim();
			if(value.startsWith("'") && value.endsWith("'"))
				value = value.substring(1, value.length() - 1);
		   
			if(key.equalsIgnoreCase("unit"))
				config.unit = value;
			else if(key.equalsIgnoreCase("room"))
	   			config.room = value;
			else if(key.equalsIgnoreCase("type"))					
	   			config.type = value;
			else if(key.equalsIgnoreCase("label"))
	   			config.label = value;
			else if(key.equalsIgnoreCase("persist"))
   				config.persist = value;
			else if(key.equalsIgnoreCase("watts"))
   				config.wattsId = value;
			else if(key.equalsIgnoreCase("accumulation"))
				config.accumulationId = value;
			else if(key.equalsIgnoreCase("invert"))
				config.invert = value.equals("1") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on");
			else if(key.equalsIgnoreCase("curmodeid") ||key.equalsIgnoreCase("currentmodeid")){
				config.curmodeId = value;
				ItemLookups.put(item.getName(), value);
			}
			else if(key.equalsIgnoreCase("curTempId") ||key.equalsIgnoreCase("currentTempId"))
				config.currentTempId = value;
			else if(key.equalsIgnoreCase("step"))
				config.step = tryParseFloat(value);
			else if(key.equalsIgnoreCase("minval"))
				config.minVal = tryParseFloat(value);
			else if(key.equalsIgnoreCase("maxval"))
				config.maxVal = tryParseFloat(value);
			else if(key.equalsIgnoreCase("availableModes"))
				config.availableModes = value.split("-");
			else if(key.equalsIgnoreCase("login") || key.equalsIgnoreCase("user"))
				config.login = value;
			else if(key.equalsIgnoreCase("password"))
				config.password = value;
			else if(key.equalsIgnoreCase("localjpegurl") || key.equalsIgnoreCase("localjpgurl") || key.equalsIgnoreCase("localjpeguri") || key.equalsIgnoreCase("localjpguri")){
				config.localjpegurl = value;
				config.type = DeviceTypes.TYPE_CAMERA;
			}
			else if(key.equalsIgnoreCase("localmjpegurl") || key.equalsIgnoreCase("localmjpgurl") || key.equalsIgnoreCase("localmjpeguri") || key.equalsIgnoreCase("localmjpguri")){
				config.localmjpegurl = value;
				config.type = DeviceTypes.TYPE_CAMERA;
			}
			else if(key.equalsIgnoreCase("remotejpegurl") || key.equalsIgnoreCase("remotejpgurl") || key.equalsIgnoreCase("remotejpeguri") || key.equalsIgnoreCase("remotejpguri")){
				config.remotejpegurl = value;
				config.type = DeviceTypes.TYPE_CAMERA;
			}
			else if(key.equalsIgnoreCase("remotemjpegurl") || key.equalsIgnoreCase("remotemjpgurl") || key.equalsIgnoreCase("remotemjpeguri") || key.equalsIgnoreCase("remotemjpguri")){
				config.remotemjpegurl = value;
				config.type = DeviceTypes.TYPE_CAMERA;
			}
			else if(key.equalsIgnoreCase("proxyjpegurl") || key.equalsIgnoreCase("proxyjpgurl") || key.equalsIgnoreCase("proxyjpeguri") || key.equalsIgnoreCase("proxyjpguri")){
				config.proxyjpegurl = value;
				config.type = DeviceTypes.TYPE_CAMERA;
				ItemLookups.put(item.getName(), value);
			}
			
			
			lastMatchPos = matcher.end();
		}
		config.name = item.getName();
		if (lastMatchPos == bindingConfig.length()) // only add if it was parsed
			addBindingConfig(item, config);		
	}
	
	public ArrayList<String> getRooms(){
		ArrayList<String> rooms = new ArrayList<String>();
		for(BindingConfig bc : this.bindingConfigs.values()){
			imperiHabBindingConfig ihbc = (imperiHabBindingConfig) bc;
			if(ihbc.room != null && ihbc.room.length() > 0 && !rooms.contains(ihbc.room))
				rooms.add(ihbc.room);
		}
		return rooms;
	}
	
	public ArrayList<String> getDevices(String hostUrl, HashMap<String, Item> items){
		if(hostUrl.endsWith("/") == false)
			hostUrl += "/";
		ArrayList<String> devices = new ArrayList<String>(); 
		for(BindingConfig bc : this.bindingConfigs.values()){
			imperiHabBindingConfig ihbc = (imperiHabBindingConfig) bc;
			if(!items.containsKey(ihbc.name)){
				logger.debug("imperiHabGenericBindingProvider: unable to find: " + ihbc.name);				
				continue;
			}
			
			Item item = items.get(ihbc.name);			
			String lowerName = ihbc.name.toLowerCase();
			
			// work out what type of device this is
			List<Class<? extends Command>> commandTypes = item.getAcceptedCommandTypes();
			if(commandTypes == null) commandTypes = new ArrayList<Class<? extends Command>>();

			if(ihbc.type == null){
				if(ihbc.availableModes != null && ihbc.availableModes.length > 0 && StringUtils.isNotBlank(ihbc.curmodeId)){
					logger.debug("imperiHabGenericBindingProvider: TYPE_THERMOSTAT FOUND! " + ihbc.name);
					ihbc.type = DeviceTypes.TYPE_THERMOSTAT;
				}
				else if(commandTypes.contains(PercentType.class))
					ihbc.type = DeviceTypes.TYPE_DIMMER;
	    		else if(commandTypes.contains(OnOffType.class))
	    			ihbc.type = DeviceTypes.TYPE_SWITCH;
	    		else if(commandTypes.contains(ContactItem.class))
	    			ihbc.type = DeviceTypes.TYPE_MOTION;
			}
			if(ihbc.type == null){
    			List<Class<? extends State>> dts = item.getAcceptedDataTypes();
    			if(dts != null){
					if(dts.contains(OpenClosedType.class))
						ihbc.type = lowerName.contains("door") ? DeviceTypes.TYPE_DOOR : DeviceTypes.TYPE_MOTION;
    			}
    		}
			if(ihbc.type == null){
				if(lowerName.contains("humidity"))
					ihbc.type = DeviceTypes.TYPE_HUMIDITY;
				else if(lowerName.contains("lux") || lowerName.contains("luminosity"))
					ihbc.type = DeviceTypes.TYPE_LUMINOSITY;
				else if(lowerName.contains("temperature") || lowerName.endsWith("_temp") || ihbc.name.endsWith("Temp"))
					ihbc.type = DeviceTypes.TYPE_TEMPERATURE;
				else
					ihbc.type = DeviceTypes.TYPE_GENERIC;
			}

			
			if(ihbc.type.equals(DeviceTypes.TYPE_THERMOSTAT)){
				Item curmode = items.get(ihbc.curmodeId);
				String curModeValue = null;
				if(curmode != null)
					curModeValue = String.valueOf(curmode.getState());
				if(StringUtils.isBlank(curModeValue) || curModeValue == "Uninitialized")
					curModeValue = ihbc.availableModes[0];
				
				Item currentTempItem = items.get(ihbc.currentTempId);
				if(currentTempItem != null){				
					String value = item.getState().toString();
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "curmode"},
    					new Object[]{"value",curModeValue}
    				));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "curtemp"},
    					new Object[]{"value",String.valueOf(currentTempItem.getState())}
    				));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "cursetpoint"},
    					new Object[]{"value",String.valueOf(getStateAsDouble(value))}
    				));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "step"},
    					new Object[]{"value",ihbc.step == 0 ? 0.5f : ihbc.step}
    				));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "minVal"},
    					new Object[]{"value",ihbc.minVal}
    				));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "maxVal"},
    					new Object[]{"value",ihbc.maxVal}
    				));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
    					new Object[]{"key", "availablemodes"},
    					new Object[]{"value",StringUtils.join(ihbc.availableModes, ",")}
    				));
				}else
				{
					log("currentTempItem not set!");
				}
			}	
			else if(ihbc.type.equals(DeviceTypes.TYPE_CAMERA)){
				if(StringUtils.isNotBlank(ihbc.login)){
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
						new Object[]{"key", "Login"},
						new Object[]{"value", ihbc.login }
					));
				}
				if(StringUtils.isNotBlank(ihbc.password)){
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
						new Object[]{"key", "Password"},
						new Object[]{"value", ihbc.password }
					));
				}
				if(StringUtils.isNotBlank(ihbc.localmjpegurl)){
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
						new Object[]{"key", "localmjpegurl"},
						new Object[]{"value", ihbc.localmjpegurl }
					));
				}
				if(StringUtils.isNotBlank(ihbc.remotemjpegurl)){
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
						new Object[]{"key", "remotemjpegurl"},
						new Object[]{"value", ihbc.remotemjpegurl }
					));
				}	
				if(StringUtils.isNotBlank(ihbc.proxyjpegurl)){					
					String proxyUri = hostUrl+ "camera/" + ihbc.name;
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
						new Object[]{"key", "remotejpegurl"},
						new Object[]{"value", proxyUri }
					));
	    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
						new Object[]{"key", "localjpegurl"},
						new Object[]{"value", proxyUri }
					));
				}else{
					if(StringUtils.isNotBlank(ihbc.localjpegurl)){
		    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
							new Object[]{"key", "localjpegurl"},
							new Object[]{"value", ihbc.localjpegurl }
						));
					}
					if(StringUtils.isNotBlank(ihbc.remotejpegurl)){
		    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
							new Object[]{"key", "remotejpegurl"},
							new Object[]{"value", ihbc.remotejpegurl }
						));
					}
				}
			}
			else if(ihbc.type.equals(DeviceTypes.TYPE_DIMMER)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Level"},
					new Object[]{"value", String.valueOf(item.getState())}
				));
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Status"},
					new Object[]{"value", item.getStateAs(OnOffType.class) == OnOffType.ON ? (ihbc.invert ? "0" : "1") : (ihbc.invert ? "1" : "0") }
				));
			}else if(ihbc.type.equals(DeviceTypes.TYPE_SWITCH) || ihbc.type.equals(DeviceTypes.TYPE_LOCK)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Status"},
					new Object[]{"value", item.getStateAs(OnOffType.class) == OnOffType.ON ? (ihbc.invert ? "0" : "1") : (ihbc.invert ? "1" : "0") }
				));
			}else if(ihbc.type.equals(DeviceTypes.TYPE_HUMIDITY)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", String.valueOf(getStateAsDouble(item.getState().toString()))},
					new Object[]{"unit", "%"},
					new Object[]{"graphable", "true"}
				));
			}else if(ihbc.type.equals(DeviceTypes.TYPE_TEMPERATURE)){
				String value = item.getState().toString();	
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", String.valueOf(getStateAsDouble(value))},
					new Object[]{"graphable", "true"},
					new Object[]{"unit", getTempUnit(ihbc.unit)}
				));
			}else if(ihbc.type.equals(DeviceTypes.TYPE_LUMINOSITY)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", String.valueOf(getStateAsDouble(item.getState().toString()))},
					new Object[]{"unit", "lux"},
					new Object[]{"graphable", true}
				));
			}else if(ihbc.type.equals(DeviceTypes.TYPE_NOISE)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", String.valueOf(getStateAsDouble(item.getState().toString()))},
					new Object[]{"unit", "db"},
					new Object[]{"graphable", true}
				));				
			}else if(ihbc.type.equals(DeviceTypes.TYPE_ELECTRICITY)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "ConsoTotal"},
					new Object[]{"value", String.valueOf(getStateAsDouble(item.getState().toString()))},
					new Object[]{"unit", "kWh"},
					new Object[]{"graphable", true}
				));		
			}else if(ihbc.type.equals(DeviceTypes.TYPE_PRESSURE)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", String.valueOf(getStateAsDouble(item.getState().toString()))},
					new Object[]{"unit", "mbar"},
					new Object[]{"graphable", true}
				));	  	
			}else if(ihbc.type.equals(DeviceTypes.TYPE_DOOR) || ihbc.type.equals(DeviceTypes.TYPE_FLOOD) || ihbc.type.equals(DeviceTypes.TYPE_MOTION) || ihbc.type.equals(DeviceTypes.TYPE_SMOKE)){
				boolean tripped = item.getStateAs(OpenClosedType.class) == OpenClosedType.OPEN;
				if(tripped && !ihbc.isTripped){
					ihbc.lastTripped = new Date();
				}				
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Tripped"},
					new Object[]{"value", tripped ? "1" : "0" },
					new Object[]{"lasttrip", ihbc.lastTripped == null ? 0 : ihbc.lastTripped.getTime()}
				));	  	    
			}else if(ihbc.type.equals(DeviceTypes.TYPE_RAIN)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", item.getState().toString() },
					new Object[]{"unit", "mm/h" },
					new Object[]{"graphable", "true" }
				));
    			if(ihbc.accumulationId != null){
    				Item otherItem = items.get(ihbc.accumulationId);
    				if(otherItem != null){
		    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
	    					new Object[]{"key", "Accumulation"},
	    					new Object[]{"graphable", true},
	    					new Object[]{"unit", "mm"},
	    					new Object[]{"value", otherItem.getState().toString()}
						));    					
    				}
    			}
			}else if(ihbc.type.equals(DeviceTypes.TYPE_SCENE)){
				// only value here is last run, so skip
			}else {
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Value"},
					new Object[]{"value", item.getState().toString() }
				));
			}
			
			if(ihbc.wattsId != null){
				Item wattItem = items.get(ihbc.wattsId);
				if(wattItem != null){
					if(ihbc.type.equals(DeviceTypes.TYPE_ELECTRICITY)){
		    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
	    					new Object[]{"key", "Watts"},
	    					new Object[]{"unit", "W"},
	    					new Object[]{"graphable", true},
	    					new Object[]{"value", String.valueOf(wattItem.getState())}
						));						
					}else{
		    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
	    					new Object[]{"key", "Energy"},
	    					new Object[]{"graphable", true},
	    					new Object[]{"value", String.valueOf(wattItem.getState())}
						));
					}
				}
			}
			

			devices.add(ihbc.getDeviceString());
			// clear the parameters as we don't need them any more
			ihbc.parameters.clear();
		}	
		return devices;
	}
	
	public String getPersistService(String name){
		for(BindingConfig bc : this.bindingConfigs.values()){
			imperiHabBindingConfig ihbc = (imperiHabBindingConfig) bc;
			if (ihbc.name.equals(name))
				return ihbc.persist;
		}
		return null;
	}
	
	private double getStateAsDouble(String state){
		double value = 0;
        try {
            value = Double.parseDouble(state);  // this can be set to UNINITIALIZED
        } catch (Exception ex) { }
        return value;
	}
	
	private float tryParseFloat(String value){
		float result = 0f;
        try {
        	result = Float.parseFloat(value);
        } catch (Exception ex) { }
        return result;
	}
	
	private String getTempUnit(String unit){
		if(StringUtils.isBlank(unit)){
			if(StringUtils.isBlank(DEFAULT_TEMPERATURE_UNIT))
				return null;
			return "\u00B0" + DEFAULT_TEMPERATURE_UNIT;
		}
		return "\u00B0" + unit;
			
	}
}
