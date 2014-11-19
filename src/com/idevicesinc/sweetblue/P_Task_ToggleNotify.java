package com.idevicesinc.sweetblue;

import java.util.UUID;

import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Result;
import com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Target;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.utils.Utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * 
 * @author dougkoellmer
 *
 */
class P_Task_ToggleNotify extends PA_Task_ReadOrWrite implements PA_Task.I_StateListener
{
	private static enum Type
	{
		NOTIFY,
		INDICATE
	}
	
	private final boolean m_enable;
	private final UUID m_descUuid;
	
	private byte[] m_writeValue = null;
	
	public P_Task_ToggleNotify(P_Characteristic characteristic, boolean enable)
	{
		this(characteristic, enable, null);
	}
	
	public P_Task_ToggleNotify(P_Characteristic characteristic, boolean enable, PE_TaskPriority priority)
	{
		super(characteristic, null, false, null, priority);
		
		m_descUuid = Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID;
		m_enable = enable;
	}
	
	private byte[] getWriteValue()
	{
		return m_writeValue != null ? m_writeValue : BleDevice.EMPTY_BYTE_ARRAY;
	}

	@Override public void execute()
	{
		BluetoothGattCharacteristic char_native = m_characteristic.getGuaranteedNative();
		
		if( char_native == null )
		{
			this.fail(Status.NO_MATCHING_TARGET, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);  return;
		}
		
		if( !getDevice().getGatt().setCharacteristicNotification(char_native, m_enable) )
		{
			this.fail(Status.FAILED_TO_REGISTER_FOR_NOTIFICATIONS, Target.CHARACTERISTIC, m_characteristic.getUuid(), Result.NON_APPLICABLE_UUID);  return;
		}
		
		BluetoothGattDescriptor descriptor = char_native.getDescriptor(m_descUuid);
		
		if( descriptor == null )
		{
			this.fail(Status.NO_MATCHING_TARGET, Target.DESCRIPTOR, m_characteristic.getUuid(), m_descUuid);  return;
		}
		
		Type type = null;
		
		if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0x0 )
		{
			type = Type.NOTIFY;
		}
		else if( (char_native.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0x0 )
		{
			type = Type.INDICATE;
		}
		else
		{
			type = Type.NOTIFY;
		}
		
		final byte[] enableValue = type == Type.NOTIFY ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
		final byte[] disableValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
		m_writeValue = m_enable ? enableValue : disableValue;
		
		if( !descriptor.setValue(getWriteValue()) )
		{
			this.fail(Status.FAILED_TO_WRITE_VALUE_TO_TARGET, Target.DESCRIPTOR, m_characteristic.getUuid(), m_descUuid);  return;
		}
		
		if( !getDevice().getGatt().writeDescriptor(descriptor) )
		{
			this.fail(Status.FAILED_TO_SEND_OUT, Target.DESCRIPTOR, m_characteristic.getUuid(), m_descUuid);  return;
		}
	}
	
	@Override protected void succeed()
	{
		Result result = newResult(Status.SUCCESS, Target.DESCRIPTOR, m_characteristic.getUuid(), m_descUuid); 
//		getDevice().addWriteTime(result.totalTime);
		m_readWriteListener.onReadOrWriteComplete(result);
		 
		super.succeed();
	}
	
	public void onDescriptorWrite(BluetoothGatt gatt, UUID descUuid, int status)
	{
		 getManager().ASSERT(gatt == getDevice().getGatt());
		 
		 if( !descUuid.equals(Uuids.CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR_UUID) )  return;

		 if( Utils.isSuccess(status) )
		 {
			 succeed();
		 }
		 else
		 {
			 fail(Status.FAILED_EVENTUALLY, Target.DESCRIPTOR, m_characteristic.getUuid(), descUuid);
		 }
	}
	
	@Override public void onStateChange(PA_Task task, PE_TaskState state)
	{
		if( state == PE_TaskState.TIMED_OUT )
		{
			m_logger.w(m_logger.charName(m_characteristic.getUuid()) + " descriptor write timed out!");
			
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.TIMED_OUT, Target.DESCRIPTOR, m_characteristic.getUuid(), m_descUuid));
			
			getManager().uhOh(UhOh.WRITE_TIMED_OUT);
		}
		else if( state == PE_TaskState.SOFTLY_CANCELLED )
		{
			Target target = this.getState() == PE_TaskState.EXECUTING ? Target.DESCRIPTOR : Target.CHARACTERISTIC;
			UUID descUuid = target == Target.DESCRIPTOR ? m_descUuid : Result.NON_APPLICABLE_UUID;
			m_readWriteListener.onReadOrWriteComplete(newResult(Status.CANCELLED, target, m_characteristic.getUuid(), descUuid));
		}
	}
	
	@Override public boolean isMoreImportantThan(PA_Task task)
	{
		return isMoreImportantThan_default(task);
	}
	
	private BleDevice.ReadWriteListener.Type getReadWriteType()
	{
		return m_enable ? BleDevice.ReadWriteListener.Type.ENABLING_NOTIFICATION : BleDevice.ReadWriteListener.Type.DISABLING_NOTIFICATION;
	}

	@Override protected Result newResult(Status status, Target target, UUID charUuid, UUID descUuid)
	{
		return new Result(getDevice(), charUuid, descUuid, getReadWriteType(), target, getWriteValue(), status, getTotalTime(), getTotalTimeExecuting());
	}
}