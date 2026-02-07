/**
 * Page-specific WebSocket Connector
 *
 * This module provides a clean, reusable way to connect pages to specific WebSocket topics.
 *
 * Usage:
 *   // In your page script
 *   const wsConnector = new PageWebSocket({
 *       topic: '/topic/dashboard/kpi',
 *       onMessage: function(data) {
 *           console.log('Received:', data);
 *           // Update your UI here
 *       }
 *   });
 *   wsConnector.connect();
 *
 * Options:
 *   - endpoint: WebSocket endpoint (default: '/ws')
 *   - topic: The topic to subscribe to (required)
 *   - onConnect: Callback when connected
 *   - onMessage: Callback when message received (receives parsed JSON)
 *   - onError: Callback on error
 *   - onDisconnect: Callback when disconnected
 *   - reconnect: Whether to auto-reconnect (default: true)
 *   - reconnectDelay: Initial reconnect delay in ms (default: 1000)
 *   - maxReconnectDelay: Maximum reconnect delay in ms (default: 30000)
 *   - debug: Enable debug logging (default: false)
 */
(function(global) {
    'use strict';

    class PageWebSocket {
        constructor(options = {}) {
            // Validate required options
            if (!options.topic) {
                throw new Error('PageWebSocket: topic is required');
            }

            this.endpoint = options.endpoint || '/ws';
            this.topic = options.topic;
            this.onConnectCallback = options.onConnect || function() {};
            this.onMessageCallback = options.onMessage || function() {};
            this.onErrorCallback = options.onError || function() {};
            this.onDisconnectCallback = options.onDisconnect || function() {};

            this.shouldReconnect = options.reconnect !== false;
            this.initialReconnectDelay = options.reconnectDelay || 1000;
            this.maxReconnectDelay = options.maxReconnectDelay || 30000;
            this.currentReconnectDelay = this.initialReconnectDelay;
            this.debug = options.debug || false;

            this.stompClient = null;
            this.subscription = null;
            this.isConnected = false;
            this.isConnecting = false;
            this.reconnectTimer = null;
        }

        log(...args) {
            if (this.debug) {
                console.log('[PageWebSocket]', ...args);
            }
        }

        connect() {
            if (this.isConnected || this.isConnecting) {
                this.log('Already connected or connecting');
                return this;
            }

            this.isConnecting = true;
            this.log('Connecting to', this.endpoint);

            try {
                // Check for required libraries
                if (typeof SockJS === 'undefined') {
                    throw new Error('SockJS library not loaded');
                }
                if (typeof Stomp === 'undefined') {
                    throw new Error('STOMP library not loaded');
                }

                const socket = new SockJS(this.endpoint);
                this.stompClient = Stomp.over(socket);

                // Disable STOMP debug logging unless in debug mode
                if (!this.debug) {
                    this.stompClient.debug = null;
                }

                const self = this;
                this.stompClient.connect({},
                    function(frame) {
                        self.onConnected(frame);
                    },
                    function(error) {
                        self.onConnectionError(error);
                    }
                );
            } catch (e) {
                this.log('Connection failed:', e);
                this.isConnecting = false;
                this.onErrorCallback(e);
                this.scheduleReconnect();
            }

            return this;
        }

        onConnected(frame) {
            this.log('Connected:', frame);
            this.isConnected = true;
            this.isConnecting = false;
            this.currentReconnectDelay = this.initialReconnectDelay;

            // Subscribe to the topic
            this.subscribe();

            // Call user's onConnect callback
            this.onConnectCallback(frame);
        }

        onConnectionError(error) {
            this.log('Connection error:', error);
            this.isConnected = false;
            this.isConnecting = false;
            this.onErrorCallback(error);
            this.scheduleReconnect();
        }

        subscribe() {
            if (!this.stompClient || !this.isConnected) {
                this.log('Cannot subscribe - not connected');
                return;
            }

            try {
                // Unsubscribe from existing subscription if any
                this.unsubscribe();

                const self = this;
                this.subscription = this.stompClient.subscribe(this.topic, function(message) {
                    self.handleMessage(message);
                });
                this.log('Subscribed to', this.topic);
            } catch (e) {
                this.log('Subscription error:', e);
                this.onErrorCallback(e);
            }
        }

        unsubscribe() {
            if (this.subscription) {
                try {
                    this.subscription.unsubscribe();
                    this.log('Unsubscribed from', this.topic);
                } catch (e) {
                    this.log('Unsubscribe error:', e);
                }
                this.subscription = null;
            }
        }

        handleMessage(message) {
            try {
                let payload = message.body;

                // Try to parse as JSON
                if (typeof payload === 'string') {
                    try {
                        payload = JSON.parse(payload);
                    } catch (e) {
                        // Keep as string if not valid JSON
                    }
                }

                this.log('Message received:', payload);
                this.onMessageCallback(payload, message);
            } catch (e) {
                this.log('Error handling message:', e);
                this.onErrorCallback(e);
            }
        }

        /**
         * Send a message to a destination
         * @param {string} destination - The destination (e.g., '/app/dashboard/refresh')
         * @param {object} payload - The message payload
         */
        send(destination, payload) {
            if (!this.stompClient || !this.isConnected) {
                this.log('Cannot send - not connected');
                return false;
            }

            try {
                const body = typeof payload === 'string' ? payload : JSON.stringify(payload);
                this.stompClient.send(destination, {}, body);
                this.log('Sent to', destination, ':', payload);
                return true;
            } catch (e) {
                this.log('Send error:', e);
                this.onErrorCallback(e);
                return false;
            }
        }

        scheduleReconnect() {
            if (!this.shouldReconnect) {
                this.log('Auto-reconnect disabled');
                return;
            }

            if (this.reconnectTimer) {
                clearTimeout(this.reconnectTimer);
            }

            this.log('Scheduling reconnect in', this.currentReconnectDelay, 'ms');
            const self = this;
            this.reconnectTimer = setTimeout(function() {
                self.currentReconnectDelay = Math.min(
                    self.currentReconnectDelay * 2,
                    self.maxReconnectDelay
                );
                self.connect();
            }, this.currentReconnectDelay);
        }

        disconnect() {
            this.shouldReconnect = false;

            if (this.reconnectTimer) {
                clearTimeout(this.reconnectTimer);
                this.reconnectTimer = null;
            }

            this.unsubscribe();

            if (this.stompClient) {
                try {
                    this.stompClient.disconnect(function() {
                        this.log('Disconnected');
                    }.bind(this));
                } catch (e) {
                    this.log('Disconnect error:', e);
                }
                this.stompClient = null;
            }

            this.isConnected = false;
            this.isConnecting = false;
            this.onDisconnectCallback();
        }

        /**
         * Change the subscription topic
         * @param {string} newTopic - The new topic to subscribe to
         */
        changeTopic(newTopic) {
            this.topic = newTopic;
            if (this.isConnected) {
                this.subscribe();
            }
        }
    }

    // Export to global scope
    global.PageWebSocket = PageWebSocket;

})(window);

