package org.mediasoup.droid.demo.vm;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomRepository;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomViewModel extends ViewModel {

  private RoomRepository repository = RoomRepository.getInstance();

  private LiveData<RoomClient.RoomState> state;

  public RoomViewModel() {
    state = Transformations.map(repository.getRoomInfo(), RoomInfo::getState);
  }

  public LiveData<RoomClient.RoomState> getState() {
    return state;
  }
}
