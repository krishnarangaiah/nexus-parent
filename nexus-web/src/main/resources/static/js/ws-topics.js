/**
 * WebSocket Topic Constants
 *
 * This file mirrors the server-side WebSocketTopics.java
 * Keep these in sync with the Java constants!
 *
 * Usage:
 *   const connector = new PageWebSocket({
 *       topic: WsTopics.DASHBOARD_KPI,
 *       onMessage: function(data) { ... }
 *   });
 */
(function(global) {
    'use strict';

    const WsTopics = {
        // ========== Dashboard Topics ==========
        DASHBOARD: '/topic/dashboard',
        DASHBOARD_KPI: '/topic/dashboard/kpi',
        DASHBOARD_ALERTS: '/topic/dashboard/alerts',
        DASHBOARD_DEPLOYS: '/topic/dashboard/deploys',

        // ========== Agent Monitoring Topics ==========
        AGENTS_STATUS: '/topic/agents/status',
        AGENTS_METRICS: '/topic/agents/metrics',

        // ========== Release Topics ==========
        RELEASE_STATUS: '/topic/release/status',
        RELEASE_PROGRESS: '/topic/release/progress',

        // ========== System Topics ==========
        SYSTEM_NOTIFICATIONS: '/topic/system/notifications',
        SYSTEM_LOGS: '/topic/system/logs',

        // ========== User Topics ==========
        USER_ACTIVITY: '/topic/user/activity'
    };

    // App destinations (for sending messages to server)
    const WsDestinations = {
        // Dashboard
        DASHBOARD_REFRESH: '/app/dashboard/refresh',
        DASHBOARD_KPI: '/app/dashboard/kpi',

        // Agents
        AGENTS_REFRESH: '/app/agents/refresh',
        METRICS: '/app/metrics'
    };

    // Freeze to prevent modifications
    Object.freeze(WsTopics);
    Object.freeze(WsDestinations);

    global.WsTopics = WsTopics;
    global.WsDestinations = WsDestinations;

})(window);

