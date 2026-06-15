package com.example.myapplication.data.remote.aidl;

import com.example.myapplication.data.remote.aidl.IControlPanelCallback;

interface IControlPanelService {
    int getRating();
    int getTemperature();
    boolean isToggledOn();
    void decreaseRating();
    void increaseRating();
    void decreaseTemperature();
    void increaseTemperature();
    void setToggledOn(boolean toggledOn);
    void registerCallback(IControlPanelCallback callback);
    void unregisterCallback(IControlPanelCallback callback);
}
