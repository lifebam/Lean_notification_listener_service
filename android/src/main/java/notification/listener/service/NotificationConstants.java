package notification.listener.service;

public final class NotificationConstants {
    // Empêcher l'instanciation de la classe
    private NotificationConstants() {}

    // L'action de l'intent doit être unique
    public static final String INTENT = "slayer.notification.listener.service.intent";

    // Clés pour les extras (il est préférable d'utiliser des constantes 'final')
    public static final String ID = "notification_id";
    public static final String PACKAGE_NAME = "package_name";
    public static final String NOTIFICATION_TITLE = "title";
    public static final String NOTIFICATION_CONTENT = "message";
    public static final String HAVE_EXTRA_PICTURE = "contain_image";
    public static final String EXTRAS_PICTURE = "extras_picture";
    public static final String NOTIFICATIONS_ICON = "notifications_icon";
    public static final String NOTIFICATIONS_LARGE_ICON = "notifications_large_icon";
    public static final String IS_REMOVED = "is_removed";
    public static final String CAN_REPLY = "can_reply_to_it";
    public static final String IS_ONGOING = "is_ongoing";
}

