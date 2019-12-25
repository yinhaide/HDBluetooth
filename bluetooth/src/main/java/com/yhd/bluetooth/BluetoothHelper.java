/**
 * 一般的蓝牙操作类(http://developer.android.com/intl/zh-cn/guide/topics/connectivity/bluetooth.html#ConnectingDevices)
 * 用过“isSecurConnect”来配置双方连接时是否需要配对
 * 通过“isServer”来配置是否可进行数据通信
 * 通过UUID来创建连接socket，其中“00001101-0000-1000-8000-00805F9B34FB”是普遍使用的SPP_UUID
 */
package com.yhd.bluetooth;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothHelper {
    public static final String TAG = BluetoothHelper.class.getSimpleName();
    /** 代表普遍使用SPP协议的UUID */
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothHelper instance;
    private static BluetoothReceiverInterface bluetoothReceiverInterface;
    private final int CONNECT_TIME = 3300;
    private final int SLEEP_TIME = 100;
    IntentFilter actionPairingRequestFilter;
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private String S_NAME = "xjkb2";
    private UUID S_UUID = SPP_UUID;
    private Context mContext;
    /** 时候具备服务的功能 */
    private boolean isServer = false;
    /** 链接是是否需要配对 */
    private boolean isAutoPare = false;
    /**
     * 创建一个广播接收器来接收蓝牙扫描结果
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @TargetApi(Build.VERSION_CODES.KITKAT)
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                if (isAutoPare) {
                    device.setPairingConfirmation(true);//自动配对
                    if (bluetoothReceiverInterface != null) {
                        bluetoothReceiverInterface.onState(State.STATE_PAIRING, device);
                    }
                }
            }
            if (bluetoothReceiverInterface == null) {
                return;
            }
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    bluetoothReceiverInterface.onState(State.STATE_SCAN_BEGIN);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    bluetoothReceiverInterface.onState(State.STATE_SCAN_END);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    bluetoothReceiverInterface.onReceiveDevice(device);
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                    switch (mode) {
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            bluetoothReceiverInterface.onState(State.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            bluetoothReceiverInterface.onState(State.SCAN_MODE_NONE);
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            bluetoothReceiverInterface.onState(State.SCAN_MODE_CONNECTABLE);
                            break;
                        default:
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            bluetoothReceiverInterface.onState(State.STATE_OFF, device);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            bluetoothReceiverInterface.onState(State.STATE_TURNING_OFF, device);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            if (isServer) {
                                startServer();
                            }
                            bluetoothReceiverInterface.onState(State.STATE_ON, device);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            bluetoothReceiverInterface.onState(State.STATE_TURNING_ON, device);
                            break;
                        default:
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    switch (bondState) {
                        case BluetoothDevice.BOND_NONE:
                            bluetoothReceiverInterface.onState(State.BOND_NONE, device);
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            bluetoothReceiverInterface.onState(State.BOND_BONDING, device);
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            bluetoothReceiverInterface.onState(State.BOND_BONDED, device);
                            break;
                        default:
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    bluetoothReceiverInterface.onState(State.STATE_CONNECTED, device);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    bluetoothReceiverInterface.onState(State.STATE_DISCONNECTED, device);
                    break;
                default:
                    break;
            }
        }
    };
    /** 创建socket时是否需要配对 */
    private boolean isSecurConnect = true;
    /** 是否注册了广播 */
    private boolean isRegisterReceiver = false;

    /**
     * 构造函数
     */
    private BluetoothHelper(Context context) {
        this.mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        actionPairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);//请求配对
    }

    /**
     * 创建单例
     * @param context 上下文
     * @return 单例
     */
    public static synchronized BluetoothHelper getInstance(Context context) {
        if (instance == null) {
            Log.v(TAG, "create Bluetooth instance");
            instance = new BluetoothHelper(context);
        }
        return instance;
    }

    /**
     * 注册蓝牙广播接收器
     */
    public BluetoothHelper registerBlueToothReceiver() {
        Log.v(TAG, "registerBlueToothReceiver");
        if (!isRegisterReceiver) {
            IntentFilter actionNoFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);//扫描之后找到设备
            IntentFilter actionAclConnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);//设备连接成功
            IntentFilter actionStateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);//连接状态改变
            IntentFilter actionDiscoveryStarted = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);//开始扫描设备
            IntentFilter actionDiscoveryFinishFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描结束
            IntentFilter actionRequestEnableFilter = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE);//打开蓝牙
            IntentFilter actionRequestDiscoverableFilter = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);//蓝牙可被发现
            IntentFilter actionScanModeChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//蓝牙扫描模式发生变化
            mContext.registerReceiver(mReceiver, actionNoFoundFilter);
            mContext.registerReceiver(mReceiver, actionAclConnectedFilter);
            mContext.registerReceiver(mReceiver, actionStateChangeFilter);
            mContext.registerReceiver(mReceiver, actionDiscoveryStarted);
            mContext.registerReceiver(mReceiver, actionDiscoveryFinishFilter);
            mContext.registerReceiver(mReceiver, actionRequestEnableFilter);
            mContext.registerReceiver(mReceiver, actionRequestDiscoverableFilter);
            mContext.registerReceiver(mReceiver, actionScanModeChangeFilter);
            mContext.registerReceiver(mReceiver, actionPairingRequestFilter);
            isRegisterReceiver = true;
        }
        return this;
    }

    public BluetoothHelper setBluetoothReceiverInterface(BluetoothReceiverInterface bluetoothReceiverInterface) {
        Log.v(TAG, "setBluetoothReceiverInterface");
        if (bluetoothReceiverInterface == null) {
            Log.v(TAG, "bluetoothReceiverInterface is null");
        } else {
            Log.v(TAG, "bluetoothReceiverInterface is not null");
        }
        this.bluetoothReceiverInterface = bluetoothReceiverInterface;
        return this;
    }

    /**
     * 取消注册蓝牙广播接收器
     */
    public void unRegisterBlueToothReceiver() {
        Log.v(TAG, "unRegisterBlueToothReceiver:");
        // this.bluetoothReceiverInterface=null;
        if (isRegisterReceiver) {
            mContext.unregisterReceiver(mReceiver);
            isRegisterReceiver = false;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
        }
        if (connectThread != null) {
            connectThread.cancel();
        }
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    /**
     * 是否支持蓝牙设备
     * @return 单例
     */
    public boolean isSurport() {
        boolean result = mBluetoothAdapter == null ? false : true;
        Log.v(TAG, "isSurport:" + String.valueOf(result));
        return result;
    }

    /**
     * 蓝牙是否打开，如若没有打开可以打开系统设置的蓝牙操作界面
     * @return 单例
     */
    public boolean isBluetoothEnable() {
        boolean result = mBluetoothAdapter.isEnabled();
        Log.v(TAG, "isBluetoothEnable:" + String.valueOf(result));
        return result;
    }

    /**
     * 设置蓝牙状态（异步）
     * @return 单例
     */
    public BluetoothHelper setEnableBluetooth(boolean enable) {
        Log.v(TAG, "setEnableBluetooth:" + enable);
        if (enable) {
            mBluetoothAdapter.enable();
        } else {
            mBluetoothAdapter.disable();
        }
        return this;
    }

    /**
     * 打开蓝牙（同步方法）
     * @return 单例
     */
    public BluetoothHelper openBluetooth() {
        Log.v(TAG, "openBluetooth");
        mBluetoothAdapter.enable();
        while (!mBluetoothAdapter.isEnabled()) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    /**
     * 关闭蓝牙（同步）
     */
    public void closeBluetooth() {
        Log.v(TAG, "closeBluetooth");
        mBluetoothAdapter.disable();
        while (mBluetoothAdapter.isEnabled()) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 搜索蓝牙设备
     * @param discoverable
     * @return 单例
     */
    public BluetoothHelper setDiscovery(boolean discoverable) {
        Log.v(TAG, "setDiscovery:" + discoverable);
        if (!mBluetoothAdapter.isEnabled()) {
            if (bluetoothReceiverInterface != null) {
                bluetoothReceiverInterface.onState(State.ERROR_BLUETOOTH_DISABLE);
            }
        } else {
            if (discoverable) {
                mBluetoothAdapter.startDiscovery();
            } else {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
        return this;
    }

    /**
     * 设置蓝牙可被发现时间，0表示永远被发现，-1表示不被发现，其他时间可设置，最长3600秒
     * setScanMode属于私有方法，利用Android反射机制直接公开使用
     * @param time 时间
     * @return 单例
     */
    public BluetoothHelper setDiscoverableTime(int time) {
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
        return this;
    }

    /**
     * 设置是否自动配对
     * @param isAutoPare 是否
     * @return 类对象
     */
    public BluetoothHelper setIsAutoPare(boolean isAutoPare) {
        Log.v(TAG, "setIsAutoPare:" + isAutoPare);
        this.isAutoPare = isAutoPare;
        //actionPairingRequestFilter.setPriority(1000);//设置请求配对广播优先级最大
        //mReceiver.abortBroadcast();//如果是有序广播,阻止广播继续向下传递，优先级较低的将不会接收到该广播，同优先级胡广播随即接收，可以设置Filter胡优先级
        return this;
    }

    /**
     * 双方连接时是否需要配对连接
     * @param isSecurConnect 是否
     * @return
     */
    public BluetoothHelper setIsSecurConnect(boolean isSecurConnect) {
        Log.v(TAG, "setIsSecurConnect:" + isSecurConnect);
        this.isSecurConnect = isSecurConnect;
        return this;
    }

    /**
     * 设置连接UUID
     * @param uuidString uuid
     * @return 单例
     */
    public BluetoothHelper setServerUUID(String uuidString) {
        Log.v(TAG, "setServerUUID:" + uuidString);
        this.S_UUID = UUID.fromString(uuidString);
        return this;
    }

    /**
     * 设置服务名
     * @param name 名字
     * @return 单例
     */
    public BluetoothHelper setServerName(String name) {
        Log.v(TAG, "setServerName:" + name);
        this.S_NAME = name;
        return this;
    }

    /**
     * 查询配对设备
     * @return 配对设备列表
     */
    public List<BluetoothDevice> getBondedDevices() {
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        List<BluetoothDevice> list = new ArrayList<>();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {
                list.add(device);
            }
        }
        Log.v(TAG, "BondedDevices:" + String.valueOf(list.size()));
        return list;
    }

    /**
     * 获得蓝牙名字
     * @return 蓝牙名
     */
    public String getName() {
        Log.v(TAG, "getName");
        return mBluetoothAdapter.getName();
    }

    /**
     * 设置蓝牙名字
     * @param name
     * @return 单例
     */
    public BluetoothHelper setName(String name) {
        Log.v(TAG, "setName:" + name);
        mBluetoothAdapter.setName(name);
        return this;
    }

    /**
     * 获得蓝牙地址
     * @return
     */
    public String getAddress() {
        Log.v(TAG, "getAddress");
        return mBluetoothAdapter.getAddress();
    }

    /**
     * 通过地址获得蓝牙设备
     * @param address 蓝牙地址
     * @return 蓝牙设备
     */
    public BluetoothDevice getRemoteDevice(String address) {
        Log.v(TAG, "getRemoteDevice：" + address);
        return mBluetoothAdapter.getRemoteDevice(address);
    }

    /**
     * 获得蓝牙适配器
     * @return mBluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        Log.v(TAG, "getBluetoothAdapter");
        return mBluetoothAdapter;
    }

    /**
     * 连接设备
     * @param device 设备
     * @return 结果
     */
    public boolean connectDevice(BluetoothDevice device) {
        Log.v(TAG, "connectDevice");
        if (!mBluetoothAdapter.isEnabled()) {
            if (bluetoothReceiverInterface != null) {
                bluetoothReceiverInterface.onState(State.ERROR_BLUETOOTH_DISABLE);
            }
            return false;
        } else {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
            connectThread = new ConnectThread(device);
            connectThread.start();
        }
        return true;
    }

    /**
     * 断开蓝牙连接
     */
    public void disconnectDevice() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    /**
     * 解除绑定
     * removeBond属于私有方法，利用Android反射机制直接公开使用
     * @param device
     * @return 结果
     */
    public boolean unPairDevice(BluetoothDevice device) {
        Log.v(TAG, "unPairDevice:");
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * 如果蓝牙是开的，开启连接服务，等待设备连接
     */
    public BluetoothHelper startServer() {
        this.isServer = true;
        Log.v(TAG, "startServer");
        if (!isBluetoothEnable()) {
            if (bluetoothReceiverInterface != null) {
                bluetoothReceiverInterface.onState(State.ERROR_BLUETOOTH_DISABLE);
            }
        } else {
            if (acceptThread != null) {
                acceptThread.cancel();
                acceptThread = null;
            }
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        return this;
    }

    /**
     * 停止服务器
     */
    public BluetoothHelper stopServer() {
        this.isServer = false;
        Log.v(TAG, "stopServer");
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        return this;
    }

    /**
     * 写入数据
     * @param bytes
     * @return 是否成功
     */
    public boolean write(byte[] bytes) {
        Log.v(TAG, "write:" + bytes);
        if (connectedThread != null) {
            return connectedThread.write(bytes);
        } else {
            return false;
        }
    }


    /** 状态枚举 */
    public enum State {
        STATE_DISCONNECTED("断开连接"),
        STATE_CONNECTED("连接成功"),
        STATE_CONNECT_CREATE_FAILED("创建设备连接失败"),
        STATE_CONNECT_FAILED("连接失败"),
        STATE_SCAN_BEGIN("开始扫描"),
        STATE_SCAN_FAILED("扫描失败"),
        STATE_SCAN_END("扫描结束"),
        STATE_PAIRING("开始自动配对"),
        STATE_ON("蓝牙打开"),
        STATE_TURNING_ON("蓝牙正在打开"),
        STATE_OFF("蓝牙关闭"),
        STATE_TURNING_OFF("蓝牙正在关闭"),
        DATA_CREATE_FAILED("创建数据通信失败"),
        DATA_PREPARED("可以数据交互了"),
        DATA_READ_FAILED("读取消息失败,Socket已经断开了"),
        BOND_NONE("删除绑定"),
        BOND_BONDING("正在绑定"),
        BOND_BONDED("绑定成功"),
        SERVER_OPEN_FAILED("服务器启动失败"),
        SERVER_QUIT("服务器已经退出了"),
        SCAN_MODE_NONE("设置蓝牙不被发现,不能连接"),
        SCAN_MODE_CONNECTABLE_DISCOVERABLE("设置蓝牙可被发现，可被连接"),
        SCAN_MODE_CONNECTABLE("设置蓝牙不被发现不被连接"),
        ERROR_BLUETOOTH_DISABLE("蓝牙未开启，操作失败"),
        BLUETOOTH_NO_SURPORT("不支持蓝牙");

        private final String state;

        /**
         * 枚举类构造函数
         * @param var 枚举子对象
         */
        State(String var) {
            this.state = var;
        }

        /**
         * 获得枚举子对象实体描述
         * @return 枚举实体
         */
        public String toString() {
            return this.state;
        }
    }

    /**
     * 扫描设备回调
     */
    public interface BluetoothReceiverInterface {
        void onReceiveDevice(BluetoothDevice device);

        void onReceiveData(byte[] buffer);

        void onState(State state, Object... args);
    }

    /**
     * 蓝牙Socket通信(客户端连接服务器)
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mDevice = device;
            try {
                if (isSecurConnect) {
                    tmp = mDevice.createRfcommSocketToServiceRecord(S_UUID);
                } else {
                    tmp = mDevice.createInsecureRfcommSocketToServiceRecord(S_UUID);
                }
            } catch (IOException e) {
                if (bluetoothReceiverInterface != null) {
                    bluetoothReceiverInterface.onState(BluetoothHelper.State.STATE_CONNECT_CREATE_FAILED, mDevice);
                }
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.v(TAG, "ConnectThread:run");
            mBluetoothAdapter.cancelDiscovery();
            if (mmSocket == null) {
                if (bluetoothReceiverInterface != null) {
                    bluetoothReceiverInterface.onState(BluetoothHelper.State.STATE_CONNECT_CREATE_FAILED, mDevice);
                }
                cancel();
                return;
            }
            try {
                Thread.sleep(CONNECT_TIME);
                mmSocket.connect();
            } catch (Exception e) {
                if (bluetoothReceiverInterface != null) {
                    bluetoothReceiverInterface.onState(BluetoothHelper.State.STATE_CONNECT_FAILED, mDevice);
                }
                cancel();
                return;
            }
            if (connectedThread != null) {
                connectedThread.cancel();
                connectedThread = null;
            }
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        public void cancel() {
            if (mmSocket == null) {
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 蓝牙Socket通信(服务器端等待客户端连接)
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (isSecurConnect) {
                    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(S_NAME, S_UUID);
                } else {
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(S_NAME, S_UUID);
                }
            } catch (IOException e) {
                if (bluetoothReceiverInterface != null) {
                    bluetoothReceiverInterface.onState(BluetoothHelper.State.SERVER_OPEN_FAILED);
                }
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.v(TAG, "AcceptThread:run");
            BluetoothSocket socket = null;
            while (true) {
                try {
                    if (mmServerSocket == null) {
                        if (bluetoothReceiverInterface != null) {
                            bluetoothReceiverInterface.onState(BluetoothHelper.State.SERVER_OPEN_FAILED);
                        }
                        break;
                    }
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    if (bluetoothReceiverInterface != null) {
                        bluetoothReceiverInterface.onState(BluetoothHelper.State.SERVER_QUIT);
                    }
                    break;
                }
                if (socket != null) {
                    if (connectedThread != null) {
                        connectedThread.cancel();
                        connectedThread = null;
                    }
                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();
                }
            }
            cancel();
        }

        public void cancel() {
            if (mmServerSocket == null) {
                return;
            }
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 连接管理（数据通信）
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public volatile boolean runEnable = true; // 运行标志位

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if (bluetoothReceiverInterface != null) {
                    bluetoothReceiverInterface.onState(BluetoothHelper.State.DATA_CREATE_FAILED);
                }
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            if (bluetoothReceiverInterface != null) {
                bluetoothReceiverInterface.onState(BluetoothHelper.State.DATA_PREPARED);
            }
        }

        public void run() {
            Log.v(TAG, "ConnectedThread:run");
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    if (!runEnable) {
                        break;
                    }
                    if (mmInStream == null) {
                        break;
                    }
                    bytes = mmInStream.read(buffer);
                    if (bytes != 0) {
                        byte[] finalBuffer = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            finalBuffer[i] = buffer[i];
                        }
                        Log.v(TAG, "bytes:" + bytes);
                        Log.v(TAG, "json:" + new String(finalBuffer));
                        if (bluetoothReceiverInterface != null) {
                            Log.v(TAG, "bluetoothReceiverInterface is not null");
                            bluetoothReceiverInterface.onReceiveData(finalBuffer);
                        } else {
                            Log.v(TAG, "bluetoothReceiverInterface is null");
                        }
                    }
                } catch (IOException e) {
                    if (bluetoothReceiverInterface != null) {
                        bluetoothReceiverInterface.onState(BluetoothHelper.State.DATA_READ_FAILED);
                    }
                    break;
                }
            }
            cancel();
        }

        public boolean write(byte[] bytes) {
            try {
                if (mmOutStream != null) {
                    mmOutStream.write(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public void cancel() {
            runEnable = false;
            try {
                if (mmInStream != null) {
                    mmInStream.close();
                }
                if (mmOutStream != null) {
                    mmOutStream.close();
                }
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
