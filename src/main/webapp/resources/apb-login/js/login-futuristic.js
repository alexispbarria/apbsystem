/**
 * APB SYSTEM - Interactividad de Pantalla de Login Futurista
 * ==========================================================
 * Script modular en JS puro (sin dependencias externas).
 * Compatible con re-renderizados AJAX de PrimeFaces.
 */
(function () {
    'use strict';

    var prefersReducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    var MODE_LABELS = {
        'login': { icon: 'pi-lock', text: 'Acceso Seguro' },
        'recovery': { icon: 'pi-key', text: 'Recuperación' },
        'recovery-sent': { icon: 'pi-check-circle', text: 'Solicitud Enviada' }
    };

    var ApbLogin = {

        init: function () {
            this.initSpotlight();
            this.initCapsLockDetector();
            this.initParticles();
            this.syncAriaStates('login');
        },

        /**
         * Cambia con transicion fluida entre las vistas dentro de la misma caja:
         * 'login' | 'recovery' | 'recovery-sent'
         */
        switchView: function (viewName) {
            var stage = document.getElementById('apbViewsStage');
            if (!stage) {
                return;
            }

            stage.setAttribute('data-active-view', viewName);
            this.syncAriaStates(viewName);
            this.updateModeChip(viewName);

            // Foco accesible tras completar la animacion
            window.setTimeout(function () {
                if (viewName === 'login') {
                    var userInput = document.getElementById('loginForm:username');
                    if (userInput) {
                        userInput.focus();
                    }
                } else if (viewName === 'recovery') {
                    var recInput = document.getElementById('recoveryForm:recoveryIdentifier');
                    if (recInput) {
                        recInput.focus();
                    }
                }
            }, 260);
        },

        syncAriaStates: function (activeView) {
            var views = document.querySelectorAll('#apbViewsStage .apb-view');
            for (var i = 0; i < views.length; i++) {
                var isCurrent = views[i].getAttribute('data-view') === activeView;
                views[i].setAttribute('aria-hidden', isCurrent ? 'false' : 'true');
            }
        },

        updateModeChip: function (viewName) {
            var chipText = document.getElementById('apbModeChipText');
            var chipIcon = document.getElementById('apbModeChipIcon');
            var cfg = MODE_LABELS[viewName] || MODE_LABELS.login;

            if (chipText) {
                chipText.textContent = cfg.text;
            }
            if (chipIcon) {
                chipIcon.className = 'pi ' + cfg.icon;
            }
        },

        /**
         * Estado visual de carga para botones PrimeFaces
         */
        setLoading: function (type, isLoading) {
            var btnId = type === 'recovery' ? 'recoveryForm:btnRecovery' : 'loginForm:btnLogin';
            var btn = document.getElementById(btnId);
            if (!btn) {
                return;
            }

            var textSpan = btn.querySelector('.ui-button-text');
            var iconSpan = btn.querySelector('.ui-icon');

            if (isLoading) {
                if (!btn.dataset.originalText && textSpan) {
                    btn.dataset.originalText = textSpan.textContent;
                }
                if (!btn.dataset.originalIcon && iconSpan) {
                    btn.dataset.originalIcon = iconSpan.className;
                }
                btn.classList.add('is-loading');
                if (textSpan) {
                    textSpan.textContent = type === 'recovery' ? 'Procesando solicitud...' : 'Validando acceso...';
                }
                if (iconSpan) {
                    iconSpan.className = 'ui-button-icon-right ui-icon ui-c pi pi-spinner';
                }
            } else {
                btn.classList.remove('is-loading');
                if (textSpan && btn.dataset.originalText) {
                    textSpan.textContent = btn.dataset.originalText;
                }
                if (iconSpan && btn.dataset.originalIcon) {
                    iconSpan.className = btn.dataset.originalIcon;
                }
            }
        },

        /**
         * Callback AJAX de PrimeFaces para Login
         */
        handleLoginComplete: function (xhr, status, args) {
            this.setLoading('login', false);
            if (status === 'error' || (args && (args.loginFailed || args.validationFailed))) {
                this.triggerShake();
            }
        },

        /**
         * Callback AJAX de PrimeFaces para Recuperacion de Contrasena
         */
        handleRecoveryComplete: function (xhr, status, args) {
            this.setLoading('recovery', false);
            if (args && args.recoverySuccess) {
                this.switchView('recovery-sent');
            } else if (status === 'error' || (args && (args.recoverySuccess === false || args.validationFailed))) {
                this.triggerShake();
            }
        },

        /**
         * Micro-animacion de sacudida sutil en caso de error
         */
        triggerShake: function () {
            var card = document.getElementById('apbAuthCard');
            if (!card || prefersReducedMotion) {
                return;
            }
            card.classList.remove('is-shaking');
            void card.offsetWidth; // Reflow para reiniciar la animacion
            card.classList.add('is-shaking');
            window.setTimeout(function () {
                card.classList.remove('is-shaking');
            }, 520);
        },

        /**
         * Spotlight que sigue suavemente el cursor sobre la tarjeta
         */
        initSpotlight: function () {
            var card = document.getElementById('apbAuthCard');
            if (!card || prefersReducedMotion) {
                return;
            }

            card.addEventListener('pointermove', function (e) {
                if (e.pointerType === 'touch') {
                    return;
                }
                var rect = card.getBoundingClientRect();
                var x = ((e.clientX - rect.left) / rect.width) * 100;
                var y = ((e.clientY - rect.top) / rect.height) * 100;
                card.style.setProperty('--mouse-x', x.toFixed(2) + '%');
                card.style.setProperty('--mouse-y', y.toFixed(2) + '%');
            });
        },

        /**
         * Detector en vivo de Bloq Mayus (delegado para resistir updates AJAX)
         */
        initCapsLockDetector: function () {
            var updateBadge = function (e) {
                var target = e.target;
                if (!target || !(target.classList.contains('ui-password-input') || target.type === 'password')) {
                    return;
                }
                var badge = document.getElementById('apbCapsLockBadge');
                if (!badge) {
                    return;
                }
                var capsOn = typeof e.getModifierState === 'function' && e.getModifierState('CapsLock');
                badge.classList.toggle('is-visible', !!capsOn);
            };

            document.addEventListener('keydown', updateBadge);
            document.addEventListener('keyup', updateBadge);
            document.addEventListener('focusout', function (e) {
                if (e.target && (e.target.classList.contains('ui-password-input') || e.target.type === 'password')) {
                    var badge = document.getElementById('apbCapsLockBadge');
                    if (badge) {
                        badge.classList.remove('is-visible');
                    }
                }
            });
        },

        /**
         * Red ligera de nodos de datos en Canvas (controlable con data-particles)
         */
        initParticles: function () {
            var scene = document.getElementById('apbScene');
            if (prefersReducedMotion || (scene && scene.getAttribute('data-particles') === 'false')) {
                return;
            }

            var canvas = document.getElementById('apb-particles-canvas');
            if (!canvas || !canvas.getContext) {
                return;
            }

            var ctx = canvas.getContext('2d');
            var particles = [];
            var animId = null;
            var width = 0;
            var height = 0;

            var resize = function () {
                var dpr = Math.min(window.devicePixelRatio || 1, 2);
                width = window.innerWidth;
                height = window.innerHeight;
                canvas.width = Math.floor(width * dpr);
                canvas.height = Math.floor(height * dpr);
                ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

                var count = Math.min(Math.max(Math.floor((width * height) / 26000), 22), 54);
                particles = [];
                for (var i = 0; i < count; i++) {
                    particles.push({
                        x: Math.random() * width,
                        y: Math.random() * height,
                        vx: (Math.random() - 0.5) * 0.38,
                        vy: (Math.random() - 0.5) * 0.38,
                        r: Math.random() * 1.6 + 0.9
                    });
                }
            };

            var render = function () {
                ctx.clearRect(0, 0, width, height);

                for (var i = 0; i < particles.length; i++) {
                    var p = particles[i];
                    p.x += p.vx;
                    p.y += p.vy;

                    if (p.x < 0 || p.x > width) { p.vx *= -1; }
                    if (p.y < 0 || p.y > height) { p.vy *= -1; }

                    ctx.beginPath();
                    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
                    ctx.fillStyle = 'rgba(0, 229, 255, 0.55)';
                    ctx.fill();

                    for (var j = i + 1; j < particles.length; j++) {
                        var q = particles[j];
                        var dx = p.x - q.x;
                        var dy = p.y - q.y;
                        var dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist < 135) {
                            var alpha = (1 - dist / 135) * 0.16;
                            ctx.beginPath();
                            ctx.moveTo(p.x, p.y);
                            ctx.lineTo(q.x, q.y);
                            ctx.strokeStyle = 'rgba(59, 130, 246, ' + alpha.toFixed(3) + ')';
                            ctx.lineWidth = 1;
                            ctx.stroke();
                        }
                    }
                }

                animId = window.requestAnimationFrame(render);
            };

            resize();
            render();

            window.addEventListener('resize', resize);
            document.addEventListener('visibilitychange', function () {
                if (document.hidden && animId) {
                    window.cancelAnimationFrame(animId);
                    animId = null;
                } else if (!document.hidden && !animId) {
                    render();
                }
            });
        }
    };

    window.ApbLogin = ApbLogin;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
            ApbLogin.init();
        });
    } else {
        ApbLogin.init();
    }
})();
