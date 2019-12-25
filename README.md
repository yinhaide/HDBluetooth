# HDBluetooth
[![Platform](https://img.shields.io/badge/平台-%20Android%20-brightgreen.svg)](https://github.com/yinhaide/HDBluetooth/wiki)
[![characteristic](https://img.shields.io/badge/特点-%20轻量级%20%7C%20简单易用%20%20%7C%20稳定%20-brightgreen.svg)](https://github.com/yinhaide/HDBluetooth/wiki)
> 谷歌中国API链接:https://developer.android.google.cn/guide/topics/connectivity/bluetooth.html <br/>
> 支持常规蓝牙的使用。 <br/>
> 支持低功耗蓝牙的使用。 <br/>

![](https://github.com/yinhaide/HDBluetooth/raw/master/resource/bluetooth_client.gif)  ![](https://github.com/yinhaide/HDBluetooth/raw/master/resource/bluetooth_service.gif) ![](https://github.com/yinhaide/HDBluetooth/raw/master/resource/bluetoothle.gif) 

## 目录
* [如何导入到项目](#Import)
* [如何使用](#Use)
* [关于我](#About)
* [License](#License)

<a name="Import"></a>
### 导入方式
在工程级别的**build.gradle**添加
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
在应用级别的**build.gradle**添加
```
api 'com.github.yinhaide:HDBluetooth:0.0.1'
```

<a name="Use"></a>
### 如何使用
> 蓝牙硬件分为两个版本：常规蓝牙和低功耗蓝牙，两种蓝牙的使用方式也完全不一样。 <br/>
> 这个库文件主要解决硬件操作繁琐的问题，实现一行代码实现所有的操作。 <br/>

#### BluetoothHelper
> 一般的蓝牙可分为服务器端以及客户端，连接以及通信方式采用Socket协议。 <br/>

* 作为客户端，能够获得蓝牙列表，能够发起连接配对请求

```
/**
 * 注:回调是在一般线程中的，如果需要刷新UI，需要在主线程中进行
 */
private void initBluetooth(){
    BluetoothHelper.getInstance(getApplicationContext())
            .registerBlueToothReceiver()       //注册需要的广播
            .setBluetoothReceiverInterface(new BluetoothHelper.BluetoothReceiverInterface() {

                @Override
                public void onReceiveDevice(BluetoothDevice device) {
                    //在执行设备扫描动作之后，每发现一个设备都会回调一次，所以如果需要显示列表的时候需要先判断是否重复
                }

                @Override
                public void onReceiveData(final byte[] buffer) {
                    //收到对方发过来的数据
                }

                @Override
                public void onState(final BluetoothHelper.State state, Object... args) {
                    //状态回调，包括蓝牙连接状态以及扫描设备状态
                    Log.v(TAG,state.toString());
                    if(state== BluetoothHelper.State.STATE_CONNECTED){
                        //连接成功
                    }
                }
            })
            .openBluetooth()                    //开启蓝牙
            .setDiscovery(true);                //开始扫描，false为结束扫描;
}
```

* 作为服务端，能够获得蓝牙连接配对请求

```
/**
 * 注:回调是在一般线程中的，如果需要刷新UI，需要在主线程中进行
 */
private void initBluetooth(){
    BluetoothHelper.getInstance(getApplicationContext())
            .registerBlueToothReceiver()        //注册需要的广播
            .setBluetoothReceiverInterface(new BluetoothHelper.BluetoothReceiverInterface() {

                @Override
                public void onReceiveDevice(BluetoothDevice device) {
                    //在执行设备扫描动作之后，每发现一个设备都会回调一次，所以如果需要显示列表的时候需要先判断是否重复
                }

                @Override
                public void onReceiveData(final byte[] buffer) {
                    //收到对方发过来的数据
                }

                @Override
                public void onState(final BluetoothHelper.State state, Object... args) {
                    //状态回调，包括蓝牙连接状态以及扫描设备状态
                    Log.v(TAG,state.toString());
                    if(state== BluetoothHelper.State.STATE_CONNECTED){
                        //连接成功
                    }
                }
            })
            .setDiscoverableTime(0)             //设置蓝牙被发现时间，0为永远被发现，－1表示永远不被发现，其他的依据时间设定
            .openBluetooth()                    //开启蓝牙
            .startServer();                     //最后执行，开启蓝牙服务器，可以接收对方的消息，如果你不需要接收只需要发送的话可以不使用该方法活着调用：stopServer()禁用接收功能
}
```

* 连接设备

```
BluetoothHelper.getInstance(BluetoothClientActivity.this).connectDevice(addressString);
```

* 因为注册了广播，在不需要用到这个广播的时候取消

```
protected void onDestroy(){
    super.onDestroy();
    BluetoothHelper.getInstance(getApplicationContext()).unRegisterBlueToothReceiver();
}
```

* 更多的操作

```
/** 设置首次连接时需不需要点击配对确认，默认false */
BluetoothHelper.getInstance(getApplicationContext()).setIsAutoPare(默认false);
/** 连接时不需要配对申请，默认true */
BluetoothHelper.getInstance(getApplicationContext()).setIsSecurConnect(true);
/** 打开或关闭蓝牙，此方法为异步方法 */
BluetoothHelper.getInstance(getApplicationContext()).setEnableBluetooth(true);
/** 关闭蓝牙，此方法为同步方法 */
BluetoothHelper.getInstance(getApplicationContext()).closeBluetooth();
/** 获取已经配对过的设备列表 */
BluetoothHelper.getInstance(getApplicationContext()).getBondedDevices();
/** 获得本机蓝牙名字 */
BluetoothHelper.getInstance(getApplicationContext()).getName();
/** 通过蓝牙地址获得蓝牙设备 */
BluetoothHelper.getInstance(getApplicationContext()).getRemoteDevice(address);
/** 设置蓝牙之间连接的UUI，如果不设置，将默认使用最为普遍实用的SPP_UUID */
BluetoothHelper.getInstance(getApplicationContext()).setServerUUID(uuid);
/** 设置蓝牙服务的名字，默认是“xjkb2” */
BluetoothHelper.getInstance(getApplicationContext()).setServerName(name);
/** 取消指定配对过的设备 */
BluetoothHelper.getInstance(getApplicationContext()).unPairDevice(device);
/** 连接指定的设备 */
BluetoothHelper.getInstance(getApplicationContext()).connectDevice(device);
/** 断开当前设备的连接，第二次发起设备连接之前需要调用此方法，不然连接不上 */
BluetoothHelper.getInstance(getApplicationContext()).disconnectDevice();
/** 在连接上设备之后可以双方进行数据通信 */
BluetoothHelper.getInstance(getApplicationContext()).write(bytes);
/** 停止接收数据功能，只保留发送数据功能 */
BluetoothHelper.getInstance(getApplicationContext()).stopServer();
/** 如果还需要其他方法的话直接获得蓝牙适配器 */
BluetoothHelper.getInstance(getApplicationContext()).getBluetoothAdapter();
```

#### BluetootLEhHelper
>蓝牙低功耗类可分为服务器端以及客户端，连接以及通信方式采用GATT协议。<br/>
>该类可实现蓝牙连接、蓝牙基本操作以及蓝牙数据交互。

* 设置你需要交互数据的UUID字符串(示例，请根据你的实际情况设定)

```
/** 配置需要交互的服务UUID */
private final static String SERVICE_UUID="00002902-0000-1000-8000-00805f9b3400";
/** 配置需要交互的特征UUID */
private final static String CHARACTER_UUID="00002902-0000-1000-8000-00805f9b3401";
```

* 由于Google API的限制，Android端低功耗的蓝牙协议只支持作为主动连接方，不能作为被连接方，相比而言，iOS端的低功耗蓝牙就没有该限制。初始化的正确姿势:

```
/**
 * 注:回调是在一般线程中的，如果需要刷新UI，需要在主线程中进行
 */
private void initBluetooth() {
    //不支持蓝牙低功耗的话请停止下面的操作
    if(!BluetoothLEHelper.getInstance(getApplicationContext()).isSurport()){
        finish();
    }
    BluetoothLEHelper.getInstance(getApplicationContext())
            .setEnableBluetooth(true)                //打开蓝牙
            .setBlutoothReceiverInterface(new BluetoothLEHelper.BluetoothLowEnergyInterface() {
                @Override
                public void onReceiveDevice(BluetoothDevice device) {
                    //在执行设备扫描动作之后，每发现一个设备都会回调一次，所以如果需要显示列表的时候需要先判断是否重复
                }

                @Override
                public void onReceiveData(final byte[] data, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    //收到对方发过来的数据
                }

                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void onState(final BluetoothLEHelper.State state, BluetoothGatt gatt, Object... args) {
                    Log.v(TAG,state.toString());
                    //这一点非常重要,当连接设备成功之后，下一步就是主动发现设备的服务，一发现服务，请允许监听您需要交互数据的Characteristic的通知
                    //如果不执行该动作，您将不会收到对方发来的数据
                    if(state==BluetoothLEHelper.State.STATE_SERVER_DISCOVERED){
                        BluetoothGattCharacteristic characteristic=gatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(CHARACTER_UUID));
                        if(characteristic!=null){
                            BluetoothLEHelper.getInstance(getApplicationContext()).setCharacteristicNotification(characteristic,true);
                        }
                    }else if(state== BluetoothLEHelper.State.STATE_CONNECTED){
                         //连接成功
                     }
                }
            })
            .scanLeDevice(true);                 //开始扫描  

}
```

* 连接设备

```
BluetoothLEHelper.getInstance(getApplicationContext()).connectGatt(addressString,isAutoConnect);
```

* 更多的操作

```
/** 设置扫描超时时间 */
BluetoothLEHelper.getInstance(getApplicationContext()).setScanPeriod(1000);
/** 断开连接 */
BluetoothLEHelper.getInstance(getApplicationContext()).disconnect();
BluetoothLEHelper.getInstance(getApplicationContext()).disconnectWithGatt(gatt);
/** 得到所有的服务信息 */
BluetoothLEHelper.getInstance(getApplicationContext()).getSupportedGattServices();
/** 设置蓝牙被发现时间，0为永远被发现，－1表示永远不被发现，其他的依据时间设定 */
BluetoothLEHelper.getInstance(getApplicationContext()).setDiscoverableTime(0);
/** 读取蓝牙名 */
BluetoothLEHelper.getInstance(getApplicationContext()).getBluetoothName();
/** 设置蓝牙名 */
BluetoothLEHelper.getInstance(getApplicationContext()).setBluetoothName("xjkb2");
/** 设置蓝牙名 */
BluetoothLEHelper.getInstance(getApplicationContext()).setBluetoothName("xjkb2");
/** 发送数据 */
BluetoothLEHelper.getInstance(getApplicationContext()).wirteCharacteristic(characteristic,stringValue);
BluetoothLEHelper.getInstance(getApplicationContext()).wirteCharacteristic(characteristic,byteValue);
/** 读取数据 */
BluetoothLEHelper.getInstance(getApplicationContext()).readCharacteristic(characteristic);
```

<a name="About"></a>
### 这个项目会持续更新中... 
> 都看到这里了，如果觉得写的可以或者对你有帮助的话，顺手给个星星点下Star~

### 关于我
+ **Email:** [123302687@qq.com](123302687@qq.com)
+ **Github:** [https://github.com/yinhaide](https://github.com/yinhaide)
+ **简书:** [https://www.jianshu.com/u/33c3dd2ceaa3](https://www.jianshu.com/u/33c3dd2ceaa3)
+ **CSDN:** [https://blog.csdn.net/yinhaide](https://blog.csdn.net/yinhaide)

<a name="License"></a>
### License

    Copyright 2017 yinhaide
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.