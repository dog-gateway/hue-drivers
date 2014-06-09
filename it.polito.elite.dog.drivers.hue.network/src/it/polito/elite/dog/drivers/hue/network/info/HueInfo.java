package it.polito.elite.dog.drivers.hue.network.info;

import it.polito.elite.dog.core.library.model.ConfigurationConstants;

public class HueInfo extends ConfigurationConstants
{
	/**
	 * the manufacturer identifier 
	 */
	public static final String MANUFACTURER = "PhilipsHue";
	
	/**
	 * The local device identifier
	 */
	public static final String LOCAL_ID ="localId";
	
	/**
	 * The gateway IP address
	 */
	public static String GATEWAY_ADDRESS = "IPAddress";
		
	/**
	 * The step percentage to use in step-based dimming
	 */
	public static final String STEP_PERCENT = "stepAsPercentage";
	
	/**
	 * The maximum brightness of a Hue lamp
	 */
	public static final int MAX_BRIGHTNESS = 255;
	
	/**
	 * The default localId for gateways
	 */
	public static final String GATEWAY_DEFAULT_LOCAL_ID = "1";
}
