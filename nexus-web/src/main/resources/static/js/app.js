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




function displayActionMsg(msg) {
    $("#actionMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayErrorMsg(msg) {
    $("#errorMsgId").text(msg).show().delay(5000).fadeOut();
}

function displayWarningMsg(msg) {
    $("#warnMsgId").text(msg).show().delay(5000).fadeOut();
}

$(function () {
    console.log('app.js ready - setting up delegated handlers');

    const sidebarSelector = '.sidebar';
    const toggleSelector = '.sidebar-toggle';

    function setToggleState($toggle, expanded) {
        $toggle.toggleClass('expanded', expanded);
        $toggle.attr('aria-expanded', expanded ? 'true' : 'false');
    }

    $(document).on('click', toggleSelector, function (e) {
        e.preventDefault();
        const $toggle = $(this);
        const $sidebar = $(sidebarSelector).first();
        const nowExpanded = !$sidebar.hasClass('expanded');
        $sidebar.toggleClass('expanded', nowExpanded);
        setToggleState($toggle, nowExpanded);
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

    $(document).on('click', '.sidebar-link.has-submenu', function (e) {
        e.preventDefault();
        const $link = $(this);
        const $submenu = $link.next('.submenu');
        const $arrow = $link.find('.menu-arrow');
        $('.submenu').not($submenu).slideUp().removeClass('show');
        $('.menu-arrow').not($arrow).removeClass('rotated');
        $submenu.slideToggle().toggleClass('show');
        $arrow.toggleClass('rotated');
    });

    // Ensure clicks directly on the small chevron icon also trigger the toggle
    $(document).on('click', '.menu-arrow', function (e) {
        e.preventDefault(); // prevent default in case icon is inside an anchor
        const $link = $(this).closest('.sidebar-link.has-submenu');
        if ($link.length) {
            $link.trigger('click');
        }
    });

    const $sidebarInit = $(sidebarSelector).first();
    const $toggleInit = $(toggleSelector).first();
    if ($sidebarInit.length && $toggleInit.length) {
        const expanded = $sidebarInit.hasClass('expanded');
        setToggleState($toggleInit, expanded);
    }
});
