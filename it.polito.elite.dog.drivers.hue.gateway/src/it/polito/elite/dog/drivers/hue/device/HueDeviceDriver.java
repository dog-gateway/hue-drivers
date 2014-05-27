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
package it.polito.elite.dog.drivers.hue.device;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.gateway.HueGatewayDriver;
import it.polito.elite.dog.drivers.hue.gateway.HueGatewayDriverInstance;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;

import com.philips.lighting.model.PHBridge;

/**
 * @author bonino
 * 
 */
public abstract class HueDeviceDriver implements Driver, ManagedService
{
	// The OSGi framework context
	protected BundleContext context;

	// System logger
	protected LogHelper logger;

	// a reference to the network driver
	protected AtomicReference<HueNetwork> network;

	// a reference to the gateway driver
	protected AtomicReference<HueGatewayDriver> gateway;

	// the list of instances controlled / spawned by this driver
	protected Hashtable<String, HueDriverInstance> managedInstances;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;

	// the filter query for listening to framework events relative to the
	// to the Hue gateway driver
	String filterQuery = String.format("(%s=%s)", Constants.OBJECTCLASS,
			HueGatewayDriver.class.getName());

	// what are the device categories that can match with this driver?
	protected Set<String> deviceCategories;

	// the driver instance class from which extracting the supported device
	// categories
	protected Class<?> driverInstanceClass;

	public HueDeviceDriver()
	{
		// initialize atomic references to the network and to the gateway
		// drivers
		this.network = new AtomicReference<HueNetwork>();
		this.gateway = new AtomicReference<HueGatewayDriver>();
		
		//initialize the list of managed device instances (indexed by device id)
		this.managedInstances = new Hashtable<String, HueDriverInstance>();
		
		//intialize the device categories matched by this driver
		this.deviceCategories = new HashSet<String>();
	}
	
	/**
	 * Handle the bundle activation
	 */
	public void activate(BundleContext bundleContext)
	{
		// init the logger
		this.logger = new LogHelper(bundleContext);

		// store the context
		this.context = bundleContext;

		// fill the device categories
		this.properFillDeviceCategories(this.driverInstanceClass);

	}

	public void deactivate()
	{
		// remove the service from the OSGi framework
		this.unRegisterHueDeviceDriver();
	}
	
	public void addedNetworkDriver(HueNetwork network)
	{
		// log network river addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG,"Added network driver");

		// store the network driver reference
		this.network.set(network);
	}

	public void removedNetworkDriver(HueNetwork network)
	{
		// null the network freeing the old reference for gc
		if (this.network.compareAndSet(network, null))
		{
			// unregister the services
			this.unRegisterHueDeviceDriver();

			// log network river removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG,"Removed network driver");

		}
	}


	public void addedGatewayDriver(HueGatewayDriver gateway)
	{
		// log network driver addition
		if (this.logger != null)
			this.logger.log(LogService.LOG_DEBUG, "Added gateway driver");

		// store the network driver reference
		this.gateway.set(gateway);

	}

	public void removedGatewayDriver(HueGatewayDriver gateway)
	{
		// null the gateway freeing the old reference for gc
		if (this.gateway.compareAndSet(gateway, null))
		{
			// unregister the services
			this.unRegisterHueDeviceDriver();
			// log network driver removal
			if (this.logger != null)
				this.logger.log(LogService.LOG_DEBUG, "Removed gateway driver");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;

		// get the given device category
		String deviceCategory = (String) reference
				.getProperty(DeviceCostants.DEVICE_CATEGORY);

		// get the given device manufacturer
		String manifacturer = (String) reference
				.getProperty(DeviceCostants.MANUFACTURER);

		// get the gateway to which the device is connected
		String gateway = (String) reference.getProperty(DeviceCostants.GATEWAY);

		// compute the matching score between the given device and
		// this driver
		if (deviceCategory != null)
		{
			if (manifacturer != null && (gateway != null)
					&& (manifacturer.equals(HueInfo.MANUFACTURER))
					&& (this.deviceCategories.contains(deviceCategory))
					&& (this.gateway.get().isGatewayAvailable(gateway)))
			{
				matchValue = Controllable.MATCH_MANUFACTURER
						+ Controllable.MATCH_TYPE;
			}

		}

		return matchValue;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized String attach(ServiceReference reference)
			throws Exception
	{
		// get the referenced device
		ControllableDevice device = ((ControllableDevice) context
				.getService(reference));

		// check if not already attached
		if (!this.managedInstances.containsKey(device.getDeviceId()))
		{
			// get the gateway to which the device is connected
			String gateway = (String) device.getDeviceDescriptor().getGateway();

			// get the corresponding end point set
			Set<String> localIdSet = device.getDeviceDescriptor()
					.getSimpleConfigurationParams().get(HueInfo.LOCAL_ID);

			// get the nodeId
			String sLocalID = localIdSet.iterator().next();

			// create a new driver instance
			HueDriverInstance driverInstance = this
					.createHueDriverInstance(network.get(), device,
							Integer.parseInt(sLocalID), ((HueGatewayDriverInstance)(this.gateway.get().getSpecificGateway(gateway)))
									.getBridge(),context);

			// connect this driver instance with the device
			device.setDriver(driverInstance);

			// store a reference to the connected driver
			synchronized (this.managedInstances)
			{
				this.managedInstances.put(device.getDeviceId(), driverInstance);
			}
		}

		return null;
	}

	public abstract HueDriverInstance createHueDriverInstance(HueNetwork hueNetwork,
			ControllableDevice device, int localId, PHBridge bridge,
			BundleContext context);

	/**
	 * Registers this driver in the OSGi framework, making its services
	 * available to all the other bundles living in the same or in connected
	 * frameworks.
	 */
	private void registerHueDeviceDriver()
	{
		if ((gateway.get() != null) && (network.get() != null)
				&& (this.context != null) && (this.regDriver == null))
		{
			// create a new property object describing this driver
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			// add the id of this driver to the properties
			propDriver.put(DeviceCostants.DRIVER_ID, this.getClass().getName());
			// register this driver in the OSGi framework
			regDriver = context.registerService(Driver.class.getName(), this,
					propDriver);
		}
	}

	/**
	 * Handle the bundle de-activation
	 */
	protected void unRegisterHueDeviceDriver()
	{
		// TODO DETACH allocated Drivers
		if (regDriver != null)
		{
			regDriver.unregister();
			regDriver = null;
		}
	}

	/**
	 * Fill a set with all the device categories whose devices can match with
	 * this driver. Automatically retrieve the device categories list by reading
	 * the implemented interfaces of its DeviceDriverInstance class bundle.
	 */
	public void properFillDeviceCategories(Class<?> cls)
	{
		if (cls != null)
		{
			for (Class<?> devCat : cls.getInterfaces())
			{
				this.deviceCategories.add(devCat.getName());
			}
		}

	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		if (properties != null)
		{
			//handle any property here
			

			// register driver
			registerHueDeviceDriver();
		}
	}

}
