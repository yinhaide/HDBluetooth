package com.yhd.bluetooth.app.frag;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.de.rocket.ue.frag.RoFragment;
import com.de.rocket.ue.injector.BindView;
import com.de.rocket.ue.injector.Event;
import com.yhd.bluetooth.BluetoothLEHelper;
import com.yhd.bluetooth.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.yhd.bluetooth.BluetoothHelper.TAG;

/**
 * 蓝牙
 * Created by haide.yin(haide.yin@tcl.com) on 2019/6/6 16:12.
 */
public class Frag_bluetoothle extends RoFragment {

    @BindView(R.id.lv_device)
    private ListView lvDevice;
    @BindView(R.id.tv_content)
    private TextView tvContent;

    private List<BluetoothDevice> bluetoothDevice =new ArrayList<>();
    private List<String> nameArray =new ArrayList<>();
    private BaseAdapter usableAdapter;
    /** 配置需要交互的服务UUID */
    private final static String SERVICE_UUID="0000ffe0-0000-1000-8000-00805f9b34fb";
    /** 配置需要交互的特征UUID */
    private final static String CHARACTOR_UUID="0000ffe1-0000-1000-8000-00805f9b34fb";

    @Override
    public int onInflateLayout() {
        return R.layout.frag_bluetoothle;
    }

    @Override
    public void initViewFinish(View inflateView) {
        initView();
    }

    @Override
    public void onNexts(Object object) {
        if(BluetoothLEHelper.getInstance(activity).isSurport()){
            initBluetooth();
        }else{
            toast("不支持");
            back();
        }
    }

    @Event(R.id.bt_send)
    private void send(View view) {
        if(BluetoothLEHelper.getInstance(activity).wirteData(SERVICE_UUID,CHARACTOR_UUID,"I am Phone")){
            toast("发送成功");
        }else{
            toast("发送失败");
        }
    }

    /**
     * 页面初始化
     */
    private void initView(){
        lvDevice.setOnItemClickListener((parent, view, position, id) -> {
            boolean result = BluetoothLEHelper.getInstance(activity).connectGatt(bluetoothDevice.get(position).getAddress(),false);
            if(result){
                toast("连接成功");
            }
        });
        usableAdapter=new BaseAdapter() {
            @Override
            public int getCount() {
                return bluetoothDevice.size();
            }

            @Override
            public Object getItem(int position) {
                return bluetoothDevice.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                BluetoothDevice device = bluetoothDevice.get(position);
                String name = nameArray.get(position);
                TextView textView=new TextView(activity);
                textView.setTextSize(30);
                textView.setText(name+"("+device.getAddress()+")");
                return textView;
            }
        };
        lvDevice.setAdapter(usableAdapter);
    }

    /**
     * 初始化蓝牙设备
     */
    private void initBluetooth() {
        BluetoothLEHelper.getInstance(activity)
                .setEnableBluetooth(true)
                .setBlutoothReceiverInterface(new BluetoothLEHelper.BluetoothLowEnergyInterface() {

                    @Override
                    public void onReceiveDevice(BluetoothDevice device,String name) {
                        if (!bluetoothDevice.contains(device)) {
                            bluetoothDevice.add(device);
                            nameArray.add(name);
                        }
                        activity.runOnUiThread(() -> usableAdapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onReceiveData(final byte[] data, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        Log.v(TAG,new String(data));
                        activity.runOnUiThread(() -> Toast.makeText(activity,"收到消息:"+new String(data),Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onState(final BluetoothLEHelper.State state, BluetoothGatt gatt, Object... args) {
                        Log.v(TAG,state.toString());
                        activity.runOnUiThread(() -> tvContent.setText(state.toString()));
                        //发现服务，如果需要交互的话需要监听指定的UUID
                        if(state == BluetoothLEHelper.State.STATE_SERVER_DISCOVERED){
                            BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
                            if(service != null){
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(CHARACTOR_UUID));
                                BluetoothLEHelper.getInstance(activity).setCharacteristicNotification(characteristic,true);
                            }
                        }else if(state== BluetoothLEHelper.State.STATE_CONNECTED){
                            //连接成功
                            activity.runOnUiThread(() -> Toast.makeText(activity,"蓝牙连接成功",Toast.LENGTH_LONG).show());
                        }
                    }
                })
                .scanLeDevice(true);

    }
}
