/**
 * QueueEase Camera-Based QR Code Scanner
 * Supports BarcodeDetector API with graceful fallback to manual queue link entry.
 */
class QrScannerManager {
    constructor() {
        this.video = null;
        this.stream = null;
        this.modal = null;
        this.barcodeDetector = null;
        this.scanInterval = null;
        this.isScanning = false;
        this.init();
    }

    init() {
        if (window.BarcodeDetector) {
            try {
                this.barcodeDetector = new BarcodeDetector({ formats: ['qr_code'] });
            } catch (e) {
                console.info('Native BarcodeDetector not available, using fallback:', e);
            }
        }

        document.addEventListener('DOMContentLoaded', () => {
            this.bindElements();
        });
    }

    bindElements() {
        const modalEl = document.getElementById('qrScannerModal');
        if (modalEl) {
            this.modal = new bootstrap.Modal(modalEl);
            modalEl.addEventListener('shown.bs.modal', () => this.startCamera());
            modalEl.addEventListener('hidden.bs.modal', () => this.stopCamera());
        }

        // Attach click listeners to any [data-action="open-scanner"] or .btn-scan-qr
        document.querySelectorAll('[data-action="open-scanner"], .btn-scan-qr').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                this.open();
            });
        });

        // Manual link submission fallback
        const manualForm = document.getElementById('manual-queue-form');
        const manualInput = document.getElementById('manual-queue-input');
        if (manualForm && manualInput) {
            manualForm.addEventListener('submit', (e) => {
                e.preventDefault();
                const raw = manualInput.value.trim();
                if (raw) {
                    this.handleScannedUrl(raw);
                }
            });
        }
    }

    open() {
        if (this.modal) {
            this.modal.show();
        }
    }

    async startCamera() {
        this.video = document.getElementById('qr-video-stream');
        const errorAlert = document.getElementById('scanner-error-alert');
        const errorMsg = document.getElementById('scanner-error-msg');
        const guide = document.getElementById('scanner-guide');

        if (errorAlert) errorAlert.classList.add('d-none');
        if (guide) guide.classList.remove('d-none');

        if (!this.video) return;

        try {
            const constraints = {
                video: {
                    facingMode: { ideal: 'environment' },
                    width: { ideal: 1280 },
                    height: { ideal: 720 }
                },
                audio: false
            };

            this.stream = await navigator.mediaDevices.getUserMedia(constraints);
            this.video.srcObject = this.stream;
            await this.video.play();
            this.isScanning = true;
            this.startDetectionLoop();
        } catch (err) {
            console.warn('Camera access issue:', err);
            if (errorAlert && errorMsg) {
                errorAlert.classList.remove('d-none');
                if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
                    errorMsg.innerText = 'Camera permission was denied. Please allow camera access in browser settings or use the link input below.';
                } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
                    errorMsg.innerText = 'No camera device found. Please enter your shop queue link below.';
                } else {
                    errorMsg.innerText = 'Could not access camera (' + err.message + '). Please enter queue link below.';
                }
            }
            if (guide) guide.classList.add('d-none');
        }
    }

    startDetectionLoop() {
        if (!this.barcodeDetector) {
            // If native barcode detector isn't supported, guide user to manual entry or check jsQR
            console.info('Waiting for manual entry or jsQR scanner frame');
            return;
        }

        this.scanInterval = setInterval(async () => {
            if (!this.isScanning || !this.video || this.video.readyState < 2) return;

            try {
                const barcodes = await this.barcodeDetector.detect(this.video);
                if (barcodes && barcodes.length > 0) {
                    const qrVal = barcodes[0].rawValue;
                    if (qrVal) {
                        this.stopCamera();
                        if (this.modal) this.modal.hide();
                        this.handleScannedUrl(qrVal);
                    }
                }
            } catch (e) {
                // Ignore detection frame dropped errors
            }
        }, 300);
    }

    stopCamera() {
        this.isScanning = false;
        if (this.scanInterval) {
            clearInterval(this.scanInterval);
            this.scanInterval = null;
        }
        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
        if (this.video) {
            this.video.srcObject = null;
        }
    }

    handleScannedUrl(urlOrSlug) {
        let clean = urlOrSlug.trim();
        // If it's a full URL e.g. http://localhost:8080/shop/salon/queue
        try {
            if (clean.startsWith('http://') || clean.startsWith('https://')) {
                const parsed = new URL(clean);
                window.location.href = parsed.pathname;
                return;
            }
        } catch (e) {
        }

        // If it's a relative path e.g. /shop/salon/queue
        if (clean.startsWith('/shop/')) {
            window.location.href = clean;
            return;
        }

        // If it's just a slug e.g. salon
        if (!clean.includes('/')) {
            window.location.href = '/shop/' + encodeURIComponent(clean) + '/queue';
            return;
        }

        // Default navigation
        window.location.href = clean;
    }
}

const qrScanner = new QrScannerManager();
