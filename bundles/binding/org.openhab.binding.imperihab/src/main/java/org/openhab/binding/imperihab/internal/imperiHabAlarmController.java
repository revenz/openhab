package org.openhab.binding.imperihab.internal;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class imperiHabAlarmController {
	
	protected static final Logger logger = LoggerFactory.getLogger(imperiHabBinding.class);

	public static ItemRegistry itemRegistry;
	public static EventPublisher eventPublisher = null;
	
	public static String AlarmAway_Switch = "Alarm_Away";
	public static String AlarmHome_Switch = "Alarm_Home";
	public static String AlarmCountdown_Number = "Alarm_Countdown";
	public static String AlarmState_Number = "Alarm_State";
	public static String Group_AlarmAway = "AlarmAway";
	public static String Group_AlarmHome = "AlarmHome";
	public static String AlarmLastTrippedItem = "AlarmLastTrippedItem";
	
	public static String CODE_MASTER = "0000";
	
	public static int ArmingCountdown = 45;
	public static int IntrusionCountdown = 45;
	

	public static final int STATE_UNKNOWN = -1;
	public static final int STATE_NOT_ARMED = 0;
	public static final int STATE_ARMED_AWAY = 1;
	public static final int STATE_ARMED_HOME = 2;
	public static final int STATE_ARMING = 3;
	public static final int STATE_INTRUSION_CONFIRMED = 4;
	public static final int STATE_INTRUSION_DETECTED = 5;
	
	private static int STATE_CURRENT;
	
	private static int COUNTDOWN = 0;
	
	private static Thread armingThread;
	
	public static String alarmStatus(){
		return "{\"state\":" + STATE_CURRENT + ", \"remaining\":" + COUNTDOWN + "}";
	}
	
	private static void setState(int state){
		STATE_CURRENT = state;
		eventPublisher.postUpdate(AlarmState_Number,DecimalType.valueOf(String.valueOf(state)));
		if(state == STATE_NOT_ARMED || state == STATE_ARMED_AWAY || state == STATE_ARMED_HOME){
			aborted = true;
			setCountdown(0);
		}
		logger.debug("ImperiHabAlarmController: State set to: " + state);
		if(state == STATE_NOT_ARMED){
			eventPublisher.postUpdate(AlarmHome_Switch, OnOffType.OFF);
			eventPublisher.postUpdate(AlarmAway_Switch, OnOffType.OFF);		
		}else if(state == STATE_ARMED_AWAY){
			eventPublisher.postUpdate(AlarmHome_Switch, OnOffType.OFF);
			eventPublisher.postUpdate(AlarmAway_Switch, OnOffType.ON);
		}else if(state == STATE_ARMED_HOME){
			eventPublisher.postUpdate(AlarmHome_Switch, OnOffType.ON);
			eventPublisher.postUpdate(AlarmAway_Switch, OnOffType.OFF);
		}
	}
	
	public static boolean arm(boolean away){
		if(armingThread != null){
			aborted = true;
			try {
				armingThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		eventPublisher.postUpdate(AlarmHome_Switch,  away ? OnOffType.OFF : OnOffType.ON);
		eventPublisher.postUpdate(AlarmAway_Switch, away ? OnOffType.ON : OnOffType.OFF);
		if(away){
			setState(STATE_ARMING);
			runArming(true);
		}else{
			setState(STATE_ARMED_HOME);
		}
		return true;
	}
	
	public static boolean disarm(String code){
		if(!code.equals(CODE_MASTER))
			return false;
		aborted = true;
		setCountdown(0);
		eventPublisher.postUpdate(AlarmHome_Switch, OnOffType.OFF);
		eventPublisher.postUpdate(AlarmAway_Switch, OnOffType.OFF);
		setState(STATE_NOT_ARMED);
		
		return true;		
	}
	private static void setCountdown(int number){
		COUNTDOWN = number;
		eventPublisher.postUpdate(AlarmCountdown_Number, DecimalType.valueOf(String.valueOf(COUNTDOWN)));
	}
	
	private static boolean aborted = false;
	private static void runArming(boolean arming){
		if(arming)
			setCountdown(ArmingCountdown);
		else
			setCountdown(IntrusionCountdown);
		aborted = false;
		armingThread = new Thread(new Runnable() {
		     public void run() {
		    	 try{
		    		while(COUNTDOWN > 0 && aborted != true){	    	 		
	    	 			Thread.sleep(1000);
	    	 			if(COUNTDOWN > 0)
	    	 				setCountdown(COUNTDOWN - 1);
		    		}
		    		if(aborted)
		    			return;
	    		 	Item alarmState;
					alarmState = itemRegistry.getItem(AlarmState_Number);
					String stateString = alarmState.getState().toString();
					if(stateString.equals(String.valueOf(STATE_ARMING))){
						eventPublisher.postUpdate(AlarmHome_Switch, OnOffType.OFF);
						eventPublisher.postUpdate(AlarmAway_Switch, OnOffType.ON);
						setState(STATE_ARMED_AWAY);						
					}else{
						setState(STATE_INTRUSION_CONFIRMED);
					}
				} catch (Exception e) {
				}
		     }
		});  
		armingThread.start();
	}
	
	public static void internalReceiveUpdate(String itemName, State newState) {
		if(newState != OnOffType.ON && newState != OpenClosedType.OPEN)
			return;
		if(STATE_CURRENT != STATE_ARMED_AWAY && STATE_CURRENT != STATE_ARMED_HOME)
			return;
			
		try {
			Item item = itemRegistry.getItem(itemName);
			boolean itemInGroup = item.getGroupNames().contains(STATE_CURRENT == STATE_ARMED_AWAY ? Group_AlarmAway : Group_AlarmHome);
			if(!itemInGroup){
				logger.debug("ImperiHabAlarmController item not in group: " + itemName);
				return;
			}

			logger.info("ImperiHabAlarmController intrusion detected: " + itemName);
			setState(STATE_INTRUSION_DETECTED);
			runArming(false);
			if(AlarmLastTrippedItem != null)
				eventPublisher.postUpdate(AlarmLastTrippedItem, new StringType(itemName));
			
		} catch (ItemNotFoundException e) {
			// TODO Auto-generated catch block
			logger.error("ImperiHabAlarmController error: " + e.getMessage());
		}
	}
	
	public static void internalReceiveCommand(String itemName, Command command){
		if(itemName.equals(AlarmAway_Switch)){
			if(command == OnOffType.ON){
				setState(STATE_ARMED_AWAY);
			}else if(command == OnOffType.OFF){
				setState(STATE_NOT_ARMED);
			}
		}else if(itemName.equals(AlarmHome_Switch)){
			if(command == OnOffType.ON){
				setState(STATE_ARMED_HOME);
			}else if(command == OnOffType.OFF){
				setState(STATE_NOT_ARMED);			
			}
		}
	}
}
