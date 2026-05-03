package org.example.integrations.pusher;

import com.pusher.rest.Pusher;
import org.example.utils.EnvConfig;

import java.util.Collections;

public class PusherIntegrationService {
    private static Pusher pusher;

    private static synchronized Pusher getPusher() {
        if (pusher == null) {
            String appId = EnvConfig.getOptional("PUSHER_APP_ID", "votre_app_id");
            String key = EnvConfig.getOptional("PUSHER_KEY", "votre_key");
            String secret = EnvConfig.getOptional("PUSHER_SECRET", "votre_secret");
            String cluster = EnvConfig.getOptional("PUSHER_CLUSTER", "eu");
            pusher = new Pusher(appId, key, secret);
            pusher.setCluster(cluster);
            pusher.setEncrypted(true);
        }
        return pusher;
    }

    public static void triggerReaction(int targetId, boolean like, String type) {
        try {
            String channel = "reaction-channel";
            String event = like ? "new-like" : "new-dislike";
            String data = "{\"targetId\": " + targetId + ", \"type\": \"" + type + "\"}";
            com.pusher.rest.data.Result result = getPusher().trigger(channel, event, data);
            System.out.println("Pusher event sent (API Externe): " + event + " for " + type + " ID=" + targetId + " | API Status: " + result.getStatus() + " | " + result.getMessage());
        } catch (Exception e) {
            System.err.println("Failed to send Pusher event: " + e.getMessage());
        }
    }
}
