/* appWebsocket.js
   Encapsulate SockJS/STOMP logic for pages to call AppWebsocket.init(options).
   Options:
     - endpointPath (default '/ws')
     - topic (default '/topic/agents/status')
     - reconnectInitial (ms)
     - reconnectMax (ms)
*/
(function (global) {
    function safeLog() { try { console.log.apply(console, arguments); } catch (e) {} }

    var state = {
        endpointPath: '/ws',
        topic: '/topic/agents/status',
        wsUrl: null,
        stompClient: null,
        currentSubscription: null,
        reconnectDelay: 1000,
        reconnectMax: 30000,
        probeTimeoutMs: 2000,
        useSockJS: null
    };

    function buildWsUrl(endpointPath) {
        var proto = (location.protocol === 'https:') ? 'wss://' : 'ws://';
        return proto + location.host + endpointPath;
    }

    function probeSockJSInfo(endpointPath) {
        var infoUrl = endpointPath.replace(/^\/+/, '/') + '/info';
        return fetch(infoUrl, { method: 'GET', cache: 'no-store' , mode: 'same-origin' })
            .then(function (resp) { return resp.ok; })
            .catch(function () { return false; });
    }

    function subscribeToTopic(topic) {
        if (!state.stompClient || !state.stompClient.connected) return;
        try {
            if (state.currentSubscription) {
                try { state.currentSubscription.unsubscribe(); } catch (e) { /* ignore */ }
            }
            state.currentSubscription = state.stompClient.subscribe(topic, handleStatusMessage);
            safeLog('Subscribed to', topic, 'id=', state.currentSubscription && state.currentSubscription.id);
        } catch (e) {
            safeLog('Subscription error', e);
        }
    }

    function handleStatusMessage(message) {
        try {
            var body = message && (message.body || message);
            safeLog('Received', state.topic, 'message body:', body);
            var payload = typeof body === 'string' ? JSON.parse(body) : body;
            var agentId = payload.agentId;
            var status = payload.status;
            var el = document.getElementById('agent-status-' + agentId);
            if (el) {
                el.textContent = status;
                if (status === 'ACTIVE') {
                    el.className = 'badge bg-success';
                } else if (status === 'INACTIVE') {
                    el.className = 'badge bg-secondary';
                } else {
                    el.className = 'badge bg-warning text-dark';
                }
            } else {
                safeLog('Status update for unknown agent', agentId, status);
            }
        } catch (e) {
            safeLog('Failed processing agent status message', e, message);
        }
    }

    function createAndConnect(useSockJS) {
        safeLog('Creating STOMP client, useSockJS=', useSockJS);
        var transport;
        if (useSockJS) {
            try {
                transport = new SockJS(state.endpointPath);
            } catch (e) {
                safeLog('Failed creating SockJS transport', e);
                scheduleReconnect();
                return;
            }
            transport.onopen = function () { safeLog('SockJS transport opened'); };
            transport.onclose = function (evt) { safeLog('SockJS transport closed', evt); scheduleReconnect(); };
            transport.onerror = function (err) { safeLog('SockJS transport error', err); };
        } else {
            try {
                transport = new WebSocket(state.wsUrl);
                transport.onopen = function () { safeLog('Native WebSocket opened'); };
                transport.onclose = function (evt) { safeLog('Native WebSocket closed', evt); scheduleReconnect(); };
                transport.onerror = function (err) { safeLog('Native WebSocket error', err); };
            } catch (e) {
                safeLog('Failed to create native WebSocket', e);
                scheduleReconnect();
                return;
            }
        }

        state.stompClient = Stomp.over(transport);
        state.stompClient.debug = function (msg) { safeLog('STOMP:', msg); };

        try {
            state.stompClient.connect({}, function (frame) {
                safeLog('Connected to STOMP, frame:', frame);
                state.reconnectDelay = Math.max(1000, state.reconnectDelay); // reset
                subscribeToTopic(state.topic);
            }, function (err) {
                safeLog('STOMP connection error', err);
                scheduleReconnect();
            });
        } catch (e) {
            safeLog('Error creating stomp connection', e);
            scheduleReconnect();
        }
    }

    function scheduleReconnect() {
        safeLog('Scheduling reconnect in', state.reconnectDelay, 'ms');
        setTimeout(function () {
            state.reconnectDelay = Math.min(state.reconnectDelay * 2, state.reconnectMax);
            initStompClient(); // try again
        }, state.reconnectDelay);
    }

    function initStompClient() {
        // disconnect previous client gracefully
        try {
            if (state.stompClient && state.stompClient.connected) {
                state.stompClient.disconnect(function () { /* noop */ });
            }
        } catch (e) { /* ignore */ }

        // prepare wsUrl
        state.wsUrl = buildWsUrl(state.endpointPath);

        // probe SockJS availability then create client
        probeSockJSInfo(state.endpointPath).then(function (available) {
            state.useSockJS = !!available;
            createAndConnect(state.useSockJS);
        }).catch(function (err) {
            safeLog('Probe error; falling back to native WebSocket', err);
            state.useSockJS = false;
            createAndConnect(false);
        });
    }

    function disconnect() {
        try {
            if (state.currentSubscription) {
                try { state.currentSubscription.unsubscribe(); } catch (e) {}
                state.currentSubscription = null;
            }
            if (state.stompClient && state.stompClient.connected) {
                state.stompClient.disconnect(function () { safeLog('STOMP disconnected'); });
            }
        } catch (e) {
            safeLog('Error during disconnect', e);
        }
    }

    function send(destination, payload) {
        try {
            if (state.stompClient && state.stompClient.connected) {
                state.stompClient.send(destination, {}, typeof payload === 'string' ? payload : JSON.stringify(payload));
                return true;
            }
            safeLog('STOMP client not connected; cannot send to', destination);
            return false;
        } catch (e) {
            safeLog('Failed to send via STOMP', e);
            return false;
        }
    }

    // Public API
    var AppWebsocket = {
        init: function (options) {
            options = options || {};
            state.endpointPath = options.endpointPath || state.endpointPath;
            state.topic = options.topic || state.topic;
            state.reconnectDelay = options.reconnectInitial || state.reconnectDelay;
            state.reconnectMax = options.reconnectMax || state.reconnectMax;
            safeLog('AppWebsocket.init', { endpointPath: state.endpointPath, topic: state.topic });
            initStompClient();
        },
        disconnect: disconnect,
        send: send,
        _state: function () { return state; } // debug helper
    };

    // expose globally
    global.AppWebsocket = AppWebsocket;

})(window);

