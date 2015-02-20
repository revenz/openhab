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
	static final Pattern rgxBindingConfig = Pattern.compile("\\G(\\w+)[\\s]*[=:][\\s]*([^;,:]+)[\\s]*[,;]?");
	
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
		
		//parse bindingconfig here ...
		log(item.getName() + " = " + bindingConfig);		
		Matcher matcher = rgxBindingConfig.matcher(bindingConfig);
		int lastMatchPos = 0;
		while (matcher.find()) {
			String key = matcher.group(1).trim();
			String value = matcher.group(2).trim();
		   
			if(key.equals("unit"))
				config.unit = value;
			else if(key.equals("room"))
	   			config.room = value;
			else if(key.equals("type"))					
	   			config.type = value;
			else if(key.equals("label"))
	   			config.label = value;
			else if(key.equals("persist"))
   				config.persist = value;
			else if(key.equals("watts"))
   				config.wattsId = value;
		   
			lastMatchPos = matcher.end();
		}
		config.name = item.getName();
		if (lastMatchPos == bindingConfig.length()) // only add it if was parsed
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
	
	public ArrayList<String> getDevices(HashMap<String, Item> items){
		ArrayList<String> devices = new ArrayList<String>(); 
		for(BindingConfig bc : this.bindingConfigs.values()){
			imperiHabBindingConfig ihbc = (imperiHabBindingConfig) bc;
			if(!items.containsKey(ihbc.name))
				continue;
			
			Item item = items.get(ihbc.name);			
			String lowerName = ihbc.name.toLowerCase();
			
			// work out what type of device this is
			List<Class<? extends Command>> commandTypes = item.getAcceptedCommandTypes();
			if(commandTypes == null) commandTypes = new ArrayList<Class<? extends Command>>();

			if(ihbc.type == null){
				if(commandTypes.contains(PercentType.class))
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
			
			
			if(ihbc.type.equals(DeviceTypes.TYPE_DIMMER)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Level"},
					new Object[]{"value", String.valueOf(item.getState())}
				));
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Status"},
					new Object[]{"value", item.getStateAs(OnOffType.class) == OnOffType.ON ? "1" : "0" }
				));
			}else if(ihbc.type.equals(DeviceTypes.TYPE_SWITCH) || ihbc.type.equals(DeviceTypes.TYPE_LOCK)){
    			ihbc.parameters.add(imperiHabBindingConfig.getParameterString(
					new Object[]{"key", "Status"},
					new Object[]{"value", item.getStateAs(OnOffType.class) == OnOffType.ON ? "1" : "0" }
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
					new Object[]{"graphable", "true"}
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
}
