package org.mediasoup.droid.demo.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

public class ClipboardCopy {

  public static void clipboardCopy(Context context, String content, int tipsResId) {
    ClipboardManager clipboard =
        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    ClipData clip = ClipData.newPlainText("label", content);
    clipboard.setPrimaryClip(clip);
    Toast.makeText(context, tipsResId, Toast.LENGTH_SHORT).show();
  }
}
