package tn.esprit.data.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.notification.NotificationItem;

/**
 * Very small STOMP-over-WebSocket client for notifications.
 *
 * Connects to:
 *   ws://10.0.2.2:8080/ws-mobile
 *
 * Subscribes to:
 *   /topic/users/{userId}/appointments
 *
 * When a MESSAGE frame arrives, it parses the body as NotificationItem
 * and notifies the registered listener.
 */
public class NotificationSocketManager {

    private static final String TAG = "NotificationSocket";
    // IMPORTANT: use the native WebSocket endpoint (no SockJS)
    private static final String WS_URL = "ws://10.0.2.2:8080/ws-mobile";
    private static final char STOMP_NULL = '\u0000';

    public interface Listener {
        void onNotification(@Nullable NotificationItem item);
    }

    private static NotificationSocketManager instance;

    public static synchronized NotificationSocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationSocketManager(context.getApplicationContext());
        }
        return instance;
    }

    private final Context appContext;
    private final OkHttpClient okHttpClient;
    private final AuthLocalDataSource authLocalDataSource;
    private final Gson gson;

    @Nullable
    private WebSocket webSocket;

    private long currentUserId = -1L;
    private boolean connected = false;
    private boolean subscribed = false;

    @Nullable
    private Listener listener;

    private NotificationSocketManager(Context appContext) {
        this.appContext = appContext;
        this.okHttpClient = new OkHttpClient.Builder().build();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.gson = new Gson();
    }

    /**
     * Start (or restart) the WebSocket connection for the given user id.
     * Safe to call multiple times; any existing connection is closed first.
     */
    public synchronized void connect(long userId) {
        currentUserId = userId;

        // Close previous socket if any
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Reconnecting");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
        connected = false;
        subscribed = false;

        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            Log.w(TAG, "connect: no auth tokens, skipping WebSocket connect");
            return;
        }

        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        String authHeader = type + " " + tokens.getAccessToken();

        Request request = new Request.Builder()
                .url(WS_URL)
                .addHeader("Authorization", authHeader)
                .build();

        webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                Log.d(TAG, "WebSocket onOpen");
                connected = true;
                subscribed = false;
                sendConnectFrame(webSocket);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // There may be multiple frames separated by the NULL char
                String[] frames = text.split(String.valueOf(STOMP_NULL));
                for (String frame : frames) {
                    if (frame.trim().isEmpty()) continue;
                    handleFrame(frame);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket onClosed: " + code + " / " + reason);
                connected = false;
                subscribed = false;
                NotificationSocketManager.this.webSocket = null;
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable okhttp3.Response response) {
                Log.e(TAG, "WebSocket onFailure", t);
                connected = false;
                subscribed = false;
                NotificationSocketManager.this.webSocket = null;
            }
        });
    }

    /**
     * Stop the WebSocket connection, if any.
     */
    public synchronized void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Client disconnect");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
        connected = false;
        subscribed = false;
    }

    /**
     * Register a listener to receive new NotificationItem pushes.
     * Passing null clears the listener.
     */
    public synchronized void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // STOMP helpers
    // -------------------------------------------------------------------------

    private void sendConnectFrame(WebSocket ws) {
        // Minimal STOMP CONNECT frame
        StringBuilder sb = new StringBuilder();
        sb.append("CONNECT\n");
        sb.append("accept-version:1.1,1.2\n");
        sb.append("heart-beat:10000,10000\n");
        sb.append("\n");
        sb.append(STOMP_NULL);
        ws.send(sb.toString());
    }

    private void sendSubscribeFrame(WebSocket ws) {
        if (currentUserId <= 0L) return;

        String destination = "/topic/users/" + currentUserId + "/appointments";

        StringBuilder sb = new StringBuilder();
        sb.append("SUBSCRIBE\n");
        sb.append("id:sub-appointments\n");
        sb.append("destination:").append(destination).append("\n");
        sb.append("\n");
        sb.append(STOMP_NULL);

        ws.send(sb.toString());
    }

    private void handleFrame(String frame) {
        String[] lines = frame.split("\n");
        int index = 0;

        // Skip any leading empty lines
        while (index < lines.length && lines[index].trim().isEmpty()) {
            index++;
        }
        if (index >= lines.length) return;

        String command = lines[index].trim();
        index++;

        Map<String, String> headers = new HashMap<>();
        // Read headers until empty line
        for (; index < lines.length; index++) {
            String line = lines[index];
            if (line.trim().isEmpty()) {
                index++;
                break;
            }
            int colonPos = line.indexOf(':');
            if (colonPos > 0) {
                String key = line.substring(0, colonPos).trim();
                String value = line.substring(colonPos + 1).trim();
                headers.put(key, value);
            }
        }

        // Remaining lines are body
        StringBuilder bodyBuilder = new StringBuilder();
        for (; index < lines.length; index++) {
            bodyBuilder.append(lines[index]);
            if (index < lines.length - 1) {
                bodyBuilder.append('\n');
            }
        }
        String body = bodyBuilder.toString();

        if ("CONNECTED".equalsIgnoreCase(command)) {
            Log.d(TAG, "STOMP CONNECTED");
            if (webSocket != null && !subscribed) {
                sendSubscribeFrame(webSocket);
                subscribed = true;
            }
        } else if ("MESSAGE".equalsIgnoreCase(command)) {
            handleMessageFrame(headers, body);
        } else if ("ERROR".equalsIgnoreCase(command)) {
            Log.e(TAG, "STOMP ERROR frame: " + body);
        }
    }

    private void handleMessageFrame(Map<String, String> headers, String body) {
        try {
            NotificationItem item = gson.fromJson(body, NotificationItem.class);
            Listener l;
            synchronized (this) {
                l = this.listener;
            }
            if (l != null) {
                l.onNotification(item);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse notification JSON", e);
        }
    }
}
