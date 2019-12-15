package org.mediasoup.droid.demo.vm;

import android.app.Application;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.lifecycle.LifecycleOwner;

import org.mediasoup.droid.demo.R;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomProps extends EdiasProps {

  private final Animation mAnimation;
  private ObservableField<String> mInvitationLink;
  private ObservableField<RoomClient.ConnectionState> mConnectionState;

  public RoomProps(@NonNull Application application, @NonNull RoomStore roomStore) {
    super(application, roomStore);
    mInvitationLink = new ObservableField<>();
    mConnectionState = new ObservableField<>();
    mAnimation = AnimationUtils.loadAnimation(getApplication(), R.anim.ani_connecting);
  }

  public Animation getAnimation() {
    return mAnimation;
  }

  public ObservableField<String> getInvitationLink() {
    return mInvitationLink;
  }

  public ObservableField<RoomClient.ConnectionState> getConnectionState() {
    return mConnectionState;
  }

  private void receiveState(RoomInfo roomInfo) {
    mConnectionState.set(roomInfo.getConnectionState());
    mInvitationLink.set(roomInfo.getUrl());
  }

  @Override
  public void connect(LifecycleOwner owner) {
    RoomStore roomStore = getRoomStore();
    roomStore.getRoomInfo().observe(owner, this::receiveState);
  }
}
