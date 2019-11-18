package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.lib.RoomClient;

public class RoomInfo {

  private String url;
  private String roomId;
  private String peerId;
  private RoomClient.RoomState state = RoomClient.RoomState.NEW;
  private String activeSpeakerId;
  private String statsPeerId;
  private boolean faceDetection = false;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public String getPeerId() {
    return peerId;
  }

  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }

  public RoomClient.RoomState getState() {
    return state;
  }

  public void setState(RoomClient.RoomState state) {
    this.state = state;
  }

  public String getActiveSpeakerId() {
    return activeSpeakerId;
  }

  public void setActiveSpeakerId(String activeSpeakerId) {
    this.activeSpeakerId = activeSpeakerId;
  }

  public String getStatsPeerId() {
    return statsPeerId;
  }

  public void setStatsPeerId(String statsPeerId) {
    this.statsPeerId = statsPeerId;
  }

  public boolean isFaceDetection() {
    return faceDetection;
  }

  public void setFaceDetection(boolean faceDetection) {
    this.faceDetection = faceDetection;
  }
}
