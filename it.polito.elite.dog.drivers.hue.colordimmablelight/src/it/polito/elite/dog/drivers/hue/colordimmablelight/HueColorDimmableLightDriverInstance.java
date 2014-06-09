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
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ColorDimmableLight;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.state.ColorStateHSB;
import it.polito.elite.dog.core.library.model.state.LevelState;
import it.polito.elite.dog.core.library.model.state.OnOffState;
import it.polito.elite.dog.core.library.model.statevalue.BrightnessStateValue;
import it.polito.elite.dog.core.library.model.statevalue.HueStateValue;
import it.polito.elite.dog.core.library.model.statevalue.LevelStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OffStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OnStateValue;
import it.polito.elite.dog.core.library.model.statevalue.SaturationStateValue;
import it.polito.elite.dog.core.library.model.statevalue.StateValue;
import it.polito.elite.dog.drivers.hue.gateway.HueGatewayDriverInstance;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.info.HueInfo;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import java.util.HashSet;
import java.util.Set;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit;

import org.osgi.framework.BundleContext;

import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * A driver instance for handling Philips Hue colored lights.
 * 
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

	// store the step percentage, default 5%;
	private int stepPercentage = 5;

	// the set of scenes to which the device belongs
	private Set<Integer> scenes;

	// the set of groups to which the device belongs
	private Set<Integer> groups;

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

		// initialize the scenes and groups sets
		this.scenes = new HashSet<Integer>();
		this.groups = new HashSet<Integer>();

		// initialize the state
		this.initializeStates();
	}

	private void initializeStates()
	{
		// prepare the device state map
		this.currentState = new DeviceStatus(this.device.getDeviceId());

		// prepare the device state
		// on/off
		OnOffState onoff = new OnOffState(new OffStateValue());

		// level state
		LevelState brightnessLevel = new LevelState(new LevelStateValue());

		// HSB state
		ColorStateHSB colorState = new ColorStateHSB(new HueStateValue(),
				new SaturationStateValue(), new BrightnessStateValue());

		// add the on/off state
		this.currentState.setState(OnOffState.class.getSimpleName(), onoff);

		// add the brightnes state
		this.currentState.setState(LevelState.class.getSimpleName(),
				brightnessLevel);

		// add the HSB state
		this.currentState.setState(ColorStateHSB.class.getSimpleName(),
				colorState);
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
		return this.currentState;
	}

	@Override
	public void storeScene(Integer sceneNumber)
	{
		// add the scene
		this.scenes.add(sceneNumber);
	}

	@Override
	public void deleteScene(Integer sceneNumber)
	{
		// remove the scene
		this.scenes.remove(sceneNumber);
	}

	@Override
	public void set(Object value)
	{
		// TODO Auto-generated method stub

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
			}
		}

	}

	@Override
	public void deleteGroup(Integer groupID)
	{
		// delete the group
		this.groups.remove(groupID);
	}

	@Override
	public void storeGroup(Integer groupID)
	{
		// add the group
		this.groups.add(groupID);
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
			}
		}

	}

	@Override
	public void notifyStoredScene(Integer sceneNumber)
	{
		((ColorDimmableLight) this.device).notifyStoredScene(sceneNumber);
	}

	@Override
	public void notifyDeletedScene(Integer sceneNumber)
	{
		((ColorDimmableLight) this.device).notifyDeletedScene(sceneNumber);
	}

	@Override
	public void notifyOn()
	{
		((ColorDimmableLight) this.device).notifyOn();
	}

	@Override
	public void notifyChangedLevel(Measure<?, ?> newLevel)
	{
		((ColorDimmableLight) this.device).notifyChangedLevel(newLevel);
	}

	@Override
	public void notifyOff()
	{
		((ColorDimmableLight) this.device).notifyOff();
	}

	@Override
	public void notifyLeftGroup(Integer groupNumber)
	{
		((ColorDimmableLight) this.device).notifyLeftGroup(groupNumber);
	}

	@Override
	public void updateStatus()
	{
		((Controllable) this.device).updateStatus();
	}

	@Override
	protected void specificConfiguration()
	{
		// empty
	}

	@Override
	public void newMessageFromHouse(PHLightState lastKnownLightState)
	{
		// handle state changes

		// on/off
		if (lastKnownLightState.isOn())
		{
			// notify
			this.notifyOn();

			// update the on/off state
			OnOffState onoff = new OnOffState(new OnStateValue());
			this.currentState.setState(OnOffState.class.getSimpleName(), onoff);

		} else
		{
			// notify
			this.notifyOff();

			// update the on/off state
			OnOffState onoff = new OnOffState(new OffStateValue());
			this.currentState.setState(OnOffState.class.getSimpleName(), onoff);
		}

		// handle HSB state
		StateValue stateValues[] = this.currentState.getState(
				ColorStateHSB.class.getSimpleName()).getCurrentStateValue();

		for (StateValue value : stateValues)
		{
			if (value.getName().equals(
					BrightnessStateValue.class.getSimpleName()))
			{
				value.setValue(lastKnownLightState.getBrightness());
			} else if (value.getName().equals(
					SaturationStateValue.class.getSimpleName()))
			{
				value.setValue(lastKnownLightState.getSaturation());
			} else if (value.getName().equals(
					HueStateValue.class.getSimpleName()))
			{
				value.setValue(lastKnownLightState.getHue());
			}
		}

		// handle HSB notification
		this.notifyChangedColorHSB(lastKnownLightState.getSaturation(),
				lastKnownLightState.getBrightness(),
				lastKnownLightState.getHue());

		// handle level state
		Measure<Integer, Dimensionless> level = DecimalMeasure.valueOf(
				lastKnownLightState.getBrightness(), Unit.ONE);

		this.currentState.getState(LevelState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(level);

		// handle level notification
		this.notifyChangedLevel(level);

		// update the status
		this.updateStatus();

	}

	@Override
	public void setColorRGB(Integer red, Integer blue, Integer green)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setColorHSB(Integer saturation, Integer brightness, Integer hue)
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

			// prepare the new lamp state
			PHLightState newLightState = new PHLightState();

			// update hue and saturation
			newLightState.setHue(hue);
			newLightState.setSaturation(saturation);
			newLightState.setBrightness(brightness);

			// update the real device
			bridge.updateLightState(light, newLightState);

			// notify...

			// update the status
			this.updateStatus();

		}
	}

	@Override
	public void notifyChangedColorRGB(Integer red, Integer blue, Integer green)
	{
		notifyChangedColorRGB(red, blue, green);
	}

	@Override
	public void notifyChangedColorHSB(Integer saturation, Integer brightness,
			Integer hue)
	{
		((ColorDimmableLight) this.device).notifyChangedColorHSB(saturation,
				brightness, hue);
	}

	@Override
	public void notifyJoinedGroup(Integer groupNumber)
	{
		((ColorDimmableLight) this.device).notifyJoinedGroup(groupNumber);
	}
}
