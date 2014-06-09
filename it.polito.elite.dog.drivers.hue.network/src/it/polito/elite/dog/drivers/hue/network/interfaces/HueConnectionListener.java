package it.polito.elite.dog.drivers.hue.network.interfaces;

import com.philips.lighting.model.PHBridge;

public interface HueConnectionListener
{
	/**
	 * Called when the network driver connects to a hue bridge
	 * 
	 * @param bridge
	 *            The {@link PHBridge} instance representing the Hue Bridge to
	 *            which the connection occured.
	 */
	public void onBridgeConnected(PHBridge bridge);

	/**
	 * Called when the network drivers disconnects froma bridge. The bridge
	 * information is implicit, i.e., it is assumed that the listener already
	 * knows the bridge to which this call refers.
	 */
	public void onBridgeDisconnected();

	/**
	 * Called when new information about a Hue bridge is known at the network
	 * driver level
	 * 
	 * @param flag
	 * @param bridge
	 *            The {@link PHBridge} instance representing the Hue Bridge for
	 *            which information has been updated.
	 */
	public void onCacheUpdated(int flag, PHBridge bridge);

	/**
	 * Called after an unsuccessful connection attempt, warns implementing
	 * classes about the necessity to perform push-link authentication.
	 */
	public void onAuthenticationRequired();
}
