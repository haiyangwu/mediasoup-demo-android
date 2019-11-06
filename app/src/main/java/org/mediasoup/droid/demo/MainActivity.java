package org.mediasoup.droid.demo;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.RoomClient;

import static org.mediasoup.droid.lib.Utils.getRandomString;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();
  private String roomId;
  private String peerId;
  private String displayName;
  private RoomClient roomClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    roomId = "poqscfd6";//getRandomString(8);
    peerId = getRandomString(8);
    displayName = getRandomString(8);
    checkPermission();
  }

  private PermissionHandler permissionHandler =
      new PermissionHandler() {
        @Override
        public void onGranted() {
          Logger.d(TAG, "permission granted");
          roomClient = new RoomClient(roomId, peerId, displayName);
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
