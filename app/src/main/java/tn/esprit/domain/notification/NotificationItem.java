package tn.esprit.domain.notification;

/**
 * Mirrors backend NotificationDto:
 *
 *  Long id;
 *  String type;
 *  String title;
 *  String message;
 *  Long appointmentId;
 *  LocalDateTime createdAt;
 *  boolean read;
 *
 * On Android we keep createdAt as ISO string.
 */
public class NotificationItem {

    private Long id;
    private String type;
    private String title;
    private String message;
    private Long appointmentId;
    private String createdAt;
    private boolean read;

    // Needed by Gson / Retrofit
    public NotificationItem() {
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    /**
     * ISO-8601 datetime string from backend.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isRead() {
        return read;
    }
}
