package it.polito.elite.dog.drivers.hue.network.interfaces;

import com.philips.lighting.model.PHBridge;

public interface HueConnectionListener
{
	public void onBridgeConnected(PHBridge bridge);

	public void onBridgeDisconnected();

	public void onCacheUpdated(int flag, PHBridge bridge);
}
