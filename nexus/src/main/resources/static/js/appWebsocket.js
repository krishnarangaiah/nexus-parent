/* appWebsocket.js
   Encapsulate SockJS/STOMP logic for pages to call AppWebsocket.init(options).
   Options:
     - endpointPath (default '/ws')
     - topic (default '/topic/agents/status')
     - reconnectInitial (ms)
     - reconnectMax (ms)
     - liveEvents: boolean
     - liveEventsPanelId: string
     - liveEventsListId: string
     - liveEventsMax: number
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
        useSockJS: null,
        // live events state
        liveEvents: false,
        liveEventsPanelId: 'live-events-panel',
        liveEventsListId: 'live-events-list',
        liveEventsMax: 50,
        liveEventsUnread: 0
    };

    // Pretty-print helpers
    function numberWithCommas(x) {
        if (x === null || typeof x === 'undefined') return '-';
        return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }
    function formatBytes(bytes) {
        if (bytes === null || typeof bytes === 'undefined') return '-';
        bytes = Number(bytes);
        if (isNaN(bytes)) return bytes;
        var units = ['B','KB','MB','GB','TB'];
        var i = 0;
        while (bytes >= 1024 && i < units.length-1) {
            bytes = bytes / 1024;
            i++;
        }
        return Math.round(bytes * 10) / 10 + ' ' + units[i];
    }

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

    function appendLiveEvent(payload) {
        try {
            if (!state.liveEvents) return;
            var listEl = document.getElementById(state.liveEventsListId);
            if (!listEl) return;
            // remove placeholder "no-events"
            if (listEl.children.length === 1 && listEl.children[0].classList.contains('no-events')) {
                listEl.innerHTML = '';
            }
            var li = document.createElement('li');
            li.className = 'list-group-item';
            li.textContent = payload;
            // prepend
            if (listEl.firstChild) listEl.insertBefore(li, listEl.firstChild);
            else listEl.appendChild(li);
            // trim
            while (listEl.children.length > state.liveEventsMax) {
                listEl.removeChild(listEl.lastChild);
            }
            // update unread badge if panel hidden
            var panel = document.getElementById(state.liveEventsPanelId);
            var badge = document.getElementById('notif-badge');
            if (panel && badge) {
                var isVisible = window.getComputedStyle(panel).display !== 'none';
                if (!isVisible) {
                    state.liveEventsUnread = Math.min(state.liveEventsUnread + 1, 999);
                    badge.textContent = state.liveEventsUnread;
                    badge.style.display = 'inline-block';
                }
            }
        } catch (e) {
            safeLog('appendLiveEvent failed', e);
        }
    }

    function clearLiveEvents() {
        try {
            var listEl = document.getElementById(state.liveEventsListId);
            if (!listEl) return;
            listEl.innerHTML = '<li class="list-group-item no-events">No events yet.</li>';
            state.liveEventsUnread = 0;
            var badge = document.getElementById('notif-badge');
            if (badge) { badge.style.display = 'none'; badge.textContent = '0'; }
        } catch (e) { safeLog('clearLiveEvents failed', e); }
    }

    function toggleLiveEvents(evt) {
        try {
            var panel = document.getElementById(state.liveEventsPanelId);
            if (!panel) return;
            var showing = window.getComputedStyle(panel).display !== 'none';
            if (showing) {
                panel.style.display = 'none';
            } else {
                panel.style.display = 'block';
                // reset unread count and hide badge
                state.liveEventsUnread = 0;
                var badge = document.getElementById('notif-badge');
                if (badge) { badge.style.display = 'none'; badge.textContent = '0'; }
            }
        } catch (e) { safeLog('toggleLiveEvents failed', e); }
    }

    function handleStatusMessage(message) {
        try {
            var body = message && (message.body || message);
            safeLog('Received', state.topic, 'message body:', body);
            var payload = null;
            if (typeof body === 'string') {
                try {
                    payload = JSON.parse(body);
                } catch (e) {
                    safeLog('Failed to parse message body as JSON', e);
                    return;
                }
            } else {
                payload = body;
            }

            if (!payload) {
                safeLog('Ignoring payload without agentId', payload);
                return;
            }

            // Add a compact live-event entry for UI
            try {
                appendLiveEvent(payload);
            } catch (e) {
                safeLog('appendLiveEvent error', e);
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
            // live events options
            if (options.liveEvents) state.liveEvents = true;
            if (options.liveEventsPanelId) state.liveEventsPanelId = options.liveEventsPanelId;
            if (options.liveEventsListId) state.liveEventsListId = options.liveEventsListId;
            if (options.liveEventsMax) state.liveEventsMax = options.liveEventsMax;
            safeLog('AppWebsocket.init', { endpointPath: state.endpointPath, topic: state.topic, liveEvents: state.liveEvents });
            initStompClient();
        },
        disconnect: disconnect,
        send: send,
        toggleLiveEvents: toggleLiveEvents,
        clearLiveEvents: clearLiveEvents,
        _state: function () { return state; } // debug helper
    };

    // expose globally
    global.AppWebsocket = AppWebsocket;

})(window);
