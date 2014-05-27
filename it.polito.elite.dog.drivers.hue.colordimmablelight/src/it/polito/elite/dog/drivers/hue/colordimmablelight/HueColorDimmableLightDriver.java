/**
 * 
 */
package it.polito.elite.dog.drivers.hue.colordimmablelight;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.log.LogService;

import com.philips.lighting.model.PHBridge;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.drivers.hue.device.HueDeviceDriver;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;



/**
 * @author bonino
 *
 */
public class HueColorDimmableLightDriver extends HueDeviceDriver
{
	// the step entity in percentage
	protected int stepPercentage;

	@Override
	public HueDriverInstance createHueDriverInstance(HueNetwork hueNetwork,
			ControllableDevice device, int localId, PHBridge bridge,
			BundleContext context)
	{
		return new HueColorDimmableLightDriverInstance(hueNetwork, device, localId, bridge, this.stepPercentage, context);
	}

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException
	{
		if (properties != null)
		{			
			// try to get the step entity
			String stepEntityAsString = (String) properties.get(HueInfo.STEP_PERCENT);
			
			// check not null
			if (stepEntityAsString != null)
			{
				// trim leading and trailing spaces
				stepEntityAsString = stepEntityAsString.trim();
				// parse the string
				this.stepPercentage = Integer.valueOf(stepEntityAsString);
			}
			else
			{
				this.logger.log(LogService.LOG_WARNING, HueInfo.STEP_PERCENT
						+ " not defined in configuraton file for "
						+ HueColorDimmableLightDriver.class.getName());
			}
			
			// call the normal updated method
			super.updated(properties);
		}
	}
}
