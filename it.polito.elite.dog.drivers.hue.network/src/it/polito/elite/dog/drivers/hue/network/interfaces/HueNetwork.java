/*
 * Dog 2.0 - Hue Network Driver
 * 
 * 
 * Copyright 2013 Dario Bonino 
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
package it.polito.elite.dog.drivers.hue.network.interfaces;

import com.philips.lighting.model.PHBridge;

/**
 * @author bonino
 * 
 */
public interface HueNetwork
{

	/**
	 * Given the IP address of a HUE bridge, provides the PHBridge instance used
	 * for handling the connected set of HUE ligths
	 */
	public PHBridge getBridge(String bridgeIp);

	/**
	 * Add a HUE bridge discovery listener to the set of objects which are
	 * notified of the detection of a new HUE bridge. Although there should be
	 * only one listener, implemented by the gateway driver instance, typically,
	 * multiple listeners can be registered.
	 * 
	 * @param listener
	 *            The HueBridgeDiscoveryListener instance to add.
	 */
	public void addHueBridgeDiscoveryListener(
			HueBridgeDiscoveryListener listener);

	/**
	 * Removes a HUE bridge discovery listener from the set of objects which are
	 * notified of the detection of new HUE bridges
	 * 
	 * @param listener
	 */
	public void removeHueBridgeDiscoveryListener(
			HueBridgeDiscoveryListener listener);

	/**
	 * Asks the network driver to set up a connection to the HUE bridge having
	 * the given IP address, optionally provides a connection listener for
	 * performing tasks upon successful connection.
	 * 
	 * @param bridgeIp
	 * @param listener
	 */
	public void connectToBridge(String bridgeIp, HueConnectionListener listener);
}
