package org.mediasoup.droid.demo;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.demo.adapter.PeerAdapter;
import org.mediasoup.droid.demo.view.MineView;
import org.mediasoup.droid.demo.vm.MineViewProps;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.demo.vm.RoomProps;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.model.Notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mediasoup.droid.lib.Utils.getRandomString;

public class RoomActivity extends AppCompatActivity {

  private static final String TAG = RoomActivity.class.getSimpleName();

  private String roomId, peerId, displayName;
  private boolean forceH264, forceVP9;

  private RoomClient.RoomOptions options = new RoomClient.RoomOptions();
  private RoomClient roomClient;

  private TextView invitationLink;
  private ImageView roomState;
  private MineView mineView;
  private RecyclerView rcRemotePeers;
  private Animation animConnection;
  private PeerAdapter peerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_room);

    loadRoomConfig();
    // TODO: save RoomClient instance to RoomContext.
    roomClient = new RoomClient(this, roomId, peerId, displayName, forceH264, forceVP9, options);

    invitationLink = findViewById(R.id.invitation_link);
    roomState = findViewById(R.id.room_state);
    mineView = findViewById(R.id.mine_view);
    rcRemotePeers = findViewById(R.id.remote_peers);
    animConnection = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.ani_connecting);

    rcRemotePeers.setLayoutManager(new LinearLayoutManager(this));
    peerAdapter = new PeerAdapter();
    rcRemotePeers.setAdapter(peerAdapter);

    invitationLink.setOnClickListener(
        view -> {
          ClipboardManager clipboard =
              (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("label", (String) view.getTag());
          clipboard.setPrimaryClip(clip);
          Toast.makeText(this, "Room link copied to the clipboard", Toast.LENGTH_SHORT).show();
        });

    initViewModel();
    checkPermission();
  }

  private void loadRoomConfig() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Room initial config.
    roomId = preferences.getString("roomId", "");
    peerId = preferences.getString("peerId", "");
    displayName = preferences.getString("displayName", "");
    forceH264 = preferences.getBoolean("forceH264", false);
    forceVP9 = preferences.getBoolean("forceVP9", false);
    if (TextUtils.isEmpty(roomId)) {
      roomId = getRandomString(8);
      preferences.edit().putString("roomId", roomId).apply();
    }
    if (TextUtils.isEmpty(peerId)) {
      peerId = getRandomString(8);
      preferences.edit().putString("peerId", peerId).apply();
    }
    if (TextUtils.isEmpty(displayName)) {
      displayName = getRandomString(8);
      preferences.edit().putString("displayName", displayName).apply();
    }

    // Room action config.
    // TODO(HaiyangWu): default produce/consume true when prepared.
    options.setProduce(preferences.getBoolean("produce", false));
    options.setConsume(preferences.getBoolean("consume", false));
    options.setForceTcp(preferences.getBoolean("forceTcp", false));

    // Device config.
    // TODO(HaiyangWu): apply this to local video source.
    String camera = preferences.getString("camera", "front");
  }

  private void initViewModel() {
    RoomProps roomProps = ViewModelProviders.of(this).get(RoomProps.class);

    // Invitation Link.
    final Observer<String> linkObserver =
        link -> {
          if (TextUtils.isEmpty(link)) {
            invitationLink.setVisibility(View.INVISIBLE);
          } else {
            invitationLink.setVisibility(View.VISIBLE);
            invitationLink.setTag(link);
          }
        };
    roomProps.getInvitationLink().observe(this, linkObserver);

    // Room state.
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
    roomProps.getState().observe(this, roomStateObserver);

    // me
    MineViewProps mineViewProps = ViewModelProviders.of(this).get(MineViewProps.class);
    mineView.bind(this, mineViewProps);

    // Peers
    roomProps
        .getPeersInfo()
        .observe(
            this,
            remotePeers -> {
              Map<String, JSONObject> item = remotePeers.getPeersInfo();
              List<Peer> peers = new ArrayList<>();
              if (item.isEmpty()) {
                rcRemotePeers.setVisibility(View.GONE);
                roomState.setVisibility(View.VISIBLE);
                peerAdapter.replacePeers(peers);
              } else {
                for (Map.Entry<String, JSONObject> info : item.entrySet()) {
                  peers.add(new Peer(info.getValue()));
                }
                peerAdapter.replacePeers(peers);
                rcRemotePeers.setVisibility(View.VISIBLE);
                roomState.setVisibility(View.GONE);
              }
            });

    // Notify
    final Observer<Notify> notifyObserver =
        notify -> {
          if (notify == null) {
            return;
          }
          if ("error".equals(notify.getType())) {
            Toast toast = Toast.makeText(this, notify.getText(), notify.getTimeout());
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.RED);
            toast.show();
          } else {
            Toast.makeText(this, notify.getText(), notify.getTimeout()).show();
          }
        };

    roomProps.getNotify().observe(this, notifyObserver);
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
      startActivity(new Intent(this, SettingsActivity.class));
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
