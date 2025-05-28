package com.djj.todoscheduleserver.service;

public interface ReminderService {
    
    /**
     * Check for upcoming tasks and send reminders
     */
    void checkAndSendReminders();
} 