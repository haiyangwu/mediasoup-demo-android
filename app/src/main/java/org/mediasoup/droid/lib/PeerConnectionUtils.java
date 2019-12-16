package org.mediasoup.droid.lib;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.MainThread;

import org.mediasoup.droid.Logger;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

@SuppressWarnings("WeakerAccess")
public class PeerConnectionUtils {

  private static final String TAG = "PeerConnectionUtils";
  private static EglBase mEglBase;
  private static PeerConnectionFactory mPeerConnectionFactory;

  private static AudioSource mAudioSource;
  private static VideoSource mVideoSource;
  private static CameraVideoCapturer mCamCapture;
  private static String mPreferCameraFace;

  // PeerConnection factory creation.
  static void createPeerConnectionFactory(Context context) {
    Logger.d(TAG, "createPeerConnectionFactory()");
    PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();
    builder.setOptions(null);

    if (mEglBase == null) {
      mEglBase = EglBase.create();
    }

    AudioDeviceModule adm = createJavaAudioDevice(context);
    VideoEncoderFactory encoderFactory =
        new DefaultVideoEncoderFactory(
            mEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, true);
    VideoDecoderFactory decoderFactory =
        new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());

    mPeerConnectionFactory =
        builder
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory();
  }

  public static EglBase.Context getEglContext() {
    if (mEglBase == null) {
      mEglBase = EglBase.create();
    }
    return mEglBase.getEglBaseContext();
  }

  static AudioDeviceModule createJavaAudioDevice(Context appContext) {
    // Enable/disable OpenSL ES playback.
    // Set audio record error callbacks.
    JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback =
        new JavaAudioDeviceModule.AudioRecordErrorCallback() {
          @Override
          public void onWebRtcAudioRecordInitError(String errorMessage) {
            Logger.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
          }

          @Override
          public void onWebRtcAudioRecordStartError(
              JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
            Logger.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
          }

          @Override
          public void onWebRtcAudioRecordError(String errorMessage) {
            Logger.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
          }
        };

    JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback =
        new JavaAudioDeviceModule.AudioTrackErrorCallback() {
          @Override
          public void onWebRtcAudioTrackInitError(String errorMessage) {
            Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackStartError(
              JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
            Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackError(String errorMessage) {
            Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
          }
        };

    return JavaAudioDeviceModule.builder(appContext)
        .setAudioRecordErrorCallback(audioRecordErrorCallback)
        .setAudioTrackErrorCallback(audioTrackErrorCallback)
        .createAudioDeviceModule();
  }

  @SuppressWarnings("unused")
  public static PeerConnectionFactory getPeerConnectionFactory(Context context) {
    if (mPeerConnectionFactory == null) {
      createPeerConnectionFactory(context);
    }
    return mPeerConnectionFactory;
  }

  // Audio source creation.
  private static void createAudioSource(Context context) {
    Logger.d(TAG, "createAudioSource()");
    if (mPeerConnectionFactory == null) {
      createPeerConnectionFactory(context);
    }

    mAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
  }

  @MainThread
  public static void setPreferCameraFace(String preferCameraFace) {
    mPreferCameraFace = preferCameraFace;
  }

  private static void createCamCapture(Context context) {
    boolean isCamera2Supported = Camera2Enumerator.isSupported(context);
    CameraEnumerator cameraEnumerator;

    if (isCamera2Supported) {
      cameraEnumerator = new Camera2Enumerator(context);
    } else {
      cameraEnumerator = new Camera1Enumerator();
    }
    final String[] deviceNames = cameraEnumerator.getDeviceNames();
    for (String deviceName : deviceNames) {
      boolean needFrontFacing = "front".endsWith(mPreferCameraFace);
      String selectedDeviceName = null;
      if (needFrontFacing) {
        if (cameraEnumerator.isFrontFacing(deviceName)) {
          selectedDeviceName = deviceName;
        }
      } else {
        if (!cameraEnumerator.isFrontFacing(deviceName)) {
          selectedDeviceName = deviceName;
        }
      }

      if (!TextUtils.isEmpty(selectedDeviceName)) {
        mCamCapture =
            cameraEnumerator.createCapturer(
                selectedDeviceName,
                new CameraVideoCapturer.CameraEventsHandler() {
                  @Override
                  public void onCameraError(String s) {
                    Logger.e(TAG, "onCameraError, " + s);
                  }

                  @Override
                  public void onCameraDisconnected() {
                    Logger.w(TAG, "onCameraDisconnected");
                  }

                  @Override
                  public void onCameraFreezed(String s) {
                    Logger.w(TAG, "onCameraFreezed, " + s);
                  }

                  @Override
                  public void onCameraOpening(String s) {
                    Logger.d(TAG, "onCameraOpening, " + s);
                  }

                  @Override
                  public void onFirstFrameAvailable() {
                    Logger.d(TAG, "onFirstFrameAvailable");
                  }

                  @Override
                  public void onCameraClosed() {
                    Logger.d(TAG, "onCameraClosed");
                  }
                });
        break;
      }
    }

    if (mCamCapture == null) {
      throw new IllegalStateException("Failed to create Camera Capture");
    }
  }

  @MainThread
  public static void switchCam(CameraVideoCapturer.CameraSwitchHandler switchHandler) {
    if (mCamCapture != null) {
      mCamCapture.switchCamera(switchHandler);
    }
  }

  // Video source creation.
  @MainThread
  private static void createVideoSource(Context context) {
    if (mPeerConnectionFactory == null) {
      createPeerConnectionFactory(context);
    }
    if (mCamCapture == null) {
      createCamCapture(context);
    }

    mVideoSource = mPeerConnectionFactory.createVideoSource(false);
    SurfaceTextureHelper surfaceTextureHelper =
        SurfaceTextureHelper.create("CaptureThread", mEglBase.getEglBaseContext());

    mCamCapture.initialize(surfaceTextureHelper, context, mVideoSource.getCapturerObserver());
    mCamCapture.startCapture(640, 480, 30);
  }

  // Audio track creation.
  @MainThread
  public static AudioTrack createAudioTrack(Context context, String id) {
    Logger.d(TAG, "createAudioTrack()");
    if (mAudioSource == null) {
      createAudioSource(context);
    }
    return mPeerConnectionFactory.createAudioTrack(id, mAudioSource);
  }

  // Video track creation.
  @MainThread
  public static VideoTrack createVideoTrack(Context context, String id) {
    if (mVideoSource == null) {
      createVideoSource(context);
    }

    return mPeerConnectionFactory.createVideoTrack(id, mVideoSource);
  }

  @MainThread
  public static void dispose() {
    if (mVideoSource != null) {
      mVideoSource.dispose();
      mVideoSource = null;
    }
    if (mAudioSource != null) {
      mAudioSource.dispose();
      mAudioSource = null;
    }
    if (mCamCapture != null) {
      mCamCapture.dispose();
      mCamCapture = null;
    }
  }
}
