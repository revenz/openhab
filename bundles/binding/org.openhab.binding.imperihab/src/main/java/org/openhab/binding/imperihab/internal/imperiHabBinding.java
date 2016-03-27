/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.imperihab.internal;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openhab.binding.imperihab.imperiHabBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
//import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;

//import com.sun.jmx.snmp.Timestamp;

/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 * 
 * @author reven
 * @since 1.7.0
 */
public class imperiHabBinding extends imperiHabBindingBase {
	
	public imperiHabBinding() {
		
	}
		
	/**
	 * Called by the SCR to activate the component with its configuration read from CAS
	 * 
	 * @param bundleContext BundleContext of the Bundle that defines this component
	 * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
		logger.debug("imperihab activate called!");
		this.bundleContext = bundleContext;

		this.openHabDirectory =  System.getProperty("user.dir");
		configGo(configuration);

		Hashtable<String, String> props = new Hashtable<String, String>();
		try {
			httpService.registerServlet(SERVLET_NAME, this, props, createHttpContext());
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			logger.error("ImperiHab, error registering servlet: "  + e.getMessage());
			e.printStackTrace();
		} catch (NamespaceException e) {
			// TODO Auto-generated catch block
			logger.error("ImperiHab, error registering servlet: "  + e.getMessage());
			e.printStackTrace();
		}
		

		setProperlyConfigured(true);
	}
	
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling) goes here ...
		logger.debug("execute() method is called!");
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("imperiHab: internalReceiveCommand({},{}) is called!", itemName, command);		
		imperiHabAlarmController.internalReceiveCommand(itemName, command);
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the 
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("imperiHab: internalReceiveUpdate({},{}) is called!", itemName, newState);
		imperiHabAlarmController.internalReceiveUpdate(itemName, newState);
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest)req; 
			HttpServletResponse response = (HttpServletResponse)res;
			String url = request.getRequestURL().toString();
			url = url.substring(url.indexOf(SERVLET_NAME) + SERVLET_NAME.length());
			//String queryString = ((HttpServletRequest)req).getQueryString();		
			if(url.toLowerCase().startsWith("/ss-image")){
				response.setHeader("Cache-Control", "max-age=604800,private");
				imperiHabAlarmPanel.returnImage(res, url.substring("/ss-image/".length()));
				return;
			}
			else if(url.toLowerCase().startsWith("/camera/")){
				String cameraName = url.substring("/camera/".length());
				cameraImage(res, cameraName);
				return;
			}
			PrintWriter writer = null;
			GZIPOutputStream gz = null;
			if(request.getHeader("Accept-Encoding") != null && request.getHeader("Accept-Encoding").contains("gzip"))
			{
				gz = new GZIPOutputStream(res.getOutputStream());
				writer = new PrintWriter(new OutputStreamWriter(gz, "UTF-8"));				
			}else{
				writer = res.getWriter();
			}
			response.setHeader("Content-Encoding","gzip");
			if(url.toLowerCase().equals("/alarm")){
				response.setHeader("Cache-Control", "max-age=600,private");
			}
			else{
				response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
				response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
				response.setDateHeader("Expires", 0); // Proxies.
			}
			if(url.toLowerCase().equals("/alarm")){
				imperiHabAlarmPanel.writeHtml(writer);
			}
			else if(url.toLowerCase().equals("/alarm-status")){
				writer.write(imperiHabAlarmController.alarmStatus());
			}
			else if(url.toLowerCase().equals("/alarm-arm-away")){
				writer.write("{\"success\":" + (imperiHabAlarmController.arm(true) ? "true" : "false") + "}");
			}
			else if(url.toLowerCase().equals("/alarm-arm-home")){
				writer.write("{\"success\":" + (imperiHabAlarmController.arm(false) ? "true" : "false") + "}");
			}
			else if(url.toLowerCase().equals("/alarm-disarm")){
				String code = req.getParameter("code");
				if(imperiHabAlarmController.disarm(code))
					writer.write("{\"success\":true}");
				else
					writer.write("{\"success\":false, \"error\":\"INVALID CODE\"}");
			}
			else{
				//res.setCharacterEncoding("UTF-8");
				response.setHeader("Content-type", "application/json; charset=utf-8");
		        if (url.toLowerCase().equals("/rooms"))
					writer.write(generateRoomsJson());
		        else if(url.toLowerCase().startsWith("/devices/") && url.toLowerCase().contains("/histo"))
	        		writer.write(generateHistoryData(url));
		        else if (url.toLowerCase().equals("/devices")){
					String host = ((HttpServletRequest) req).getRequestURL().toString();
					host = host.substring(0,  host.indexOf(("/devices")));
					writer.write(generateDevicesJson(host));	   
		        }
		        else if(url.toLowerCase().equals("/system"))
					writer.write("{\"id\":\"" + openHabInstanceName + "\", \"apiversion\":\"1\"}");	        
		        else if(url.toLowerCase().startsWith("/devices/"))
					writer.write(performAction(url));
			}
			writer.flush();	    
			writer.close();    
			if(gz != null){
				gz.flush();
				gz.close();
			}
		}		
	}

    private String generateHistoryData(String requestUrl) {
        String[] parts = requestUrl.substring(1).split("/");
        String deviceId = parts[1];
        String paramKey = parts[2];
        Date start = new Date(Long.parseLong(parts[4]));
        Date end = new Date(Long.parseLong(parts[5]));
        log("deviceId: " + deviceId + ", paramKey: " + paramKey + ", start: " + start + ", end:" + end);
        Item item = null;
		try {
			item = itemRegistry.getItem(deviceId);
		} catch (ItemNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}
        
		String persistService = getPersistService(item.getName());
        
        ArrayList<String> data = new ArrayList<String>();
        if(item != null){
    		DateTime date = new DateTime(start.getTime());
        	while(date.isAfter(end.getTime()) == false){      
            	HistoricItem histItem;
				if (persistService == null)
					histItem = PersistenceExtensions.historicState(item, date);
				else
					histItem = PersistenceExtensions.historicState(item, date, persistService);
            	if(histItem != null)
            		data.add("{\"value\":" + histItem.getState().toString() + ",\"date\":" + date.getMillis() + "}");            	
        		date = date.plusMinutes(30);
        	}
        }
        
        return "{\"values\":[" + imperiHabUtils.join(data, ",") + "]}";
	}

	String performAction(String requestUrl){
        String[] parts = requestUrl.substring(1).split("/");
        String deviceId = parts[1];
        String actionName = parts[3];
        String actionParam = parts.length == 5 ? parts[4] : "";
        log("deviceId: " + deviceId + ", actionName: " + actionName + ", actionParam: " + actionParam);
        try {
            if (actionName.equalsIgnoreCase("setStatus")){
            	eventPublisher.postCommand(deviceId, actionParam.equals("1") ? OnOffType.ON : OnOffType.OFF);
            	return "ok";
            }
            else if (actionName.equalsIgnoreCase("setLevel")){            	
            	eventPublisher.postCommand(deviceId, PercentType.valueOf(actionParam));
                return "ok";
            }
            else if (actionName.equalsIgnoreCase("setSetPoint")){            	
            	eventPublisher.postCommand(deviceId, PercentType.valueOf(actionParam));
                return "ok";
            }
            else if (actionName.equalsIgnoreCase("stopShutter")){            	
            	eventPublisher.postCommand(deviceId, StopMoveType.STOP);
                return "ok";
            }
            else if (actionName.equalsIgnoreCase("pulseShutter")){            
            	if(actionParam.equalsIgnoreCase("up"))
            		eventPublisher.postCommand(deviceId, IncreaseDecreaseType.INCREASE);
            	else if(actionParam.equalsIgnoreCase("down"))
            		eventPublisher.postCommand(deviceId, IncreaseDecreaseType.DECREASE);
            		
                return "ok";
            }
            else if (actionName.equalsIgnoreCase("setColor")){     
            	int alpha = (int)(Integer.parseInt(actionParam.substring(0, 2), 16) / 255f * 100);
            	int red = Integer.parseInt(actionParam.substring(2, 4), 16);
            	int green = Integer.parseInt(actionParam.substring(4, 6), 16);
            	int blue = Integer.parseInt(actionParam.substring(6, 8), 16);
            	// colour command is redvalue +"," + greenValue + ","+ blueValue
            	String colourCommand = red + "," + green + "," + blue;
            	log("#### setting RGB colour to: " + colourCommand);
            	eventPublisher.postCommand(deviceId, new StringType(colourCommand));
            	eventPublisher.postCommand(deviceId, PercentType.valueOf(String.valueOf(alpha)));
                return "ok";
            }
            else if (actionName.equalsIgnoreCase("setChoice") || actionName.equalsIgnoreCase("setMode")){
            	String targetItemId = imperiHabGenericBindingProvider.ItemLookups.get(deviceId);
            	if(StringUtils.isBlank(targetItemId)){
            		log("Failed to find targetItemId for device: " + deviceId);
            		return "No item id found.";
            	}
            	Item item = findItem(targetItemId);
            	if(item == null){
            		log("Failed to find targetItem from Id: " + targetItemId);
            		return "not found";
            	}
            	eventPublisher.postCommand(targetItemId, new StringType(actionParam));
                return "ok";
            }
        }catch (Exception ex){
        	log("Error: "+ ex.getMessage());
        }
        return "bad action";
    }
    
	
	String generateRoomsJson(){
        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("{\"rooms\":[");
		ArrayList<String> rooms = new ArrayList<String>();
		for (imperiHabBindingProvider provider : this.providers) {
			for(String room : provider.getRooms())
			{
				if(!rooms.contains(room)){
					rooms.add(room);
		            sBuilder.append("{\"id\":\"" + room + "\",\"name\":\"" + room + "\"},");
				}
			}
		}
        if(rooms.size() > 0){
        	sBuilder = sBuilder.replace(sBuilder.length() -1, sBuilder.length(), "");
        }
        sBuilder.append("]}");

        return sBuilder.toString();
	}
	
	String generateDevicesJson(String hostUrl){		
		ArrayList<String> devices = new ArrayList<String>();
		HashMap<String, Item> items = new HashMap<String, Item>();
		for(Item item : itemRegistry.getItems())
			items.put(item.getName(), item);
		for (imperiHabBindingProvider provider : this.providers) {			
			devices.addAll(provider.getDevices(hostUrl, items));
		}		
		return "{\"devices\":[\n\t" + imperiHabUtils.join(devices,  ",\n\t")  + "\n]}";
	}
	
	Item findItem(String name){
		for(Item item : itemRegistry.getItems())
			if(name.equals(item.getName()))
				return item;
		return null;
	}
	
	String getPersistService(String name){
		for (imperiHabBindingProvider provider : this.providers) {			
			String persistService = provider.getPersistService(name);
			if (persistService != null)
				return persistService;
		}		
		return null;
	}
	

	public static void cameraImage(ServletResponse response, String cameraName)  {
		String cameraUrl = imperiHabGenericBindingProvider.ItemLookups.get(cameraName);
		
		logger.debug("imerpihab2: returning image: " + cameraUrl);
		try {
			response.setContentType("image/jpeg");			
			
			URL url = new URL(cameraUrl);
	        InputStream is = url.openStream();
			OutputStream out = response.getOutputStream();

	        byte[] b = new byte[2048];
	        int length;

	        while ((length = is.read(b)) != -1) {
		        out.write(b, 0, length);
	        }

	        is.close();
	        out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug("imperihab2: error: " + e.getMessage());
			e.printStackTrace();
			
		}
	}
}
