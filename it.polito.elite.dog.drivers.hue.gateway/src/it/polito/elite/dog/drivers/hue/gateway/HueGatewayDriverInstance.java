/*
 * Dog 2.0 - Hue Gateway Driver
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

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceDescriptor;
import it.polito.elite.dog.core.library.model.DeviceDescriptorFactory;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ColorDimmableLight;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.DimmableLight;
import it.polito.elite.dog.core.library.model.devicecategory.HueBridge;
import it.polito.elite.dog.core.library.model.devicecategory.OnOffLight;
import it.polito.elite.dog.core.library.model.state.ConnectionState;
import it.polito.elite.dog.core.library.model.state.PushLinkAuthenticationState;
import it.polito.elite.dog.core.library.model.statevalue.ActivePushLinkAuthenticationStateValue;
import it.polito.elite.dog.core.library.model.statevalue.AuthenticatedStateValue;
import it.polito.elite.dog.core.library.model.statevalue.ConnectedStateValue;
import it.polito.elite.dog.core.library.model.statevalue.DisconnectedStateValue;
import it.polito.elite.dog.core.library.model.statevalue.NeedingAuthenticationStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueConnectionListener;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class HueGatewayDriverInstance extends HueDriverInstance implements
		HueBridge, HueConnectionListener
{
	// the device factory to use for device (light) creation
	private DeviceFactory deviceFactory;

	// the device descriptor factory needed to create new device instances.
	private DeviceDescriptorFactory descriptorFactory;

	// a set holding the currently known devices
	private HashMap<String, HueDriverInstance> knownDevices;

	// the drive instance logger
	private LogHelper logger;

	// the device discovery delay
	// TODO: find a more "OSGi" way of performing this task (i.e., waiting for
	// complete device attachment before starting discovery)
	private int deviceDiscoveryDelayMillis = 30000;
	private int pushLinkTimeoutMillis = 30000;

	Timer pushLinkAuthenticationTimer;

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
		this.knownDevices = new HashMap<String, HueDriverInstance>();

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
		}, this.deviceDiscoveryDelayMillis);
	}

	/**
	 * Provides the {@link PHBridge} instance representing the Hue bridge
	 * managed by this driver instance.
	 * 
	 * @return
	 */
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
	public void updateStatus()
	{
		((Controllable) this.device).updateStatus();
	}

	@Override
	public void stopPushLinkAuth()
	{
		// get the current authentication state
		PushLinkAuthenticationState currentPushLinkAuthState = (PushLinkAuthenticationState) this.currentState
				.getState(PushLinkAuthenticationState.class.getSimpleName());

		// if when called it is still in the push-link authentication stata,
		// then turns back to the needing authentication state.
		if ((currentPushLinkAuthState.getCurrentStateValue()[0].getName()
				.equals(ActivePushLinkAuthenticationStateValue.class
						.getSimpleName())))
		{
			// check that the authentication timer is not null, i.e., possibly
			// active
			if (this.pushLinkAuthenticationTimer != null)
				// stop the timer
				this.pushLinkAuthenticationTimer.cancel();

			// get the current connection state
			ConnectionState currentConnectionState = (ConnectionState) this.currentState
					.getState(ConnectionState.class.getSimpleName());

			// if connected set the authentication state to authenticated
			if (currentConnectionState.getCurrentStateValue()[0].getName()
					.equals(ConnectedStateValue.class.getSimpleName()))
			{
				// update the authentication state
				this.currentState.setState(PushLinkAuthenticationState.class
						.getSimpleName(), new PushLinkAuthenticationState(
						new AuthenticatedStateValue()));
			} else
			{
				// update the current authentication state
				this.currentState.setState(PushLinkAuthenticationState.class
						.getSimpleName(), new PushLinkAuthenticationState(
						new NeedingAuthenticationStateValue()));
			}

			// notify deactivation of the push-link authentication
			this.notifyDeactivatedPushLinkAuth();
		}

	}

	@Override
	public void startPushLinkAuth()
	{
		// start the authentication process
		this.network.startPushLinkAuthentication(this.bridgeIp);

		// change the state
		this.currentState.setState(PushLinkAuthenticationState.class
				.getSimpleName(), new PushLinkAuthenticationState(
				new ActivePushLinkAuthenticationStateValue()));

		// notify activation
		this.notifyActivatedPushLinkAuth();

		// start the discovery enable timer...
		// TODO: improve this in order to start as soon as possible
		this.pushLinkAuthenticationTimer = new Timer();
		this.pushLinkAuthenticationTimer.schedule(new TimerTask()
		{

			@Override
			public void run()
			{
				stopPushLinkAuth();
			}
		}, this.pushLinkTimeoutMillis);

	}

	@Override
	public void notifyDeactivatedPushLinkAuth()
	{
		((HueBridge) this.device).notifyDeactivatedPushLinkAuth();
	}

	@Override
	public void notifyActivatedPushLinkAuth()
	{
		((HueBridge) this.device).notifyActivatedPushLinkAuth();
	}

	@Override
	public void onBridgeConnected(PHBridge bridge)
	{
		// store the reference to the hue bridge
		this.hueBridge = bridge;

		// update the device state
		this.currentState.setState(ConnectionState.class.getSimpleName(),
				new ConnectionState(new ConnectedStateValue()));

		// stop any authentication being on
		this.stopPushLinkAuth();

		// log connection
		this.logger.log(LogService.LOG_DEBUG,
				"Connected to the HueBridge located at: " + this.bridgeIp);

		// handle device discovery ...
		if (this.discoveryEnabled)
			this.findNewDevices(bridge);
	}

	@Override
	public void onBridgeDisconnected()
	{
		// remove the reference to the hue bridge
		this.hueBridge = null;

		// update the device state
		this.currentState.setState(ConnectionState.class.getSimpleName(),
				new ConnectionState(new DisconnectedStateValue()));

		// log disconnection
		this.logger.log(LogService.LOG_INFO,
				"Disconnected from the HueBridge located at: " + this.bridgeIp);

	}

	@Override
	public void onCacheUpdated(int flag, PHBridge bridge)
	{
		// handle the bridge status update if needed...

		// trigger device update...
		List<PHLight> allLights = bridge.getResourceCache().getAllLights();

		for (PHLight light : allLights)
		{
			// get the attached driver, if available
			HueDriverInstance driverInstance = this.knownDevices.get(light
					.getIdentifier());

			// notify the driver
			driverInstance.newMessageFromHouse(light.getLastKnownLightState());
		}

	}

	@Override
	public void onAuthenticationRequired()
	{
		// update the device state
		this.currentState.setState(PushLinkAuthenticationState.class
				.getSimpleName(), new PushLinkAuthenticationState(
				new NeedingAuthenticationStateValue()));

	}

	/**
	 * Adds the device with the given local Id to the set of devices already
	 * "known" by the gateway instance. This avoids duplication of devices
	 * during the discovery process
	 * 
	 * @param localId
	 */
	public void addDevice(String localId, HueDriverInstance driverInstance)
	{
		this.knownDevices.put(localId, driverInstance);
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

	/**
	 * Defined the initial state of the device connected to this driver
	 * instance.
	 */
	private void initializeStates()
	{

		// initialize the state
		this.currentState.setState(ConnectionState.class.getSimpleName(),
				new ConnectionState(new DisconnectedStateValue()));

		// set the push-link authetication status at idle
		this.currentState.setState(PushLinkAuthenticationState.class
				.getSimpleName(), new PushLinkAuthenticationState(
				new NeedingAuthenticationStateValue()));
	}

	/**
	 * Search for new devices (color dimmable lights) and handle the discovery
	 * process for devices that not yet handled by drivers "connected" to the
	 * current gateway instance.
	 * 
	 * @param bridge
	 */
	private void findNewDevices(PHBridge bridge)
	{
		// get the latest information from the api cache (updated when the
		// bridge connects and on subsequent heart beats).
		PHBridgeResourcesCache cache = bridge.getResourceCache();

		// find all lights connected to the bridge
		List<PHLight> allLights = cache.getAllLights();

		// iterate over all lights and check if they are already handled or not
		for (PHLight light : allLights)
		{
			// debug, log the found light
			this.logger.log(LogService.LOG_DEBUG, "Found light with local id:"
					+ light.getIdentifier() + " type: " + light.getLightType());

			// check if the device is already registered
			if (!this.knownDevices.keySet().contains(light.getIdentifier()))
			{
				// the light is new and a new device should be created in Dog
				DeviceDescriptor newDevice = this.buildDeviceDescriptor(light);

				// check not null
				if (newDevice != null)
				{
					// create the device and cross your fingers...
					this.deviceFactory.addNewDevice(newDevice);

					// in theory the device could be set as known here, however
					// in the first release it will be set as known only when
					// Dog actually recognizes it and attaches it to the right
					// driver. As this might generate some issue, in next
					// releases this choice might change...
				}
			}
		}

	}

	/**
	 * Given a {@link PHLight} instance, builds the corresponding Dog device
	 * descriptor.
	 * 
	 * @param light
	 *            The light instance to model.
	 * @return the resulting model as a String.
	 */
	private DeviceDescriptor buildDeviceDescriptor(PHLight light)
	{
		// the device descriptor to return
		DeviceDescriptor descriptor = null;

		if (this.descriptorFactory != null)
		{
			// create a descriptor definition map
			HashMap<String, Object> descriptorDefinitionData = new HashMap<String, Object>();

			// define the device class
			// TODO: handle missing light types, now default to on-off
			String deviceClass = null;

			switch (light.getLightType())
			{
			case CT_COLOR_LIGHT:
			{
				deviceClass = ColorDimmableLight.class.getSimpleName();
				break;
			}
			case DIM_LIGHT:
			{
				deviceClass = DimmableLight.class.getSimpleName();
				break;
			}
			case ON_OFF_LIGHT:
			default:
			{
				deviceClass = OnOffLight.class.getSimpleName();
				break;
			}
			}

			if ((deviceClass != null) && (!deviceClass.isEmpty()))
			{
				// store the device name
				descriptorDefinitionData.put(DeviceDescriptorFactory.NAME,
						this.device.getDeviceId() + "_" + deviceClass + "_"
								+ light.getIdentifier());

				// store the device description
				descriptorDefinitionData.put(
						DeviceDescriptorFactory.DESCRIPTION,
						"New Device of type " + deviceClass);

				// store the device gateway
				descriptorDefinitionData.put(DeviceDescriptorFactory.GATEWAY,
						this.device.getDeviceId());

				// store the device location
				descriptorDefinitionData.put(DeviceDescriptorFactory.LOCATION,
						"");

				// store the device local id
				descriptorDefinitionData.put(HueInfo.LOCAL_ID,
						light.getIdentifier());

				// get the device descriptor
				try
				{
					descriptor = this.descriptorFactory.getDescriptor(
							descriptorDefinitionData, deviceClass);
				} catch (Exception e)
				{
					this.logger
							.log(LogService.LOG_ERROR,
									"Error while creating DeviceDescriptor for the just added device ",
									e);
				}

				// debug dump
				this.logger.log(
						LogService.LOG_INFO,
						"Detected new device:\n\tlocalId: "
								+ light.getIdentifier() + "\n\tdeviceClass: "
								+ deviceClass);
			}
		}

		return descriptor;
	}

	@Override
	public void newMessageFromHouse(PHLightState lastKnownLightState)
	{
		// intentionally left empty
	}

}
