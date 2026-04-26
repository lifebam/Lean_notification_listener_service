package notification.listener.service;

import static notification.listener.service.NotificationConstants.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.RequiresApi;

import io.flutter.plugin.common.EventChannel.EventSink;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class NotificationReceiver extends BroadcastReceiver {

    private EventSink eventSink;

    public NotificationReceiver(EventSink eventSink) {
        this.eventSink = eventSink;
    }

    // Compress and scale until under maxBytes
    private byte[] compressUnderLimit(byte[] rawBytes, int maxBytes) {
        if (rawBytes == null) return null;
        Bitmap bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
        if (bmp == null) return null;

        int quality = 100;
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        while (stream.size() > maxBytes && (quality > 10 || width > 64)) {
            stream.reset();

            if (quality > 10) {
                quality -= 10; // reduce quality first
            } else {
                // scale down dimensions
                width = (int)(width * 0.8);
                height = (int)(height * 0.8);
                bmp = Bitmap.createScaledBitmap(bmp, width, height, true);
            }

            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        }

        return stream.toByteArray();
    }

    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getStringExtra(PACKAGE_NAME);
        String title = intent.getStringExtra(NOTIFICATION_TITLE);
        String content = intent.getStringExtra(NOTIFICATION_CONTENT);
        byte[] notificationIcon = intent.getByteArrayExtra(NOTIFICATIONS_ICON);
        byte[] notificationExtrasPicture = intent.getByteArrayExtra(EXTRAS_PICTURE);
        byte[] largeIcon = intent.getByteArrayExtra(NOTIFICATIONS_LARGE_ICON);
        boolean haveExtraPicture = intent.getBooleanExtra(HAVE_EXTRA_PICTURE, false);
        boolean hasRemoved = intent.getBooleanExtra(IS_REMOVED, false);
        boolean canReply = intent.getBooleanExtra(CAN_REPLY, false);
        boolean isOngoing = intent.getBooleanExtra(IS_ONGOING, false);
        int id = intent.getIntExtra(ID, -1);

        HashMap<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("packageName", packageName);
        data.put("title", title);
        data.put("content", content);

        // Apply compression to all images
        data.put("notificationIcon", compressUnderLimit(notificationIcon, 1024 * 1024));
        data.put("notificationExtrasPicture", compressUnderLimit(notificationExtrasPicture, 1024 * 1024));
        data.put("largeIcon", compressUnderLimit(largeIcon, 1024 * 1024));

        data.put("haveExtraPicture", haveExtraPicture);
        data.put("hasRemoved", hasRemoved);
        data.put("canReply", canReply);
        data.put("onGoing", isOngoing);

        eventSink.success(data);
    }
}