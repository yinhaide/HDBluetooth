package com.yhd.bluetooth.bean;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * 类作用描述
 * Created by haide.yin(haide.yin@tcl.com) on 2019/12/26 13:01.
 */
public class BleAdvertisedBean implements Serializable {

    private List<UUID> uuids;
    private String name;

    public BleAdvertisedBean(List<UUID> uuids, String name) {
        this.uuids = uuids;
        this.name = name;
    }

    public List<UUID> getUuids() {
        return uuids;
    }

    public void setUuids(List<UUID> uuids) {
        this.uuids = uuids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "BleAdvertisedBean{" +
                "uuids=" + uuids +
                ", name='" + name + '\'' +
                '}';
    }
}
