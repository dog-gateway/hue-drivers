/**
 * 
 */
package it.polito.elite.dog.drivers.hue.huemanager;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceDescriptor;
import it.polito.elite.dog.core.library.model.DeviceDescriptorFactory;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.HueBridge;
import it.polito.elite.dog.core.library.model.devicecategory.HueManager;
import it.polito.elite.dog.core.library.model.state.DiscoveryState;
import it.polito.elite.dog.core.library.model.statevalue.ActiveDiscoveryStateValue;
import it.polito.elite.dog.core.library.model.statevalue.IdleStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueBridgeDiscoveryListener;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.model.PHLightState;

/**
 * @author bonino
 * 
 */
public class HueManagerDriverInstance extends HueDriverInstance implements
		HueManager, HueBridgeDiscoveryListener
{

	// the device factory to use for device (gateway) creation
	private DeviceFactory deviceFactory;

	// the device descriptor factory needed to create new device instances.
	private DeviceDescriptorFactory descriptorFactory;

	// the set of known (already discovered) bridges
	private Set<String> knownGateways;

	// the drive instance logger
	private LogHelper logger;

	public HueManagerDriverInstance(HueNetwork hueNetwork,
			DeviceFactory deviceFactory, ControllableDevice device,
			BundleContext context)
	{
		// call the superclass constructor
		super(hueNetwork, device, null);

		// create the instance logger
		this.logger = new LogHelper(context);

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

		// initialize the device state
		this.initializeStates();

		// add this as discovery listener
		this.network.addHueBridgeDiscoveryListener(this);
	}

	private void initializeStates()
	{
		// set the discovery state at idle
		this.currentState.setState(DiscoveryState.class.getSimpleName(),
				new DiscoveryState(new IdleStateValue()));
	}

	@Override
	public DeviceStatus getState()
	{
		// return the current state (global) of the device
		return this.currentState;
	}

	@Override
	public void stopDiscovery()
	{
		// discovery cannot be stopped...

		// set the current state at discovering
		this.currentState.setState(DiscoveryState.class.getSimpleName(),
				new DiscoveryState(new IdleStateValue()));

		// notify
		this.notifyDeactivatedDiscovery();

	}

	@Override
	public void startDiscovery()
	{
		// trigger a new discovery
		this.network.discoverNewBridges();

		// set the current state at discovering
		this.currentState.setState(DiscoveryState.class.getSimpleName(),
				new DiscoveryState(new ActiveDiscoveryStateValue()));

		// notify
		this.notifyActivatedDiscovery();
	}

	@Override
	public void notifyDeactivatedDiscovery()
	{
		((HueManager) this.device).notifyDeactivatedDiscovery();
	}

	@Override
	public void notifyActivatedDiscovery()
	{
		((HueManager) this.device).notifyActivatedDiscovery();
	}

	@Override
	public void updateStatus()
	{
		((Controllable) this.device).updateStatus();
	}

	@Override
	protected void specificConfiguration()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void newMessageFromHouse(PHLightState lastKnownLightState)
	{
		// Intentionally left empty as it does not apply for this synthetic
		// device
	}

	/**
	 * Adds the given IP address to the set of the IP addresses of the already
	 * discovered gateways
	 * 
	 * @param gwIpAddress
	 */
	public void addGateway(String gwIpAddress)
	{
		this.knownGateways.add(gwIpAddress);
	}

	@Override
	public void onAccessPointsFound(List<PHAccessPoint> accessPoints)
	{
		// handle bridge discovery here...

		for (PHAccessPoint accessPoint : accessPoints)
		{
			// check if not yet known
			if (!this.knownGateways.contains(accessPoint.getIpAddress()))
			{
				// build a device descriptor for the gateway
				DeviceDescriptor gatewayDescriptor = this
						.buildGatewayDescriptor(accessPoint);

				// add the gateway
				// create the device and cross your fingers...
				this.deviceFactory.addNewDevice(gatewayDescriptor);
			}
		}

	}

	private DeviceDescriptor buildGatewayDescriptor(PHAccessPoint accessPoint)
	{
		// the device descriptor to return
		DeviceDescriptor descriptor = null;

		if (this.descriptorFactory != null)
		{
			// create a descriptor definition map
			HashMap<String, Object> descriptorDefinitionData = new HashMap<String, Object>();

			// the device class is fixed
			String deviceClass = HueBridge.class.getSimpleName();

			// build the gateway name (TODO: check if it is suitable)
			descriptorDefinitionData.put(DeviceDescriptorFactory.NAME,
					deviceClass + "_" + accessPoint.getIpAddress());

			// store the device description
			descriptorDefinitionData.put(DeviceDescriptorFactory.DESCRIPTION,
					"New Device of type " + deviceClass);

			// store the device location
			descriptorDefinitionData.put(DeviceDescriptorFactory.LOCATION, "");

			// store the device local id
			descriptorDefinitionData.put(HueInfo.LOCAL_ID,
					HueInfo.GATEWAY_DEFAULT_LOCAL_ID);

			// store the gateway ip address
			descriptorDefinitionData.put(HueInfo.GATEWAY_ADDRESS,
					accessPoint.getIpAddress());

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
			this.logger.log(LogService.LOG_INFO,
					"Detected new device:\n\ttdeviceClass: " + deviceClass);

		}

		return descriptor;
	}
}
