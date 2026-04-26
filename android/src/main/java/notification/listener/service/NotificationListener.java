package notification.listener.service;

import static notification.listener.service.NotificationUtils.getBitmapFromDrawable;
import static notification.listener.service.models.ActionCache.cachedNotifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;

import notification.listener.service.models.Action;

@SuppressLint("OverrideAbstract")
@RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private static NotificationListener instance;

    public static NotificationListener getInstance() {
        return instance;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        instance = null;
    }

    @RequiresApi(api = VERSION_CODES.KITKAT)
    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        handleNotification(notification, false);
    }

    @RequiresApi(api = VERSION_CODES.KITKAT)
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
         // Supprime l'action du cache pour libérer la mémoire
    	//ActionCache.cachedNotifications.remove(sbn.getId());
	handleNotification(sbn, true);
    }

    @RequiresApi(api = VERSION_CODES.KITKAT)
    private void handleNotification(StatusBarNotification notification, boolean isRemoved) {
        String packageName = notification.getPackageName();
        Notification notif = notification.getNotification();
        Bundle extras = notif.extras;
        boolean isOngoing = (notif.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        
        byte[] appIcon = getAppIcon(packageName);
        byte[] largeIcon = null;
        
        // On récupère l'action une seule fois
        Action action = NotificationUtils.getQuickReplyAction(notif, packageName);

        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            largeIcon = getNotificationLargeIcon(getApplicationContext(), notif);
        }

        Intent intent = new Intent(NotificationConstants.INTENT);
        intent.putExtra(NotificationConstants.PACKAGE_NAME, packageName);
        intent.putExtra(NotificationConstants.ID, notification.getId());
        intent.putExtra(NotificationConstants.CAN_REPLY, action != null);
        intent.putExtra(NotificationConstants.IS_ONGOING, isOngoing);

        if (action != null) {
            cachedNotifications.put(notification.getId(), action);
        }

        intent.putExtra(NotificationConstants.NOTIFICATIONS_ICON, appIcon);
        intent.putExtra(NotificationConstants.NOTIFICATIONS_LARGE_ICON, largeIcon);

        if (extras != null) {
            CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

            intent.putExtra(NotificationConstants.NOTIFICATION_TITLE, title == null ? null : title.toString());
            intent.putExtra(NotificationConstants.NOTIFICATION_CONTENT, text == null ? null : text.toString());
            intent.putExtra(NotificationConstants.IS_REMOVED, isRemoved);
            
            if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                intent.putExtra(NotificationConstants.HAVE_EXTRA_PICTURE, true);
                Object photo = extras.get(Notification.EXTRA_PICTURE);
                if (photo instanceof Bitmap) {
                    // Limite à 100Ko pour éviter TransactionTooLargeException
                    byte[] safeBytes = compressUnderLimit((Bitmap) photo, 100 * 1024);
                    intent.putExtra(NotificationConstants.EXTRAS_PICTURE, safeBytes);
                }
            } else {
                intent.putExtra(NotificationConstants.HAVE_EXTRA_PICTURE, false);
            }
        }
        sendBroadcast(intent);
    }

    private byte[] compressUnderLimit(Bitmap bmp, int maxBytes) {
        int quality = 90;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        // Boucle de réduction agressive pour ne pas bloquer le Binder
        while (stream.size() > maxBytes && quality > 10) {
            stream.reset();
            quality -= 15;
            if (quality < 30) { // Si la qualité baisse trop, on réduit la taille
                width *= 0.8;
                height *= 0.8;
                bmp = Bitmap.createScaledBitmap(bmp, width, height, true);
            }
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        }
        return stream.toByteArray();
    }

    public byte[] getAppIcon(String packageName) {
        try {
            Drawable icon = getPackageManager().getApplicationIcon(packageName);
            Bitmap bitmap = getBitmapFromDrawable(icon);
            if (bitmap == null) return null;
            
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @RequiresApi(api = VERSION_CODES.M)
    private byte[] getNotificationLargeIcon(Context context, Notification notification) {
        try {
            Icon largeIcon = notification.getLargeIcon();
            if (largeIcon == null) return null;

            Drawable drawable = largeIcon.loadDrawable(context);
            Bitmap bitmap = getBitmapFromDrawable(drawable); // Utilisation de ta méthode utilitaire
            
            if (bitmap == null) return null;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Error extraction large icon", e);
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public List<Map<String, Object>> getActiveNotificationData() {
        List<Map<String, Object>> notificationList = new ArrayList<>();
        try {
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            if (activeNotifications == null) return notificationList;

            for (StatusBarNotification sbn : activeNotifications) {
                Map<String, Object> notifData = new HashMap<>();
                Bundle extras = sbn.getNotification().extras;

                notifData.put("id", sbn.getId());
                notifData.put("packageName", sbn.getPackageName());
                notifData.put("title", extras.getCharSequence(Notification.EXTRA_TITLE) != null
                        ? extras.getCharSequence(Notification.EXTRA_TITLE).toString() : null);
                notifData.put("content", extras.getCharSequence(Notification.EXTRA_TEXT) != null
                        ? extras.getCharSequence(Notification.EXTRA_TEXT).toString() : null);
                notifData.put("onGoing", (sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != 0);

                notificationList.add(notifData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting active notifications", e);
        }
        return notificationList;
    }
}


