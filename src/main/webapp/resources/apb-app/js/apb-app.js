/**
 * APB SALES CORE - Interactividad global
 * ============================================================================
 * - Menus dropdown desde Topbar (clic, teclado ESC y cierre exterior)
 * - Drawer movil
 * - Formateador y validador en vivo de RUT (convencion xxxxxxxx-x)
 * - Escaner de codigo de barras por camara (WebJar html5-qrcode) + atajo F2
 */
(function () {
    'use strict';

    var qrScannerInstance = null;
    var scannerRunning = false;
    var lastScanText = '';
    var lastScanTime = 0;

    var ApbApp = {

        init: function () {
            this.initDropdowns();
            this.initMobileNav();
            this.initRutInputs();
            this.initBarcodeShortcuts();
        },

        /* ==================================================================
           MENUS DROPDOWN DESDE TOPBAR
           ================================================================== */
        initDropdowns: function () {
            var closeAll = function (except) {
                var openDropdowns = document.querySelectorAll('.apb-dropdown.is-open');
                for (var i = 0; i < openDropdowns.length; i++) {
                    if (openDropdowns[i] !== except) {
                        openDropdowns[i].classList.remove('is-open');
                        var trig = openDropdowns[i].querySelector('[aria-expanded]');
                        if (trig) {
                            trig.setAttribute('aria-expanded', 'false');
                        }
                    }
                }
            };

            document.addEventListener('click', function (e) {
                var trigger = e.target.closest('.apb-dropdown-trigger');
                if (trigger) {
                    e.preventDefault();
                    e.stopPropagation();
                    var parent = trigger.closest('.apb-dropdown');
                    if (!parent) {
                        return;
                    }
                    var willOpen = !parent.classList.contains('is-open');
                    closeAll(parent);
                    parent.classList.toggle('is-open', willOpen);
                    trigger.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
                    return;
                }

                if (!e.target.closest('.apb-dropdown-menu')) {
                    closeAll(null);
                } else if (e.target.closest('.apb-dropdown-link')) {
                    window.setTimeout(function () {
                        closeAll(null);
                    }, 80);
                }
            });

            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape') {
                    closeAll(null);
                }
            });
        },

        /* ==================================================================
           NAVEGACION MOVIL
           ================================================================== */
        initMobileNav: function () {
            var toggle = document.getElementById('apbMobileToggle');
            var drawer = document.getElementById('apbMobileNav');
            if (!toggle || !drawer) {
                return;
            }
            toggle.addEventListener('click', function () {
                var isOpen = drawer.classList.toggle('is-open');
                toggle.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
            });
        },

        /* ==================================================================
           FORMATEO Y VALIDACION EN VIVO DE RUT (CONVENCION: xxxxxxxx-x)
           ================================================================== */
        formatRutValue: function (raw) {
            if (!raw) {
                return '';
            }
            var clean = raw.toUpperCase().replace(/[^0-9K]/g, '');
            if (clean.length > 9) {
                clean = clean.substring(0, 9);
            }
            if (clean.length <= 1) {
                return clean;
            }
            var body = clean.substring(0, clean.length - 1).replace(/K/g, '');
            var dv = clean.substring(clean.length - 1);
            if (!body) {
                return dv;
            }
            return body + '-' + dv;
        },

        validateRutModulo11: function (rutStr) {
            if (!/^\d{7,8}-[0-9K]$/.test(rutStr)) {
                return false;
            }
            var parts = rutStr.split('-');
            var num = parseInt(parts[0], 10);
            if (!num || num <= 0) {
                return false;
            }
            var dv = parts[1];
            var m = 0;
            var s = 1;
            for (; num !== 0; num = Math.floor(num / 10)) {
                s = (s + (num % 10) * (9 - (m++ % 6))) % 11;
            }
            var expected = s !== 0 ? String.fromCharCode(s + 47) : 'K';
            return dv === expected;
        },

        initRutInputs: function () {
            var self = this;

            var handleRutEvent = function (target) {
                var formatted = self.formatRutValue(target.value);
                if (target.value !== formatted) {
                    target.value = formatted;
                }

                var statusId = target.getAttribute('data-rut-status') || 'apbRutLiveStatus';
                var badge = document.getElementById(statusId);
                if (!badge) {
                    return;
                }

                if (!formatted) {
                    badge.className = 'apb-rut-live-badge';
                    badge.innerHTML = '<i class="pi pi-info-circle"></i><span>Convención: xxxxxxxx-x (sin puntos)</span>';
                } else if (self.validateRutModulo11(formatted)) {
                    badge.className = 'apb-rut-live-badge is-valid';
                    badge.innerHTML = '<i class="pi pi-check-circle"></i><span>RUT válido (' + formatted + ')</span>';
                } else if (/^\d{7,8}-[0-9K]$/.test(formatted)) {
                    badge.className = 'apb-rut-live-badge is-invalid';
                    badge.innerHTML = '<i class="pi pi-times-circle"></i><span>Dígito verificador incorrecto</span>';
                } else {
                    badge.className = 'apb-rut-live-badge';
                    badge.innerHTML = '<i class="pi pi-pencil"></i><span>Formato esperado: xxxxxxxx-x</span>';
                }
            };

            document.addEventListener('input', function (e) {
                if (e.target && e.target.classList && e.target.classList.contains('apb-rut-input')) {
                    handleRutEvent(e.target);
                }
            });

            document.addEventListener('blur', function (e) {
                if (e.target && e.target.classList && e.target.classList.contains('apb-rut-input')) {
                    handleRutEvent(e.target);
                }
            }, true);
        },

        /* ==================================================================
           ATAJO F2 + ESCANER DE CODIGO DE BARRAS POR CAMARA (html5-qrcode)
           ================================================================== */
        initBarcodeShortcuts: function () {
            var self = this;
            document.addEventListener('keydown', function (e) {
                if (e.key === 'F2') {
                    var input = document.getElementById('barcodeForm:barcodeInput');
                    if (input) {
                        e.preventDefault();
                        self.focusBarcodeInput();
                    }
                }
            });
        },

        focusBarcodeInput: function () {
            var input = document.getElementById('barcodeForm:barcodeInput');
            if (input) {
                input.focus();
                input.select();
            }
        },

        playBeep: function (ok) {
            try {
                var AudioCtx = window.AudioContext || window.webkitAudioContext;
                if (!AudioCtx) {
                    return;
                }
                var ctx = new AudioCtx();
                var osc = ctx.createOscillator();
                var gain = ctx.createGain();
                osc.type = 'sine';
                osc.frequency.value = ok ? 920 : 320;
                gain.gain.value = 0.08;
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + (ok ? 0.09 : 0.18));
            } catch (err) {
                // Silencioso si el navegador bloquea audio automatico
            }
        },

        toggleCameraScanner: function () {
            if (scannerRunning) {
                this.stopCameraScanner();
            } else {
                this.startCameraScanner();
            }
        },

        startCameraScanner: function () {
            var stage = document.getElementById('apbCameraStage');
            var statusEl = document.getElementById('apbCameraStatus');
            var btn = document.getElementById('apbCameraToggleBtn');

            if (!stage) {
                return;
            }
            stage.classList.add('is-active');

            if (typeof window.Html5Qrcode === 'undefined') {
                if (statusEl) {
                    statusEl.textContent = 'Librería html5-qrcode no cargada. Verifica Clean & Build de Maven.';
                }
                return;
            }

            if (statusEl) {
                statusEl.textContent = 'Iniciando cámara para lectura de códigos de barras (EAN-13, Code-128, QR)...';
            }

            try {
                if (!qrScannerInstance) {
                    qrScannerInstance = new window.Html5Qrcode('apbQrReader');
                }
                var self = this;
                var config = {
                    fps: 10,
                    qrbox: { width: 280, height: 130 }
                };

                qrScannerInstance.start(
                    { facingMode: 'environment' },
                    config,
                    function (decodedText) {
                        var now = Date.now();
                        if (decodedText === lastScanText && (now - lastScanTime) < 1500) {
                            return;
                        }
                        lastScanText = decodedText;
                        lastScanTime = now;

                        if (statusEl) {
                            statusEl.textContent = 'Código detectado: ' + decodedText;
                        }
                        self.playBeep(true);

                        if (typeof window.rcCodigoBarraEscaneado === 'function') {
                            window.rcCodigoBarraEscaneado([{ name: 'scannedCode', value: decodedText }]);
                        }
                    },
                    function () {
                        // Frame sin lectura, se ignora silenciosamente
                    }
                ).then(function () {
                    scannerRunning = true;
                    if (statusEl) {
                        statusEl.textContent = 'Cámara activa · Apunta al código de barras del producto.';
                    }
                    if (btn) {
                        btn.innerHTML = '<i class="pi pi-times"></i><span>Detener cámara</span>';
                    }
                }).catch(function (err) {
                    scannerRunning = false;
                    if (statusEl) {
                        statusEl.textContent = 'No se pudo acceder a la cámara (' + err + '). Puedes usar el lector USB o escribir el código.';
                    }
                });
            } catch (ex) {
                if (statusEl) {
                    statusEl.textContent = 'Error al inicializar cámara: ' + ex.message;
                }
            }
        },

        stopCameraScanner: function () {
            var stage = document.getElementById('apbCameraStage');
            var btn = document.getElementById('apbCameraToggleBtn');

            if (qrScannerInstance && scannerRunning) {
                qrScannerInstance.stop().then(function () {
                    scannerRunning = false;
                    if (stage) { stage.classList.remove('is-active'); }
                    if (btn) { btn.innerHTML = '<i class="pi pi-camera"></i><span>Cámara</span>'; }
                }).catch(function () {
                    scannerRunning = false;
                    if (stage) { stage.classList.remove('is-active'); }
                    if (btn) { btn.innerHTML = '<i class="pi pi-camera"></i><span>Cámara</span>'; }
                });
            } else {
                scannerRunning = false;
                if (stage) { stage.classList.remove('is-active'); }
                if (btn) { btn.innerHTML = '<i class="pi pi-camera"></i><span>Cámara</span>'; }
            }
        }
    };

    window.ApbApp = ApbApp;

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
            ApbApp.init();
        });
    } else {
        ApbApp.init();
    }
})();
