package org.mediasoup.droid.demo.vm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomContext;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.RoomInfo;

public class MineViewProps extends AndroidViewModel {

  public LiveData<Boolean> connected;
  public LiveData<Me> me;
  public LiveData<Producer> audioProducer;
  public LiveData<Producer> videoProducer;
  public LiveData<Boolean> faceDetection;

  public MineViewProps(@NonNull Application application) {
    super(application);

    RoomContext repository = RoomContext.getInstance();
    connected =
        Transformations.map(
            repository.getRoomInfo(),
            roomInfo -> RoomClient.RoomState.CONNECTED.equals(roomInfo.getState()));
    me = repository.getMe();
    audioProducer =
        Transformations.map(repository.getProducers(), producers -> producers.filter("audio"));
    videoProducer =
        Transformations.map(repository.getProducers(), producers -> producers.filter("video"));
    faceDetection = Transformations.map(repository.getRoomInfo(), RoomInfo::isFaceDetection);
  }
}
