/**
 * 
 */
package it.polito.elite.dog.drivers.hue.colordimmablelight;

import javax.measure.Measure;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ColorDimmableLight;
import it.polito.elite.dog.drivers.hue.network.HueDriverInstance;
import it.polito.elite.dog.drivers.hue.network.interfaces.HueNetwork;

import org.osgi.framework.BundleContext;

import com.philips.lighting.model.PHBridge;

/**
 * @author bonino
 *
 */
public class HueColorDimmableLightDriverInstance extends HueDriverInstance implements ColorDimmableLight
{

	public HueColorDimmableLightDriverInstance(HueNetwork hueNetwork,
			ControllableDevice device, int localId, PHBridge bridge,
			int stepPercentage, BundleContext context)
	{
		super(hueNetwork, device, bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
	}

	@Override
	public void stepDown()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stepUp()
	{
		// TODO Auto-generated method stub
		
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

	@Override
	public void on()
	{
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
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
