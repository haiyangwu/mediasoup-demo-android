package org.mediasoup.droid.demo.vm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomContext;
import org.mediasoup.droid.lib.lv.SupplierMutableLiveData;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peers;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomProps extends AndroidViewModel {

  private RoomContext repository;
  private LiveData<String> invitationLink;
  private LiveData<RoomClient.RoomState> state;

  public RoomProps(@NonNull Application application) {
    super(application);

    repository = RoomContext.getInstance();
    invitationLink = Transformations.map(repository.getRoomInfo(), RoomInfo::getUrl);
    state = Transformations.map(repository.getRoomInfo(), RoomInfo::getState);
  }

  public LiveData<String> getInvitationLink() {
    return invitationLink;
  }

  public SupplierMutableLiveData<Peers> getPeersInfo() {
    return repository.getPeers();
  }

  public LiveData<RoomClient.RoomState> getState() {
    return state;
  }

  public LiveData<Notify> getNotify() {
    return repository.getNotify();
  }

  public LiveData<Me> getMe() {
    return repository.getMe();
  }
}
