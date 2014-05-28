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

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.HueBridge;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class HueGatewayDriver implements Driver, ManagedService
{
	// The OSGi framework context
	protected BundleContext context;

	// System logger
	LogHelper logger;

	// String identifier for driver id
	public static final String DRIVER_ID = "it.polito.elite.drivers.hue.gateway";

	// a reference to the network driver
	private AtomicReference<HueNetwork> network;

	// a reference to the device factory service used to handle run-time
	// creation of devices in dog.
	private AtomicReference<DeviceFactory> deviceFactory;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object used by the
	// bundle for registering the provided service, to update the service's
	// properties, or to unregister the service).
	private ServiceRegistration<?> regDriver;

	// register this driver as a gateway used by device-specific drivers
	private ServiceRegistration<?> regHueGateway;

	// the set of currently connected gateways... indexed by their IP addresses
	// as strings.
	private ConcurrentHashMap<String, HueGatewayDriverInstance> connectedGateways;

	/**
	 * The class constructor, initializes inner datastructures to prepare the
	 * bundle activation.
	 */
	public HueGatewayDriver()
	{
		// initialize the map of connected gateways
		this.connectedGateways = new ConcurrentHashMap<String, HueGatewayDriverInstance>();

		// initialize the network driver reference
		this.network = new AtomicReference<HueNetwork>();

		// initialize the device factory reference
		this.deviceFactory = new AtomicReference<DeviceFactory>();
	}

	/**
	 * Handle the bundle activation
	 */
	public void activate(BundleContext bundleContext)
	{
		// store the context
		context = bundleContext;

		// init the logger
		logger = new LogHelper(context);

		this.registerDriver();
	}

	public void deactivate()
	{
		// remove the service from the OSGi framework
		this.unRegister();
	}

	/**
	 * Registers this driver in the OSGi framework, making its services
	 * available to all the other bundles living in the same or in connected
	 * frameworks.
	 */
	private void registerDriver()
	{
		if ((network.get() != null) && (this.context != null)
				&& (this.regDriver == null))
		{
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			propDriver.put(DeviceCostants.DRIVER_ID, HueGatewayDriver.DRIVER_ID);
			propDriver.put(DeviceCostants.GATEWAY_COUNT,
					connectedGateways.size());

			this.regDriver = this.context.registerService(
					Driver.class.getName(), this, propDriver);
			this.regHueGateway = this.context.registerService(
					HueGatewayDriver.class.getName(), this, null);
		}
	}

	/**
	 * Handle the bundle de-activation
	 */
	protected void unRegister()
	{
		// un-registers this driver
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}

		// un-register the gateway service
		if (this.regHueGateway != null)
		{
			this.regHueGateway.unregister();
			this.regHueGateway = null;
		}
	}

	// --------------- Handling service binding ---------------------

	/**
	 * 
	 * @param networkDriver
	 */
	public void addedNetworkDriver(HueNetwork networkDriver)
	{
		this.network.set(networkDriver);
	}

	/**
	 * 
	 * @param networkDriver
	 */
	public void removedNetworkDriver(HueNetwork networkDriver)
	{
		if (this.network.compareAndSet(networkDriver, null))
			// unregisters this driver from the OSGi framework
			unRegister();
	}

	/**
	 * 
	 * @param deviceFactory
	 */
	public void addedDeviceFactory(DeviceFactory deviceFactory)
	{
		this.deviceFactory.set(deviceFactory);

	}

	/**
	 * 
	 * @param deviceFactory
	 */
	public void removedDeviceFactory(DeviceFactory deviceFactory)
	{
		if (this.deviceFactory.compareAndSet(deviceFactory, null))
			// unregisters this driver from the OSGi framework
			unRegister();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;

		// get the given device category
		String deviceCategory = (String) reference
				.getProperty(DeviceCostants.DEVICE_CATEGORY);

		// get the given device manufacturer
		String manufacturer = (String) reference
				.getProperty(DeviceCostants.MANUFACTURER);

		// compute the matching score between the given device and this driver
		if (deviceCategory != null)
		{
			if (manufacturer != null
					&& manufacturer.equals(HueInfo.MANUFACTURER)
					&& (deviceCategory.equals(HueBridge.class.getName())))
			{
				matchValue = HueBridge.MATCH_MANUFACTURER
						+ HueBridge.MATCH_TYPE;
			}

		}
		return matchValue;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public String attach(ServiceReference reference) throws Exception
	{
		if (this.regDriver != null)
		{
			// get the controllable device to attach
			@SuppressWarnings("unchecked")
			ControllableDevice device = (ControllableDevice) this.context
					.getService(reference);

			// get the device id
			String deviceId = device.getDeviceId();

			// get the device ip
			Set<String> allGatewayIpAddresses = device.getDeviceDescriptor()
					.getSimpleConfigurationParams()
					.get(HueInfo.GATEWAY_ADDRESS);

			// if not null, it is a singleton
			if (allGatewayIpAddresses != null)
			{
				// get the endpoint address of the connecting gateway
				String gatewayAddress = allGatewayIpAddresses.iterator().next();

				// check not null
				if ((gatewayAddress != null) && (!gatewayAddress.isEmpty()))
				{
					if (!this.isGatewayAvailable(deviceId))
					{
						// create a new driver instance
						HueGatewayDriverInstance driverInstance = new HueGatewayDriverInstance(
								this.network.get(), this.deviceFactory.get(),
								device, gatewayAddress, this.context);

						// associate device and driver
						device.setDriver(driverInstance);

						// store the just created gateway instance
						synchronized (connectedGateways)
						{
							// store a reference to the gateway driver
							connectedGateways.put(device.getDeviceId(),
									driverInstance);
						}

						// modify the service description causing a forcing the
						// framework to send a modified service notification
						final Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
						propDriver.put(DeviceCostants.DRIVER_ID, DRIVER_ID);
						propDriver.put(DeviceCostants.GATEWAY_COUNT,
								connectedGateways.size());

						this.regDriver.setProperties(propDriver);
					}
				}
			}
		}

		return null;
	}

	/**
	 * check if the gateway identified by the given gateway id is currently
	 * registered with this driver
	 * 
	 * @param gatewayId
	 * @return true if the gateway corresponding to the given id is already
	 *         registered, false otherwise.
	 */
	public boolean isGatewayAvailable(String gatewayId)
	{
		return connectedGateways.containsKey(gatewayId);
	}

	/**
	 * Returns the {@link HueGatewayDriverInstance} associated to the given
	 * gateway URI
	 * 
	 * @param gateway
	 *            the gateway URI.
	 * @return the corresponding instance, if exists, null otherwise.
	 */
	public HueGatewayDriverInstance getSpecificGateway(String gateway)
	{
		return this.connectedGateways.get(gateway);
	}

	@Override
	public void updated(Dictionary<String, ?> arg0)
			throws ConfigurationException
	{
		// TODO Handle configuration here....
		this.logger.log(LogService.LOG_DEBUG, "updated");
	}

}
