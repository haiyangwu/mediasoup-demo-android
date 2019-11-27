package org.mediasoup.droid.demo.vm;

import android.app.Application;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import org.mediasoup.droid.demo.R;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomRepository;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomViewModel extends AndroidViewModel {

  private String roomId;
  private String peerId;

  private RoomRepository repository;
  private MediatorLiveData<String> roomInfo;
  private LiveData<String> invitationLink;
  private LiveData<RoomClient.RoomState> state;

  public RoomViewModel(@NonNull Application application) {
    super(application);

    repository = RoomRepository.getInstance();
    invitationLink = Transformations.map(repository.getRoomInfo(), RoomInfo::getUrl);

    roomInfo = new MediatorLiveData<>();
    roomInfo.addSource(
        repository.getRoomInfo(),
        rawInfo -> {
          roomId = rawInfo.getRoomId();
          roomInfo.setValue(generateRoomInfo());
        });
    roomInfo.addSource(
        repository.getMe(),
        me -> {
          peerId = me.getId();
          roomInfo.setValue(generateRoomInfo());
        });
    state = Transformations.map(repository.getRoomInfo(), RoomInfo::getState);
  }

  private String generateRoomInfo() {
    if (TextUtils.isEmpty(roomId) || TextUtils.isEmpty(peerId)) {
      return "";
    }
    return getApplication().getString(R.string.room_info, roomId, peerId);
  }

  public LiveData<String> getInvitationLink() {
    return invitationLink;
  }

  public MediatorLiveData<String> getRoomInfo() {
    return roomInfo;
  }

  public LiveData<RoomClient.RoomState> getState() {
    return state;
  }

  public LiveData<Notify> getNotify() {
    return repository.getNotify();
  }
}
