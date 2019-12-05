package org.mediasoup.droid.demo.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.json.JSONObject;
import org.mediasoup.droid.demo.R;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.model.Peer;
import org.webrtc.SurfaceViewRenderer;

public class PeerView extends RelativeLayout {

  public PeerView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public PeerView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public PeerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public PeerView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private CheckBox cbInfo;
  private View vStats;
  private View vMicOff;
  private View vCamOff;
  private SurfaceViewRenderer svVideoRenderer;
  private View vVideoHidden;
  private TextView tvDisplayName;
  private TextView tvDeviceVersion;

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.view_peerview, this);

    cbInfo = findViewById(R.id.info);
    vStats = findViewById(R.id.stats);
    vMicOff = findViewById(R.id.mic_off);
    vCamOff = findViewById(R.id.cam_off);
    svVideoRenderer = findViewById(R.id.video_renderer);
    svVideoRenderer.init(PeerConnectionUtils.getEglContext(), null);
    vVideoHidden = findViewById(R.id.video_hidden);
    tvDisplayName = findViewById(R.id.display_name);
    tvDeviceVersion = findViewById(R.id.device_version);
  }

  public void receive(Peer peer) {
    if (peer == null) {
      return;
    }

    JSONObject info = peer.getInfo();
    String displayName = info.optString("displayName");
    tvDisplayName.setText(displayName);

    JSONObject device = info.optJSONObject("device");
    int deviceIcon = R.drawable.ic_unknown;
    if (device != null) {
      String deviceFlag = device.optString("flag").toLowerCase();
      String deviceName = device.optString("name");
      String deviceVersion = device.optString("version");
      switch (deviceFlag) {
        case "chrome":
          deviceIcon = R.mipmap.chrome;
          break;
        case "firefox":
          deviceIcon = R.mipmap.firefox;
          break;
        case "safari":
          deviceIcon = R.mipmap.safari;
          break;
        case "opera":
          deviceIcon = R.mipmap.opera;
          break;
        case "edge":
          deviceIcon = R.mipmap.edge;
          break;
        case "android":
          deviceIcon = R.mipmap.android;
          break;
      }
      tvDeviceVersion.setText(deviceName + " " + deviceVersion);
    } else {
      tvDeviceVersion.setText("");
    }
    tvDeviceVersion.setCompoundDrawablesWithIntrinsicBounds(deviceIcon, 0, 0, 0);

    vVideoHidden.setVisibility(View.VISIBLE);
    svVideoRenderer.setVisibility(View.INVISIBLE);

    vMicOff.setVisibility(peer.isAudioMuted() ? View.VISIBLE : View.GONE);
    vCamOff.setVisibility(!peer.isVideoVisible() ? View.VISIBLE : View.GONE);
  }
}
