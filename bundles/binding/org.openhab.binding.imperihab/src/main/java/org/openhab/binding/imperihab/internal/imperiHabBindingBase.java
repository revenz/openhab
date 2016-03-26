package org.openhab.binding.imperihab.internal;

import java.util.Dictionary;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.openhab.binding.imperihab.imperiHabBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.io.net.http.SecureHttpContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class imperiHabBindingBase extends AbstractActiveBinding<imperiHabBindingProvider> implements ManagedService, Servlet {

	protected static final Logger logger = LoggerFactory.getLogger(imperiHabBinding.class);
	
	protected void log(String message){
		logger.debug("ImperiHab: " + message);
	}
	
	protected final String SERVLET_NAME = "/imperihab";
	protected String openHabDirectory = "";
	protected String openHabInstanceName = "ImperiHAB";
	
	protected HttpService httpService;		
	public void setHttpService(HttpService httpService) { this.httpService = httpService; }
	public void unsetHttpService(HttpService httpService) { this.httpService = null; }
	
	protected EventPublisher eventPublisher;	
	public void setEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
		imperiHabAlarmController.eventPublisher = eventPublisher;
	}	
	public void unsetEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = null;
		imperiHabAlarmController.eventPublisher = null;
	}
	
	protected ItemRegistry itemRegistry;
	public void setItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = itemRegistry;
		imperiHabAlarmController.itemRegistry = this.itemRegistry;
	}
	public void unsetItemRegistry(ItemRegistry itemRegistry) {
		this.itemRegistry = null;
		imperiHabAlarmController.itemRegistry = null;
	}
	
	/**
	 * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
	 * method and must not be accessed anymore once the deactivate() method was called or before activate()
	 * was called.
	 */
	protected BundleContext bundleContext;

	
	/** 
	 * the refresh interval which is used to poll values from the imperiHab
	 * server (optional, defaults to 60000ms)
	 */
	protected long refreshInterval = 60000;
	

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "imperiHab Refresh Service";
	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException {
		
	}
	
	/**
	 * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
	 * @param configuration Updated configuration properties
	 */
	public void modified(final Map<String, Object> configuration) {
		// update the internal configuration accordingly
		configGo(configuration);
	}
	
	
	protected void configGo(final Map<String, Object> configuration){
		imperiHabAlarmPanel.slideShowDir = getConfigStringValue(configuration, "slideshowdir", null);
		imperiHabAlarmController.CODE_MASTER = getConfigStringValue(configuration, "pincode", "0000");
		if(!imperiHabAlarmController.CODE_MASTER.matches("^[\\d]+$"))
		{
			logger.error("Invalid pincode for imperihab, must be in format [\\d]+, i.e all numbers");
			imperiHabAlarmController.CODE_MASTER = "0000";
		}
		
		openHabInstanceName = getConfigStringValue(configuration, "name", "ImperiHAB");
		
		imperiHabAlarmController.AlarmAway_Switch = getConfigStringValue(configuration, "alarmAwaySwitch", "Alarm_Away");
		imperiHabAlarmController.AlarmHome_Switch = getConfigStringValue(configuration, "alarmHomeSwitch", "Alarm_Home");
		imperiHabAlarmController.AlarmCountdown_Number = getConfigStringValue(configuration, "alarmCountdownNumber", "Alarm_Countdown");
		imperiHabAlarmController.AlarmState_Number = getConfigStringValue(configuration, "alarmStateNumber", "Alarm_State");
		imperiHabAlarmController.Group_AlarmAway = getConfigStringValue(configuration, "alarmAwayGroup", "AlarmAway");
		imperiHabAlarmController.Group_AlarmHome = getConfigStringValue(configuration, "alarmHomeGroup", "AlarmHome");
		imperiHabAlarmController.AlarmLastTrippedItem = getConfigStringValue(configuration, "alarmLastTrippedItem", "Alarm_Last_Tripped_Item");
		imperiHabAlarmController.ArmingCountdown  = getConfigIntValue(configuration, "armingCountdown", 45, 1, 600);
		
		imperiHabAlarmController.IntrusionCountdown  = getConfigIntValue(configuration, "intrusionCountdown", 30, 1, 600);
		imperiHabGenericBindingProvider.DEFAULT_TEMPERATURE_UNIT = getConfigStringValue(configuration, "tempunit", "C");

		log("imperiHabAlarmPanel.slideShowDir: " + imperiHabAlarmPanel.slideShowDir);
		log("imperiHabAlarmController.CODE_MASTER: " + imperiHabAlarmController.CODE_MASTER);
		log("imperiHabAlarmController.AlarmAway_Switch: " + imperiHabAlarmController.AlarmAway_Switch);
		log("imperiHabAlarmController.AlarmHome_Switch: " + imperiHabAlarmController.AlarmHome_Switch);
		log("imperiHabAlarmController.AlarmCountdown_Number: " + imperiHabAlarmController.AlarmCountdown_Number);
		log("imperiHabAlarmController.AlarmState_Number: " + imperiHabAlarmController.AlarmState_Number);
		log("imperiHabAlarmController.Group_AlarmAway: " + imperiHabAlarmController.Group_AlarmAway);
		log("imperiHabAlarmController.Group_AlarmHome: " + imperiHabAlarmController.Group_AlarmHome);
		log("imperiHabAlarmController.ArmingCountdown: " + imperiHabAlarmController.ArmingCountdown);
		log("imperiHabAlarmController.IntrusionCountdown: " + imperiHabAlarmController.IntrusionCountdown);
	}
	
	private int getConfigIntValue(final Map<String, Object> configuration, String key, int defaultValue, int min, int max){
		Object v = getConfigValue(configuration, key);
		if(v == null || v.toString().isEmpty())
			return defaultValue;
		try{
			int result = Integer.parseInt(v.toString());
			if(result < min || result > max)
				return defaultValue;
			return result;
		}catch(Exception ex){
			return defaultValue;
		}
	}
	
	private String getConfigStringValue(final Map<String, Object> configuration, String key, String defaultValue){
		Object v = getConfigValue(configuration, key);
		if(v == null || v.toString().isEmpty())
			return defaultValue;
		return v.toString();
	}
	
	private Object getConfigValue(final Map<String, Object> configuration, String key){
		if(configuration.containsKey(key))
			return configuration.get(key);
		key = key.toLowerCase();
		for(String k2 : configuration.keySet()){
			if(key.equals(k2.toLowerCase()))
				return configuration.get(k2);
		}
		return null;
	}
	
	/**
	 * Called by the SCR to deactivate the component when either the configuration is removed or
	 * mandatory references are no longer satisfied or the component has simply been stopped.
	 * @param reason Reason code for the deactivation:<br>
	 * <ul>
	 * <li> 0 – Unspecified
     * <li> 1 – The component was disabled
     * <li> 2 – A reference became unsatisfied
     * <li> 3 – A configuration was changed
     * <li> 4 – A configuration was deleted
     * <li> 5 – The component was disposed
     * <li> 6 – The bundle was stopped
     * </ul>
	 */
	public void deactivate(final int reason) {
		httpService.unregister(SERVLET_NAME);
		
		this.bundleContext = null;
		// deallocate resources here that are no longer needed and 
		// should be reset when activating this binding again
	}
	

	/**
	 * Creates a {@link SecureHttpContext} which handles the security for this
	 * Servlet  
	 * @return a {@link SecureHttpContext}
	 */
	protected HttpContext createHttpContext() {
		HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
		return new SecureHttpContext(defaultHttpContext, "openHAB.org");
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {		
	}

	@Override
	public ServletConfig getServletConfig() {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public String getServletInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
}
