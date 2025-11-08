function displayActionMsg(msg) {
    $("#actionMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayErrorMsg(msg) {
    $("#errorMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayWarningMsg(msg) {
    $("#warnMsgId").text(msg).show().delay(5000).fadeOut();
}

/* ----------------- D3 sample chart renderer -----------------
   Renders a simple responsive line chart into #dashboard-chart.
   No-op if D3 or target element is absent.
-------------------------------------------------------------- */
function renderSampleChart() {
    if (typeof d3 === 'undefined') {
        console.warn('D3 not loaded; skipping sample chart render.');
        return;
    }
    const container = d3.select('#dashboard-chart');
    if (container.empty()) return;

    // sample data (7 points)
    const data = [
        {day: 'Mon', value: 120},
        {day: 'Tue', value: 150},
        {day: 'Wed', value: 100},
        {day: 'Thu', value: 180},
        {day: 'Fri', value: 140},
        {day: 'Sat', value: 170},
        {day: 'Sun', value: 160}
    ];

    // clear previous content
    container.selectAll('*').remove();

    const margin = {top: 12, right: 20, bottom: 28, left: 38};
    const width = Math.max(200, container.node().clientWidth) - margin.left - margin.right;
    const height = Math.max(120, container.node().clientHeight) - margin.top - margin.bottom;

    const svg = container.append('svg')
        .attr('width', width + margin.left + margin.right)
        .attr('height', height + margin.top + margin.bottom)
        .style('display', 'block');

    const g = svg.append('g').attr('transform', `translate(${margin.left},${margin.top})`);

    const x = d3.scalePoint()
        .domain(data.map(d => d.day))
        .range([0, width])
        .padding(0.5);

    const y = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.value) * 1.12])
        .range([height, 0]);

    const line = d3.line()
        .x(d => x(d.day))
        .y(d => y(d.value))
        .curve(d3.curveMonotoneX);

    // axes
    g.append('g')
        .attr('transform', `translate(0,${height})`)
        .call(d3.axisBottom(x))
        .selectAll('text')
        .attr('font-size', '11px');

    g.append('g')
        .call(d3.axisLeft(y).ticks(4))
        .selectAll('text')
        .attr('font-size', '11px');

    // area fill
    g.append('path')
        .datum(data)
        .attr('fill', 'rgba(13,110,253,0.08)')
        .attr('stroke', 'none')
        .attr('d', d3.area()
            .x(d => x(d.day))
            .y0(height)
            .y1(d => y(d.value))
            .curve(d3.curveMonotoneX)
        );

    // line
    g.append('path')
        .datum(data)
        .attr('fill', 'none')
        .attr('stroke', '#0d6efd')
        .attr('stroke-width', 2.2)
        .attr('d', line);

    // points
    g.selectAll('.point')
        .data(data)
        .enter()
        .append('circle')
        .attr('class', 'point')
        .attr('cx', d => x(d.day))
        .attr('cy', d => y(d.value))
        .attr('r', 3.6)
        .attr('fill', '#0d6efd')
        .attr('stroke', '#fff')
        .attr('stroke-width', 1);

    // simple tooltip on hover
    const tooltip = container.append('div')
        .style('position', 'absolute')
        .style('pointer-events', 'none')
        .style('background', '#fff')
        .style('border', '1px solid rgba(0,0,0,0.08)')
        .style('padding', '6px 8px')
        .style('font-size', '12px')
        .style('border-radius', '4px')
        .style('box-shadow', '0 6px 16px rgba(0,0,0,0.06)')
        .style('display', 'none');

    g.selectAll('.point')
        .on('mouseenter', function(event, d) {
            tooltip.style('display', 'block')
                .html(`<strong>${d.day}</strong><div style="color:#0d6efd">${d.value}</div>`);
        })
        .on('mousemove', function(event) {
            tooltip.style('left', (event.layerX + 12) + 'px')
                   .style('top', (event.layerY - 18) + 'px');
        })
        .on('mouseleave', function() {
            tooltip.style('display', 'none');
        });
}

// redraw on resize (debounced)
let __dashboardChartResizeTimer = null;
function scheduleDashboardChartRender() {
    clearTimeout(__dashboardChartResizeTimer);
    __dashboardChartResizeTimer = setTimeout(renderSampleChart, 180);
}

// Sidebar expand/collapse and submenu logic
// Use delegated handlers so clicks propagate even if fragments are replaced dynamically
$(function() {
    console.log('app.js ready - setting up delegated handlers');

    const sidebarSelector = '.sidebar';
    const toggleSelector = '.sidebar-toggle';

    // Helper to set icon class and aria state
    function setToggleState($toggle, expanded) {
        $toggle.toggleClass('expanded', expanded);
        $toggle.attr('aria-expanded', expanded ? 'true' : 'false');
    }

    // Toggle sidebar expansion (delegated)
    $(document).on('click', toggleSelector, function(e) {
        console.log('Toggle element clicked (delegated)');
        e.preventDefault();
        e.stopPropagation();

        const $toggle = $(this);
        const $sidebar = $(sidebarSelector).first();

        const nowExpanded = !$sidebar.hasClass('expanded');
        $sidebar.toggleClass('expanded', nowExpanded);

        // Update toggle UI/ARIA deterministically
        setToggleState($toggle, nowExpanded);
    });

    // Clicking the icon inside toggle should trigger the toggle as well
    $(document).on('click', toggleSelector + ' i', function(e) {
        // ensure event reaches the toggle handler
        e.preventDefault();
        e.stopPropagation();
        $(this).closest(toggleSelector).trigger('click');
    });

    // Keyboard support: Enter or Space triggers toggle
    $(document).on('keydown', toggleSelector, function(e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            $(this).trigger('click');
        }
    });

    // Submenu click handler (delegated)
    $(document).on('click', '.sidebar-link.has-submenu', function(e) {
        console.log('Submenu link clicked (delegated)');
        e.preventDefault();
        e.stopPropagation();

        const $link = $(this);
        const $submenu = $link.next('.submenu');
        const $arrow = $link.find('.menu-arrow');

        // Close other submenus
        $('.submenu').not($submenu).slideUp().removeClass('show');
        $('.menu-arrow').not($arrow).removeClass('rotated');

        // Toggle current submenu
        $submenu.slideToggle().toggleClass('show');
        $arrow.toggleClass('rotated');
    });

    // Safety / initial sync: if sidebar exists at load, set toggle icon & aria accordingly
    const $sidebarInit = $(sidebarSelector).first();
    const $toggleInit = $(toggleSelector).first();
    if ($sidebarInit.length && $toggleInit.length) {
        const expanded = $sidebarInit.hasClass('expanded');
        setToggleState($toggleInit, expanded);
    }

    // render D3 chart after DOM ready
    try { renderSampleChart(); } catch (err) { console.warn('renderSampleChart error', err); }

    // removed automatic window.wrapCardsWithAnchors() invocation so clicks only act when explicitly wired

    // resize hook
    window.addEventListener('resize', scheduleDashboardChartRender);
});

// helper to perform action for a card click using href + method
window.handleCardAction = function(href, method) {
    if (!href) return;
    method = (method || 'GET').toString().toUpperCase();

    if (method === 'GET') {
        window.location.href = href;
        return;
    }

    if (method === 'POST') {
        // create and submit a simple POST form (no payload). Add CSRF tokens if required.
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = href;
        // preserve same-origin cookies
        form.style.display = 'none';
        document.body.appendChild(form);
        form.submit();
        return;
    }

    // For other HTTP methods, use fetch and attempt to follow redirects
    fetch(href, { method: method, credentials: 'same-origin' })
        .then(response => {
            if (response.redirected) {
                window.location.href = response.url;
                return;
            }
            if (!response.ok) {
                console.warn('Request failed', response.status, response.statusText);
            }
            // optionally handle response body / update UI
        })
        .catch(err => {
            console.warn('handleCardAction error', err);
        });
};

// ----------------------------------------------------------------------
// Converted wrapper => callable helper for onclick usage
// Call from markup as: onclick="wrapCardOnClick(event, '/Monitoring/Agent/Landing', 'GET')"
window.wrapCardOnClick = function(e, href, method) {
    try {
        // support being called with (this, href, method) where first arg might be element
        if (e && e.tagName) {
            // called as onclick="wrapCardOnClick(this, href, method)"
            var el = e;
            e = window.event || {};
            e.preventDefault = e.preventDefault || function(){};
            // no-op
        }
        if (e && typeof e.preventDefault === 'function') {
            e.preventDefault();
        }
    } catch (err) {
        // defensive
    }
    // delegating to centralized action handler
    window.handleCardAction(href, method);
};
// ----------------------------------------------------------------------
// ----------------- Live Events stream simulator -----------------
(function() {
    const LIVE_EVENTS_MAX = 150;
    const PREPOPULATE = 20;
    const INTERVAL_MS = 900;

    const users = ['John Doe','Jane Smith','Alice','Bob','Eve','Service Bot','AutoScaler'];
    const actions = [
        'deployed', 'restarted', 'scaled up', 'scaled down', 'failed to start',
        'connected', 'disconnected', 'reported error', 'recovered'
    ];
    const services = ['service-a','auth','worker','db-cluster','ingest','api-gateway'];

    let liveTimer = null;
    let livePaused = false;
    // notification state
    let notifCount = 0;

    function fmtTime(d) {
        return d.toLocaleTimeString();
    }

    function randomFrom(arr) {
        return arr[Math.floor(Math.random()*arr.length)];
    }

    function generateRandomEvent() {
        const user = randomFrom(users);
        const action = randomFrom(actions);
        const service = randomFrom(services);
        const time = new Date();
        return { user, action, service, time };
    }

    function createEventLI(ev) {
        const li = document.createElement('li');
        li.className = 'activity-item py-1';
        li.style.display = 'flex';
        li.style.gap = '0.5rem';

        const iconWrap = document.createElement('div');
        iconWrap.className = 'me-2';
        iconWrap.innerHTML = `<i class="bi bi-broadcast" style="font-size:1.05rem;color:#0d6efd;"></i>`;

        const body = document.createElement('div');
        body.className = 'small';
        const title = document.createElement('div');
        title.innerHTML = `<strong>${escapeHtml(ev.user)}</strong> ${escapeHtml(ev.action)} <span class="text-muted">${escapeHtml(ev.service)}</span>`;
        const ts = document.createElement('div');
        ts.className = 'text-muted';
        ts.style.fontSize = '11px';
        ts.textContent = fmtTime(ev.time);

        body.appendChild(title);
        body.appendChild(ts);

        li.appendChild(iconWrap);
        li.appendChild(body);
        return li;
    }

    // escape minimal HTML to avoid injection when using sample data
    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function updateCount(listEl, countEl) {
        if (!countEl) return;
        countEl.textContent = `${listEl.children.length} events`;
    }

    // notification helpers
    function updateBadge() {
        const badge = document.getElementById('notif-badge');
        if (!badge) return;
        if (notifCount > 0) {
            badge.textContent = String(notifCount);
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
    function incrementNotification(by = 1) {
        notifCount += by;
        updateBadge();
        // brief pulse (class toggle) if button exists
        const btn = document.getElementById('notif-btn');
        if (btn) {
            btn.classList.add('notif-pulse');
            setTimeout(() => btn.classList.remove('notif-pulse'), 450);
        }
    }
    function resetNotification() {
        notifCount = 0;
        updateBadge();
    }

    function addLiveEventToList(listEl, ev, countEl) {
        const li = createEventLI(ev);
        // newest at top:
        if (listEl.firstChild) listEl.insertBefore(li, listEl.firstChild);
        else listEl.appendChild(li);

        // trim if over cap
        while (listEl.children.length > LIVE_EVENTS_MAX) {
            listEl.removeChild(listEl.lastChild);
        }

        updateCount(listEl, countEl);

        // increment notification unless the live-events wrapper is scrolled to top (user sees newest)
        try {
            const wrapper = document.getElementById('live-events-wrapper');
            if (!wrapper) {
                // if wrapper absent, increment
                incrementNotification();
            } else {
                // if user not scrolled to top, increment
                if (wrapper.scrollTop > 0) {
                    incrementNotification();
                } else {
                    // if scrolled at top, user likely sees new items — do not increment
                }
            }
        } catch (err) {
            // on any error, increment to be safe
            incrementNotification();
        }
    }

    function startLiveEvents(containerId) {
        const listEl = document.getElementById(containerId);
        const countEl = document.getElementById('live-events-count');
        if (!listEl) return;

        // prepopulate
        for (let i=0; i<PREPOPULATE; i++) {
            const ev = generateRandomEvent();
            // stagger timestamp backwards a bit for realism
            ev.time = new Date(Date.now() - (PREPOPULATE - i) * 60000);
            addLiveEventToList(listEl, ev, countEl);
        }

        // start ticking
        liveTimer = setInterval(() => {
            if (livePaused) return;
            const ev = generateRandomEvent();
            addLiveEventToList(listEl, ev, countEl);
        }, INTERVAL_MS);
    }

    function stopLiveEvents() {
        if (liveTimer) {
            clearInterval(liveTimer);
            liveTimer = null;
        }
    }

    // Toggle handler
    document.addEventListener('click', function(e) {
        const btn = e.target.closest && e.target.closest('#live-events-toggle');
        if (!btn) return;
        e.preventDefault();
        livePaused = !livePaused;
        btn.textContent = livePaused ? 'Resume' : 'Pause';
        // toggle visual state
        if (livePaused) btn.classList.remove('btn-outline-secondary'), btn.classList.add('btn-outline-primary');
        else btn.classList.remove('btn-outline-primary'), btn.classList.add('btn-outline-secondary');
    });

    // Expose start/stop and control functions for manual control if needed
    window.startLiveEvents = function() { startLiveEvents('live-events-list'); };
    window.stopLiveEvents = function() { stopLiveEvents(); };

    // Expose openLiveEvents: scrolls Live Events card into view, focuses wrapper and resets badge
    window.openLiveEvents = function(e) {
        try {
            const card = document.getElementById('live-events-card');
            const wrapper = document.getElementById('live-events-wrapper');

            // If the card is hidden, unhide it
            if (card && (card.style.display === 'none' || window.getComputedStyle(card).display === 'none')) {
                card.style.display = ''; // let CSS default apply (it was display:none earlier)
            }

            // If the stream isn't started yet, start it (ensures content exists)
            if (typeof startLiveEvents === 'function') {
                // startLiveEvents will guard if list missing or already started
                try { startLiveEvents('live-events-list'); } catch (err) { /* ignore */ }
            }

            if (card) {
                // smooth scroll the card into center of viewport
                card.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
            if (wrapper) {
                // bring newest items into view (top)
                wrapper.scrollTop = 0;
                // focus for keyboard users
                wrapper.focus && wrapper.focus();
            }
        } catch (err) {
            console.warn('openLiveEvents error', err);
        }
        // reset notification badge after opening
        try { resetNotification(); } catch (err) { /* ignore */ }

        // prevent outer handlers if called from onclick
        if (e && typeof e.preventDefault === 'function') e.preventDefault();
    };

    // Auto-start when DOM ready (only if landing has live-events-list)
    document.addEventListener('DOMContentLoaded', function() {
        if (document.getElementById('live-events-list')) {
            try { startLiveEvents('live-events-list'); } catch (err) { console.warn('startLiveEvents error', err); }
        }
    });
})();
// ----------------- end live events stream -----------------
