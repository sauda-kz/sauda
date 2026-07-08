package com.sauda.service.notification;

/**
 * Strategy for delivering a notification through a single channel (in-app, email, push, ...). New
 * channels are added by introducing a new bean without modifying the dispatcher (Open/Closed).
 */
public interface NotificationChannel {

    void deliver(NotificationPayload payload);
}
