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
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.HueBridge;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import org.osgi.framework.BundleContext;

import com.philips.lighting.model.PHBridge;

public class HueGatewayDriverInstance implements HueBridge
{

	public HueGatewayDriverInstance(HueNetwork hueNetwork,
			DeviceFactory deviceFactory, ControllableDevice device,
			BundleContext context)
	{
		// TODO Auto-generated constructor stub
	}

	public PHBridge getBridge()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DeviceStatus getState()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void link()
	{
		// TODO Auto-generated method stub
		
	}

}
