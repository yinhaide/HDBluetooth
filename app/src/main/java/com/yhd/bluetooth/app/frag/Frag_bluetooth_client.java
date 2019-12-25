package com.yhd.bluetooth.app.frag;

import android.bluetooth.BluetoothDevice;
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
import com.yhd.bluetooth.BluetoothHelper;
import com.yhd.bluetooth.app.R;

import java.util.ArrayList;
import java.util.List;

import static com.yhd.bluetooth.BluetoothLEHelper.TAG;

/**
 * 蓝牙
 * Created by haide.yin(haide.yin@tcl.com) on 2019/6/6 16:12.
 */
public class Frag_bluetooth_client extends RoFragment {

    @BindView(R.id.lv_device)
    private ListView lvDevice;
    @BindView(R.id.tv_content)
    private TextView tvContent;

    private List<BluetoothDevice> usableDevice=new ArrayList<>();
    private BaseAdapter bluetoothAdapter;

    @Override
    public int onInflateLayout() {
        return R.layout.frag_bluetooth_client;
    }

    @Override
    public void initViewFinish(View inflateView) {
        initView();
    }

    @Override
    public void onNexts(Object object) {
        if(BluetoothHelper.getInstance(activity).isSurport()){
            initBluetooth();
        }else{
            toast("不支持");
            back();
        }
    }

    /**
     * 页面初始化
     */
    private void initView(){
        //点击
        lvDevice.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothHelper.getInstance(activity).connectDevice(usableDevice.get(position));
        });
        bluetoothAdapter =new BaseAdapter() {
            @Override
            public int getCount() {
                return usableDevice.size();
            }

            @Override
            public Object getItem(int position) {
                return usableDevice.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView=new TextView(activity);
                textView.setTextSize(30);
                textView.setText(usableDevice.get(position).getName());
                return textView;
            }
        };
        lvDevice.setAdapter(bluetoothAdapter);
    }

    /**
     * 蓝牙初始化
     */
    private void initBluetooth(){
        BluetoothHelper.getInstance(activity)
                .registerBlueToothReceiver()//注册需要的广播与接口
                .setBluetoothReceiverInterface(new BluetoothHelper.BluetoothReceiverInterface() {

                    @Override
                    public void onReceiveDevice(BluetoothDevice device) {
                        //在执行设备扫描动作之后，每发现一个设备都会回调一次，所以如果需要显示列表的时候需要先判断是否重复
                        //该部分是在一般线程中的，如果需要刷新UI，需要在主线程中进行
                        if (!usableDevice.contains(device)) {
                            usableDevice.add(device);
                        }
                        activity.runOnUiThread(() -> bluetoothAdapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onReceiveData(final byte[] buffer) {
                        //收到对方发过来的数据
                        activity.runOnUiThread(() -> Toast.makeText(activity,new String(buffer),Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onState(final BluetoothHelper.State state, Object... args) {
                        //状态回调，包括蓝牙连接状态以及扫描设备状态
                        Log.v(TAG,state.toString());
                        activity.runOnUiThread(() -> tvContent.setText(state.toString()));
                    }
                })
                .openBluetooth()    //开启蓝牙
                .setDiscovery(true);//开始扫描，false为结束扫描;
    }

    @Event(R.id.bt_send)
    private void send(View view) {
        if(BluetoothHelper.getInstance(activity).write("I am client".getBytes())){
            Toast.makeText(activity,"send success",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        BluetoothHelper.getInstance(activity).unRegisterBlueToothReceiver();
    }
}
