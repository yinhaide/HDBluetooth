package com.yhd.bluetooth.app.frag;

import android.view.View;

import com.de.rocket.ue.frag.RoFragment;
import com.de.rocket.ue.injector.Event;
import com.yhd.bluetooth.app.R;

/**
 * 蓝牙
 * Created by haide.yin(haide.yin@tcl.com) on 2019/6/6 16:12.
 */
public class Frag_main extends RoFragment {

    @Override
    public int onInflateLayout() {
        return R.layout.frag_main;
    }

    @Override
    public void initViewFinish(View inflateView) {

    }

    @Override
    public void onNexts(Object object) {

    }

    @Event(R.id.bt_send)
    private void blClient(View view) {
        toFrag(Frag_bluetooth_client.class);
    }

    @Event(R.id.bt_bl_server)
    private void blServer(View view) {
        toFrag(Frag_bluetooth_server.class);
    }

    @Event(R.id.bt_ble)
    private void ble(View view) {
        toFrag(Frag_bluetoothle.class);
    }
}
