package org.mediasoup.droid.demo.vm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;

import org.json.JSONArray;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Info;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class PeerViewProps extends EdiasProps {

  boolean mIsMe;
  ObservableField<Boolean> mShowInfo;
  ObservableField<Info> mPeer;
  ObservableField<String> mAudioProducerId;
  ObservableField<String> mVideoProducerId;
  ObservableField<String> mAudioConsumerId;
  ObservableField<String> mVideoConsumerId;
  ObservableField<String> mAudioRtpParameters;
  ObservableField<String> mVideoRtpParameters;
  ObservableField<Integer> mConsumerSpatialLayers;
  ObservableField<Integer> mConsumerTemporalLayers;
  ObservableField<Integer> mConsumerCurrentSpatialLayer;
  ObservableField<Integer> mConsumerCurrentTemporalLayer;
  ObservableField<Integer> mConsumerPreferredSpatialLayer;
  ObservableField<Integer> mConsumerPreferredTemporalLayer;
  ObservableField<AudioTrack> mAudioTrack;
  ObservableField<VideoTrack> mVideoTrack;
  ObservableField<Boolean> mAudioMuted;
  ObservableField<Boolean> mVideoVisible;
  ObservableField<Boolean> mVideoMultiLayer;
  ObservableField<String> mAudioCodec;
  ObservableField<String> mVideoCodec;
  ObservableField<JSONArray> mAudioScore;
  ObservableField<JSONArray> mVideoScore;
  ObservableField<Boolean> mFaceDetection;

  public PeerViewProps(@NonNull Application application, @NonNull RoomStore roomStore) {
    super(application, roomStore);
    // Add default value to avoid null check in layout.
    mShowInfo = new ObservableField<>(Boolean.FALSE);
    mPeer = new ObservableField<>(new Info());
    mAudioProducerId = new ObservableField<>();
    mVideoProducerId = new ObservableField<>();
    mAudioConsumerId = new ObservableField<>();
    mVideoConsumerId = new ObservableField<>();
    mAudioRtpParameters = new ObservableField<>();
    mVideoRtpParameters = new ObservableField<>();
    mConsumerSpatialLayers = new ObservableField<>();
    mConsumerTemporalLayers = new ObservableField<>();
    mConsumerCurrentSpatialLayer = new ObservableField<>();
    mConsumerCurrentTemporalLayer = new ObservableField<>();
    mConsumerPreferredSpatialLayer = new ObservableField<>();
    mConsumerPreferredTemporalLayer = new ObservableField<>();
    mAudioTrack = new ObservableField<>();
    mVideoTrack = new ObservableField<>();
    mAudioMuted = new ObservableField<>();
    mVideoVisible = new ObservableField<>();
    mVideoMultiLayer = new ObservableField<>();
    mAudioCodec = new ObservableField<>();
    mVideoCodec = new ObservableField<>();
    mAudioScore = new ObservableField<>();
    mVideoScore = new ObservableField<>();
    mFaceDetection = new ObservableField<>();
  }

  public void setMe(boolean me) {
    mIsMe = me;
  }

  public boolean isMe() {
    return mIsMe;
  }

  public ObservableField<Boolean> getShowInfo() {
    return mShowInfo;
  }

  public ObservableField<Info> getPeer() {
    return mPeer;
  }

  public ObservableField<String> getAudioProducerId() {
    return mAudioProducerId;
  }

  public ObservableField<String> getVideoProducerId() {
    return mVideoProducerId;
  }

  public ObservableField<String> getAudioConsumerId() {
    return mAudioConsumerId;
  }

  public ObservableField<String> getVideoConsumerId() {
    return mVideoConsumerId;
  }

  public ObservableField<String> getAudioRtpParameters() {
    return mAudioRtpParameters;
  }

  public ObservableField<String> getVideoRtpParameters() {
    return mVideoRtpParameters;
  }

  public ObservableField<Integer> getConsumerSpatialLayers() {
    return mConsumerSpatialLayers;
  }

  public ObservableField<Integer> getConsumerTemporalLayers() {
    return mConsumerTemporalLayers;
  }

  public ObservableField<Integer> getConsumerCurrentSpatialLayer() {
    return mConsumerCurrentSpatialLayer;
  }

  public ObservableField<Integer> getConsumerCurrentTemporalLayer() {
    return mConsumerCurrentTemporalLayer;
  }

  public ObservableField<Integer> getConsumerPreferredSpatialLayer() {
    return mConsumerPreferredSpatialLayer;
  }

  public ObservableField<Integer> getConsumerPreferredTemporalLayer() {
    return mConsumerPreferredTemporalLayer;
  }

  public ObservableField<AudioTrack> getAudioTrack() {
    return mAudioTrack;
  }

  public ObservableField<VideoTrack> getVideoTrack() {
    return mVideoTrack;
  }

  public ObservableField<Boolean> getAudioMuted() {
    return mAudioMuted;
  }

  public ObservableField<Boolean> getVideoVisible() {
    return mVideoVisible;
  }

  public ObservableField<Boolean> getVideoMultiLayer() {
    return mVideoMultiLayer;
  }

  public ObservableField<String> getAudioCodec() {
    return mAudioCodec;
  }

  public ObservableField<String> getVideoCodec() {
    return mVideoCodec;
  }

  public ObservableField<JSONArray> getAudioScore() {
    return mAudioScore;
  }

  public ObservableField<JSONArray> getVideoScore() {
    return mVideoScore;
  }

  public ObservableField<Boolean> getFaceDetection() {
    return mFaceDetection;
  }
}
