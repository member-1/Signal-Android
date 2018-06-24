package org.thoughtcrime.securesms.components.emoji.parsing;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import org.thoughtcrime.securesms.logging.Log;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.components.emoji.EmojiPageModel;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.util.ListenableFutureTask;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;

import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;

public class EmojiPageBitmap {

  private static final String TAG = EmojiPageBitmap.class.getName();

  private final Context        context;
  private final EmojiPageModel model;
  private final float          decodeScale;

  private SoftReference<Bitmap>        bitmapReference;
  private ListenableFutureTask<Bitmap> task;

  public EmojiPageBitmap(@NonNull Context context, @NonNull EmojiPageModel model, float decodeScale) {
    this.context     = context.getApplicationContext();
    this.model       = model;
    this.decodeScale = decodeScale;
  }

  public ListenableFutureTask<Bitmap> get() {
    Util.assertMainThread();

    if (bitmapReference != null && bitmapReference.get() != null) {
      return new ListenableFutureTask<>(bitmapReference.get());
    } else if (task != null) {
      return task;
    } else {
      Callable<Bitmap> callable = () -> {
        try {
          Log.i(TAG, "loading page " + model.getSprite());
          return loadPage();
        } catch (Exception e) {
          Log.w(TAG, e);
        }
        return null;
      };
      task = new ListenableFutureTask<>(callable);
      new AsyncTask<Void, Void, Void>() {
        @Override protected Void doInBackground(Void... params) {
          task.run();
          return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
          task = null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    return task;
  }

  private Bitmap loadPage() {
    if (bitmapReference != null && bitmapReference.get() != null) return bitmapReference.get();
    Bitmap bitmap = null;
    switch (TextSecurePreferences.getEmojiStyle(context)) {
      case 1:
        bitmap = fromExternalAssets(ConversationListActivity.EMOJI_ANDROID);
        break;
      case 2:
        bitmap = fromExternalAssets(ConversationListActivity.EMOJI_TWITTER);
        break;
      case 3:
        bitmap = fromExternalAssets(ConversationListActivity.EMOJI_EMOJIONE);
        break;
    }
    if (bitmap == null) {
      // The selected emoji is either missing or we've selected to use iOS
      bitmap = fromAssets();
    }

    bitmapReference = new SoftReference<>(bitmap);
    Log.i(TAG, "onPageLoaded(" + model.getSprite() + ")");
    return bitmap;
  }

  private Bitmap fromAssets() {
    try {
      Bitmap bitmap = GlideApp.with(context.getApplicationContext())
                              .asBitmap()
                              .load("file:///android_asset/" + model.getSprite())
                              .skipMemoryCache(true)
                              .diskCacheStrategy(DiskCacheStrategy.NONE)
                              .submit()
                              .get();
      return Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * decodeScale), (int)(bitmap.getHeight() * decodeScale), false);
    } catch (Exception e) {
      Log.w(TAG, e);
    }
    return null;
  }

  private Bitmap fromExternalAssets(String packageName) {
    try {
      PackageManager packageManager = context.getPackageManager();
      if (packageManager.getPackageInfo(packageName, 0).versionCode >= getMinimumVersion()) {
        setEmojiUpdateTime(context, packageManager.getPackageInfo(packageName, 0).lastUpdateTime);
        Resources res = packageManager.getResourcesForApplication(packageName);
        Bitmap bitmap = GlideApp.with(context.getApplicationContext())
                                .asBitmap()
                                .load(BitmapFactory.decodeStream(res.getAssets().open(model.getSprite())))
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .submit()
                                .get();
        return Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * decodeScale), (int)(bitmap.getHeight() * decodeScale), false);
      }
    } catch (Exception e) {
      // Don't log info about a missing apk
    }
    return null;
  }

  private int getMinimumVersion() {
    return context.getResources().getInteger(R.integer.emoji_version);
  }

  private void setEmojiUpdateTime(Context context, long value) {
    TextSecurePreferences.setEmojiLastUpdateTime(context, value);
  }

  @Override
  public String toString() {
    return model.getSprite();
  }
}
