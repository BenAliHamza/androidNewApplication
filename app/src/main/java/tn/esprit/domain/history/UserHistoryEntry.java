package tn.esprit.domain.history;

/**
 * Domain model for a single user history entry.
 *
 * Matches backend entity dto shape:
 *  - id
 *  - eventType
 *  - message
 *  - detailsJson (optional)
 *  - createdAt (ISO datetime)
 *  - updatedAt (optional)
 *  - deleted (optional, soft delete)
 *
 * Extra fields sent by backend but not present here will be ignored by Gson.
 */
public class UserHistoryEntry {

    private Long id;
    private String eventType;
    private String message;
    private String detailsJson;
    private String createdAt;
    private String updatedAt;
    private Boolean deleted;

    // Required empty constructor for Gson/Retrofit
    public UserHistoryEntry() {
    }

    public UserHistoryEntry(Long id,
                            String eventType,
                            String message,
                            String detailsJson,
                            String createdAt,
                            String updatedAt,
                            Boolean deleted) {
        this.id = id;
        this.eventType = eventType;
        this.message = message;
        this.detailsJson = detailsJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    /**
     * Returns a non-null message for display.
     * Falls back to the raw event type if message is missing.
     */
    public String getSafeMessage() {
        if (message != null && !message.trim().isEmpty()) {
            return message.trim();
        }
        return eventType != null ? eventType : "";
    }
}
