package org.mediasoup.droid.lib.model;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class Peer extends Info {

  private String mId;
  private String mDisplayName;
  private DeviceInfo mDevice;

  private Set<String> mConsumers;
  private Set<String> mDataConsumers;

  public Peer(@NonNull JSONObject info) {
    mId = info.optString("id");
    mDisplayName = info.optString("displayName");
    JSONObject deviceInfo = info.optJSONObject("device");
    if (deviceInfo != null) {
      mDevice =
          new DeviceInfo()
              .setFlag(deviceInfo.optString("flag"))
              .setName(deviceInfo.optString("name"))
              .setVersion(deviceInfo.optString("version"));
    } else {
      mDevice = DeviceInfo.unknownDevice();
    }
    mConsumers = new HashSet<>();
    mDataConsumers = new HashSet<>();
  }

  @Override
  public String getId() {
    return mId;
  }

  @Override
  public String getDisplayName() {
    return mDisplayName;
  }

  @Override
  public DeviceInfo getDevice() {
    return mDevice;
  }

  public void setDisplayName(String displayName) {
    this.mDisplayName = displayName;
  }

  public void setDevice(DeviceInfo device) {
    this.mDevice = device;
  }

  public Set<String> getConsumers() {
    return mConsumers;
  }
  public Set<String> getDataConsumers() {
    return mDataConsumers;
  }
}
