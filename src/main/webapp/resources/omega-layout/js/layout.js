/**
 * PrimeFaces Omega Layout
 */
PrimeFaces.widget.Omega = PrimeFaces.widget.BaseWidget.extend({

    init: function(cfg) {
        this._super(cfg);
        this.wrapper = $(document.body).children('.wrapper');
        this.sidebar = this.wrapper.find('.sidebar');
        this.menu = this.sidebar.find('div.menu');
        this.menulinks = this.menu.find('a');
        this.menuButton = $('#omega-menu-button');
        this.optionsMenuButton = $('#options-menu-button');
        this.profileButton = $('#profile-button');
        this.topbarIcons = $('#topbar-icons');
        this.expandedMenuitems = this.expandedMenuitems||[];

        this.configButton = $('#layout-config-button');
        this.configurator = this.wrapper.children('.layout-config');
        this.configClicked = false;

        this.bindEvents();
        this.restoreMenuState();
    },

    bindEvents: function() {
        var $this = this;

        this.menulinks.off('click.menulink').on('click.menulink', function(e) {
            var link = $(this),
            item = link.parent(),
            submenu = item.children('ul');

            if(item.hasClass('active-menuitem')) {
                if(submenu.length) {
                    $this.removeMenuitem(item.attr('id'));
                    submenu.slideUp(function() {
                        item.removeClass('active-menuitem');
                    });
                }
            }
            else {
                $this.deactivateItems(item.siblings());
                $this.activate(item);
                $this.addMenuitem(item.attr('id'));
            }

            if(submenu.length) {
                e.preventDefault();
            }
        });

        this.menuButton.off('click.menubutton').on('click.menubutton', function(e) {
            $(this).toggleClass('active');

            if($this.isDesktop() && !$(document.body).hasClass('menu-layout-overlay')) {
                $this.wrapper.toggleClass('sidebar-inactive-l');

                if($this.wrapper.hasClass('sidebar-inactive-l')) {
                    $this.wrapper.removeClass('sidebar-active-m');
                }
            }
            else {
                $this.wrapper.toggleClass('sidebar-active-m');

                if($this.wrapper.hasClass('sidebar-active-m')) {
                    $this.wrapper.removeClass('sidebar-inactive-l');
                }
            }

            $this.topbarIcons.removeClass('topbar-icons-visible');
            e.preventDefault();
        });

        this.profileButton.off('click.profilebutton').on('click.profilebutton', function(e) {
            var profileMenu = $(this).next('ul');
            if(profileMenu.is(':visible')) {
                profileMenu.slideUp();
                $this.clearProfileMenuState();
            }
            else {
                profileMenu.slideDown();
                $this.saveProfileMenuState();
            }

            e.preventDefault();
        });

        this.optionsMenuButton.off('click.options-menubutton').on('click.options-menubutton', function(e) {
            if(!$this.animatingOptionsMenu) {
                $this.animatingOptionsMenu = true;
                if($this.topbarIcons.hasClass('topbar-icons-visible')) {
                    $this.topbarIcons.addClass('flipOutX');
                    setTimeout(function() {
                        $this.topbarIcons.removeClass('topbar-icons-visible flipOutX');
                        $this.animatingOptionsMenu = false;
                    },450);
                } else {
                    $this.topbarIcons.addClass('topbar-icons-visible');
                    $this.topbarIcons.addClass('flipInX');
                    $this.animatingOptionsMenu = false;
                }
            }

            $this.wrapper.removeClass('sidebar-active-m sidebar-inactive-l');
            e.preventDefault();
        });

        this.configButton.off('click.configbutton').on('click.configbutton', function(e) {
            $this.configurator.toggleClass('layout-config-active');
            $this.configClicked = true;
        });

        this.configurator.off('click.config').on('click.config', function() {
            $this.configClicked = true;
        });

        $(document.body).off('click.layoutBody').on('click.layoutBody', function() {
            if (!$this.configClicked && $this.configurator.hasClass('layout-config-active')) {
                $this.configurator.removeClass('layout-config-active');
            }

            $this.configClicked = false;
        });
    },

    activate: function(item) {
        var submenu = item.children('ul');
        item.addClass('active-menuitem');

        if(submenu.length) {
            submenu.slideDown();
        }
    },

    deactivate: function(item) {
        var submenu = item.children('ul');
        item.removeClass('active-menuitem');

        if(submenu.length) {
            submenu.hide();
        }
    },

    deactivateItems: function(items, animate) {
        var $this = this;

        for(var i = 0; i < items.length; i++) {
            var item = items.eq(i),
            submenu = item.children('ul');

            if(submenu.length) {
                if(item.hasClass('active-menuitem')) {
                    var activeSubItems = item.find('.active-menuitem');
                    submenu.slideUp('normal', function() {
                        var currentItem = $(this).parent();
                        currentItem.removeClass('active-menuitem');
                        currentItem.find('.active-menuitem').each(function() {
                            $this.deactivate($(this));
                        });
                    });

                    $this.removeMenuitem(item.attr('id'));
                    activeSubItems.each(function() {
                        $this.removeMenuitem($(this).attr('id'));
                    });
                }
                else {
                    item.find('.active-menuitem').each(function() {
                        var subItem = $(this);
                        $this.deactivate(subItem);
                        $this.removeMenuitem(subItem.attr('id'));
                    });
                }
            }
            else if(item.hasClass('active-menuitem')) {
                $this.deactivate(item);
                $this.removeMenuitem(item.attr('id'));
            }
        }
    },

    removeMenuitem: function (id) {
        this.expandedMenuitems = $.grep(this.expandedMenuitems, function (value) {
            return value !== id;
        });
        this.saveMenuState();
    },

    addMenuitem: function (id) {
        if ($.inArray(id, this.expandedMenuitems) === -1) {
            this.expandedMenuitems.push(id);
        }
        this.saveMenuState();
    },

    saveMenuState: function() {
        $.cookie('omega_expandeditems', this.expandedMenuitems.join(','), {path: '/'});
    },

    clearMenuState: function() {
        $.removeCookie('omega_expandeditems', {path: '/'});
    },

    saveProfileMenuState: function() {
        $.cookie('omega_profile_expanded', "1", {path: '/'});
    },

    clearProfileMenuState: function() {
        $.removeCookie('omega_profile_expanded', {path: '/'});
    },

    restoreMenuState: function() {
        var menucookie = $.cookie('omega_expandeditems');
        if (menucookie) {
            this.expandedMenuitems = menucookie.split(',');
            for (var i = 0; i < this.expandedMenuitems.length; i++) {
                var id = this.expandedMenuitems[i];
                if (id) {
                    var menuitem = $("#" + this.expandedMenuitems[i].replace(/:/g, "\\:"));
                    menuitem.addClass('active-menuitem');

                    var submenu = menuitem.children('ul');
                    if(submenu.length) {
                        submenu.show();
                    }
                }
            }
        }

        var profileMenuCookie = $.cookie('omega_profile_expanded');
        if(profileMenuCookie) {
            this.profileButton.next('ul').show();
        }
    },

    isDesktop: function() {
        return window.innerWidth > 1024;
    },

    clearActiveItems: function() {
        var activeItems = this.jq.find('li.active-menuitem'),
        subContainers = activeItems.children('ul');

        activeItems.removeClass('active-menuitem');
        if(subContainers && subContainers.length) {
            subContainers.hide();
        }
    }

});

PrimeFaces.OmegaConfigurator = {

    changeMenuMode: function(menuMode) {
        var body = $(document.body);
        if (menuMode === 'menu-layout-static')
            body.addClass('menu-layout-static').removeClass('menu-layout-overlay');
        else
            body.addClass('menu-layout-overlay').removeClass('menu-layout-static');
    },

    updateInputStyle: function(value) {
        if (value === 'filled')
            $(document.body).addClass('ui-input-filled');
        else
            $(document.body).removeClass('ui-input-filled');
    },

    changeMenuColor: function(color) {
        var wrapper = $(document.body).children('.wrapper');

        if (color === 'light')
            wrapper.addClass('layout-light');
        else
            wrapper.removeClass('layout-light');
    },

    changeLayout: function(layoutTheme) {
        var linkElement = $('link[href*="layout-"]');
        var href = linkElement.attr('href');
        var startIndexOf = href.indexOf('layout-') + 7;
        var endIndexOf = href.indexOf('.css');
        var currentColor = href.substring(startIndexOf, endIndexOf);

        this.replaceLink(linkElement, href.replace(currentColor, layoutTheme));
    },

    changeScheme: function(theme) {
        var library = 'primefaces-omega';
        var linkElement = $('link[href*="theme.css"]');
        var href = linkElement.attr('href');
        var index = href.indexOf(library) + 1;
        var currentTheme = href.substring(index + library.length);

        this.replaceLink(linkElement, href.replace(currentTheme, theme));
    },

    replaceLink: function(linkElement, href) {
        PrimeFaces.ajax.RESOURCE = 'javax.faces.Resource';

        var isIE = this.isIE();

        if (isIE) {
            linkElement.attr('href', href);
        }
        else {
            var cloneLinkElement = linkElement.clone(false);

            cloneLinkElement.attr('href', href);
            linkElement.after(cloneLinkElement);

            cloneLinkElement.off('load').on('load', function() {
                linkElement.remove();
            });
        }
    },

    isIE: function() {
        return /(MSIE|Trident\/|Edge\/)/i.test(navigator.userAgent);
    },

    beforeResourceChange: function() {
        PrimeFaces.ajax.RESOURCE = null;    //prevent resource append
    },
};

/*!
 * jQuery Cookie Plugin v1.4.1
 * https://github.com/carhartl/jquery-cookie
 *
 * Copyright 2006, 2014 Klaus Hartl
 * Released under the MIT license
 */
(function (factory) {
	if (typeof define === 'function' && define.amd) {
		// AMD (Register as an anonymous module)
		define(['jquery'], factory);
	} else if (typeof exports === 'object') {
		// Node/CommonJS
		module.exports = factory(require('jquery'));
	} else {
		// Browser globals
		factory(jQuery);
	}
}(function ($) {

	var pluses = /\+/g;

	function encode(s) {
		return config.raw ? s : encodeURIComponent(s);
	}

	function decode(s) {
		return config.raw ? s : decodeURIComponent(s);
	}

	function stringifyCookieValue(value) {
		return encode(config.json ? JSON.stringify(value) : String(value));
	}

	function parseCookieValue(s) {
		if (s.indexOf('"') === 0) {
			// This is a quoted cookie as according to RFC2068, unescape...
			s = s.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, '\\');
		}

		try {
			// Replace server-side written pluses with spaces.
			// If we can't decode the cookie, ignore it, it's unusable.
			// If we can't parse the cookie, ignore it, it's unusable.
			s = decodeURIComponent(s.replace(pluses, ' '));
			return config.json ? JSON.parse(s) : s;
		} catch(e) {}
	}

	function read(s, converter) {
		var value = config.raw ? s : parseCookieValue(s);
		return $.isFunction(converter) ? converter(value) : value;
	}

	var config = $.cookie = function (key, value, options) {

		// Write

		if (arguments.length > 1 && !$.isFunction(value)) {
			options = $.extend({}, config.defaults, options);

			if (typeof options.expires === 'number') {
				var days = options.expires, t = options.expires = new Date();
				t.setMilliseconds(t.getMilliseconds() + days * 864e+5);
			}

			return (document.cookie = [
				encode(key), '=', stringifyCookieValue(value),
				options.expires ? '; expires=' + options.expires.toUTCString() : '', // use expires attribute, max-age is not supported by IE
				options.path    ? '; path=' + options.path : '',
				options.domain  ? '; domain=' + options.domain : '',
				options.secure  ? '; secure' : ''
			].join(''));
		}

		// Read

		var result = key ? undefined : {},
			// To prevent the for loop in the first place assign an empty array
			// in case there are no cookies at all. Also prevents odd result when
			// calling $.cookie().
			cookies = document.cookie ? document.cookie.split('; ') : [],
			i = 0,
			l = cookies.length;

		for (; i < l; i++) {
			var parts = cookies[i].split('='),
				name = decode(parts.shift()),
				cookie = parts.join('=');

			if (key === name) {
				// If second argument (value) is a function it's a converter...
				result = read(cookie, value);
				break;
			}

			// Prevent storing a cookie that we couldn't decode.
			if (!key && (cookie = read(cookie)) !== undefined) {
				result[name] = cookie;
			}
		}

		return result;
	};

	config.defaults = {};

	$.removeCookie = function (key, options) {
		// Must not alter options, thus extending a fresh object...
		$.cookie(key, '', $.extend({}, options, { expires: -1 }));
		return !$.cookie(key);
	};

}));

/* Issue #924 is fixed for 5.3+ and 6.0. (compatibility with 5.3) */
if(window['PrimeFaces'] && window['PrimeFaces'].widget.Dialog) {
    PrimeFaces.widget.Dialog = PrimeFaces.widget.Dialog.extend({

        enableModality: function() {
            this._super();
            $(document.body).children(this.jqId + '_modal').addClass('ui-dialog-mask');
        },

        syncWindowResize: function() {}
    });
}

if (PrimeFaces.widget.SelectOneMenu) {
    PrimeFaces.widget.SelectOneMenu = PrimeFaces.widget.SelectOneMenu.extend({
        init: function (cfg) {
            this._super(cfg);

            var $this = this;
            if (this.jq.parent().hasClass('ui-float-label')) {
                this.m_panel = $(this.jqId + '_panel');
                this.m_focusInput = $(this.jqId + '_focus');

                this.m_panel.addClass('ui-input-overlay-panel');
                this.jq.addClass('ui-inputwrapper');

                if (this.input.val() != '') {
                    this.jq.addClass('ui-inputwrapper-filled');
                }

                this.input.off('change').on('change', function () {
                    $this.inputValueControl($(this));
                });

                this.m_focusInput.on('focus.ui-selectonemenu', function () {
                    $this.jq.addClass('ui-inputwrapper-focus');
                })
                    .on('blur.ui-selectonemenu', function () {
                        $this.jq.removeClass('ui-inputwrapper-focus');
                    });

                if (this.cfg.editable) {
                    this.label.on('input', function (e) {
                        $this.inputValueControl($(this));
                    }).on('focus', function () {
                        $this.jq.addClass('ui-inputwrapper-focus');
                    }).on('blur', function () {
                        $this.jq.removeClass('ui-inputwrapper-focus');
                        $this.inputValueControl($(this));
                    });
                }
            }
        },

        inputValueControl: function (input) {
            if (input.val() != '')
                this.jq.addClass('ui-inputwrapper-filled');
            else
                this.jq.removeClass('ui-inputwrapper-filled');
        }
    });
}

if (PrimeFaces.widget.Chips) {
    PrimeFaces.widget.Chips = PrimeFaces.widget.Chips.extend({
        init: function (cfg) {
            this._super(cfg);

            var $this = this;
            if (this.jq.parent().hasClass('ui-float-label')) {
                this.jq.addClass('ui-inputwrapper');

                if ($this.jq.find('.ui-chips-token').length !== 0) {
                    this.jq.addClass('ui-inputwrapper-filled');
                }

                this.input.on('focus.ui-chips', function () {
                    $this.jq.addClass('ui-inputwrapper-focus');
                }).on('input.ui-chips', function () {
                    $this.inputValueControl();
                }).on('blur.ui-chips', function () {
                    $this.jq.removeClass('ui-inputwrapper-focus');
                    $this.inputValueControl();
                });

            }
        },

        inputValueControl: function () {
            if (this.jq.find('.ui-chips-token').length !== 0 || this.input.val() != '')
                this.jq.addClass('ui-inputwrapper-filled');
            else
                this.jq.removeClass('ui-inputwrapper-filled');
        }
    });
}

if (PrimeFaces.widget.DatePicker) {
    PrimeFaces.widget.DatePicker = PrimeFaces.widget.DatePicker.extend({
        init: function (cfg) {
            this._super(cfg);

            var $this = this;
            if (this.jq.parent().hasClass('ui-float-label') && !this.cfg.inline) {
                if (this.input.val() != '') {
                    this.jq.addClass('ui-inputwrapper-filled');
                }

                this.jqEl.off('focus.ui-datepicker blur.ui-datepicker change.ui-datepicker')
                    .on('focus.ui-datepicker', function () {
                        $this.jq.addClass('ui-inputwrapper-focus');
                    })
                    .on('blur.ui-datepicker', function () {
                        $this.jq.removeClass('ui-inputwrapper-focus');
                    })
                    .on('change.ui-datepicker', function () {
                        $this.inputValueControl($(this));
                    });
            }
        },

        inputValueControl: function (input) {
            if (input.val() != '')
                this.jq.addClass('ui-inputwrapper-filled');
            else
                this.jq.removeClass('ui-inputwrapper-filled');
        }
    });
}