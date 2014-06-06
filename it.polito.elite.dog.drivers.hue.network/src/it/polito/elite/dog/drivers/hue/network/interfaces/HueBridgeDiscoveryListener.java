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
package it.polito.elite.dog.drivers.hue.network.interfaces;

import java.util.List;

import com.philips.lighting.hue.sdk.PHAccessPoint;

/**
 * @author bonino
 * 
 */
public interface HueBridgeDiscoveryListener
{
	/**
	 * Called when one or more hue bridges have been found
	 * 
	 * @param the
	 *            {@link List}<{@link PHAccessPoint}> of found bridges.
	 */
	public void onAccessPointsFound(List<PHAccessPoint> arg0);
}
