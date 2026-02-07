// File: nexus-web/src/main/resources/static/js/app.js
// Expose wrapCardOnClick globally for inline `onclick="wrapCardOnClick(...)"` usage.

(function () {
  function getCsrfToken() {
    // Common meta names for CSRF tokens
    var meta = document.querySelector('meta[name="_csrf"], meta[name="csrf-token"], meta[name="csrf"]');
    if (!meta) return null;
    return meta.getAttribute('content');
  }

  function submitForm(href, method) {
    var form = document.createElement('form');
    form.style.display = 'none';
    form.method = (method && method.toUpperCase() === 'GET') ? 'GET' : 'POST';
    form.action = href;

    // For non-POST methods, use _method pattern commonly used by frameworks
    var m = (method || 'GET').toUpperCase();
    if (m !== 'GET' && m !== 'POST') {
      var override = document.createElement('input');
      override.type = 'hidden';
      override.name = '_method';
      override.value = m;
      form.appendChild(override);
    }

    // Attach CSRF token if available
    var csrf = getCsrfToken();
    if (csrf) {
      var csrfInput = document.createElement('input');
      csrfInput.type = 'hidden';
      // common param names: _csrf, csrf-token (adjust if your backend expects a specific name)
      csrfInput.name = '_csrf';
      csrfInput.value = csrf;
      form.appendChild(csrfInput);
    }

    document.body.appendChild(form);
    form.submit();
  }

  window.wrapCardOnClick = function (event, href, method) {
    // allow direct invocation without event
    var e = event || window.event;

    // Resolve href/method from args or from clicked element dataset
    if (!href) {
      var el = e && e.currentTarget ? e.currentTarget : null;
      if (el && el.dataset && el.dataset.href) href = el.dataset.href;
    }
    if (!method) {
      var el2 = e && e.currentTarget ? e.currentTarget : null;
      if (el2 && el2.dataset && el2.dataset.method) method = el2.dataset.method;
    }
    href = href || '/';
    method = (method || 'GET').toUpperCase();

    // If event present, ignore non-left clicks and let native anchors/buttons proceed
    if (e) {
      // Ignore right/middle click
      if (e.button && e.button !== 0) return;

      // If click target is an interactive control (link, button, input...), allow default
      var interactive = e.target && e.target.closest && e.target.closest('a[href], button, input, textarea, select, label');
      if (interactive) return;

      // If user requested new tab/window via modifier keys, open there
      if (e.ctrlKey || e.metaKey || e.shiftKey) {
        window.open(href, '_blank');
        e.preventDefault && e.preventDefault();
        return;
      }

      e.preventDefault && e.preventDefault();
    }

    if (method === 'GET') {
      // simple navigation
      window.location.href = href;
      return;
    }

    // for POST/PUT/DELETE etc., submit a form (supports common CSRF meta token)
    submitForm(href, method);
  };
})();

/* ==========================================================================
   1.5 SPA NAVIGATION (AJAX-based content loading)
   ========================================================================== */
(function() {
    'use strict';

    var SPA = {
        contentSelector: '.main-content',
        loadingClass: 'spa-loading',
        fadeClass: 'spa-fade',
        currentRequest: null,
        initialized: false
    };

    /**
     * Initialize SPA navigation
     */
    function init() {
        if (SPA.initialized) return;
        SPA.initialized = true;

        // Create loading indicator
        createLoadingIndicator();

        // Intercept link clicks
        setupLinkInterception();

        // Handle browser back/forward
        window.addEventListener('popstate', handlePopState);

        // Mark initial page in history
        if (!history.state) {
            history.replaceState({ url: window.location.href, title: document.title }, document.title, window.location.href);
        }

        console.log('SPA Navigation initialized');
    }

    /**
     * Create the loading indicator elements
     */
    function createLoadingIndicator() {
        // Top progress bar
        if (!document.querySelector('.spa-progress')) {
            var progress = document.createElement('div');
            progress.className = 'spa-progress';
            progress.innerHTML = '<div class="spa-progress-bar"></div>';
            document.body.appendChild(progress);
        }

        // Content overlay (subtle)
        if (!document.querySelector('.spa-overlay')) {
            var overlay = document.createElement('div');
            overlay.className = 'spa-overlay';
            overlay.innerHTML = '<div class="spa-spinner"></div>';
            document.body.appendChild(overlay);
        }
    }

    /**
     * Show loading state
     */
    function showLoading() {
        document.body.classList.add(SPA.loadingClass);
        var $content = $(SPA.contentSelector);
        if ($content.length) {
            $content.addClass(SPA.fadeClass);
        }
    }

    /**
     * Hide loading state
     */
    function hideLoading() {
        document.body.classList.remove(SPA.loadingClass);
        var $content = $(SPA.contentSelector);
        if ($content.length) {
            $content.removeClass(SPA.fadeClass);
        }
    }

    /**
     * Navigate to a URL via AJAX
     */
    function navigateTo(url, pushToHistory) {
        if (pushToHistory === undefined) pushToHistory = true;

        // Cancel any pending request
        if (SPA.currentRequest) {
            SPA.currentRequest.abort();
        }

        // Show loading
        showLoading();

        // Make AJAX request
        SPA.currentRequest = $.ajax({
            url: url,
            type: 'GET',
            headers: {
                'X-SPA-Request': 'true',
                'X-Requested-With': 'XMLHttpRequest'
            },
            success: function(response) {
                handleResponse(response, url, pushToHistory);
            },
            error: function(xhr, status, error) {
                if (status === 'abort') return;

                console.error('SPA navigation error:', error);
                // Fallback to regular navigation on error
                window.location.href = url;
            },
            complete: function() {
                SPA.currentRequest = null;
            }
        });
    }

    /**
     * Handle the AJAX response
     */
    function handleResponse(html, url, pushToHistory) {
        try {
            // Parse the response HTML
            var $response = $('<div>').html(html);

            // Extract the main content
            var $newContent = $response.find(SPA.contentSelector);

            if ($newContent.length === 0) {
                // If no main-content found, try to get the body content
                // This handles full page responses
                var bodyMatch = html.match(/<body[^>]*>([\s\S]*?)<\/body>/i);
                if (bodyMatch) {
                    $response = $('<div>').html(bodyMatch[1]);
                    $newContent = $response.find(SPA.contentSelector);
                }
            }

            if ($newContent.length === 0) {
                // Fallback to regular navigation
                window.location.href = url;
                return;
            }

            // Extract and update title
            var newTitle = $response.find('title').text() || document.title;
            var titleMatch = html.match(/<title[^>]*>(.*?)<\/title>/i);
            if (titleMatch) {
                newTitle = titleMatch[1];
            }
            document.title = newTitle;

            // Update the content with animation
            var $currentContent = $(SPA.contentSelector);

            // Prepare new content (hidden initially)
            $newContent.addClass('spa-enter');

            // Replace content
            $currentContent.replaceWith($newContent);

            // Trigger enter animation
            requestAnimationFrame(function() {
                $newContent.removeClass('spa-enter').addClass('spa-enter-active');

                setTimeout(function() {
                    $newContent.removeClass('spa-enter-active');
                    hideLoading();
                }, 300);
            });

            // Update history
            if (pushToHistory) {
                history.pushState({ url: url, title: newTitle }, newTitle, url);
            }

            // Update active sidebar link
            updateActiveSidebarLink(url);

            // Re-initialize page-specific scripts
            reinitializePage();

            // Scroll to top
            window.scrollTo({ top: 0, behavior: 'smooth' });

        } catch (e) {
            console.error('Error processing SPA response:', e);
            window.location.href = url;
        }
    }

    /**
     * Handle browser back/forward navigation
     */
    function handlePopState(e) {
        if (e.state && e.state.url) {
            navigateTo(e.state.url, false);
        }
    }

    /**
     * Update active sidebar link based on URL
     */
    function updateActiveSidebarLink(url) {
        var path = new URL(url, window.location.origin).pathname;

        $('.sidebar-link').removeClass('active');

        var $matchedLink = null;
        var longestMatch = 0;

        $('.sidebar-link').each(function() {
            var href = $(this).attr('href');
            if (href && href !== '#' && path.indexOf(href) === 0) {
                if (href.length > longestMatch) {
                    longestMatch = href.length;
                    $matchedLink = $(this);
                }
            }
        });

        if ($matchedLink) {
            $matchedLink.addClass('active');

            // Expand parent submenu if exists
            var $submenu = $matchedLink.closest('.submenu');
            if ($submenu.length) {
                $submenu.addClass('show').show();
                $submenu.prev('.sidebar-link.has-submenu').find('.menu-arrow').addClass('rotated');
            }
        }
    }

    /**
     * Re-initialize page-specific scripts after content load
     */
    function reinitializePage() {
        // Hide any existing loaders
        if (window.AppUI) {
            AppUI.hidePageLoader();
        }

        // Add loaded class
        document.body.classList.add('loaded');

        // Re-initialize counters
        var $counters = $('.stat-value[data-count]');
        $counters.each(function(index) {
            var $el = $(this);
            var value = parseInt($el.attr('data-count'), 10) || 0;

            if (value >= 0) {
                $el.text('0');
                setTimeout(function() {
                    if (window.AppUI) {
                        AppUI.animateCounter($el, value, 600);
                    } else {
                        $el.text(value);
                    }
                }, 100 + (index * 80));
            }
        });

        // Re-initialize chart if present
        if ($('#dashboard-chart').length && window.AppUI) {
            AppUI.revealChart('dashboard-chart', function() {
                if (typeof drawMaterialChart === 'function') {
                    drawMaterialChart();
                }
            }, 600);
        }

        // Trigger custom event for page-specific initialization
        $(document).trigger('spa:pageLoaded');
    }

    /**
     * Setup link interception
     */
    function setupLinkInterception() {
        $(document).on('click', 'a[href], [data-href]', function(e) {
            var $link = $(this);

            // Skip if modifier keys are pressed (open in new tab)
            if (e.ctrlKey || e.metaKey || e.shiftKey) return;

            // Skip buttons and interactive elements
            if (e.target.closest('button, input, textarea, select')) return;

            // Get URL
            var href = $link.attr('href') || $link.attr('data-href');

            // Skip invalid URLs
            if (!href || href === '#' || href.startsWith('javascript:')) return;

            // Skip external links
            if (href.startsWith('http') && !href.startsWith(window.location.origin)) return;
            if (href.startsWith('mailto:') || href.startsWith('tel:')) return;

            // Skip if has submenu
            if ($link.hasClass('has-submenu')) return;

            // Skip dropdown toggles
            if ($link.attr('data-bs-toggle')) return;

            // Skip non-GET methods
            var method = ($link.attr('data-method') || 'GET').toUpperCase();
            if (method !== 'GET') return;

            // Prevent default and navigate via SPA
            e.preventDefault();
            e.stopPropagation();

            navigateTo(href);
        });
    }

    // Initialize on DOM ready
    $(function() {
        init();
    });

    // Expose SPA API globally
    window.SPA = {
        navigateTo: navigateTo,
        refresh: function() {
            navigateTo(window.location.href, false);
        }
    };

})();

/* ==========================================================================
   2. MESSAGE DISPLAY
   ========================================================================== */
function displayActionMsg(msg) {
    $("#actionMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayErrorMsg(msg) {
    $("#errorMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayWarningMsg(msg) {
    $("#warnMsgId").text(msg).show().delay(5000).fadeOut();
}

/* ==========================================================================
   3. UI UTILITIES MODULE (Material Design Enhancements)
   ========================================================================== */
window.AppUI = (function() {
    'use strict';

    var notifCount = 0;

    /**
     * Animate a number counting up with Material motion
     */
    function animateCounter($el, targetValue, duration) {
        duration = duration || 600;
        var startValue = 0;
        var startTime = null;

        $el.addClass('animate');

        function step(timestamp) {
            if (!startTime) startTime = timestamp;
            var progress = Math.min((timestamp - startTime) / duration, 1);

            // Material decelerate easing
            var eased = 1 - Math.pow(1 - progress, 3);
            var current = Math.floor(startValue + (targetValue - startValue) * eased);

            $el.text(current.toLocaleString());

            if (progress < 1) {
                requestAnimationFrame(step);
            } else {
                $el.text(targetValue.toLocaleString());
                $el.addClass('visible');
            }
        }

        requestAnimationFrame(step);
    }

    /**
     * Initialize all stat counters on page
     */
    function initCounters() {
        $('.stat-value[data-count]').each(function() {
            var $el = $(this);
            var target = parseInt($el.data('count'), 10) || 0;
            var delay = parseInt($el.data('delay'), 10) || 0;

            setTimeout(function() {
                animateCounter($el, target, 600);
            }, delay);
        });
    }

    /**
     * Show skeleton loader, then reveal content
     */
    function revealChart(containerId, callback, delay) {
        delay = delay || 600;
        var $container = $('#' + containerId);
        var $skeleton = $container.find('.skeleton-loader');

        if ($skeleton.length === 0) {
            $container.prepend('<div class="skeleton-loader"></div>');
            $skeleton = $container.find('.skeleton-loader');
        }

        setTimeout(function() {
            $skeleton.addClass('hidden');
            if (typeof callback === 'function') {
                callback();
            }
        }, delay);
    }

    /**
     * Update notification badge with Material animation
     */
    function updateNotifBadge(count) {
        notifCount = count || 0;
        var $badge = $('#notif-badge');

        if ($badge.length === 0) return;

        if (notifCount <= 0) {
            $badge.hide();
        } else {
            $badge.text(notifCount > 99 ? '99+' : notifCount).show();
            $badge.addClass('pulse');
            setTimeout(function() {
                $badge.removeClass('pulse');
            }, 400);
        }
    }

    /**
     * Add live event to list with Material motion
     */
    function addLiveEvent(text, meta) {
        var $list = $('#live-events-list');
        if ($list.length === 0) return;

        $list.find('.no-events').remove();

        var time = new Date().toLocaleTimeString();
        var $item = $('<li class="list-group-item event"></li>');
        $item.html(
            '<div class="event-text" style="font-weight: 400; color: rgba(0,0,0,0.87);">' + escapeHtml(text) + '</div>' +
            '<div class="meta">' + escapeHtml(meta || time) + '</div>'
        );

        // Material enter animation
        $item.css({ opacity: 0, transform: 'translateX(-16px)' });
        $list.prepend($item);

        setTimeout(function() {
            $item.css({
                opacity: 1,
                transform: 'translateX(0)',
                transition: 'all 250ms cubic-bezier(0, 0, 0.2, 1)'
            });
        }, 10);

        updateNotifBadge(notifCount + 1);
        $list.children().slice(50).remove();
        $('#live-events-count').text($list.children().length + ' events');
    }

    /**
     * Clear all live events
     */
    function clearLiveEvents() {
        var $list = $('#live-events-list');
        $list.empty();
        $list.append('<li class="list-group-item no-events" style="color: rgba(0,0,0,0.38); text-align: center; padding: 2rem;">No events yet.</li>');
        updateNotifBadge(0);
        $('#live-events-count').text('0 events');
    }

    /**
     * Toggle live events panel with Material motion
     */
    function toggleLiveEvents() {
        var $panel = $('#live-events-panel');
        var $card = $('#live-events-card');

        if ($panel.length) {
            var isHidden = $panel.attr('aria-hidden') === 'true';
            $panel.attr('aria-hidden', isHidden ? 'false' : 'true');
            $panel.toggleClass('show', isHidden);
        }

        if ($card.length) {
            $card.slideToggle(250);
        }
    }

    /**
     * Show Material snackbar notification
     */
    function showSnackbar(text, duration, actionText, actionCallback) {
        duration = duration || 4000;

        // Remove existing snackbar
        $('.md-snackbar').remove();

        var $snackbar = $('<div class="md-snackbar"></div>');
        $snackbar.css({
            position: 'fixed',
            bottom: '24px',
            left: '50%',
            transform: 'translateX(-50%) translateY(100px)',
            background: '#323232',
            color: '#fff',
            padding: '14px 24px',
            borderRadius: '4px',
            boxShadow: '0 3px 5px -1px rgba(0,0,0,0.2), 0 6px 10px 0 rgba(0,0,0,0.14), 0 1px 18px 0 rgba(0,0,0,0.12)',
            zIndex: 9999,
            display: 'flex',
            alignItems: 'center',
            gap: '24px',
            fontFamily: 'Roboto, sans-serif',
            fontSize: '14px',
            fontWeight: 400,
            maxWidth: '568px',
            transition: 'transform 250ms cubic-bezier(0, 0, 0.2, 1)'
        });

        $snackbar.append('<span>' + escapeHtml(text) + '</span>');

        if (actionText) {
            var $action = $('<button style="background:none;border:none;color:#bb86fc;font-weight:500;text-transform:uppercase;cursor:pointer;padding:0;font-size:14px;letter-spacing:0.02em;">' + escapeHtml(actionText) + '</button>');
            $action.on('click', function() {
                if (actionCallback) actionCallback();
                hideSnackbar($snackbar);
            });
            $snackbar.append($action);
        }

        $('body').append($snackbar);

        setTimeout(function() {
            $snackbar.css('transform', 'translateX(-50%) translateY(0)');
        }, 10);

        setTimeout(function() {
            hideSnackbar($snackbar);
        }, duration);
    }

    function hideSnackbar($snackbar) {
        $snackbar.css('transform', 'translateX(-50%) translateY(100px)');
        setTimeout(function() {
            $snackbar.remove();
        }, 250);
    }

    /**
     * Escape HTML to prevent XSS
     */
    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Hide page loader with Material fade
     */
    function hidePageLoader() {
        var $loader = $('.page-loader');
        if ($loader.length) {
            setTimeout(function() {
                $loader.addClass('hidden');
                setTimeout(function() { $loader.remove(); }, 400);
            }, 200);
        }
    }

    /**
     * Navigate with smooth transition
     */
    function navigateTo(href, method) {
        if (window.PageTransition) {
            window.PageTransition.navigate(href, method);
        } else {
            window.location.href = href;
        }
    }

    // Public API
    return {
        animateCounter: animateCounter,
        initCounters: initCounters,
        revealChart: revealChart,
        updateNotifBadge: updateNotifBadge,
        addLiveEvent: addLiveEvent,
        clearLiveEvents: clearLiveEvents,
        toggleLiveEvents: toggleLiveEvents,
        showSnackbar: showSnackbar,
        showHeaderNotification: showSnackbar,
        hidePageLoader: hidePageLoader,
        navigateTo: navigateTo
    };
})();

/* Helper for notification button */
function openLiveEvents(e) {
    if (e) e.preventDefault();
    window.AppUI.toggleLiveEvents();
}

/* ==========================================================================
   4. SIDEBAR & NAVIGATION (Modern Rail)
   ========================================================================== */
$(function () {
    console.log('Nexus Dashboard - Material Design initialized');

    var sidebarSelector = '.sidebar';
    var toggleSelector = '.sidebar-toggle';

    function setToggleState($toggle, expanded) {
        $toggle.toggleClass('expanded', expanded);
        $toggle.attr('aria-expanded', expanded ? 'true' : 'false');
    }

    // Sidebar toggle click
    $(document).on('click', toggleSelector, function (e) {
        e.preventDefault();
        var $toggle = $(this);
        var $sidebar = $(sidebarSelector).first();
        var nowExpanded = !$sidebar.hasClass('expanded');
        $sidebar.toggleClass('expanded', nowExpanded);
        setToggleState($toggle, nowExpanded);

        // Save preference
        try {
            localStorage.setItem('sidebar-expanded', nowExpanded ? 'true' : 'false');
        } catch (err) {}
    });

    $(document).on('click', toggleSelector + ' i', function (e) {
        e.preventDefault();
        $(this).closest(toggleSelector).trigger('click');
    });

    $(document).on('keydown', toggleSelector, function (e) {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            $(this).trigger('click');
        }
    });

    // Submenu toggle
    $(document).on('click', '.sidebar-link.has-submenu', function (e) {
        e.preventDefault();
        var $link = $(this);
        var $submenu = $link.next('.submenu');
        var $arrow = $link.find('.menu-arrow');

        // Close other submenus
        $('.submenu').not($submenu).slideUp(150).removeClass('show');
        $('.menu-arrow').not($arrow).removeClass('rotated');

        // Toggle current submenu
        $submenu.slideToggle(150).toggleClass('show');
        $arrow.toggleClass('rotated');
    });

    $(document).on('click', '.menu-arrow', function (e) {
        e.preventDefault();
        e.stopPropagation();
        $(this).closest('.sidebar-link.has-submenu').trigger('click');
    });

    // Initialize sidebar state
    var $sidebarInit = $(sidebarSelector).first();
    var $toggleInit = $(toggleSelector).first();
    if ($sidebarInit.length && $toggleInit.length) {
        var savedState = false;
        try {
            savedState = localStorage.getItem('sidebar-expanded') === 'true';
        } catch (err) {}

        if (savedState) {
            $sidebarInit.addClass('expanded');
        }
        setToggleState($toggleInit, savedState);
    }

    // Set active link based on current URL
    setActiveSidebarLink();

    // Add ripple effect
    addRippleEffect();

    // Initialize dashboard
    initDashboard();
});

/**
 * Set active sidebar link based on current URL
 */
function setActiveSidebarLink() {
    var currentPath = window.location.pathname;

    $('.sidebar-link').removeClass('active');

    // Find matching link
    var $matchedLink = null;
    var longestMatch = 0;

    $('.sidebar-link').each(function() {
        var href = $(this).attr('href');
        if (href && href !== '#' && currentPath.indexOf(href) === 0) {
            if (href.length > longestMatch) {
                longestMatch = href.length;
                $matchedLink = $(this);
            }
        }
    });

    if ($matchedLink) {
        $matchedLink.addClass('active');

        // Expand parent submenu if exists
        var $submenu = $matchedLink.closest('.submenu');
        if ($submenu.length) {
            $submenu.addClass('show').show();
            $submenu.prev('.sidebar-link.has-submenu').find('.menu-arrow').addClass('rotated');
        }
    } else {
        // Default to first link (Dashboard)
        $('.sidebar-link').first().addClass('active');
    }
}

/**
 * Add Material ripple effect to buttons
 */
function addRippleEffect() {
    $(document).on('click', '.btn, .sidebar-link, .dropdown-item', function(e) {
        var $el = $(this);

        // Remove old ripples
        $el.find('.ripple-effect').remove();

        var rect = this.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;

        var $ripple = $('<span class="ripple-effect"></span>');
        $ripple.css({
            position: 'absolute',
            borderRadius: '50%',
            background: 'currentColor',
            opacity: 0.2,
            width: '0',
            height: '0',
            left: x + 'px',
            top: y + 'px',
            transform: 'translate(-50%, -50%)',
            pointerEvents: 'none'
        });

        $el.css('position', 'relative').css('overflow', 'hidden');
        $el.append($ripple);

        var size = Math.max(rect.width, rect.height) * 2;

        $ripple.animate({
            width: size,
            height: size,
            opacity: 0
        }, 600, function() {
            $ripple.remove();
        });
    });
}

/* ==========================================================================
   5. DASHBOARD INITIALIZATION
   ========================================================================== */
function initDashboard() {
    // Hide page loader and transition
    AppUI.hidePageLoader();
    if (window.PageTransition) {
        window.PageTransition.hide();
    }

    // Add loaded class to body
    document.body.classList.add('loaded');

    // Initialize counters with staggered Material animation
    var $counters = $('.stat-value[data-count]');
    $counters.each(function(index) {
        var $el = $(this);
        var value = parseInt($el.attr('data-count'), 10) || 0;

        if (value >= 0) {
            $el.text('0');
            setTimeout(function() {
                AppUI.animateCounter($el, value, 600);
            }, 100 + (index * 80));
        }
    });

    // Initialize chart skeleton with reveal
    if ($('#dashboard-chart').length) {
        AppUI.revealChart('dashboard-chart', function() {
            drawMaterialChart();
        }, 800);
    }
}

/**
 * Draw a Material-styled decorative chart
 */
function drawMaterialChart() {
    var $chart = $('#dashboard-chart');
    if ($chart.find('svg').length) return;

    var width = $chart.width();
    var height = $chart.height();

    // Generate smooth wave points
    var points = [];
    var numPoints = 24;
    for (var i = 0; i < numPoints; i++) {
        var x = (i / (numPoints - 1)) * width;
        var y = height * 0.3 + Math.sin(i * 0.5) * height * 0.15 + Math.random() * height * 0.1;
        points.push(x + ',' + y);
    }

    var svg = '<svg width="100%" height="100%" viewBox="0 0 ' + width + ' ' + height + '" preserveAspectRatio="none">' +
        '<defs>' +
        '<linearGradient id="mdChartGradient" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0%" stop-color="rgba(63, 81, 181, 0.25)"/>' +
        '<stop offset="100%" stop-color="rgba(63, 81, 181, 0.02)"/>' +
        '</linearGradient>' +
        '</defs>' +
        '<polygon fill="url(#mdChartGradient)" points="0,' + height + ' ' + points.join(' ') + ' ' + width + ',' + height + '"/>' +
        '<polyline fill="none" stroke="#3f51b5" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" points="' + points.join(' ') + '"/>' +
        '</svg>';

    $chart.append(svg);
}
