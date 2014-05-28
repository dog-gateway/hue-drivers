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

import java.awt.Color;
import java.awt.MultipleGradientPaint.ColorSpaceType;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.Unit;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ColorDimmableLight;
import it.polito.elite.dog.drivers.hue.gateway.HueGatewayDriverInstance;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import org.osgi.framework.BundleContext;

import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLight.PHLightColorMode;
import com.philips.lighting.model.PHLightState;

/**
 * @author bonino
 * 
 */
public class HueColorDimmableLightDriverInstance extends HueDriverInstance
		implements ColorDimmableLight
{
	// store the lamp local id
	private String localId;

	// store the gateway instance
	private HueGatewayDriverInstance gateway;

	// store the step percentage, defualt 5%;
	private int stepPercentage = 5;

	public HueColorDimmableLightDriverInstance(HueNetwork hueNetwork,
			ControllableDevice device, String localId,
			HueGatewayDriverInstance gateway, int stepPercentage,
			BundleContext context)
	{
		// call the super class constructor
		super(hueNetwork, device, gateway.getBridgeIp());

		// store the hue gateway
		this.gateway = gateway;

		// store the local id of this lamp
		this.localId = localId;
		
		// initialize the state
		this.initializeStates();
	}

	private void initializeStates()
	{
		//prepare the state
		// get the bridge
		PHBridge bridge = this.gateway.getBridge();
		
		// get this lamp information
		PHLight light = bridge.getResourceCache().getLights()
				.get(this.localId);
		
		// get that last known state
		PHLightState lightState = light.getLastKnownLightState();
		
		//fill the states
		
		
	}

	@Override
	public void stepDown()
	{
		// decrease luminosity by one step

		// get the bridge object (asks it to the network driver to get the most
		// updated version) TODO: check if the received reference is kept up to
		// date
		PHBridge bridge = this.gateway.getBridge();

		// check not null
		if (bridge != null)
		{

			// get the light
			PHLight light = bridge.getResourceCache().getLights()
					.get(this.localId);

			// get the latest light state
			PHLightState lightState = light.getLastKnownLightState();

			// create a new light state
			PHLightState newLightState = new PHLightState();

			// get the brightness
			int brightness = lightState.getBrightness();

			// decrease the brightness by step percentage, if less than 0 turn
			// the
			// lamp off
			brightness = (int) (brightness - (HueInfo.MAX_BRIGHTNESS
					* (double) this.stepPercentage / 100.0));

			if (brightness <= 0)
			{
				// turn off the lamp
				newLightState.setOn(false);

				// set brightness at zer
				brightness = 0;
			}

			// set the new light state
			newLightState.setBrightness(brightness);

			// set the new brightness
			bridge.updateLightState(light, newLightState);

			// notify level change
			this.notifyChangedLevel(DecimalMeasure
					.valueOf(brightness, Unit.ONE));

			// update the status
			this.updateStatus();
		}
	}

	@Override
	public void stepUp()
	{
		// increase luminosity by one step

		// get the bridge object (asks it to the network driver to get the most
		// updated version) TODO: check if the received reference is kept up to
		// date
		PHBridge bridge = this.gateway.getBridge();

		// check not null
		if (bridge != null)
		{

			// get the light
			PHLight light = bridge.getResourceCache().getLights()
					.get(this.localId);

			// get the latest light state
			PHLightState lightState = light.getLastKnownLightState();

			// create a new light state
			PHLightState newLightState = new PHLightState();

			// get the brightness
			int brightness = lightState.getBrightness();

			// decrease the brightness by step percentage, if less than 0 turn
			// the
			// lamp off
			brightness = (int) (brightness + (HueInfo.MAX_BRIGHTNESS
					* (double) this.stepPercentage / 100.0));

			if (!lightState.isOn())
				// turn off the lamp
				newLightState.setOn(true);

			if (brightness >= 0)
			{
				if (brightness >= HueInfo.MAX_BRIGHTNESS)
					// set brightness at 255
					brightness = HueInfo.MAX_BRIGHTNESS;
			}

			// set the new light state
			newLightState.setBrightness(brightness);

			// set the new brightness
			bridge.updateLightState(light, newLightState);

			// notify level change
			this.notifyChangedLevel(DecimalMeasure
					.valueOf(brightness, Unit.ONE));

			this.updateStatus();
		}
	}

	@Override
	public DeviceStatus getState()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeScene(Integer sceneNumber)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteScene(Integer sceneNumber)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void set(Object value)
	{
		// TODO Auto-generated method stub

	}

	public void setColor(int hue, int saturation)
	{
		// get the bridge object (asks it to the network driver to get the most
		// updated version) TODO: check if the received reference is kept up to
		// date
		PHBridge bridge = this.gateway.getBridge();

		// check not null
		if (bridge != null)
		{

			// get the light
			PHLight light = bridge.getResourceCache().getLights()
					.get(this.localId);
			
			//prepare the new lamp state
			PHLightState newLightState = new PHLightState();
			
			//update hue and saturation
			newLightState.setHue(hue);
			newLightState.setSaturation(saturation);
			
			//update the real device
			bridge.updateLightState(light, newLightState);
			
			//notify...
			
			//update the status
			this.updateStatus();
		}
	}

	@Override
	public void on()
	{
		// turns the lamp on

		// get the bridge object (asks it to the network driver to get the most
		// updated version) TODO: check if the received reference is kept up to
		// date
		PHBridge bridge = this.gateway.getBridge();

		// check not null
		if (bridge != null)
		{
			// not already on
			// get the light
			PHLight light = bridge.getResourceCache().getLights()
					.get(this.localId);

			// get the latest light state
			PHLightState lightState = light.getLastKnownLightState();

			// check current state
			if (!lightState.isOn())
			{
				// create a new state object
				PHLightState newLightState = new PHLightState();

				// set the state at on
				newLightState.setOn(true);

				// updated the state on the real device
				bridge.updateLightState(light, newLightState);

				// notify
				this.notifyOn();

				// update the status
				this.updateStatus();
			}
		}

	}

	@Override
	public void deleteGroup(Integer groupID)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void storeGroup(Integer groupID)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void off()
	{
		// turns the lamp off

		// get the bridge object (asks it to the network driver to get the most
		// updated version) TODO: check if the received reference is kept up to
		// date
		PHBridge bridge = this.gateway.getBridge();

		// check not null
		if (bridge != null)
		{
			// not already on
			// get the light
			PHLight light = bridge.getResourceCache().getLights()
					.get(this.localId);

			// get the latest light state
			PHLightState lightState = light.getLastKnownLightState();

			// check current state
			if (lightState.isOn())
			{
				// create a new state object
				PHLightState newLightState = new PHLightState();

				// set the state at on
				newLightState.setOn(false);

				// updated the state on the real device
				bridge.updateLightState(light, newLightState);

				// notify
				this.notifyOff();

				// update the status
				this.updateStatus();
			}
		}

	}

	@Override
	public void notifyStoredScene(Integer sceneNumber)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyDeletedScene(Integer sceneNumber)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyChangedColor(String colorRGB)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyJoinedGroup(Integer groupNumber)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOn()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyChangedLevel(Measure<?, ?> newLevel)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyOff()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyLeftGroup(Integer groupNumber)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updateStatus()
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected void specificConfiguration()
	{
		// TODO Auto-generated method stub

	}

}
