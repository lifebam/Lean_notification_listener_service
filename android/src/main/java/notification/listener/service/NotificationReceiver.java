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

    private byte[] resizeIcon(byte[] rawBytes) {
        if (rawBytes == null) return null;
        Bitmap original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
        if (original == null) return null;

        // Resize to 64x64 pixels (adjust as needed)
        Bitmap scaled = Bitmap.createScaledBitmap(original, 64, 64, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // Compress to PNG with 80% quality
        scaled.compress(Bitmap.CompressFormat.PNG, 80, stream);

        return stream.toByteArray();
    }

    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getStringExtra(PACKAGE_NAME);
        String title = intent.getStringExtra(NOTIFICATION_TITLE);
        String content = intent.getStringExtra(NOTIFICATION_CONTENT);
        //byte[] notificationIcon = intent.getByteArrayExtra(NOTIFICATIONS_ICON);
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

        // Use resized versions instead of raw bytes
        //data.put("notificationIcon", resizeIcon(notificationIcon));
        data.put("notificationExtrasPicture",notificationExtrasPicture);
        data.put("largeIcon", largeIcon);

        data.put("haveExtraPicture", haveExtraPicture);
        data.put("hasRemoved", hasRemoved);
        data.put("canReply", canReply);
        data.put("onGoing", isOngoing);

        eventSink.success(data);
    }
}
