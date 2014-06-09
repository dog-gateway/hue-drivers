/*
 * Dog 2.0 - Hue Network Driver
 * 
 * 
 * Copyright 2014 Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.drivers.hue.network;

import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueBridgeDiscoveryListener;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueConnectionListener;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;

/**
 * Differently from the typical structure adopted in network drivers, since most
 * functions related to device management / discovery are strictly dependent on
 * the connected gateway, such functionalities are delegated to the gateway
 * bundle. The network driver mainly acts as a bridge discovery and connection
 * utility.
 * 
 * @author bonino
 * 
 */
public class HueNetworkDriver implements HueNetwork, PHSDKListener
{
	// the default HUE username
	private String hueUsername = "newdeveloper";

	// the default gateway identifier on HUE bridges
	private String deviceName = "TheDogGateway";

	// the set of bridge discovery listeners
	private Set<HueBridgeDiscoveryListener> hueBridgeDiscoveryListeners;

	// the set of listeners to notify for bridge connection
	private Map<String, Set<HueConnectionListener>> hueBridgeConnectionListeners;

	// the map of currently connected bridges
	private Map<String, PHBridge> connectedBridges;

	// the service registration handle
	private ServiceRegistration<?> regServiceHueNetworkDriver;

	// the service context
	private BundleContext context;

	// the logger
	private LogHelper logger;

	// the HUE sdk singleton
	private PHHueSDK sdk;

	/**
	 * Class constructor, creates a HUE network driver instance and initializes
	 * all the needed data structures. It performs the initial bridge discovery
	 * and user set-up.
	 */
	public HueNetworkDriver()
	{
		// initialize the set of listeners for new bridge detection
		this.hueBridgeDiscoveryListeners = Collections
				.synchronizedSet(new HashSet<HueBridgeDiscoveryListener>());

		// initialize the set of listeners for bridge connection
		this.hueBridgeConnectionListeners = new ConcurrentHashMap<String, Set<HueConnectionListener>>();

		// initialise the set of currently connected gateways
		this.connectedBridges = new ConcurrentHashMap<String, PHBridge>();

		// create the sdk singleton
		this.sdk = PHHueSDK.create();

		// set the device name to store in the white list of connected bridges
		this.sdk.setDeviceName(this.deviceName);

	}

	/**
	 * Called when the driver is activated, performs required initializations
	 * and service registration
	 * 
	 * @param context
	 *            The OSGi {@link BundleContext} needed to perform service
	 *            registration (de-registration).
	 */
	public void activate(BundleContext context)
	{
		// store the bundle context
		this.context = context;

		// initialize the class logger...
		this.logger = new LogHelper(context);

		// debug: signal activation...
		this.logger.log(LogService.LOG_DEBUG, "Activated...");

		// initialize the HUE link
		this.initializeHueLink();

		// register the service
		this.registerNetworkService();
	}

	/**
	 * Deactivates the driver, i.e, removes its services from the framework.
	 */
	public void deactivate()
	{
		// unregister the service
		this.unregisterNetworkService();

		// log
		this.logger.log(LogService.LOG_INFO, "Deactivated...");
	}

	/**
	 * Register this bundle as network driver
	 */
	private void registerNetworkService()
	{
		if (this.regServiceHueNetworkDriver == null)
			this.regServiceHueNetworkDriver = this.context.registerService(
					HueNetwork.class.getName(), this, null);

	}

	/**
	 * Unregister this bundle
	 */
	private void unregisterNetworkService()
	{

		if (this.regServiceHueNetworkDriver != null)
		{
			this.regServiceHueNetworkDriver.unregister();
		}
	}

	/**
	 * Initializes the HUE SDK passing this instance of network driver as SDK
	 * listener
	 */
	private void initializeHueLink()
	{
		// initialize the HUE sdk listener
		this.sdk.getNotificationManager().registerSDKListener(this);
	}

	/**
	 * Performs a search for new bridges, using UPnP
	 */
	public void discoverNewBridges()
	{
		// get the bridge search manager
		PHBridgeSearchManager sm = (PHBridgeSearchManager) this.sdk
				.getSDKService(PHHueSDK.SEARCH_BRIDGE);

		// Start the UPNP Searching of local bridges.
		sm.search(true, true, true);
	}

	@Override
	public PHBridge getBridge(String bridgeIp)
	{
		// return the corresponding bridge object if present and connected
		return this.connectedBridges.get(bridgeIp);
	}

	@Override
	public void addHueBridgeDiscoveryListener(
			HueBridgeDiscoveryListener listener)
	{
		// add the listener to the set of objects to notify when a new bridge is
		// detected
		this.hueBridgeDiscoveryListeners.add(listener);
	}

	@Override
	public void removeHueBridgeDiscoveryListener(
			HueBridgeDiscoveryListener listener)
	{
		// remove the listener from the set of objects to notify when a new
		// bridge is detected
		this.hueBridgeDiscoveryListeners.remove(listener);

	}

	@Override
	public void connectToBridge(String bridgeIp, HueConnectionListener listener)
	{
		if ((bridgeIp != null) && (!bridgeIp.isEmpty()))
		{
			// check if a listener is provided
			if (listener != null)
			{
				// check if a set of listeners is already available
				Set<HueConnectionListener> listeners = this.hueBridgeConnectionListeners
						.get(bridgeIp);

				// if null create a new set for holding bridge listeners
				if (listeners == null)
				{
					listeners = new HashSet<HueConnectionListener>();
					this.hueBridgeConnectionListeners.put(bridgeIp, listeners);
				}

				// add the listener
				listeners.add(listener);
			}

			// create a temporary bridge representation object
			PHAccessPoint accessPoint = new PHAccessPoint();
			accessPoint.setIpAddress(bridgeIp);
			accessPoint.setUsername(this.hueUsername);

			// try connecting to the bridge, on success, the onBridgeConnected
			// method will be called
			this.sdk.connect(accessPoint);
		}
	}

	@Override
	public void startPushLinkAuthentication(String bridgeIp)
	{
		// build the PHAccessPoint instance
		PHAccessPoint accessPoint = new PHAccessPoint();
		accessPoint.setIpAddress(bridgeIp);
		accessPoint.setUsername(this.hueUsername);

		// start the pushLinkAuthentication
		this.sdk.startPushlinkAuthentication(accessPoint);
	}

	@Override
	public void onAccessPointsFound(List<PHAccessPoint> accessPoints)
	{
		// debug log
		this.logger.log(LogService.LOG_DEBUG, "Detected new bridges...\n"
				+ accessPoints);

		// get the list of registered listeners, if any
		for (HueBridgeDiscoveryListener listener : this.hueBridgeDiscoveryListeners)
		{
			// notify the listener
			listener.onAccessPointsFound(accessPoints);
		}

	}

	@Override
	public void onAuthenticationRequired(PHAccessPoint accessPoint)
	{
		// debug
		this.logger.log(LogService.LOG_DEBUG, "Authentication required for: "
				+ accessPoint);

		// get the registered listeners
		Set<HueConnectionListener> listeners = this.hueBridgeConnectionListeners
				.get(accessPoint.getIpAddress());

		// notify the registered listeners
		for (HueConnectionListener listener : listeners)
		{
			// deliver the authentication-required event to registered listeners
			listener.onAuthenticationRequired();
		}

	}

	@Override
	public void onBridgeConnected(PHBridge bridge)
	{
		// called when connection to a bridge is successful

		// switch to the just connected con bridge
		this.sdk.setSelectedBridge(bridge);

		// start the bridge heartbeat
		this.sdk.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);

		// get the bridge ip address
		String ipAddress = bridge.getResourceCache().getBridgeConfiguration()
				.getIpAddress();

		// store the connected bridge (both internally and externally)
		this.sdk.addBridge(bridge);
		this.connectedBridges.put(ipAddress, bridge);

		// get the set of listeners registered for the just connected bridge
		Set<HueConnectionListener> listenersToNotify = this.hueBridgeConnectionListeners
				.get(ipAddress);

		// check not null
		if (listenersToNotify != null)
		{
			// dispatch the bridge connection event
			for (HueConnectionListener listener : listenersToNotify)
				listener.onBridgeConnected(bridge);
		}
	}

	@Override
	public void onCacheUpdated(int flag, PHBridge bridge)
	{
		// call the corresponding handler on the associated listeners

		// get the bridge ip address
		String ipAddress = bridge.getResourceCache().getBridgeConfiguration()
				.getIpAddress();

		// get the set of listeners registered for the just connected bridge
		Set<HueConnectionListener> listenersToNotify = this.hueBridgeConnectionListeners
				.get(ipAddress);

		// check not null
		if (listenersToNotify != null)
		{
			// dispatch the bridge connection event
			for (HueConnectionListener listener : listenersToNotify)
				listener.onCacheUpdated(flag, bridge);
		}
	}

	@Override
	public void onConnectionLost(PHAccessPoint accessPoint)
	{
		// get the access point ip address
		String ipAddress = accessPoint.getIpAddress();

		// remove the bridge from the set of connected bridges
		this.sdk.removeBridge(this.connectedBridges.get(ipAddress));
		this.connectedBridges.remove(ipAddress);

		// notify the bridge connection listeners
		Set<HueConnectionListener> listenersToNotify = this.hueBridgeConnectionListeners
				.get(ipAddress);

		// check not null
		if (listenersToNotify != null)
		{
			// call disconnection handler for each listener
			for (HueConnectionListener listener : listenersToNotify)
				listener.onBridgeDisconnected();
		}
	}

	@Override
	public void onConnectionResumed(PHBridge bridge)
	{
		// get the bridge ip address
		String ipAddress = bridge.getResourceCache().getBridgeConfiguration()
				.getIpAddress();

		// store the connected bridge (both internally and externally)
		this.sdk.addBridge(bridge);
		this.connectedBridges.put(ipAddress, bridge);

		// get the set of listeners registered for the just connected bridge
		Set<HueConnectionListener> listenersToNotify = this.hueBridgeConnectionListeners
				.get(ipAddress);

		if (listenersToNotify != null)
		{
			// dispatch the bridge connection event
			for (HueConnectionListener listener : listenersToNotify)
				listener.onBridgeConnected(bridge);
		}

	}

	@Override
	public void onError(int arg0, String arg1)
	{
		// TODO: handle connection errors
		this.logger.log(LogService.LOG_ERROR, "Error:" + arg1 + ":" + arg0);
	}

}
