package org.mediasoup.droid.demo;

import android.Manifest;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.demo.vm.RoomViewModel;
import org.mediasoup.droid.lib.RoomClient;

import static org.mediasoup.droid.lib.Utils.getRandomString;

public class RoomActivity extends AppCompatActivity {

  private static final String TAG = RoomActivity.class.getSimpleName();

  private ImageView roomState;
  private RoomClient roomClient;
  private Animation animConnection;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_room);

    // TODO(HaiyangWU): load from setting activity.
    String roomId = getRandomString(8);
    String peerId = getRandomString(8);
    String displayName = getRandomString(8);
    roomClient = new RoomClient(roomId, peerId, displayName);

    String roomInfo = getString(R.string.room_info, roomId, peerId);
    Logger.d(TAG, "roomInfo: " + roomInfo);

    roomState = findViewById(R.id.room_state);
    ((TextView) findViewById(R.id.room_info)).setText(roomInfo);
    animConnection = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.ani_connecting);

    findViewById(R.id.enable_mic)
        .setOnClickListener(
            v -> {
              if (roomClient != null) {
                roomClient.enableMic(RoomActivity.this);
              }
            });
    findViewById(R.id.enable_cam)
        .setOnClickListener(
            v -> {
              if (roomClient != null) {
                roomClient.enableCam(RoomActivity.this);
              }
            });

    initViewModel();
    checkPermission();
  }

  private void initViewModel() {
    RoomViewModel roomViewModel = ViewModelProviders.of(this).get(RoomViewModel.class);

    final Observer<RoomClient.RoomState> roomStateObserver =
        state -> {
          if (RoomClient.RoomState.CONNECTING.equals(state)) {
            roomState.setImageResource(R.drawable.ic_state_connecting);
            roomState.startAnimation(animConnection);
          } else if (RoomClient.RoomState.CONNECTED.equals(state)) {
            roomState.setImageResource(R.drawable.ic_state_connected);
            animConnection.cancel();
            roomState.clearAnimation();
          } else {
            roomState.setImageResource(R.drawable.ic_state_new_close);
            animConnection.cancel();
            roomState.clearAnimation();
          }
        };
    roomViewModel.getState().observe(this, roomStateObserver);
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
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.room_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    if (item.getItemId() == R.id.setting) {
      // startActivity(new Intent(this, SettingsActivity.class));
      Toast.makeText(this, "coming soon", Toast.LENGTH_SHORT).show();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (roomClient != null) {
      roomClient.close();
    }
  }
}
