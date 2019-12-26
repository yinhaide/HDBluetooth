package com.yhd.bluetooth.bean;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * 类作用描述
 * Created by haide.yin(haide.yin@tcl.com) on 2019/12/26 12:53.
 */
public class HDBluetoothDevice implements Serializable {

    private String name;
    private BluetoothDevice bluetoothDevice;

    public HDBluetoothDevice(){

    }

    public HDBluetoothDevice(String name, BluetoothDevice bluetoothDevice) {
        this.name = name;
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public String toString() {
        return "HDBluetoothDevice{" +
                "name='" + name + '\'' +
                ", bluetoothDevice=" + bluetoothDevice +
                '}';
    }
}
