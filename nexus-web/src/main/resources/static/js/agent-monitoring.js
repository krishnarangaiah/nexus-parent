/**
 * Agent Monitoring Page WebSocket Integration
 *
 * This script handles real-time updates for the Agent Monitoring page.
 * It uses the structured PageWebSocket connector with dedicated topics.
 *
 * Topics subscribed:
 * - /topic/agents/status - Agent status updates
 * - /topic/agents/metrics - Agent metrics data
 */
(function(global, $) {
    'use strict';

    class AgentMonitoringWebSocket {
        constructor(options = {}) {
            this.debug = options.debug || false;
            this.connectors = {};

            this.init();
        }

        log(...args) {
            if (this.debug) {
                console.log('[AgentMonitoring]', ...args);
            }
        }

        init() {
            // Initialize Agent Status WebSocket connection
            this.connectors.status = new PageWebSocket({
                topic: WsTopics.AGENTS_STATUS,
                debug: this.debug,
                onConnect: this.onStatusConnect.bind(this),
                onMessage: this.onStatusMessage.bind(this),
                onError: this.onError.bind(this)
            });

            // Initialize Agent Metrics WebSocket connection
            this.connectors.metrics = new PageWebSocket({
                topic: WsTopics.AGENTS_METRICS,
                debug: this.debug,
                onConnect: this.onMetricsConnect.bind(this),
                onMessage: this.onMetricsMessage.bind(this),
                onError: this.onError.bind(this)
            });

            this.log('Agent Monitoring WebSocket initialized');
        }

        connect() {
            Object.values(this.connectors).forEach(connector => connector.connect());
            this.log('Connecting to all agent topics...');
            return this;
        }

        disconnect() {
            Object.values(this.connectors).forEach(connector => connector.disconnect());
            this.log('Disconnected from all agent topics');
        }

        // ========== Status Handlers ==========
        onStatusConnect(frame) {
            this.log('Connected to Agent Status topic');
            // Request initial status
            this.requestStatusRefresh();
        }

        onStatusMessage(data) {
            this.log('Agent status update received:', data);

            if (data.type === 'agent_status_bulk') {
                this.updateAllAgentCards(data.agents);
            } else if (data.type === 'agent_status') {
                this.updateSingleAgentCard(data);
            } else if (data.type === 'agent_connected') {
                this.handleAgentConnected(data.agentId);
            } else if (data.type === 'agent_disconnected') {
                this.handleAgentDisconnected(data.agentId);
            }
        }

        updateAllAgentCards(agents) {
            if (!agents) return;

            $.each(agents, (agentId, agentData) => {
                this.updateAgentCard(agentId, agentData);
            });
        }

        updateSingleAgentCard(data) {
            this.updateAgentCard(data.agentId, data);
        }

        updateAgentCard(agentId, agentData) {
            const $card = $(`#${agentId}`);
            if (!$card.length) {
                this.log('Agent card not found for:', agentId);
                return;
            }

            // Update display name
            if (agentData.displayName) {
                $card.find('.agent-name').text(agentData.displayName);
            }

            // Update status
            if (agentData.status) {
                const isOnline = agentData.status === 'ONLINE';
                $card.find('.agent-status')
                    .text(`Status: ${agentData.status}`)
                    .removeClass('text-success text-danger')
                    .addClass(isOnline ? 'text-success' : 'text-danger');

                $card.removeClass('agent-online agent-offline')
                    .addClass(isOnline ? 'agent-online' : 'agent-offline');
            }

            // Update heartbeat if present
            if (agentData.heartbeat) {
                $card.find('.agent-heartbeat').text(`Heartbeat: ${agentData.heartbeat}`);
            }
        }

        handleAgentConnected(agentId) {
            this.log('Agent connected:', agentId);
            const $card = $(`#${agentId}`);
            if ($card.length) {
                $card.addClass('agent-online').removeClass('agent-offline');
                $card.find('.agent-status').text('Status: ONLINE').addClass('text-success');
            }
            // Show notification
            this.showNotification(`Agent ${agentId} is now online`, 'success');
        }

        handleAgentDisconnected(agentId) {
            this.log('Agent disconnected:', agentId);
            const $card = $(`#${agentId}`);
            if ($card.length) {
                $card.addClass('agent-offline').removeClass('agent-online');
                $card.find('.agent-status').text('Status: OFFLINE').addClass('text-danger');
            }
            // Show notification
            this.showNotification(`Agent ${agentId} went offline`, 'warning');
        }

        requestStatusRefresh() {
            if (this.connectors.status) {
                this.connectors.status.send(WsDestinations.AGENTS_REFRESH, { action: 'refresh' });
            }
        }

        // ========== Metrics Handlers ==========
        onMetricsConnect(frame) {
            this.log('Connected to Agent Metrics topic');
        }

        onMetricsMessage(data) {
            this.log('Agent metrics received:', data);

            if (data.type === 'agent_metrics' && data.agentId) {
                this.updateAgentMetrics(data.agentId, data);
            }
        }

        updateAgentMetrics(agentId, metrics) {
            const $card = $(`#${agentId}`);
            if (!$card.length) return;

            // Update CPU
            if (metrics.cpuUsage !== undefined) {
                $card.find('.agent-cpu')
                    .text(`CPU: ${metrics.cpuUsage.toFixed(1)}%`);
                $card.find('.cpu-progress')
                    .css('width', `${metrics.cpuUsage}%`)
                    .attr('aria-valuenow', metrics.cpuUsage);
            }

            // Update Memory
            if (metrics.memoryUsage !== undefined) {
                $card.find('.agent-memory')
                    .text(`Memory: ${metrics.memoryUsage.toFixed(1)}%`);
                $card.find('.memory-progress')
                    .css('width', `${metrics.memoryUsage}%`)
                    .attr('aria-valuenow', metrics.memoryUsage);
            }

            // Update Disk
            if (metrics.diskUsage !== undefined) {
                $card.find('.agent-disk')
                    .text(`Disk: ${metrics.diskUsage.toFixed(1)}%`);
            }
        }

        // ========== Common Methods ==========
        onError(error) {
            console.error('[AgentMonitoring] WebSocket error:', error);
        }

        showNotification(message, type) {
            // TODO: Implement toast notification
            console.log(`[${type}] ${message}`);
        }
    }

    // ========== Page Initialization ==========
    let agentMonitoringWs = null;

    // Auto-initialize on DOM ready if we're on the agent monitoring page
    $(document).ready(function() {
        // Check if we're on the agent monitoring page
        if ($('#agent-monitoring-container').length ||
            window.location.pathname.includes('/Monitoring/Agent')) {
            agentMonitoringWs = new AgentMonitoringWebSocket({ debug: false });
            agentMonitoringWs.connect();
        }
    });

    // Expose to global scope
    global.AgentMonitoringWebSocket = AgentMonitoringWebSocket;
    global.agentMonitoringWs = agentMonitoringWs;

})(window, jQuery);

