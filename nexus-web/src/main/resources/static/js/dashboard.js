/**
 * Dashboard WebSocket Integration
 *
 * This script connects the dashboard page to real-time WebSocket updates.
 *
 * Topics Subscribed:
 *   /topic/dashboard/kpi     - KPI card updates (agents, deploys, jobs, alerts)
 *   /topic/dashboard/alerts  - Alert notifications
 *   /topic/dashboard/deploys - Deployment status updates
 *
 * UI Elements Updated:
 *   #kpi-active-agents       - Active agents count
 *   #kpi-deployments-today   - Today's deployment count
 *   #kpi-active-jobs         - Active jobs count
 *   #kpi-active-alerts       - Active alerts count
 *   #live-events-list        - Live events panel
 *   #recent-deploys-table    - Recent deployments table
 *
 * TO CUSTOMIZE:
 *   1. Change element IDs in the updateXxx() methods to match your HTML
 *   2. Modify the data handling in onXxxMessage() methods
 *   3. Enable debug mode to see all incoming messages: new DashboardWebSocket({ debug: true })
 */
(function(global, $) {
    'use strict';

    class DashboardWebSocket {

        constructor(options = {}) {
            this.debug = options.debug || false;
            this.connectors = {};
            this.liveEventsMax = options.liveEventsMax || 50;
            this.unreadCount = 0;
        }

        // ==================== INITIALIZATION ====================

        /**
         * Initialize and connect to all dashboard topics.
         */
        connect() {
            this.log('Connecting to dashboard topics...');

            // KPI Updates (every 5 seconds from server)
            this.connectors.kpi = new PageWebSocket({
                topic: WsTopics.DASHBOARD_KPI,
                debug: this.debug,
                onMessage: this.onKpiMessage.bind(this)
            }).connect();

            // Alert Notifications
            this.connectors.alerts = new PageWebSocket({
                topic: WsTopics.DASHBOARD_ALERTS,
                debug: this.debug,
                onMessage: this.onAlertMessage.bind(this)
            }).connect();

            // Deployment Updates
            this.connectors.deploys = new PageWebSocket({
                topic: WsTopics.DASHBOARD_DEPLOYS,
                debug: this.debug,
                onMessage: this.onDeployMessage.bind(this)
            }).connect();

            return this;
        }

        /**
         * Disconnect from all topics.
         */
        disconnect() {
            Object.values(this.connectors).forEach(c => c.disconnect());
            this.log('Disconnected from all topics');
        }

        // ==================== MESSAGE HANDLERS ====================

        /**
         * Handle KPI updates from server.
         * Data format: { activeAgents, deploymentsToday, activeJobs, activeAlerts }
         */
        onKpiMessage(data) {
            this.log('KPI update:', data);

            // Update KPI cards
            this.updateKpiCard('#kpi-active-agents', data.activeAgents);
            this.updateKpiCard('#kpi-deployments-today', data.deploymentsToday);
            this.updateKpiCard('#kpi-active-jobs', data.activeJobs);
            this.updateKpiCard('#kpi-active-alerts', data.activeAlerts);

            // Also try alternative selectors (for flexibility)
            this.updateKpiCard('#activeAgentCountDivId', data.activeAgents);
        }

        /**
         * Handle alert notifications from server.
         * Data format: { alertId, severity, title, message, timestamp }
         */
        onAlertMessage(data) {
            this.log('Alert:', data);

            // Add to live events
            this.addLiveEvent('alert', data);

            // Show toast notification (if you have a toast system)
            this.showToast(data.severity, data.title, data.message);

            // Update notification badge
            this.incrementNotificationBadge();
        }

        /**
         * Handle deployment updates from server.
         * Data format: { serviceName, version, status, timestamp }
         */
        onDeployMessage(data) {
            this.log('Deployment:', data);

            // Add to live events
            this.addLiveEvent('deploy', data);

            // Add to recent deploys table
            this.addDeployRow(data);

            // Update notification badge
            this.incrementNotificationBadge();
        }

        // ==================== UI UPDATE METHODS ====================

        /**
         * Update a KPI card with animated counter.
         */
        updateKpiCard(selector, value) {
            const $el = $(selector);
            if (!$el.length) return;

            const currentValue = parseInt($el.text()) || 0;
            if (currentValue === value) return;

            // Animate the number change
            $({ count: currentValue }).animate({ count: value }, {
                duration: 400,
                step: function() {
                    $el.text(Math.floor(this.count));
                },
                complete: function() {
                    $el.text(value);
                }
            });
        }

        /**
         * Add a row to the recent deployments table.
         */
        addDeployRow(deploy) {
            const $table = $('#recent-deploys-table tbody');
            if (!$table.length) return;

            // Determine status badge color
            const badgeClass = {
                'Success': 'bg-success',
                'Partial': 'bg-warning',
                'Failed': 'bg-danger'
            }[deploy.status] || 'bg-secondary';

            const $row = $(`
                <tr class="fade-in">
                    <td>${deploy.serviceName}</td>
                    <td>${deploy.version}</td>
                    <td><span class="badge ${badgeClass}">${deploy.status}</span></td>
                    <td>${deploy.timestamp}</td>
                </tr>
            `);

            // Add to top of table
            $table.prepend($row);

            // Keep only last 10 rows
            $table.find('tr').slice(10).remove();
        }

        /**
         * Add an event to the live events panel.
         */
        addLiveEvent(type, data) {
            const $list = $('#live-events-list');
            if (!$list.length) return;

            // Remove "no events" placeholder
            $list.find('.no-events').remove();

            // Format the event
            const icon = this.getEventIcon(type);
            const label = this.getEventLabel(type, data);

            const $item = $(`
                <li class="list-group-item d-flex justify-content-between align-items-start">
                    <div>
                        <span class="me-2">${icon}</span>
                        <span>${label}</span>
                    </div>
                    <small class="text-muted">${data.timestamp || 'now'}</small>
                </li>
            `);

            $list.prepend($item);

            // Trim to max size
            $list.find('li').slice(this.liveEventsMax).remove();

            // Update event count
            const count = $list.find('li').length;
            $('#live-events-count').text(count + ' events');
        }

        /**
         * Get icon for event type.
         */
        getEventIcon(type) {
            const icons = {
                'kpi': '📊',
                'alert': '🔔',
                'deploy': '🚀'
            };
            return icons[type] || '📌';
        }

        /**
         * Get display label for event.
         */
        getEventLabel(type, data) {
            if (type === 'alert') {
                return `<strong>${data.title}</strong>: ${data.message}`;
            }
            if (type === 'deploy') {
                return `<strong>${data.serviceName}</strong> v${data.version} - ${data.status}`;
            }
            return JSON.stringify(data).substring(0, 80);
        }

        /**
         * Show a toast notification (requires Bootstrap toast or similar).
         */
        showToast(severity, title, message) {
            // If you have a toast container, create and show toast here
            // For now, just log to console
            const prefix = severity === 'danger' ? '❌' : severity === 'warning' ? '⚠️' : 'ℹ️';
            console.log(`${prefix} ${title}: ${message}`);
        }

        /**
         * Increment the notification badge counter.
         */
        incrementNotificationBadge() {
            this.unreadCount = Math.min(this.unreadCount + 1, 99);
            const $badge = $('#notif-badge');
            if ($badge.length) {
                $badge.text(this.unreadCount).show();
            }
        }

        /**
         * Clear notification badge (call when user opens notifications).
         */
        clearNotificationBadge() {
            this.unreadCount = 0;
            $('#notif-badge').text('0').hide();
        }

        /**
         * Toggle the live events panel visibility.
         */
        toggleLiveEvents() {
            const $panel = $('#live-events-card');
            if ($panel.is(':visible')) {
                $panel.slideUp();
            } else {
                $panel.slideDown();
                this.clearNotificationBadge();
            }
        }

        /**
         * Clear all live events.
         */
        clearLiveEvents() {
            $('#live-events-list').empty().append(
                '<li class="list-group-item no-events text-muted">No events yet</li>'
            );
            $('#live-events-count').text('0 events');
        }

        // ==================== UTILITY METHODS ====================

        log(...args) {
            if (this.debug) {
                console.log('[Dashboard]', ...args);
            }
        }
    }

    // ==================== AUTO-INITIALIZATION ====================

    let instance = null;

    $(document).ready(function() {
        // Auto-connect when page loads
        instance = new DashboardWebSocket({
            debug: false  // Set to true to see all WebSocket messages
        }).connect();

        // Expose instance globally for debugging
        global.dashboardWs = instance;
    });

    // ==================== GLOBAL EXPORTS ====================

    // Export class for manual instantiation
    global.DashboardWebSocket = DashboardWebSocket;

    // Helper function for notification toggle button
    global.toggleLiveEvents = function() {
        if (instance) instance.toggleLiveEvents();
    };

})(window, jQuery);