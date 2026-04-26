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

    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (eventSink == null) return;

        HashMap<String, Object> data = new HashMap<>();
        
        // Extraction simple des données
        data.put("id", intent.getIntExtra(ID, -1));
        data.put("packageName", intent.getStringExtra(PACKAGE_NAME));
        data.put("title", intent.getStringExtra(NOTIFICATION_TITLE));
        data.put("content", intent.getStringExtra(NOTIFICATION_CONTENT));
        
        // On passe les byte[] directement (ils ont déjà été compressés par le Service)
        data.put("notificationIcon", intent.getByteArrayExtra(NOTIFICATIONS_ICON));
        data.put("notificationExtrasPicture", intent.getByteArrayExtra(EXTRAS_PICTURE));
        data.put("largeIcon", intent.getByteArrayExtra(NOTIFICATIONS_LARGE_ICON));

        data.put("haveExtraPicture", intent.getBooleanExtra(HAVE_EXTRA_PICTURE, false));
        data.put("hasRemoved", intent.getBooleanExtra(IS_REMOVED, false));
        data.put("canReply", intent.getBooleanExtra(CAN_REPLY, false));
        data.put("onGoing", intent.getBooleanExtra(IS_ONGOING, false));

        // Envoi vers Flutter
        eventSink.success(data);
    }
}
