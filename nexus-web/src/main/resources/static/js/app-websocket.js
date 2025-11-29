// language: javascript
(function (global) {
    class AppWebsocket extends WsConnection {
        constructor(options = {}) {
            super(options || {});
            // live events UI state
            this.liveEventsPanelId = options.liveEventsPanelId || 'live-events-panel';
            this.liveEventsListId = options.liveEventsListId || 'live-events-list';
            this.liveEventsMax = options.liveEventsMax || 50;
            this.liveEventsUnread = 0;
        }

        onConnect() {
            // subscribe to configured topic on connect
            this.subscribe(this.topic);
        }

        onMessage(payload) {
            // payload is expected to be a map of agentId -> agentData
            try {
                this.updateAgentCards(payload);
                this.updateNotificationBadge();
                this.addLiveEvent(payload);
            } catch (e) {
                this.safeLog('AppWebsocket.onMessage error', e);
            }
        }

        updateAgentCards(jsonData) {
            $.each(jsonData, function (agentId, agentData) {
                const $agentCard = $(`#${agentId}`);
                if ($agentCard.length) {
                    if (agentData.displayName) {
                        $agentCard.find('.agent-name').text(agentData.displayName);
                    }
                    if (agentData.status) {
                        $agentCard.find('.agent-status').text(`Status: ${agentData.status}`);
                        $agentCard.toggleClass('agent-online', agentData.status === 'ONLINE');
                        $agentCard.toggleClass('agent-offline', agentData.status !== 'ONLINE');
                    }
                    $.each(agentData, function (key, value) {
                        if (key !== 'agentId' && key !== 'displayName' && key !== 'status') {
                            let $element = $agentCard.find(`.agent-${key}`);
                            if ($element.length) {
                                $element.text(`${key}: ${value}`);
                            } else {
                                const $newElement = $('<p></p>')
                                    .addClass(`agent-${key}`)
                                    .text(`${key}: ${value}`);
                                $agentCard.append($newElement);
                            }
                        }
                    });
                } else {
                    // optional: log missing card
                    // this.safeLog(`Agent with ID ${agentId} not found in the DOM.`);
                }
            });
        }

        updateNotificationBadge() {
            const $badge = $('#notif-badge');
            this.liveEventsUnread = Math.min(this.liveEventsUnread + 1, 999);
            $badge.text(this.liveEventsUnread).show();
        }

        toggleLiveEvents() {
            const $panel = $(`#${this.liveEventsPanelId}`);
            if (!$panel.length) return;
            const showing = $panel.is(':visible');
            if (showing) {
                $panel.hide();
            } else {
                $panel.show();
                this.liveEventsUnread = 0;
                $('#notif-badge').hide().text('0');
            }
        }

        addLiveEvent(payload) {
            const $list = $(`#${this.liveEventsListId}`);
            if (!$list.length) return;
            try {
                const text = JSON.stringify(payload);
                const $item = $('<li></li>').text(text);
                $list.prepend($item);
                // trim list
                $list.find('li').slice(this.liveEventsMax).remove();
            } catch (e) {
                this.safeLog('Failed to add live event', e);
            }
        }
    }

    // Provide a page-friendly singleton with init/toggleLiveEvents
    let singleton = null;
    const PublicApi = {
        init: function (options) {
            if (!singleton) singleton = new AppWebsocket(options || {});
            // allow runtime override of endpoint/topic before connect
            if (options && options.endpointPath) singleton.endpointPath = options.endpointPath;
            if (options && options.topic) singleton.topic = options.topic;
            singleton.initStompClient();
            return singleton;
        },
        toggleLiveEvents: function () {
            if (singleton) singleton.toggleLiveEvents();
        }
    };

    global.AppWebsocket = PublicApi;
})(window);
