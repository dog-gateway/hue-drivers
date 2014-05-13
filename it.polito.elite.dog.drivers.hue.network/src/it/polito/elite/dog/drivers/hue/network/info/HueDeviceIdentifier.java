/**
 * 
 */
package it.polito.elite.dog.drivers.hue.network.info;

/**
 * @author bonino
 *
 */
public class HueDeviceIdentifier
{
	// the local identifier of the HUE device
	private String localId;
	
	// the bridge to which the device is attached
	private String bridgeIp;

	/**
	 * @param localId
	 * @param bridgeIp
	 */
	public HueDeviceIdentifier(String localId, String bridgeIp)
	{
		//store the local id of the HUE device
		this.localId = localId;
		
		//store the IP address of the bridge to which the device is connected
		this.bridgeIp = bridgeIp;
	}
	
	public HueDeviceIdentifier()
	{
		//intentionally left empty
	}

	/**
	 * @return the localId
	 */
	public String getLocalId()
	{
		return localId;
	}

	/**
	 * @param localId the localId to set
	 */
	public void setLocalId(String localId)
	{
		this.localId = localId;
	}

	/**
	 * @return the bridgeIp
	 */
	public String getBridgeIp()
	{
		return bridgeIp;
	}

	/**
	 * @param bridgeIp the bridgeIp to set
	 */
	public void setBridgeIp(String bridgeIp)
	{
		this.bridgeIp = bridgeIp;
	}
	
	
}
