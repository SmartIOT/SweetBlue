package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.BleDevice.StateListener;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_NativeStateTracker extends PA_StateTracker
{
	private BleManager.NativeStateListener m_stateListener;
	private final BleManager m_mngr;
	
	P_NativeStateTracker(BleManager mngr)
	{
		super(mngr.getLogger());
		
		m_mngr = mngr;
	}
	
	public void setListener(BleManager.NativeStateListener listener)
	{
		if( listener != null )
		{
			m_stateListener = new P_WrappingBleStateListener(listener, m_mngr.m_mainThreadHandler, m_mngr.m_config.postCallbacksToMainThread);
		}
		else
		{
			m_stateListener = null;
		}
	}

	@Override protected void onStateChange(int oldStateBits, int newStateBits)
	{
		if( m_stateListener != null )
		{
			m_stateListener.onNativeBleStateChange(m_mngr, oldStateBits, newStateBits);
		}
	}
}