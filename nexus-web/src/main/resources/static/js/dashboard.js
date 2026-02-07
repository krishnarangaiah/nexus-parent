/**
 * Dashboard Real-Time Updates
 *
 * Subscribes to WebSocket for real-time agent status updates.
 * Updates the dashboard UI without page refresh when agents connect/disconnect.
 */
(function(global, $) {
    'use strict';

    class DashboardRealtime {

        constructor(options = {}) {
            this.debug = options.debug || false;
            this.stompClient = null;
            this.connected = false;
        }

        /**
         * Connect to WebSocket and subscribe to agent status topic.
         */
        connect() {
            this.log('Connecting to WebSocket...');

            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);

            // Disable debug logging from STOMP unless debug mode
            if (!this.debug) {
                this.stompClient.debug = null;
            }

            const self = this;
            this.stompClient.connect({}, function(frame) {
                self.connected = true;
                self.log('Connected to WebSocket');

                // Subscribe to agent status updates
                self.stompClient.subscribe('/topic/agents/status', function(message) {
                    try {
                        const data = JSON.parse(message.body);
                        self.handleMessage(data);
                    } catch (e) {
                        console.error('Error parsing message:', e);
                    }
                });

            }, function(error) {
                self.connected = false;
                console.error('WebSocket connection error:', error);
                // Try to reconnect after 5 seconds
                setTimeout(function() {
                    self.connect();
                }, 5000);
            });

            return this;
        }

        /**
         * Handle incoming WebSocket message.
         */
        handleMessage(data) {
            this.log('Received:', data);

            switch (data.type) {
                case 'agent_connected':
                    this.onAgentConnected(data);
                    break;
                case 'agent_disconnected':
                    this.onAgentDisconnected(data);
                    break;
                case 'dashboard_stats':
                    this.onDashboardStats(data);
                    break;
                default:
                    this.log('Unknown message type:', data.type);
            }
        }

        /**
         * Handle agent connected event.
         */
        onAgentConnected(data) {
            this.log('Agent connected:', data.agentUuid);

            // Update agent row status
            this.updateAgentStatus(data.agentUuid, 'ONLINE', data.displayName, data.hostname);

            // Show notification
            this.showNotification('success',
                `Agent "${data.displayName || data.agentUuid}" connected`);
        }

        /**
         * Handle agent disconnected event.
         */
        onAgentDisconnected(data) {
            this.log('Agent disconnected:', data.agentUuid);

            // Update agent row status
            this.updateAgentStatus(data.agentUuid, 'OFFLINE', data.displayName);

            // Show notification
            this.showNotification('warning',
                `Agent "${data.displayName || data.agentUuid}" disconnected`);
        }

        /**
         * Handle dashboard stats update.
         */
        onDashboardStats(data) {
            this.log('Dashboard stats:', data);

            // Update KPI cards
            this.updateElement('#total-agents', data.totalAgents);
            this.updateElement('#online-agents', data.onlineAgents);

            // Also update any elements with data-stat attributes
            $('[data-stat="totalAgents"]').text(data.totalAgents);
            $('[data-stat="onlineAgents"]').text(data.onlineAgents);
        }

        /**
         * Update agent status in the table.
         */
        updateAgentStatus(agentUuid, status, displayName, hostname) {
            // Find the agent row by UUID
            const $row = $(`tr[data-agent-uuid="${agentUuid}"]`);

            if ($row.length > 0) {
                // Update status badge
                const $statusCell = $row.find('.agent-status');
                if (status === 'ONLINE') {
                    $statusCell.html('<span class="badge bg-success"><i class="bi bi-circle-fill me-1" style="font-size: 0.5rem;"></i>Online</span>');
                } else {
                    $statusCell.html('<span class="badge bg-secondary"><i class="bi bi-circle-fill me-1" style="font-size: 0.5rem;"></i>Offline</span>');
                }

                // Update hostname if provided
                if (hostname) {
                    $row.find('.agent-hostname').text(hostname);
                }

                // Highlight the row briefly
                $row.addClass('table-highlight');
                setTimeout(function() {
                    $row.removeClass('table-highlight');
                }, 2000);
            }

            // Also update agent cards if present (on Console page)
            const $card = $(`.agent-card[data-uuid="${agentUuid}"]`);
            if ($card.length > 0) {
                const $dot = $card.find('.agent-status-dot');
                if (status === 'ONLINE') {
                    $card.removeClass('offline');
                    $card.attr('data-connected', 'true');
                    $dot.removeClass('offline').addClass('online');
                } else {
                    $card.addClass('offline');
                    $card.attr('data-connected', 'false');
                    $dot.removeClass('online').addClass('offline');
                }
            }
        }

        /**
         * Update element text with animation.
         */
        updateElement(selector, value) {
            const $el = $(selector);
            if ($el.length > 0) {
                const oldValue = parseInt($el.text()) || 0;
                if (oldValue !== value) {
                    $el.text(value);
                    $el.addClass('value-changed');
                    setTimeout(function() {
                        $el.removeClass('value-changed');
                    }, 1000);
                }
            }
        }

        /**
         * Show a toast notification.
         */
        showNotification(type, message) {
            // Check if we have a toast container
            let $container = $('#toast-container');
            if ($container.length === 0) {
                $container = $('<div id="toast-container" class="toast-container position-fixed bottom-0 end-0 p-3"></div>');
                $('body').append($container);
            }

            const bgClass = type === 'success' ? 'bg-success' :
                           type === 'warning' ? 'bg-warning' :
                           type === 'error' ? 'bg-danger' : 'bg-info';

            const $toast = $(`
                <div class="toast align-items-center text-white ${bgClass} border-0" role="alert">
                    <div class="d-flex">
                        <div class="toast-body">${this.escapeHtml(message)}</div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                    </div>
                </div>
            `);

            $container.append($toast);
            const toast = new bootstrap.Toast($toast[0], { delay: 4000 });
            toast.show();

            // Remove from DOM after hidden
            $toast.on('hidden.bs.toast', function() {
                $(this).remove();
            });
        }

        escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        /**
         * Disconnect from WebSocket.
         */
        disconnect() {
            if (this.stompClient && this.connected) {
                this.stompClient.disconnect();
                this.connected = false;
                this.log('Disconnected');
            }
        }

        log(...args) {
            if (this.debug) {
                console.log('[DashboardRealtime]', ...args);
            }
        }
    }

    // Auto-initialize on page load
    let instance = null;

    $(document).ready(function() {
        // Only connect if SockJS and Stomp are available
        if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
            instance = new DashboardRealtime({ debug: false }).connect();
            global.dashboardRealtime = instance;
        }
    });

    global.DashboardRealtime = DashboardRealtime;

})(window, jQuery);