package notification.listener.service;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import notification.listener.service.models.Action;

public final class NotificationUtils {

    private static final String[] REPLY_KEYWORDS = {"reply", "android.intent.extra.text"};
    private static final CharSequence INPUT_KEYWORD = "input";

    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
    if (drawable == null) {
        return null;
    }

    // Si c'est déjà un BitmapDrawable, on peut extraire le bitmap directement
    if (drawable instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        if (bitmapDrawable.getBitmap() != null) {
            return bitmapDrawable.getBitmap();
        }
    }

    // Gestion sécurisée de la taille (évite le crash si intrinsicWidth == -1)
    int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
    int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;

    // On limite la taille pour les icônes très larges pour économiser la mémoire
    int maxWidth = 512;
    if (width > maxWidth || height > maxWidth) {
        float ratio = (float) width / height;
        if (width > height) {
            width = maxWidth;
            height = (int) (width / ratio);
        } else {
            height = maxWidth;
            width = (int) (height * ratio);
        }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);

    return bitmap;
}


    public static boolean isPermissionGranted(Context context) {
        String packageName = context.getPackageName();
        String flat = Settings.Secure.getString(context.getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName componentName = ComponentName.unflattenFromString(name);
                boolean nameMatch = TextUtils.equals(packageName, componentName.getPackageName());
                if (nameMatch) {
                    return true;
                }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static Action getQuickReplyAction(Notification n, String packageName) {
        NotificationCompat.Action action = null;
        if (Build.VERSION.SDK_INT >= 24)
            action = getQuickReplyAction(n);
        if (action == null)
            action = getWearReplyAction(n);
        if (action == null)
            return null;
        return new Action(action, packageName, true);
    }

    private static NotificationCompat.Action getQuickReplyAction(Notification n) {
        for (int i = 0; i < NotificationCompat.getActionCount(n); i++) {
            NotificationCompat.Action action = NotificationCompat.getAction(n, i);
            if (action.getRemoteInputs() != null) {
                for (int x = 0; x < action.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = action.getRemoteInputs()[x];
                    if (isKnownReplyKey(remoteInput.getResultKey()))
                        return action;
                }
            }
        }
        return null;
    }

    private static NotificationCompat.Action getWearReplyAction(Notification n) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(n);
        for (NotificationCompat.Action action : wearableExtender.getActions()) {
            if (action.getRemoteInputs() != null) {
                for (int x = 0; x < action.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = action.getRemoteInputs()[x];
                    if (isKnownReplyKey(remoteInput.getResultKey()))
                        return action;
                    else if (remoteInput.getResultKey().toLowerCase().contains(INPUT_KEYWORD))
                        return action;
                }
            }
        }
        return null;
    }

    private static boolean isKnownReplyKey(String resultKey) {
        if (TextUtils.isEmpty(resultKey))
            return false;

        resultKey = resultKey.toLowerCase();
        for (String keyword : REPLY_KEYWORDS)
            if (resultKey.contains(keyword))
                return true;

        return false;
    }

}
