package org.mediasoup.droid.lib;

import androidx.annotation.NonNull;

import org.mediasoup.droid.lib.model.DeviceInfo;

public class RoomOptions {

  // Device info.
  @NonNull private DeviceInfo mDevice = DeviceInfo.androidDevice();
  // Whether we want to force RTC over TCP.
  private boolean mForceTcp = false;
  // Whether we want to produce audio/video.
  private boolean mProduce = true;
  // Whether we should consume.
  private boolean mConsume = true;
  // Whether we want DataChannels.
  private boolean mUseDataChannel;

  public RoomOptions setDevice(@NonNull DeviceInfo device) {
    this.mDevice = device;
    return this;
  }

  public RoomOptions setForceTcp(boolean forceTcp) {
    this.mForceTcp = forceTcp;
    return this;
  }

  public RoomOptions setProduce(boolean produce) {
    this.mProduce = produce;
    return this;
  }

  public RoomOptions setConsume(boolean consume) {
    this.mConsume = consume;
    return this;
  }

  public RoomOptions setUseDataChannel(boolean useDataChannel) {
    this.mUseDataChannel = useDataChannel;
    return this;
  }

  @NonNull
  public DeviceInfo getDevice() {
    return mDevice;
  }

  public boolean isForceTcp() {
    return mForceTcp;
  }

  public boolean isProduce() {
    return mProduce;
  }

  public boolean isConsume() {
    return mConsume;
  }

  public boolean isUseDataChannel() {
    return mUseDataChannel;
  }
}
