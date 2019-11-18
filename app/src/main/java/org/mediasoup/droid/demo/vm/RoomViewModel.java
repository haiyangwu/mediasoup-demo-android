package org.mediasoup.droid.demo.vm;

import android.app.Application;
import android.net.Uri;
import android.net.UrlQuerySanitizer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import org.mediasoup.droid.demo.R;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomRepository;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomViewModel extends AndroidViewModel {

  private LiveData<String> invitationLink;
  private LiveData<String> roomInfo;
  private LiveData<RoomClient.RoomState> state;

  public RoomViewModel(@NonNull Application application) {
    super(application);

    RoomRepository repository = RoomRepository.getInstance();
    invitationLink = Transformations.map(repository.getRoomInfo(), RoomInfo::getUrl);
    roomInfo =
        Transformations.map(
            repository.getRoomInfo(),
            roomInfo ->
                application.getString(
                    R.string.room_info, roomInfo.getRoomId(), roomInfo.getPeerId()));
    state = Transformations.map(repository.getRoomInfo(), RoomInfo::getState);
  }

  public LiveData<String> getInvitationLink() {
    return invitationLink;
  }

  public LiveData<String> getRoomInfo() {
    return roomInfo;
  }

  public LiveData<RoomClient.RoomState> getState() {
    return state;
  }
}
