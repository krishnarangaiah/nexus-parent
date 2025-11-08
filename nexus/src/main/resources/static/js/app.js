function displayActionMsg(msg) {
    $("#actionMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayErrorMsg(msg) {
    $("#errorMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayWarningMsg(msg) {
    $("#warnMsgId").text(msg).show().delay(5000).fadeOut();
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
});
