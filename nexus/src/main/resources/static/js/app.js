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

    // resize hook
    window.addEventListener('resize', scheduleDashboardChartRender);
});
