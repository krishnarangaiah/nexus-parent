// language: javascript
(function (global) {
    class WsConnection {
        constructor(options = {}) {
            this.endpointPath = options.endpointPath || '/ws';
            this.topic = options.topic || '/topic/agents/status';
            this.reconnectInitial = options.reconnectInitial || 1000;
            this.reconnectMax = options.reconnectMax || 30000;
            this.reconnectDelay = this.reconnectInitial;
            this.stompClient = null;
            this.currentSubscription = null;
            this.wsUrl = null;
            this.safeLog = options.safeLog || function () { try { console.log.apply(console, arguments); } catch (e) {} };
        }

        buildWsUrl() {
            var proto = (location.protocol === 'https:') ? 'wss://' : 'ws://';
            return proto + location.host + this.endpointPath;
        }

        initStompClient() {
            // avoid multiple parallel inits
            if (this.stompClient && this.stompClient.connected) return;
            this.wsUrl = this.buildWsUrl();

            // Use the global Stomp object from stomp-websocket WebJar
            const StompClient = global.Stomp;
            if (!StompClient) {
                this.safeLog('STOMP client not available. Please include STOMP and SockJS libraries.');
                return;
            }

            // SockJS accepts an endpoint path; keep consistent with project usage
            this.stompClient = StompClient.over(new SockJS(this.endpointPath));
            const self = this;
            this.safeLog('Attempting STOMP connect to', this.wsUrl);
            try {
                this.stompClient.connect({}, function () {
                    self.safeLog('Connected to STOMP');
                    // reset backoff after success
                    self.reconnectDelay = self.reconnectInitial;
                    self.onConnect();
                }, function (err) {
                    self.safeLog('STOMP connection error', err);
                    self.onError(err);
                    self.scheduleReconnect();
                });
            } catch (e) {
                this.safeLog('initStompClient failed', e);
                this.scheduleReconnect();
            }
        }

        scheduleReconnect() {
            const delay = this.reconnectDelay;
            this.safeLog('Scheduling reconnect in', delay, 'ms');
            setTimeout(() => {
                // exponential backoff with cap
                this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.reconnectMax);
                this.initStompClient();
            }, delay);
        }

        subscribe(topic) {
            if (!this.stompClient || !this.stompClient.connected) return;
            try {
                if (this.currentSubscription) {
                    try { this.currentSubscription.unsubscribe(); } catch (e) { /* ignore */ }
                }
                const self = this;
                this.currentSubscription = this.stompClient.subscribe(topic, function (message) {
                    self._handleFrame(message);
                });
                this.safeLog('Subscribed to', topic, 'id=', this.currentSubscription && this.currentSubscription.id);
            } catch (e) {
                this.safeLog('Subscription error', e);
            }
        }

        unsubscribe() {
            try {
                if (this.currentSubscription) {
                    this.currentSubscription.unsubscribe();
                }
            } catch (e) { /* ignore */ }
            this.currentSubscription = null;
        }

        disconnect() {
            try {
                this.unsubscribe();
                if (this.stompClient) {
                    this.stompClient.disconnect();
                }
            } catch (e) {
                this.safeLog('Error during disconnect', e);
            } finally {
                this.stompClient = null;
            }
        }

        _handleFrame(frame) {
            try {
                const body = frame && (frame.body || frame);
                this.safeLog('Received frame for', this.topic, body);
                let payload = null;
                if (typeof body === 'string') {
                    try {
                        payload = JSON.parse(body);
                    } catch (e) {
                        this.safeLog('Failed to parse message body as JSON', e);
                        return;
                    }
                } else {
                    payload = body;
                }
                this.onMessage(payload, frame);
            } catch (e) {
                this.safeLog('Failed processing message', e, frame);
            }
        }

        // Hooks to override
        onConnect() { /* override to subscribe or any setup */ }
        onMessage(payload /*, frame */) { /* override to process messages */ }
        onError(err) { /* optional override */ }
    }

    global.WsConnection = WsConnection;
})(window);
