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
import org.mediasoup.droid.lib.model.Me;
import org.webrtc.SurfaceViewRenderer;

public class MineView extends RelativeLayout {

  public MineView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public MineView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public MineView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public MineView(
      @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private CheckBox cbInfo;
  private View vStats;
  private CheckBox cbMic;
  private CheckBox cbCam;
  private CheckBox cbChangeCam;
  private SurfaceViewRenderer svVideoRenderer;
  private View vVideoHidden;
  private TextView tvDisplayName;
  private TextView tvDeviceVersion;

  private void init(Context context) {
    LayoutInflater.from(context).inflate(R.layout.view_mineview, this);

    cbInfo = findViewById(R.id.info);
    vStats = findViewById(R.id.stats);
    cbMic = findViewById(R.id.mic);
    cbCam = findViewById(R.id.cam);
    cbChangeCam = findViewById(R.id.change_cam);
    svVideoRenderer = findViewById(R.id.video_renderer);
    vVideoHidden = findViewById(R.id.video_hidden);
    tvDisplayName = findViewById(R.id.display_name);
    tvDeviceVersion = findViewById(R.id.device_version);
  }

  public void bind(Me me) {
    if (me == null) {
      return;
    }

    tvDisplayName.setText(me.getDisplayName());

    JSONObject device = me.getDevice();
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

    if (me.isCanSendMic()) {
      cbMic.setVisibility(View.VISIBLE);
    } else {
      cbMic.setVisibility(View.GONE);
    }

    if (me.isCanSendCam()) {
      cbCam.setVisibility(View.VISIBLE);
    } else {
      cbCam.setVisibility(View.VISIBLE);
    }

    cbChangeCam.setEnabled(!me.isCamInProgress());
    if (me.isCanChangeCam()) {
      cbCam.setVisibility(View.VISIBLE);
    } else {
      cbCam.setVisibility(View.VISIBLE);
    }
  }
}
