/*
 * Dog 2.0 - Hue Color Dimmable Light Driver
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
package it.polito.elite.dog.drivers.hue.colordimmablelight;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.ColorDimmableLight;
import it.polito.elite.dog.drivers.hue.device.HueDeviceDriver;
import it.polito.elite.dog.drivers.hue.gateway.HueGatewayDriverInstance;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.log.LogService;

/**
 * The driver manager for handling Philips Hue colored lights
 * 
 * @author bonino
 * 
 */
public class HueColorDimmableLightDriver extends HueDeviceDriver
{
	// the step entity in percentage
	protected int stepPercentage;

	/**
	 * Class constructor, initializes inner data structures
	 */
	public HueColorDimmableLightDriver()
	{
		// fill supported device categories
		this.deviceCategories.add(ColorDimmableLight.class.getName());
	}

	@Override
	public HueDriverInstance createHueDriverInstance(HueNetwork hueNetwork,
			ControllableDevice device, String localId,
			HueGatewayDriverInstance gateway, BundleContext context)
	{
		return new HueColorDimmableLightDriverInstance(hueNetwork, device,
				localId, gateway, this.stepPercentage, context);
	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		if (properties != null)
		{
			// try to get the step entity
			String stepEntityAsString = (String) properties
					.get(HueInfo.STEP_PERCENT);

			// check not null
			if (stepEntityAsString != null)
			{
				// trim leading and trailing spaces
				stepEntityAsString = stepEntityAsString.trim();
				// parse the string
				this.stepPercentage = Integer.valueOf(stepEntityAsString);
			} else
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
