package org.mediasoup.droid.demo;

import android.Manifest;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.widget.TextView;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.demo.vm.Room;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomRepository;
import org.mediasoup.droid.lib.model.RoomInfo;

import static org.mediasoup.droid.lib.Utils.getRandomString;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private String roomId;
  private String peerId;
  private String displayName;
  private RoomClient roomClient;
  private TextView roomState;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // TODO(HaiyangWU): load from setting activity.
    roomId = getRandomString(8);
    peerId = getRandomString(8);
    displayName = getRandomString(8);
    roomClient = new RoomClient(roomId, peerId, displayName);

    String roomInfo = getString(R.string.room_info, roomId, peerId);
    Logger.d(TAG, "roomInfo: " + roomInfo);
    ((TextView) findViewById(R.id.room_info)).setText(roomInfo);
    roomState = findViewById(R.id.room_state);

    findViewById(R.id.enable_mic)
        .setOnClickListener(
            v -> {
              if (roomClient != null) {
                roomClient.enableMic(MainActivity.this);
              }
            });
    findViewById(R.id.enable_cam)
        .setOnClickListener(
            v -> {
              if (roomClient != null) {
                roomClient.enableCam(MainActivity.this);
              }
            });

    initViewModel();
    checkPermission();
  }

  private void initViewModel() {
    RoomRepository roomRepository = roomClient.getRoomRepository();
    final Observer<RoomInfo> roomInfoObserver =
        info -> roomState.setText(info.getState().name().toLowerCase());
    roomRepository.getRoomInfo().observe(this, roomInfoObserver);

    // TODO(HaiyangWu): use view model.
//    Room room = ViewModelProviders.of(this).get(Room.class);
//    room.getRoomInfo().observe(this, roomInfoObserver);
  }

  private PermissionHandler permissionHandler =
      new PermissionHandler() {
        @Override
        public void onGranted() {
          Logger.d(TAG, "permission granted");
          roomClient.join();
        }
      };

  private void checkPermission() {
    String[] permissions = {
      Manifest.permission.INTERNET,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.CAMERA,
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    String rationale = "Please provide permissions";
    Permissions.Options options =
        new Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning");
    Permissions.check(this, permissions, rationale, options, permissionHandler);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (roomClient != null) {
      roomClient.close();
    }
  }
}
