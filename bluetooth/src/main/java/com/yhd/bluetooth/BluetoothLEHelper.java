package com.yhd.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLEHelper {
    public static final String TAG = BluetoothLEHelper.class.getSimpleName();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static BluetoothLEHelper instance;
    public ArrayList<BluetoothGatt> mBluetoothGattList;
    private BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLowEnergyInterface bluetoothLowEnergyInterface;
    /**
     * 蓝牙状态数据回调类
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.v(TAG, "onConnectionStateChange:status:" + status + ",newState:" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onState(State.STATE_CONNECTED, gatt);
                }
                gatt.discoverServices();
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onState(State.STATE_SERVER_DISCOVERING, gatt);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onState(State.STATE_DISCONNECTED, gatt);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.v(TAG, "onServicesDiscovered:" + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onState(State.STATE_SERVER_DISCOVERED, gatt);
                }
            } else {
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onState(State.STATE_SERVER_DISCOVER_FAILED, gatt);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.v(TAG, "onDescriptorWriteonDescriptorWrite = " + status + ", descriptor =" + descriptor.getUuid().toString());
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_DESCRIPTOR_WRITE, gatt, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.v(TAG, "onCharacteristicChanged - ");
            if (characteristic.getValue() != null) {
                Log.v(TAG, "characteristic.getStringValue - " + characteristic.getStringValue(0));
            }
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onReceiveData(characteristic.getValue(), gatt, characteristic);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.v(TAG, "rssi = " + rssi);
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_READ_REMOTE_RSSI, gatt, rssi, status);
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "onCharacteristicWrite:write success-status:" + status);
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_CHARATERISTIC_WRITE, gatt, status);
            }
        }

        ;

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.v(TAG, "onMtuChanged");
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_MTU_CHANGE, gatt, mtu, status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.v(TAG, "onReliableWriteCompleted");
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_RELIABLE_WRITE_COMPLETE, gatt, status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.v(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }
                }
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onReceiveData(data, gatt, characteristic);
                }
            }
        }
    };
    private BluetoothGatt bluetoothGatt;
    private String mBluetoothDeviceAddress;
    private ArrayList<BluetoothDevice> mLeDevices;
    /**
     * 蓝牙扫描结果回调
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);

                Log.v(TAG, "LeScanCallback:" + device.getName());
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onReceiveDevice(device);
                }
            }
        }
    };

    /**
     * 构造函数
     */
    private BluetoothLEHelper(Context context) {
        this.mContext = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        mLeDevices = new ArrayList<BluetoothDevice>();
        mBluetoothGattList = new ArrayList<BluetoothGatt>();
    }

    public static synchronized BluetoothLEHelper getInstance(Context context) {
        if (instance == null) {
            Log.v(TAG, "Create BluetoothLEHelper instance");
            instance = new BluetoothLEHelper(context);
        }
        return instance;
    }

    /**
     * 设置扫描回调接口
     *
     * @param receiverInterface
     */
    public BluetoothLEHelper setBlutoothReceiverInterface(BluetoothLowEnergyInterface receiverInterface) {
        Log.v(TAG, "setBlutoothReceiverInterface");
        this.bluetoothLowEnergyInterface = receiverInterface;
        return instance;
    }

    /**
     * 是否支持蓝牙低功耗
     *
     * @return
     */
    public boolean isSurport() {
        boolean result = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        Log.v(TAG, "isSurport:" + String.valueOf(result));
        return result;
    }

    /**
     * 蓝牙是否打开，如若没有打开可以打开系统设置的蓝牙操作界面
     * Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
     * startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
     *
     * @return
     */
    public boolean isBluetoothEnable() {
        boolean result = mBluetoothAdapter.isEnabled();
        Log.v(TAG, "isBluetoothEnable:" + String.valueOf(result));
        return result;
    }

    /**
     * 设置蓝牙状态
     *
     * @return
     */
    public BluetoothLEHelper setEnableBluetooth(boolean enable) {
        boolean result;
        if (enable) {
            result = mBluetoothAdapter.enable();
        } else {
            result = mBluetoothAdapter.disable();
        }
        Log.v(TAG, "setEnableBluetooth:" + enable);
        return instance;
    }

    /**
     * 获取Gatt对象
     *
     * @return GATT对象
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    public String getBluetoothName() {
        return mBluetoothAdapter.getName();
    }

    /**
     * 设置蓝牙名字
     *
     * @return
     */
    public boolean setBluetoothName(String name) {
        Log.v(TAG, "setBluetoothName：" + name);
        return mBluetoothAdapter.setName(name);
    }

    /**
     * 设置蓝牙可被发现时间，0表示永远被发现，-1表示不被发现，其他时间可设置，最长3600秒
     * setScanMode属于私有方法，利用Android反射机制直接公开使用
     *
     * @param time 时间
     * @return 单例
     */
    public BluetoothLEHelper setDiscoverableTime(int time) {
        Log.v(TAG, "setDiscoverableTime:" + time);
        if (time == -1) {
            try {
                Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
                setScanMode.setAccessible(true);
                setScanMode.invoke(mBluetoothAdapter, BluetoothAdapter.SCAN_MODE_NONE, time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
                setDiscoverableTimeout.setAccessible(true);
                Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
                setScanMode.setAccessible(true);
                setDiscoverableTimeout.invoke(mBluetoothAdapter, time);
                setScanMode.invoke(mBluetoothAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    /**
     * 扫描设备
     *
     * @param enable
     */
    public void scanLeDevice(final boolean enable) {
        Log.v(TAG, "scanLeDevice:" + enable);

        clear();
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if (bluetoothLowEnergyInterface != null) {
                        bluetoothLowEnergyInterface.onState(State.STATE_SCAN_END, bluetoothGatt);
                    }
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_SCAN_BEGIN, bluetoothGatt);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_SCAN_END, bluetoothGatt);
            }
        }
    }

    /**
     * 设置扫描超时
     *
     * @param time 时间
     */
    public void setScanPeriod(long time) {
        Log.v(TAG, "setScanPeriod：" + String.valueOf(time));
        SCAN_PERIOD = time;
    }

    /**
     * 连接蓝牙
     *
     * @param address       对方设备地址
     * @param isAutoConnect 设置是否以后自动连接
     */
    public boolean connectGatt(String address, boolean isAutoConnect) {
        Log.v(TAG, "connectGatt");
        boolean ifContained = false;
        /** 如果已经存在连接池里不再连接 */
        for (int i = 0; i < mBluetoothGattList.size(); i++) {
            BluetoothGatt bluetoothGatt = mBluetoothGattList.get(i);
            String sAddress = bluetoothGatt.getDevice().getAddress();
            if (address.equals(sAddress)) {
                ifContained = true;
                break;
            }
        }
        if (ifContained) {
            return false;
        }
        if (bluetoothLowEnergyInterface != null) {
            bluetoothLowEnergyInterface.onState(State.STATE_CONNECTING, bluetoothGatt);
        }
        if (mBluetoothAdapter == null || address == null) {
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_CONNECT_FAILED, bluetoothGatt);
            }
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && bluetoothGatt != null) {
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_CONNECTING_FORMAL, bluetoothGatt);
            }
            if (bluetoothGatt.connect()) {
                return true;
            } else {
                if (bluetoothLowEnergyInterface != null) {
                    bluetoothLowEnergyInterface.onState(State.STATE_CONNECT_FAILED, bluetoothGatt);
                }
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            if (bluetoothLowEnergyInterface != null) {
                bluetoothLowEnergyInterface.onState(State.STATE_DEVICE_NO_EXIST, bluetoothGatt);
            }
            return false;
        }
        Log.v(TAG, "connectGatt run");
        bluetoothGatt = device.connectGatt(mContext, isAutoConnect, mGattCallback);

        addBluetoothDeviceAddress(bluetoothGatt);
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * 添加链接
     *
     * @param bluetoothGatt
     */
    private void addBluetoothDeviceAddress(BluetoothGatt bluetoothGatt) {
        if (mBluetoothGattList == null) {
            return;
        }
        if (bluetoothGatt != null) {
            mBluetoothGattList.add(bluetoothGatt);
        }
    }

    /**
     * 关闭所有链接
     */
    private void closeBluetoothDeviceAddress() {
        if (mBluetoothGattList == null) {
            return;
        }
        for (int i = 0; i < mBluetoothGattList.size(); i++) {
            BluetoothGatt mBluetoothGatt = mBluetoothGattList.get(i);
            mBluetoothGatt.close();
            mBluetoothGattList.remove(mBluetoothGatt);
        }
    }

    /**
     * 关闭所有链接
     */
    private void disconnectBluetoothDeviceAddress() {
        if (mBluetoothGattList == null) {
            return;
        }
        for (int i = 0; i < mBluetoothGattList.size(); i++) {
            BluetoothGatt mBluetoothGatt = mBluetoothGattList.get(i);
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGattList.remove(i);
        }
    }

    /**
     * 关闭指定连接
     */
    public void closeWithGatt(BluetoothGatt gatt) {
        for (int i = 0; i < mBluetoothGattList.size(); i++) {
            BluetoothGatt mBluetoothGatt = mBluetoothGattList.get(i);
            if (gatt.getDevice().getAddress().equals(mBluetoothGatt.getDevice().getAddress()) &&
                    gatt.getDevice().getName().equals(mBluetoothGatt.getDevice().getName())) {
                mBluetoothGatt.close();
                mBluetoothGattList.remove(mBluetoothGatt);
            }
        }
    }

    /**
     * 关闭所有连接
     */
    public void disconnectWithGatt(BluetoothGatt gatt) {
        for (BluetoothGatt mBluetoothGatt : mBluetoothGattList) {
            if (gatt.getDevice().getAddress().equals(mBluetoothGatt.getDevice().getAddress()) &&
                    gatt.getDevice().getName().equals(mBluetoothGatt.getDevice().getName())) {
                mBluetoothGatt.disconnect();
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        Log.v(TAG, "disconnect");
        /*if (mBluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }*/
        this.mBluetoothDeviceAddress = null;
        disconnectBluetoothDeviceAddress();
    }

    public void clear() {
        if (mLeDevices != null) {
            mLeDevices.clear();
        }
    }

    /**
     * 设置指定BluetoothGattCharacteristic是否接收远程数据传送通知
     * 如果设置接收推送，一旦对方BluetoothGattCharacteristic发生改变将会在回调：onCharacteristicChanged()中收到信息
     *
     * @param characteristic 指定BluetoothGattCharacteristic
     * @param enabled        是否接收通知
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        boolean success = bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (!success) {
            Log.v(TAG, "Seting proper notification status for characteristic failed!");
        }
        // This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
        // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * 读取指定的BluetoothGattCharacteristic
     *
     * @param characteristic
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 写入字符串
     *
     * @param serviceUUID        发送对象服务的UUID
     * @param characteristicUUIS 发送特征服务的UUID
     * @param value              要发送的字符串
     */
    public boolean wirteData(String serviceUUID, String characteristicUUIS, String value) {
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            return false;
        }
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(serviceUUID));
        BluetoothGattCharacteristic characteristic;
        if (service != null) {
            characteristic = service.getCharacteristic(UUID.fromString(characteristicUUIS));
            if (characteristic != null) {
                return wirteCharacteristic(characteristic, value);
            }
        }
        return false;
    }

    /**
     * 向指定的BluetoothGattCharacteristic写入字节流
     *
     * @param characteristic 发送对象
     * @param value          要发送的字节流
     */
    public boolean wirteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            return false;
        }
        characteristic.setValue(value);
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * 向指定的BluetoothGattCharacteristic写入字符串
     *
     * @param characteristic 发送对象
     * @param value          要发送的字符串
     */
    public boolean wirteCharacteristic(BluetoothGattCharacteristic characteristic, String value) {
        if (mBluetoothAdapter == null || bluetoothGatt == null) {
            return false;
        }
        characteristic.setValue(value);
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * 获取服务列表
     *
     * @return
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null)
            return null;
        return bluetoothGatt.getServices();
    }

    /**
     * 读取设备信号
     *
     * @return
     */
    public boolean getRssiVal() {
        if (bluetoothGatt == null)
            return false;

        return bluetoothGatt.readRemoteRssi();
    }

    /**
     * 关闭蓝牙
     */
    public void close(BluetoothGatt gatt) {
        if (gatt == null) {
            return;
        }
        closeWithGatt(gatt);
    }

    public void close() {
        closeAllGatt();
    }

    /**
     * 关闭蓝牙
     */
    public void closeAllGatt() {
        if (bluetoothGatt == null) {
            return;
        }
        // bluetoothGatt.close();
        closeBluetoothDeviceAddress();
        bluetoothGatt = null;
    }

    /**
     * 状态枚举
     */
    public enum State {
        STATE_DISCONNECTED("断开连接"),
        STATE_CONNECTING("正在连接中"),
        STATE_CONNECTED("连接成功"),
        STATE_CONNECTING_FORMAL("尝试连接上一次的设备"),
        STATE_CONNECT_FAILED("连接失败"),
        STATE_DEVICE_NO_EXIST("设备不存在"),
        STATE_SERVER_DISCOVERING("服务扫描中"),
        STATE_SERVER_DISCOVER_FAILED("服务扫描失败"),
        STATE_SERVER_DISCOVERED("服务扫描成功"),
        STATE_SCAN_BEGIN("开始扫描"),
        STATE_SCAN_END("扫描结束"),
        STATE_DESCRIPTOR_WRITE("onDescriptorWrite"),
        STATE_READ_REMOTE_RSSI("onReadRemoteRssi"),
        STATE_CHARATERISTIC_WRITE("onCharacteristicWrite"),
        STATE_MTU_CHANGE("onMtuChanged"),
        STATE_RELIABLE_WRITE_COMPLETE("onReliableWriteCompleted");

        private final String state;

        State(String var) {
            this.state = var;
        }

        public String toString() {
            return this.state;
        }
    }

    /**
     * 扫描设备回调
     */
    public interface BluetoothLowEnergyInterface {
        void onReceiveDevice(BluetoothDevice device);

        void onReceiveData(byte[] data, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        void onState(State state, BluetoothGatt gatt, Object... args);
    }
}
