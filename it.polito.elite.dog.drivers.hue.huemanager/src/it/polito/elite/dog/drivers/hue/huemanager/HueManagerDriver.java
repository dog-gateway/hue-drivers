/*
 * Dog 2.0 - Hue Manager Driver
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
package it.polito.elite.dog.drivers.hue.huemanager;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.HueBridge;
import it.polito.elite.dog.core.library.model.devicecategory.HueManager;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.Dictionary;
import java.util.Hashtable;
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
 * The HueManager is a virtual device representing the dog gateway and its
 * ability (acquired when the HueNetworkDriver is available) to discover new hue
 * bridges reachable over the available network connections, and to perform
 * push-link authentication to the discovered bridges.
 * 
 * It supports dynamic creation and persistence of HueBridge devices, handled by
 * the HueGatewayDriver
 * 
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 * 
 */
public class HueManagerDriver implements Driver, ManagedService
{
	// The OSGi framework context
	protected BundleContext context;

	// System logger
	LogHelper logger;

	// String identifier for driver id
	public static final String DRIVER_ID = "it.polito.elite.drivers.hue.huemanager";

	// a reference to the network driver
	private AtomicReference<HueNetwork> network;

	// a reference to the device factory service used to handle run-time
	// creation of bridge devices in dog.
	private AtomicReference<DeviceFactory> deviceFactory;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object used by the
	// bundle for registering the provided service, to update the service's
	// properties, or to unregister the service).
	private ServiceRegistration<?> regDriver;

	// registration object needed to publish the services offered by this hue
	// manager driver
	// TODO: define a proper service interface and register the interface
	// instead of the class
	private ServiceRegistration<?> regHueManager;
	
	//the only manager device
	private HueManagerDriverInstance theInstance;

	/**
	 * Class constructor, initializes inner data structures.
	 */
	public HueManagerDriver()
	{
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

	/**
	 * Handles bundle de-activation and relative house keeping tasks
	 */
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
			propDriver
					.put(DeviceCostants.DRIVER_ID, HueManagerDriver.DRIVER_ID);

			this.regDriver = this.context.registerService(
					Driver.class.getName(), this, propDriver);

			// register the hue manager service
			this.regHueManager = this.context.registerService(
					HueManagerDriver.class.getName(), this, null);
		}
	}

	/**
	 * Remove the bundle services from the framework.
	 */
	protected void unRegister()
	{
		// un-registers this driver
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}

		// un-register the manager service
		if (this.regHueManager != null)
		{
			this.regHueManager.unregister();
			this.regHueManager = null;
		}
	}

	// --------------- Handling service binding ---------------------

	/**
	 * Binds the given {@link HueNetwork} driver
	 * 
	 * @param networkDriver
	 *            The driver service to bind.
	 */
	public void addedNetworkDriver(HueNetwork networkDriver)
	{
		this.network.set(networkDriver);
	}

	/**
	 * UnBinds the given {@link HueNetwork} driver
	 * 
	 * @param networkDriver
	 *            The driver service to un-bind.
	 */
	public void removedNetworkDriver(HueNetwork networkDriver)
	{
		if (this.network.compareAndSet(networkDriver, null))
			// unregisters this driver from the OSGi framework
			unRegister();
	}

	/**
	 * Binds the given {@link DeviceFactory} service.
	 * 
	 * @param deviceFactory
	 *            The device factory service to bind.
	 */
	public void addedDeviceFactory(DeviceFactory deviceFactory)
	{
		this.deviceFactory.set(deviceFactory);

	}

	/**
	 * Un-binds the given {@link DeviceFactory} service.
	 * 
	 * @param deviceFactory
	 *            The device factory service to un-bind.
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
					&& (deviceCategory.equals(HueManager.class.getName())))
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

			// create a HueManagerDriver instance (it is a singleton)
			this.theInstance = new HueManagerDriverInstance(
					this.network.get(), this.deviceFactory.get(), device,
					this.context);

			// associate device and driver
			device.setDriver(this.theInstance);

		}
		//
		return null;
	}

	@Override
	public void updated(Dictionary<String, ?> arg0)
			throws ConfigurationException
	{
		// TODO Handle configuration here....
		this.logger.log(LogService.LOG_DEBUG, "updated");

	}

	/**
	 * @return the theInstance
	 */
	public HueManagerDriverInstance getTheInstance()
	{
		return theInstance;
	}

	/**
	 * @param theInstance the theInstance to set
	 */
	public void setTheInstance(HueManagerDriverInstance theInstance)
	{
		this.theInstance = theInstance;
	}
	
	

}
