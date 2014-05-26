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
package it.polito.elite.dog.drivers.hue.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceDescriptorFactory;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.HueBridge;
import it.polito.elite.dog.core.library.model.state.ConnectionState;
import it.polito.elite.dog.core.library.model.statevalue.ConnectedStateValue;
import it.polito.elite.dog.core.library.model.statevalue.DisconnectedStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueConnectionListener;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHLight;

public class HueGatewayDriverInstance extends HueDriverInstance implements
		HueBridge, HueConnectionListener
{
	// the device factory to use for device (light) creation
	private DeviceFactory deviceFactory;

	// the device descriptor factory needed to create new device instances.
	private DeviceDescriptorFactory descriptorFactory;

	// a set holding the currently known devices
	private Set<String> knownDevices;

	// the drive instance logger
	private LogHelper logger;

	// the device discovery delay
	// TODO: find a more "OSGi" way of performing this task (i.e., waiting for
	// complete device attachment before starting discovery)
	private int deviceDiscoveryDelayMillis = 180000;

	// the device discovery enabled flag
	private boolean discoveryEnabled = false;

	public HueGatewayDriverInstance(HueNetwork hueNetwork,
			DeviceFactory deviceFactory, ControllableDevice device,
			String gatewayAddress, BundleContext context)
	{
		// call the superclass constructor
		super(hueNetwork, device, gatewayAddress);

		// create the instance logger
		this.logger = new LogHelper(context);

		// create the set for storing the currently known devices
		this.knownDevices = new HashSet<String>();

		// store the device factory instance
		this.deviceFactory = deviceFactory;

		// create the device descriptor factory
		try
		{
			this.descriptorFactory = new DeviceDescriptorFactory(context
					.getBundle().getEntry("/deviceTemplates"));
		} catch (Exception e)
		{

			this.logger.log(LogService.LOG_ERROR,
					"Error while creating DeviceDescriptorFactory ", e);
		}

		// create the gateway status object
		this.currentState = new DeviceStatus(device.getDeviceId());

		// attach the hue bridge
		this.network.connectToBridge(this.bridgeIp, this);

		// initialize the device state
		this.initializeStates();

		// start the discovery enable timer...
		// TODO: improve this in order to start as soon as possible
		Timer discoveryEnablingTimer = new Timer();
		discoveryEnablingTimer.schedule(new TimerTask()
		{

			@Override
			public void run()
			{
				discoveryEnabled = true;
			}
		}, deviceDiscoveryDelayMillis);
	}

	public PHBridge getBridge()
	{
		return this.hueBridge;
	}

	@Override
	public DeviceStatus getState()
	{
		return this.currentState;
	}

	@Override
	public void link()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updateStatus()
	{
		((Controllable) this.device).updateStatus();
	}

	@Override
	public void onBridgeConnected(PHBridge bridge)
	{
		// store the reference to the hue bridge
		this.hueBridge = bridge;

		// update the device state
		this.currentState.setState(ConnectionState.class.getSimpleName(),
				new ConnectionState(new ConnectedStateValue()));

		// log connection
		this.logger.log(LogService.LOG_INFO,
				"Connected to the HueBridge located at: " + this.bridgeIp);

		// handle device discovery ...
		PHBridgeResourcesCache cache = bridge.getResourceCache();

        List<PHLight> allLights = cache.getAllLights();
        
        for(PHLight light : allLights)
        {
        	this.logger.log(LogService.LOG_DEBUG, "Found light with local id:"+light.getIdentifier());
        }
	}

	@Override
	public void onBridgeDisconnected()
	{
		// remove the reference to the hue bridge
		this.hueBridge = null;

		// log disconnection
		this.logger.log(LogService.LOG_INFO,
				"Disconnected from the HueBridge located at: " + this.bridgeIp);

	}

	@Override
	public void onCacheUpdated(int flag, PHBridge bridge)
	{
		// handle the bridge status update if needed...
		
		// trigger device update...

		// handle device discovery ...
		if (this.discoveryEnabled)
		{
			// TODO: handle discovery
		}

	}

	/**
	 * Adds the device with the given local Id to the set of devices already
	 * "known" by the gateway instance. This avoids duplication of devices
	 * during the discovery process
	 * 
	 * @param localId
	 */
	public void addDevice(String localId)
	{
		this.knownDevices.add(localId);
	}

	/**
	 * Removes the device with the given localId from the set of known devices,
	 * thus re-enabling its discovery
	 * 
	 * @param localId
	 * @return The just removed localId
	 */
	public String removeDevice(String localId)
	{
		return this.removeDevice(localId);
	}

	@Override
	protected void specificConfiguration()
	{
		// Initially left empty
	}

	private void initializeStates()
	{

		// initialize the state
		this.currentState.setState(ConnectionState.class.getSimpleName(),
				new ConnectionState(new DisconnectedStateValue()));
	}

}
