package org.mediasoup.droid.lib.model;

import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

public class Peer {

  private String audioConsumerId;
  private String videoConsumerId;
  private JSONObject info;
  private AudioTrack audioTrack;
  private VideoTrack videoTrack;
  private boolean audioMuted;
  private boolean videoVisible;
  private boolean videoMultiLayer;
  private String audioCodec;
  private String videoCodec;
  private int audioScore;
  private int videoScore;

  public Peer(JSONObject info) {
    this.info = info;
  }

  public String getAudioConsumerId() {
    return audioConsumerId;
  }

  public void setAudioConsumerId(String audioConsumerId) {
    this.audioConsumerId = audioConsumerId;
  }

  public String getVideoConsumerId() {
    return videoConsumerId;
  }

  public void setVideoConsumerId(String videoConsumerId) {
    this.videoConsumerId = videoConsumerId;
  }

  public JSONObject getInfo() {
    return info;
  }

  public void setInfo(JSONObject info) {
    this.info = info;
  }

  public AudioTrack getAudioTrack() {
    return audioTrack;
  }

  public void setAudioTrack(AudioTrack audioTrack) {
    this.audioTrack = audioTrack;
  }

  public VideoTrack getVideoTrack() {
    return videoTrack;
  }

  public void setVideoTrack(VideoTrack videoTrack) {
    this.videoTrack = videoTrack;
  }

  public boolean isAudioMuted() {
    return audioMuted;
  }

  public void setAudioMuted(boolean audioMuted) {
    this.audioMuted = audioMuted;
  }

  public boolean isVideoVisible() {
    return videoVisible;
  }

  public void setVideoVisible(boolean videoVisible) {
    this.videoVisible = videoVisible;
  }

  public boolean isVideoMultiLayer() {
    return videoMultiLayer;
  }

  public void setVideoMultiLayer(boolean videoMultiLayer) {
    this.videoMultiLayer = videoMultiLayer;
  }

  public String getAudioCodec() {
    return audioCodec;
  }

  public void setAudioCodec(String audioCodec) {
    this.audioCodec = audioCodec;
  }

  public String getVideoCodec() {
    return videoCodec;
  }

  public void setVideoCodec(String videoCodec) {
    this.videoCodec = videoCodec;
  }

  public int getAudioScore() {
    return audioScore;
  }

  public void setAudioScore(int audioScore) {
    this.audioScore = audioScore;
  }

  public int getVideoScore() {
    return videoScore;
  }

  public void setVideoScore(int videoScore) {
    this.videoScore = videoScore;
  }
}
