/**
 * 
 */
package it.polito.elite.dog.drivers.hue.huemanager;

import java.util.List;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceDescriptorFactory;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.HueManager;
import it.polito.elite.dog.core.library.model.state.DiscoveryState;
import it.polito.elite.dog.core.library.model.statevalue.ActiveDiscoveryStateValue;
import it.polito.elite.dog.core.library.model.statevalue.IdleStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
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
		//discovery cannot be stopped...
		
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

		//notify
		this.notifyActivatedDiscovery();
	}

	
	@Override
	public void notifyDeactivatedDiscovery()
	{
		((HueManager)this.device).notifyDeactivatedDiscovery();
	}

	@Override
	public void notifyActivatedDiscovery()
	{
		((HueManager)this.device).notifyActivatedDiscovery();
	}

	@Override
	public void updateStatus()
	{
		((Controllable)this.device).updateStatus();
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

	@Override
	public void onAccessPointsFound(List<PHAccessPoint> accessPoints)
	{
		//handle bridge discovery here...
		
	}

}
