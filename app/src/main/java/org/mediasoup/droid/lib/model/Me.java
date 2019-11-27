package org.mediasoup.droid.lib.model;

import org.json.JSONObject;

public class Me {

  private String id;
  private String displayName;
  private boolean displayNameSet;
  private JSONObject device;
  private boolean canSendMic;
  private boolean canSendCam;
  private boolean canChangeCam;
  private boolean camInProgress;
  private boolean shareInProgress;
  private boolean audioOnly;
  private boolean audioOnlyInProgress;
  private boolean audioMuted;
  private boolean restartIceInProgress;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public boolean isDisplayNameSet() {
    return displayNameSet;
  }

  public void setDisplayNameSet(boolean displayNameSet) {
    this.displayNameSet = displayNameSet;
  }

  public JSONObject getDevice() {
    return device;
  }

  public void setDevice(JSONObject device) {
    this.device = device;
  }

  public boolean isCanSendMic() {
    return canSendMic;
  }

  public void setCanSendMic(boolean canSendMic) {
    this.canSendMic = canSendMic;
  }

  public boolean isCanSendCam() {
    return canSendCam;
  }

  public void setCanSendCam(boolean canSendCam) {
    this.canSendCam = canSendCam;
  }

  public boolean isCanChangeCam() {
    return canChangeCam;
  }

  public void setCanChangeCam(boolean canChangeCam) {
    this.canChangeCam = canChangeCam;
  }

  public boolean isCamInProgress() {
    return camInProgress;
  }

  public void setCamInProgress(boolean camInProgress) {
    this.camInProgress = camInProgress;
  }

  public boolean isShareInProgress() {
    return shareInProgress;
  }

  public void setShareInProgress(boolean shareInProgress) {
    this.shareInProgress = shareInProgress;
  }

  public boolean isAudioOnly() {
    return audioOnly;
  }

  public void setAudioOnly(boolean audioOnly) {
    this.audioOnly = audioOnly;
  }

  public boolean isAudioOnlyInProgress() {
    return audioOnlyInProgress;
  }

  public void setAudioOnlyInProgress(boolean audioOnlyInProgress) {
    this.audioOnlyInProgress = audioOnlyInProgress;
  }

  public boolean isAudioMuted() {
    return audioMuted;
  }

  public void setAudioMuted(boolean audioMuted) {
    this.audioMuted = audioMuted;
  }

  public boolean isRestartIceInProgress() {
    return restartIceInProgress;
  }

  public void setRestartIceInProgress(boolean restartIceInProgress) {
    this.restartIceInProgress = restartIceInProgress;
  }

  public void clear() {
    camInProgress = false;
    shareInProgress = false;
    audioOnly = false;
    audioOnlyInProgress = false;
    audioMuted = false;
    restartIceInProgress = false;
  }
}
