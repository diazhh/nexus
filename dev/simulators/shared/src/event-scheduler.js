/*
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"use strict";
/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Event Scheduler for simulation events and phase transitions
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.EventScheduler = void 0;
class EventScheduler {
    events = [];
    intervalId = null;
    isRunning = false;
    /**
     * Schedule an event to run after a delay
     *
     * @param delayMs - Delay in milliseconds
     * @param callback - Function to execute
     * @param description - Optional description for logging
     * @returns Event ID
     */
    schedule(delayMs, callback, description) {
        const id = `evt-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        const event = {
            id,
            time: Date.now() + delayMs,
            callback,
            description
        };
        this.events.push(event);
        this.events.sort((a, b) => a.time - b.time);
        if (description) {
            console.log(`📅 Scheduled event "${description}" in ${(delayMs / 1000).toFixed(1)}s`);
        }
        return id;
    }
    /**
     * Schedule an event at a specific timestamp
     *
     * @param timestamp - Timestamp in ms
     * @param callback - Function to execute
     * @param description - Optional description
     * @returns Event ID
     */
    scheduleAt(timestamp, callback, description) {
        const delayMs = timestamp - Date.now();
        if (delayMs < 0) {
            console.warn('⚠️  Cannot schedule event in the past');
            callback(); // Execute immediately
            return 'immediate';
        }
        return this.schedule(delayMs, callback, description);
    }
    /**
     * Cancel a scheduled event
     */
    cancel(eventId) {
        const index = this.events.findIndex(e => e.id === eventId);
        if (index !== -1) {
            const event = this.events[index];
            this.events.splice(index, 1);
            console.log(`❌ Cancelled event: ${event.description || eventId}`);
            return true;
        }
        return false;
    }
    /**
     * Start the scheduler
     *
     * @param checkIntervalMs - How often to check for due events (default: 100ms)
     */
    start(checkIntervalMs = 100) {
        if (this.isRunning) {
            console.warn('⚠️  Scheduler already running');
            return;
        }
        this.isRunning = true;
        console.log('▶️  Event scheduler started');
        this.intervalId = setInterval(() => {
            const now = Date.now();
            while (this.events.length > 0 && this.events[0].time <= now) {
                const event = this.events.shift();
                if (event.description) {
                    console.log(`⏰ Executing event: ${event.description}`);
                }
                try {
                    event.callback();
                }
                catch (error) {
                    console.error(`❌ Error executing event ${event.id}:`, error);
                }
            }
        }, checkIntervalMs);
    }
    /**
     * Stop the scheduler
     */
    stop() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
        this.isRunning = false;
        console.log('⏸️  Event scheduler stopped');
    }
    /**
     * Get count of pending events
     */
    getPendingCount() {
        return this.events.length;
    }
    /**
     * Get next event info
     */
    getNextEvent() {
        return this.events.length > 0 ? this.events[0] : null;
    }
    /**
     * Clear all scheduled events
     */
    clearAll() {
        const count = this.events.length;
        this.events = [];
        console.log(`🗑️  Cleared ${count} scheduled events`);
    }
}
exports.EventScheduler = EventScheduler;
//# sourceMappingURL=event-scheduler.js.map