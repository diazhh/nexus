///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Event Scheduler for simulation events and phase transitions
 */
export interface ScheduledEvent {
    id: string;
    time: number;
    callback: () => void;
    description?: string;
}
export declare class EventScheduler {
    private events;
    private intervalId;
    private isRunning;
    /**
     * Schedule an event to run after a delay
     *
     * @param delayMs - Delay in milliseconds
     * @param callback - Function to execute
     * @param description - Optional description for logging
     * @returns Event ID
     */
    schedule(delayMs: number, callback: () => void, description?: string): string;
    /**
     * Schedule an event at a specific timestamp
     *
     * @param timestamp - Timestamp in ms
     * @param callback - Function to execute
     * @param description - Optional description
     * @returns Event ID
     */
    scheduleAt(timestamp: number, callback: () => void, description?: string): string;
    /**
     * Cancel a scheduled event
     */
    cancel(eventId: string): boolean;
    /**
     * Start the scheduler
     *
     * @param checkIntervalMs - How often to check for due events (default: 100ms)
     */
    start(checkIntervalMs?: number): void;
    /**
     * Stop the scheduler
     */
    stop(): void;
    /**
     * Get count of pending events
     */
    getPendingCount(): number;
    /**
     * Get next event info
     */
    getNextEvent(): ScheduledEvent | null;
    /**
     * Clear all scheduled events
     */
    clearAll(): void;
}
//# sourceMappingURL=event-scheduler.d.ts.map