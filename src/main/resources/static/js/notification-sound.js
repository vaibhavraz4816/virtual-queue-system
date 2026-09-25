/**
 * Web Audio API synthesizer for clean notification chimes without external audio dependencies.
 */
class QueueNotification {
    constructor() {
        this.audioCtx = null;
        this.hasNotificationPermission = false;
        this.initNotificationPermission();
    }

    getAudioContext() {
        if (!this.audioCtx) {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                this.audioCtx = new AudioContext();
            }
        }
        if (this.audioCtx && this.audioCtx.state === 'suspended') {
            this.audioCtx.resume();
        }
        return this.audioCtx;
    }

    /**
     * Plays a pleasant, melodious dual-tone chime (E5 -> A5)
     */
    playChime() {
        try {
            const ctx = this.getAudioContext();
            if (!ctx) return;

            const now = ctx.currentTime;

            // Tone 1: 659.25 Hz (E5)
            const osc1 = ctx.createOscillator();
            const gain1 = ctx.createGain();
            osc1.type = 'sine';
            osc1.frequency.setValueAtTime(659.25, now);
            gain1.gain.setValueAtTime(0, now);
            gain1.gain.linearRampToValueAtTime(0.3, now + 0.05);
            gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
            osc1.connect(gain1);
            gain1.connect(ctx.destination);
            osc1.start(now);
            osc1.stop(now + 0.6);

            // Tone 2: 880 Hz (A5), slight delay
            const osc2 = ctx.createOscillator();
            const gain2 = ctx.createGain();
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(880, now + 0.15);
            gain2.gain.setValueAtTime(0, now + 0.15);
            gain2.gain.linearRampToValueAtTime(0.35, now + 0.2);
            gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.9);
            osc2.connect(gain2);
            gain2.connect(ctx.destination);
            osc2.start(now + 0.15);
            osc2.stop(now + 0.9);
        } catch (e) {
            console.warn('Audio synthesis note:', e);
        }
    }

    initNotificationPermission() {
        if ('Notification' in window) {
            if (Notification.permission === 'granted') {
                this.hasNotificationPermission = true;
            }
        }
    }

    requestPermission() {
        if ('Notification' in window && Notification.permission !== 'granted' && Notification.permission !== 'denied') {
            Notification.requestPermission().then(permission => {
                this.hasNotificationPermission = (permission === 'granted');
            });
        }
    }

    showDesktopNotification(title, message) {
        if (this.hasNotificationPermission && 'Notification' in window) {
            try {
                new Notification(title, {
                    body: message,
                    icon: '/favicon.ico',
                    requireInteraction: true
                });
            } catch (e) {
                console.warn('Desktop notification error:', e);
            }
        }
    }
}

const queueNotifier = new QueueNotification();

// Enable audio context on any first user interaction
document.addEventListener('click', () => {
    queueNotifier.getAudioContext();
}, { once: true });
