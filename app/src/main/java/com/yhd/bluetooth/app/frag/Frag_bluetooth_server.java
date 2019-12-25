package com.yhd.bluetooth.app.frag;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.de.rocket.ue.frag.RoFragment;
import com.de.rocket.ue.injector.BindView;
import com.de.rocket.ue.injector.Event;
import com.yhd.bluetooth.BluetoothHelper;
import com.yhd.bluetooth.app.R;

/**
 * 蓝牙
 * Created by haide.yin(haide.yin@tcl.com) on 2019/6/6 16:12.
 */
public class Frag_bluetooth_server extends RoFragment {

    @BindView(R.id.tv_content)
    private TextView tvContent;

    @Override
    public int onInflateLayout() {
        return R.layout.frag_bluetooth_server;
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

    @Event(R.id.bt_send)
    private void send(View view) {
        if(BluetoothHelper.getInstance(activity).write("I am service".getBytes())){
            Toast.makeText(activity,"send success",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 页面初始化
     */
    private void initView(){

    }

    /**
     * 页面蓝牙
     */
    private void initBluetooth(){
        BluetoothHelper.getInstance(activity)
                .registerBlueToothReceiver()        //注册需要的广播与接口
                .setBluetoothReceiverInterface(new BluetoothHelper.BluetoothReceiverInterface() {

                    @Override
                    public void onReceiveDevice(BluetoothDevice device) {
                        //在执行设备扫描动作之后，每发现一个设备都会回调一次，所以如果需要显示列表的时候需要先判断是否重复
                        //该部分是在一般线程中的，如果需要刷新UI，需要在主线程中进行
                    }

                    @Override
                    public void onReceiveData(final byte[] buffer) {
                        //收到对方发过来的数据
                        activity.runOnUiThread(() -> Toast.makeText(activity,new String(buffer),Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onState(final BluetoothHelper.State state, Object... args) {
                        //状态回调，包括蓝牙连接状态以及扫描设备状态
                        activity.runOnUiThread(() -> tvContent.setText(state.toString()));
                    }
                })
                .setDiscoverableTime(0)//设置蓝牙被发现时间，0为永远被发现，－1表示永远不被发现，其他的依据时间设定
                .openBluetooth()//开启蓝牙
                .startServer();//最后执行，开启蓝牙服务器，可以接收对方的消息，如果你不需要接收只需要发送的话可以不使用该方法活着调用：stopServer()禁用接收功能
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        BluetoothHelper.getInstance(activity).unRegisterBlueToothReceiver();
    }
}
