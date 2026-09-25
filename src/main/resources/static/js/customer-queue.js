/**
 * Customer Queue Live Polling and Action Handler
 */
document.addEventListener('DOMContentLoaded', () => {
    const accessToken = document.getElementById('queue-data')?.dataset?.accessToken;
    if (!accessToken) return;

    let previousStatus = null;
    let countdownInterval = null;
    let secondsLeft = 0;

    // Elements
    const tokenDisplay = document.getElementById('token-display');
    const statusBadge = document.getElementById('status-badge');
    const positionDisplay = document.getElementById('position-display');
    const peopleAheadDisplay = document.getElementById('people-ahead-display');
    const waitTimeDisplay = document.getElementById('wait-time-display');
    const currentServingDisplay = document.getElementById('current-serving-display');
    const tokenCard = document.getElementById('token-card');
    const calledAlertSection = document.getElementById('called-alert-section');
    const countdownDisplay = document.getElementById('countdown-timer');
    const btnImHere = document.getElementById('btn-im-here');
    const btnLeaveQueue = document.getElementById('btn-leave-queue');
    const statusMessage = document.getElementById('status-message');

    // Request notification permission early
    if (queueNotifier) {
        queueNotifier.requestPermission();
    }

    async function pollQueueStatus() {
        try {
            const response = await fetch(`/api/queue/${accessToken}/status`);
            if (!response.ok) {
                if (response.status === 404) {
                    clearInterval(pollInterval);
                    if (statusMessage) statusMessage.innerText = 'Queue ticket not found.';
                }
                return;
            }

            const resData = await response.json();
            if (resData.success && resData.data) {
                updateUI(resData.data);
            }
        } catch (error) {
            console.warn('Queue polling error:', error);
        }
    }

    function updateUI(data) {
        if (tokenDisplay) tokenDisplay.innerText = data.token;
        if (positionDisplay) positionDisplay.innerText = data.position > 0 ? `#${data.position}` : '--';
        if (peopleAheadDisplay) peopleAheadDisplay.innerText = data.peopleAhead;
        if (waitTimeDisplay) waitTimeDisplay.innerText = `${data.estimatedWait} min`;
        if (currentServingDisplay) currentServingDisplay.innerText = data.currentServing;

        // Check for state change to CALLED
        if (previousStatus !== 'CALLED' && data.status === 'CALLED') {
            onCustomerCalled(data);
        }
        previousStatus = data.status;

        // Update Status Badge and Styles
        updateStatusStyle(data.status, data.statusDisplayName);

        // Handle CALLED Countdown
        if (data.status === 'CALLED') {
            calledAlertSection?.classList.remove('d-none');
            secondsLeft = data.secondsRemaining;
            startCountdownTimer();
        } else {
            calledAlertSection?.classList.add('d-none');
            clearInterval(countdownInterval);
        }

        // Handle SERVING
        if (data.status === 'SERVING') {
            tokenCard?.classList.add('state-serving-active');
            if (statusMessage) statusMessage.innerHTML = '<span class="text-success fw-bold"><i class="bi bi-check-circle-fill me-1"></i> You are currently being served at the counter!</span>';
        } else {
            tokenCard?.classList.remove('state-serving-active');
        }

        // Handle SERVED / SKIPPED / CANCELLED / EXPIRED
        if (['SERVED', 'SKIPPED', 'CANCELLED', 'EXPIRED'].includes(data.status)) {
            btnLeaveQueue?.classList.add('d-none');
            if (btnImHere) btnImHere.disabled = true;
            if (data.status === 'SERVED') {
                if (statusMessage) statusMessage.innerHTML = '<span class="text-secondary fw-semibold"><i class="bi bi-check2-all me-1"></i> You have been served. Thank you for your visit!</span>';
            } else if (data.status === 'SKIPPED' || data.status === 'EXPIRED') {
                if (statusMessage) statusMessage.innerHTML = '<span class="text-danger fw-semibold"><i class="bi bi-exclamation-triangle me-1"></i> Your turn was skipped due to response time expiry.</span>';
            } else if (data.status === 'CANCELLED') {
                if (statusMessage) statusMessage.innerHTML = '<span class="text-muted"><i class="bi bi-x-circle me-1"></i> You have left this queue.</span>';
            }
        }
    }

    function onCustomerCalled(data) {
        // Play chime sound
        if (queueNotifier) {
            queueNotifier.playChime();
            queueNotifier.showDesktopNotification(
                'YOUR TURN IS NEXT!',
                `Your token ${data.token} has been called. Please arrive at the counter!`
            );
        }
    }

    function startCountdownTimer() {
        clearInterval(countdownInterval);
        updateTimerDisplay();

        countdownInterval = setInterval(() => {
            secondsLeft--;
            if (secondsLeft <= 0) {
                clearInterval(countdownInterval);
                updateTimerDisplay();
                // Fast poll to capture auto-expiry
                pollQueueStatus();
            } else {
                updateTimerDisplay();
            }
        }, 1000);
    }

    function updateTimerDisplay() {
        if (!countdownDisplay) return;
        const mins = Math.floor(Math.max(0, secondsLeft) / 60);
        const secs = Math.max(0, secondsLeft) % 60;
        countdownDisplay.innerText = `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }

    function updateStatusStyle(status, displayName) {
        if (!statusBadge) return;
        statusBadge.className = 'badge';
        statusBadge.innerText = displayName || status;

        tokenCard?.classList.remove('state-called-active', 'state-serving-active');

        switch (status) {
            case 'WAITING':
                statusBadge.classList.add('badge-waiting');
                if (statusMessage) statusMessage.innerText = 'Please stay nearby. Your turn is approaching.';
                break;
            case 'CALLED':
                statusBadge.classList.add('badge-called');
                tokenCard?.classList.add('state-called-active');
                if (statusMessage) statusMessage.innerText = 'Your token is being called right now!';
                break;
            case 'SERVING':
                statusBadge.classList.add('badge-serving');
                break;
            case 'SERVED':
                statusBadge.classList.add('badge-served');
                break;
            case 'SKIPPED':
            case 'EXPIRED':
                statusBadge.classList.add('badge-skipped');
                break;
            case 'CANCELLED':
                statusBadge.classList.add('badge-cancelled');
                break;
            default:
                statusBadge.classList.add('bg-secondary');
        }
    }

    // Action: "I'M HERE" button
    if (btnImHere) {
        btnImHere.addEventListener('click', async () => {
            btnImHere.disabled = true;
            btnImHere.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Confirming...';
            try {
                const response = await fetch(`/api/queue/${accessToken}/arrive`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });
                const result = await response.json();
                if (result.success) {
                    pollQueueStatus();
                } else {
                    alert(result.message || 'Unable to confirm arrival.');
                    btnImHere.disabled = false;
                    btnImHere.innerHTML = '<i class="bi bi-hand-thumbs-up-fill me-1"></i> I\'M HERE';
                }
            } catch (err) {
                alert('Connection error. Please try again.');
                btnImHere.disabled = false;
                btnImHere.innerHTML = '<i class="bi bi-hand-thumbs-up-fill me-1"></i> I\'M HERE';
            }
        });
    }

    // Action: "Leave Queue" button
    if (btnLeaveQueue) {
        btnLeaveQueue.addEventListener('click', async () => {
            if (!confirm('Are you sure you want to leave the queue? Your spot will be forfeited.')) {
                return;
            }
            btnLeaveQueue.disabled = true;
            try {
                const response = await fetch(`/api/queue/${accessToken}/leave`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });
                const result = await response.json();
                if (result.success) {
                    pollQueueStatus();
                } else {
                    alert(result.message || 'Could not leave queue.');
                    btnLeaveQueue.disabled = false;
                }
            } catch (err) {
                alert('Connection error.');
                btnLeaveQueue.disabled = false;
            }
        });
    }

    // Initial poll and recurring poll every 3 seconds
    pollQueueStatus();
    const pollInterval = setInterval(pollQueueStatus, 3000);
});
