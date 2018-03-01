package com.xiaopo.flying.photolayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.xiaopo.flying.puzzle.PuzzleView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by snowbean on 16-8-5.
 */
public class FileUtils {
  private static final String TAG = "FileUtils";

  public static String getFolderName(String name) {
    File mediaStorageDir =
        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            name);

    if (!mediaStorageDir.exists()) {
      if (!mediaStorageDir.mkdirs()) {
        return "";
      }
    }

    return mediaStorageDir.getAbsolutePath();
  }

  private static boolean isSDAvailable() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }

  public static File getNewFile(Context context, String folderName) {

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);

    String timeStamp = simpleDateFormat.format(new Date());

    String path;
    if (isSDAvailable()) {
      path = getFolderName(folderName) + File.separator + timeStamp + ".jpg";
    } else {
      path = context.getFilesDir().getPath() + File.separator + timeStamp + ".jpg";
    }

    if (TextUtils.isEmpty(path)) {
      return null;
    }

    return new File(path);
  }

  public static Bitmap createBitmap(PuzzleView puzzleView) {
    puzzleView.clearHandling();

    puzzleView.invalidate();

    Bitmap bitmap =
        Bitmap.createBitmap(puzzleView.getWidth(), puzzleView.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    puzzleView.draw(canvas);

    return bitmap;
  }

  /**
   * @param sourceView - source for the bitmap.
   * @param targetSize - width and height of the bitmap.
   *
   * @return - Bitmap based on @param sourceView with @param targetSize width and height.
   */
  @SuppressWarnings("SameParameterValue")
  @SuppressLint("InflateParams")
  public static Bitmap createScaledBitmap(PuzzleView sourceView, int targetSize) {
    Context context = sourceView.getContext();

    View view = LayoutInflater.from(context).inflate(R.layout.target_view, null);
    PuzzleView targetView = view.findViewById(R.id.puzzle_view);

    int screenWidth = getScreenWidth(context);

    float scaleDiff = (float) targetSize / screenWidth;

    PuzzleView.copyState(sourceView, targetView, scaleDiff);

    int widthSpec = View.MeasureSpec.makeMeasureSpec(targetSize, View.MeasureSpec.EXACTLY);
    int heightSpec = View.MeasureSpec.makeMeasureSpec(targetSize, View.MeasureSpec.EXACTLY);

    targetView.measure(widthSpec, heightSpec);
    targetView.layout(0, 0, targetView.getMeasuredWidth(), targetView.getMeasuredHeight());

    Canvas canvas = new Canvas();
    Bitmap bitmap = Bitmap.createBitmap(
            targetView.getWidth(), targetView.getHeight(), Bitmap.Config.ARGB_8888
    );
    canvas.setBitmap(bitmap);
    targetView.draw(canvas);

    return bitmap;
  }

  public static int getScreenWidth(Context context) {
    DisplayMetrics dm = new DisplayMetrics();
    WindowManager windowsManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    windowsManager.getDefaultDisplay().getMetrics(dm);
    return dm.widthPixels;
  }

  public static void savePuzzle(PuzzleView puzzleView, File file, int quality, Callback callback) {
    Bitmap bitmap = null;
    FileOutputStream outputStream = null;

    try {
      if (true) {
        bitmap = createScaledBitmap(puzzleView, 1080);
      } else {
        bitmap = createBitmap(puzzleView);
      }
      outputStream = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

      if (!file.exists()) {
        Log.e(TAG, "notifySystemGallery: the file do not exist.");
        return;
      }

      try {
        MediaStore.Images.Media.insertImage(puzzleView.getContext().getContentResolver(),
            file.getAbsolutePath(), file.getName(), null);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }

      puzzleView.getContext()
          .sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

      if (callback != null) {
        callback.onSuccess();
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (callback != null) {
        callback.onFailed();
      }
    } finally {
      if (bitmap != null) {
        bitmap.recycle();
      }

      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}