package org.openhab.binding.imperihab.internal;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.ContactItem;
//import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.types.DecimalType;
//import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
//import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
//import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
//import org.xml.sax.helpers.XMLReaderFactory;
//import sun.net.www.http.HttpClient;

import java.io.*;
//import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
//import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class imperiHabListener {

	static EventPublisher eventPublisher;
	static ItemRegistry itemRegistry;
	
    ServerSocket ss = null;
    final Pattern patternUrl = Pattern.compile("(?<=(GET ))[^\\s]+");
    boolean listening = false;
    boolean cancelPending = false;
    Logger logger;
    
    final Pattern patternDeviceId = Pattern.compile("(?<=([\\s]+item=))[^\\s]+");
    final Pattern patternRoom = Pattern.compile("(?<=(Frame label=\"))[^\"]+");
	
	public imperiHabListener(Logger logger){
		this.logger = logger;
	}
	
	private void log(String message){
		logger.debug("ImperiHabListener: " + message);
	}
	
	
	public void start(int port, String siteMapName, String openHabDirectory){
		
		try{
		ss = new ServerSocket(port);
        // Now enter an infinite loop, waiting for & handling connections.
        for (; ; ) {
            // Wait for a client to connect. The method will block;
            // when it returns the socket will be connected to the client
            Socket client = ss.accept();

            // Get input and output streams to talk to the client
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8")), true);
            
            // Start sending our reply, using the HTTP 1.1 protocol
            out.print("HTTP/1.1 200 \r\n"); // Version & status code
            out.print("Content-Type: text/plain\r\n"); // The type of data
            out.print("Connection: close\r\n"); // Will close stream
            out.print("\r\n"); // End of headers            

            String response = "";
            String line;
            while ((line = in.readLine()) != null && !cancelPending) {
                if (line.length() == 0)
                    break;
                if (line.startsWith("GET")) {
                    Matcher matcher = patternUrl.matcher(line);
                    if (matcher.find()) {
                        String url = matcher.group();
                        response += "got url: " + url + "\r\n";
                    	processUrl(url, openHabDirectory, siteMapName);
                    }
                }
            }

            if (response.equals(""))
                response = "bad request";

            out.print(response);

            // Close socket, breaking the connection to the client, and
            // closing the input and output streams
            out.close(); // Flush and close the output stream
            in.close(); // Close the input stream
            client.close(); // Close the socket itself
        } // Now loop again, waiting for the next connection
		}catch(Exception ex){
			log(ex.getMessage() + "\r\n" + ex.getStackTrace());
		}
		ss = null;
	}
	
	public void stop(){
		cancelPending = true;
		while(ss != null)
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}
	
	public String processUrl(String url, String openHabDirectory, String siteMapName){
		String response = "";
        if (url.toLowerCase().equals("/rooms"))
            response = rooms(openHabDirectory, siteMapName);
        else if (url.toLowerCase().equals("/devices")) {
            response = devices(openHabDirectory, siteMapName);
        }
        else if(url.toLowerCase().equals("/system")){
            response = "{\"id\":\"ImperiHAB\", \"apiversion\":\"1\"}";
        }
        else if(url.toLowerCase().startsWith("/devices/")){
            response = action(url);
        }
        else {
        	log("Unknown url: " + url);

        }
        return response;
	}

    String action(String requestUrl){
        String[] parts = requestUrl.substring(1).split("/");
        String deviceId = parts[1];
        String actionName = parts[3];
        String actionParam = parts.length == 5 ? parts[4] : "";
        log("deviceId: " + deviceId + ", actionName: " + actionName + ", actionParam: " + actionParam);
        try {
            if (actionName.equalsIgnoreCase("setStatus")){
            	eventPublisher.postCommand(deviceId, actionParam.equals("1") ? OnOffType.ON : OnOffType.OFF);
                //return httpHelper.sendPost(openHabUrl + "rest/items/" + deviceId, actionParam.equals("1") ? "ON" : "OFF");
            	return "ok";
            }
            else if (actionName.equalsIgnoreCase("setLevel")){            	
            	eventPublisher.postCommand(deviceId, PercentType.valueOf(actionParam));
            	//eventPublisher.postCommand(deviceId, actionParam);
                //return httpHelper.sendPost(openHabUrl + "rest/items/" + deviceId, actionParam);
                return "ok";
            }
        }catch (Exception ex){
        	log("Error: "+ ex.getMessage());
        }
        return "bad action";
    }
    
    
    String rooms(String openHabDirectory, String siteMapName) {        
        List<Room> rooms = new ArrayList<Room>();
        try {
            File file = new File(openHabDirectory + "/configurations/sitemaps/" + siteMapName + ".sitemap");
            if(!file.exists())
            	throw new Exception("ImperiHab: Sitemap '" + file.getAbsolutePath() + "' not found.");
            BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.equals("") || line.startsWith("//") || line.equals("{") || line.equals("}") || line.length() < 5)
                    continue;
                if (line.indexOf("Frame label=\"") >= 0) {
                    Matcher matcher = patternRoom.matcher(line);
                    if (matcher.find()) {
                        String room = matcher.group();
                        rooms.add(new Room(room.replace(" ", "_"), room));
                    }
                    continue;
                }
            }
            br.close();
        } catch (Exception ex) {
        	logger.error("ImperiHabListener: " + ex.getMessage());
        }

        StringBuilder sBuilder = new StringBuilder();
        sBuilder.append("{\"rooms\":[");

        int numberOfRooms = rooms.size() - 1;
        for (int i = 0; i <= numberOfRooms; i++) {
            Room room = rooms.get(i);
            sBuilder.append("{\"id\":\"" + room.id + "\",\"name\":\"" + room.name + "\"}");
            if (i != numberOfRooms)
                sBuilder.append(",");
        }

        sBuilder.append("]}");

        return sBuilder.toString();
    }
    
    String devices(String openHabDirectory, String siteMapName) {
        HashMap<String, ArrayList<ImperiHomeDevice>> devices = new HashMap<String, ArrayList<ImperiHomeDevice>>();

        try {
        	HashMap<String, ImperiHomeDevice> loadedDevices = getDevicesFromItemRegistry();//getDevices(openHabUrl);
        	for(String key : loadedDevices.keySet())
        		log("loaded device: " + key);
        	
            File file = new File(openHabDirectory + "/configurations/sitemaps/" + siteMapName + ".sitemap");
            if(!file.exists())
            	throw new Exception("ImperiHabListener: Sitemap '" + file.getAbsolutePath() + "' not found.");
            
            String room = null;
            BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()));
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("//") || line.equals("{") || line.equals("}") || line.length() < 5)
                    continue;

                if (line.indexOf("Frame label=\"") >= 0) {
                    Matcher matcher = patternRoom.matcher(line);
                    if (matcher.find()) {
                        room = matcher.group();
                    }
                    continue;
                }

                Matcher matchDeviceId = patternDeviceId.matcher(line);
                if (!matchDeviceId.find()) {
                	log("no match!: " + line);
                    continue;
                }

                String deviceId = matchDeviceId.group();
                if (!loadedDevices.containsKey(deviceId)) {
                	log("Failed to locate device: " + deviceId);
                    continue;
                }
                
                log("located device: " + deviceId);
                
                ImperiHomeDevice device = null;
                ImperiHomeDevice handlerDevice = loadedDevices.get(deviceId);
                log("handlerdevice: " + handlerDevice);
                log("handlerdevice type: " + handlerDevice.type);
                log("handlerdevice state: " + handlerDevice.state);
                if (handlerDevice.type.equalsIgnoreCase("SwitchItem")) {
                    device = new ImperiHomeDevice();
                    device.type = "DevSwitch";
                    device.params = "[{\"key\":\"Status\",\"value\":\"" + (handlerDevice.state.equals("ON") ? "1" : "0") + "\"}]";
                } else if (handlerDevice.type.equalsIgnoreCase("ContactItem")) {
                    device = new ImperiHomeDevice();
                    device.type = "DevMotion";
                    device.params = "[{\"key\":\"Tripped\",\"value\":\"" + (handlerDevice.state.equals("OPEN") ? "1" : "0") + "\"}]";
                }
                else if (handlerDevice.type.equalsIgnoreCase("DimmerItem")) {
                    device = new ImperiHomeDevice();
                    device.type = "DevDimmer";
                    int state = 0;
                    try {
                        state = Integer.parseInt(handlerDevice.state);  // this can be set to UNINITIALIZED
                    } catch (Exception exInner) {

                    }

                    device.params = "[{\"key\":\"Status\",\"value\":\"" + (state > 0 ? "1" : "0") + "\"},";
                    device.params += "{\"key\":\"Level\", \"value\":\"" + state + "\"}]";
                }           
                else if(handlerDevice.type.equals("NumberItem")){
                	if(deviceId.endsWith("_Watts")){
                		device = new ImperiHomeDevice();
                		device.type = "Watts";
                        try {                        	
                            device.state = "" + ((int)Double.parseDouble(handlerDevice.state));  // this can be set to UNINITIALIZED
                        } catch (Exception exInner) {
                    		device.state = "0";
                        }
                	}
                	else if(deviceId.endsWith("Temp") || deviceId.contains("_Temp")){
                		device = new ImperiHomeDevice();
                		device.type = "DevTemperature";
                		device.params = "[{\"key\":\"Value\",\"value\":\"" + handlerDevice.state + "\",\"unit\":\"°C\"}]";                		
                	}
                	else if(deviceId.endsWith("_Humidity")){
                		device = new ImperiHomeDevice();
                		device.type = "DevHygrometry";
                		device.params = "[{\"key\":\"Value\",\"value\":\"" + handlerDevice.state + "\",\"unit\":\"%\"}]";                		
                	}
                	else if(deviceId.endsWith("_Lux") || deviceId.endsWith("_Lum") || deviceId.endsWith("_Luminosity")){
                		device = new ImperiHomeDevice();
                		device.type = "DevLuminosity";
                		device.params = "[{\"key\":\"Value\",\"value\":\"" + handlerDevice.state + "\",\"unit\":\"lux\"}]";                    		
                	}
                }

                if (device != null) {
                	log("Device isn't null: " + deviceId);
                	try{
                    device.room = room.replace(" ", "_");
                    device.id = deviceId;
                    device.name = deviceId.replace("_", " ");
                    if(!devices.containsKey(device.room))
                    	devices.put(device.room, new ArrayList<ImperiHomeDevice>());
                    devices.get(device.room).add(device);
                	}catch(Exception ex2){
                		log("stupid error: " + ex2.getMessage());
                	}
                }else {
                }
            }
            br.close();
            
            logger.debug("imperihab: building device list");
            logger.debug("imperihab: number of devices: " + devices.size());
	        StringBuilder sBuilder = new StringBuilder();
	        sBuilder.append("{\"devices\":[");
	
	        for(String roomName : devices.keySet()){
	        	ArrayList<ImperiHomeDevice> roomDevices = devices.get(roomName);
	            int numberOfDevices = roomDevices.size();        	
		        for (int i = 0; i < numberOfDevices; i++) {
		            ImperiHomeDevice device = roomDevices.get(i);
		            String parameters = device.params;
		            if(i < numberOfDevices - 1 && roomDevices.get(i+1).name.equals(device.name + " Watts")){
		            	// next one is a watt value, so add it to this parameters
		            	parameters = parameters.substring(0, parameters.length() - 1);
		            	parameters += ",{\"key\":\"Energy\",\"value\":\"" + roomDevices.get(i+1).state + "\",\"unit\":\"W\"}]";
		            	i++;
		            }
		            sBuilder.append("{\"id\":\"" + device.id + "\",\"name\":\"" + device.name + "\",\"type\":\"" + device.type + "\",\"room\":\"" + device.room + "\",\"params\":" + parameters + "},");
		        }
	        }
	        String result = sBuilder.toString();
	        if(result.endsWith(","))
	        	result = result.substring(0, result.length() - 1);
	        result += "]}";
	        return result;
	        
        } catch (Exception ex) {
        	log("Error: " + ex.getMessage() + "\r\n" + ex.getStackTrace());
        	return ex.getMessage();
        }
    }    
    
//    private HashMap<String, ImperiHomeDevice> getDevices(String openHabUrl) throws SAXException, MalformedURLException, IOException{    	
//    	DeviceHandler handler = new DeviceHandler();
//        XMLReader myReader = XMLReaderFactory.createXMLReader();
//        myReader.setContentHandler(handler);
//        myReader.parse(new InputSource(new URL(openHabUrl + "rest/items").openStream()));
//        return handler.devices;        
//    }
    
    private HashMap<String, ImperiHomeDevice> getDevicesFromItemRegistry(){
    	log("Getting devices from Item registry");    	
    	HashMap<String, ImperiHomeDevice> devices = new HashMap<String, ImperiHomeDevice>();
    	for(Item item : itemRegistry.getItems()){
    		String id = item.getName();
    		String state = item.getState().toString();
    		String type = null;
    		Object _state = item.getState();
    		List<Class<? extends Command>> commandTypes = item.getAcceptedCommandTypes();
    		if(commandTypes != null){
	    		if(commandTypes.contains(PercentType.class))
	    			type = "DimmerItem";
	    		else if(commandTypes.contains(OnOffType.class))
	    			type = "SwitchItem";
	    		else if(commandTypes.contains(ContactItem.class))
	    			type = "ContactItem";    	
	    		else if(commandTypes.contains(DecimalType.class))
        			type = "NumberItem";
    		}    		
    		if(type == null){
    			List<Class<? extends State>> dts = item.getAcceptedDataTypes();
    			if(dts != null){
					if(dts.contains(OpenClosedType.class))
						type = "ContactItem";
    			}
    		}

    		if(type == null){
    			if(_state instanceof DecimalType)
        			type = "NumberItem";
    		}
    		
    		if(type != null)
    			devices.put(id,  new ImperiHomeDevice(id, state, type));
    		else{
        		if(commandTypes != null){
		        	for(Object o : commandTypes){
		        		log(id + " = " + o);
		        	}
        		}
        		for(Class<? extends State> dt : item.getAcceptedDataTypes()){
        			log(id + " data type = " + dt + " = " + dt.getName());
        		}
    			log("unsupport item: " + id + " , " + item.getState());
    		}
    	}
    	return devices;
    }
}

class Room {
    String id, name;

    Room(String id, String name) {
        this.id = id;
        this.name = name;
    }
}

class ImperiHomeDevice {
    String id, name, type, room, state, params;

    ImperiHomeDevice() {
    }
    ImperiHomeDevice(String id, String state, String type) {
    	this.id = id;
    	this.state = state;
    	this.type = type;
    }
}

class DeviceHandler extends DefaultHandler {
	
    boolean type = false;
    boolean name = false;
    boolean state = false;
    ImperiHomeDevice current = null;
    HashMap<String, ImperiHomeDevice> devices = new HashMap<String, ImperiHomeDevice>();

    public void startElement(String nsURI, String localName,
                             String rawName, Attributes attributes) throws SAXException {
        // Consult rawName since we aren't using xmlns prefixes here.
        if (rawName.equalsIgnoreCase("item")) {
            current = new ImperiHomeDevice();
        } else if (rawName.equalsIgnoreCase("type"))
            type = true;
        else if (rawName.equalsIgnoreCase("name"))
            name = true;
        else if (rawName.equalsIgnoreCase("state"))
            state = true;
    }

    public void characters(char[] ch, int start, int length) {
        if (type) {
            current.type = new String(ch, start, length);
            type = false;
        } else if (name) {
            current.name = new String(ch, start, length);
            devices.put(current.name, current);
            name = false;
        } else if (state) {
            current.state = new String(ch, start, length);
            state = false;
        }
    }
}
