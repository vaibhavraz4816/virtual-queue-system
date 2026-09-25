/**
 * QueueEase - Customer My Queue Real-Time Live Dashboard Engine
 * Handles guest tokens, local storage synchronization, live status polling,
 * turn alerts with audio chime, 2-minute countdown timer, and multi-queue switching.
 */
document.addEventListener('DOMContentLoaded', () => {
    let tokenSet = new Set();
    let currentSelectedToken = null;
    let chimedTokens = new Set();
    let countdownTimerInterval = null;
    let ticketDataMap = new Map();

    const emptyStateCard = document.getElementById('empty-state-card');
    const myQueueContainer = document.getElementById('my-queue-container');
    const multiQueueTabs = document.getElementById('multi-queue-tabs');
    const queueTabList = document.getElementById('queue-tab-list');
    const leaveModalEl = document.getElementById('leaveQueueModal');
    let leaveModal = null;
    if (leaveModalEl && window.bootstrap) {
        leaveModal = new bootstrap.Modal(leaveModalEl);
    }
    let tokenToLeave = null;

    // 1. Synchronize tokens from Server SSR, LocalStorage, and Cookies
    function initTokens() {
        // Server initial tokens
        try {
            if (window.INITIAL_MY_TOKENS) {
                const parsed = JSON.parse(window.INITIAL_MY_TOKENS);
                if (Array.isArray(parsed)) {
                    parsed.forEach(t => { if (t) tokenSet.add(t.trim()); });
                }
            }
        } catch (e) {}

        // LocalStorage tokens
        try {
            const localSaved = localStorage.getItem('qe_my_tokens');
            if (localSaved) {
                const parsed = JSON.parse(localSaved);
                if (Array.isArray(parsed)) {
                    parsed.forEach(t => { if (t) tokenSet.add(t.trim()); });
                }
            }
        } catch (e) {}

        // URL param override/addition (?token=...)
        const urlParams = new URLSearchParams(window.location.search);
        const urlToken = urlParams.get('token');
        if (urlToken && urlToken.trim()) {
            tokenSet.add(urlToken.trim());
            currentSelectedToken = urlToken.trim();
        }

        persistTokens();
    }

    function persistTokens() {
        const arr = Array.from(tokenSet);
        try {
            localStorage.setItem('qe_my_tokens', JSON.stringify(arr));
        } catch (e) {}
        // Also sync cookie for server rendering
        document.cookie = `q_tokens=${arr.join(',')}; path=/; max-age=604800; SameSite=Lax`;
    }

    function removeToken(token) {
        tokenSet.delete(token);
        persistTokens();
        ticketDataMap.delete(token);
        if (currentSelectedToken === token) {
            currentSelectedToken = tokenSet.size > 0 ? Array.from(tokenSet)[0] : null;
        }
    }

    // 2. Polling API
    async function pollTickets() {
        if (tokenSet.size === 0) {
            renderEmptyState();
            return;
        }

        try {
            const res = await fetch('/api/queue/my-active-tickets', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(Array.from(tokenSet))
            });

            if (!res.ok) return;

            const data = await res.json();
            if (data && data.success && Array.isArray(data.data)) {
                handleTicketsResponse(data.data);
            }
        } catch (e) {
            console.warn('My Queue polling error:', e);
        }
    }

    function handleTicketsResponse(tickets) {
        // Filter active tickets
        const activeTickets = tickets.filter(t => 
            t.status !== 'SERVED' && t.status !== 'CANCELLED' && t.status !== 'SKIPPED' && t.status !== 'EXPIRED'
        );

        // Update local map
        tickets.forEach(t => ticketDataMap.set(t.guestAccessToken, t));

        if (activeTickets.length === 0) {
            // Check if there are completed/expired tickets we just saw
            const finishedTickets = tickets.filter(t => 
                t.status === 'SERVED' || t.status === 'CANCELLED' || t.status === 'SKIPPED' || t.status === 'EXPIRED'
            );
            if (finishedTickets.length > 0) {
                renderFinishedState(finishedTickets[0]);
                return;
            }
            renderEmptyState();
            return;
        }

        // Set default selected token
        if (!currentSelectedToken || !activeTickets.some(t => t.guestAccessToken === currentSelectedToken)) {
            currentSelectedToken = activeTickets[0].guestAccessToken;
        }

        // Render Tabs if more than 1 queue
        renderTabs(activeTickets);

        // Render the active ticket card
        const currentTicket = activeTickets.find(t => t.guestAccessToken === currentSelectedToken) || activeTickets[0];
        renderTicketCard(currentTicket);
    }

    function renderTabs(activeTickets) {
        if (activeTickets.length > 1) {
            multiQueueTabs.classList.remove('d-none');
            queueTabList.innerHTML = '';
            activeTickets.forEach(t => {
                const btn = document.createElement('button');
                btn.type = 'button';
                const isActive = t.guestAccessToken === currentSelectedToken;
                btn.className = `btn btn-sm rounded-pill px-3 fw-semibold text-nowrap ${isActive ? 'btn-primary' : 'btn-outline-secondary'}`;
                btn.innerHTML = `<i class="bi bi-shop me-1"></i> ${escapeHtml(t.shopName)} <span class="badge bg-white text-dark ms-1">${t.token}</span>`;
                btn.addEventListener('click', () => {
                    currentSelectedToken = t.guestAccessToken;
                    renderTabs(activeTickets);
                    renderTicketCard(t);
                });
                queueTabList.appendChild(btn);
            });
        } else {
            multiQueueTabs.classList.add('d-none');
        }
    }

    function renderTicketCard(t) {
        if (emptyStateCard) emptyStateCard.classList.add('d-none');
        if (myQueueContainer) myQueueContainer.classList.remove('d-none');

        const isCalled = t.status === 'CALLED';
        const isServing = t.status === 'SERVING' || t.status === 'ARRIVED';
        const isWaiting = t.status === 'WAITING';

        // Sound alert & Web Notification for CALLED state
        if (isCalled && !chimedTokens.has(t.guestAccessToken)) {
            chimedTokens.add(t.guestAccessToken);
            if (window.queueNotifier) {
                window.queueNotifier.playChime();
                window.queueNotifier.showDesktopNotification(
                    "🔔 IT'S YOUR TURN!",
                    `Your token ${t.token} at ${t.shopName} has been called. Please arrive at the counter!`
                );
            }
        }

        const categoryDisplay = t.shopCategory ? t.shopCategory.displayName || t.shopCategory : 'Business';

        let html = `
            <div class="card card-custom my-queue-card p-4 p-md-5 text-center mb-4 ${isCalled ? 'state-called-active' : (isServing ? 'state-serving-active' : '')}">
                <!-- Shop Details Header -->
                <div class="mb-3">
                    <div class="d-inline-flex align-items-center gap-2 px-3 py-1 bg-light rounded-pill border">
                        <i class="bi bi-shop text-primary"></i>
                        <span class="fw-bold text-dark">${escapeHtml(t.shopName)}</span>
                        <span class="badge bg-primary bg-opacity-10 text-primary small">${escapeHtml(categoryDisplay)}</span>
                    </div>
                    ${t.shopAddress ? `<div class="text-muted small mt-1"><i class="bi bi-geo-alt"></i> ${escapeHtml(t.shopAddress)}</div>` : ''}
                </div>

                ${t.shopStatus === 'QUEUE_PAUSED' ? `
                    <div class="alert alert-warning py-2 small mb-3">
                        <i class="bi bi-pause-circle-fill me-1"></i> <strong>QUEUE PAUSED:</strong> Your position is preserved. The counter will resume shortly.
                    </div>
                ` : ''}

                <!-- CALLED Turn Alert Banner -->
                <div class="p-3 bg-warning bg-opacity-15 rounded-4 border border-warning my-3 text-start ${isCalled ? '' : 'd-none'}" id="active-called-box">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <div class="stat-widget-icon bg-warning text-dark flex-shrink-0" style="width: 38px; height: 38px;">
                            <i class="bi bi-bell-fill fs-5"></i>
                        </div>
                        <div>
                            <h5 class="mb-0 fw-bold text-dark">🔔 IT'S YOUR TURN!</h5>
                            <span class="small text-muted">Token <strong class="text-dark">${t.token}</strong></span>
                        </div>
                    </div>
                    <p class="small text-secondary mb-3">
                        Please head to the counter immediately. Time remaining: <strong class="text-danger fs-6" id="countdown-display">02:00</strong>
                    </p>
                    <button type="button" class="btn btn-warning w-100 fw-bold py-2 shadow-sm text-dark" id="btn-im-here-action">
                        <i class="bi bi-hand-thumbs-up-fill me-1"></i> I'M HERE
                    </button>
                </div>

                <!-- Status Pill -->
                <div class="mb-2">
                    <span class="badge ${isWaiting ? 'badge-waiting' : (isCalled ? 'badge-called' : (isServing ? 'badge-serving' : 'badge-served'))}">
                        ${escapeHtml(t.humanStatusMessage || t.statusDisplayName || t.status)}
                    </span>
                </div>

                <span class="text-muted small text-uppercase fw-bold letter-spacing-1 d-block mt-2">YOUR TOKEN</span>
                
                <!-- Token Large -->
                <div class="token-badge-large my-2">
                    ${t.token}
                </div>

                <!-- Live Metrics Grid -->
                <div class="row g-2 border-top border-bottom py-3 my-3">
                    <div class="col-4">
                        <span class="text-muted small d-block">Position</span>
                        <span class="fw-bold fs-3 text-dark">${t.position > 0 ? '#' + t.position : '--'}</span>
                    </div>
                    <div class="col-4 border-start border-end">
                        <span class="text-muted small d-block">People Ahead</span>
                        <span class="fw-bold fs-3 text-dark">${t.peopleAhead}</span>
                    </div>
                    <div class="col-4">
                        <span class="text-muted small d-block">Est. Wait</span>
                        <span class="fw-bold fs-3 text-primary">${t.estimatedWait} min</span>
                    </div>
                </div>

                <!-- Wait Disclaimer -->
                <p class="text-muted small mb-3" style="font-size: 0.8rem;">
                    <i class="bi bi-info-circle me-1"></i> Approximate — actual wait may vary.
                </p>

                <!-- Currently Serving & Joined Info -->
                <div class="d-flex align-items-center justify-content-between text-muted small px-1 pt-2 border-top">
                    <span>Now Serving: <strong class="text-dark fs-6">${t.currentServing || 'None'}</strong></span>
                    ${t.joinedTimeFormatted ? `<span>Joined: ${t.joinedTimeFormatted}</span>` : ''}
                </div>
            </div>

            <!-- Card Actions Footer -->
            <div class="d-flex justify-content-between align-items-center px-2">
                ${t.shopSlug ? `
                    <a href="/shop/${encodeURIComponent(t.shopSlug)}/queue" class="text-decoration-none small text-muted">
                        <i class="bi bi-arrow-left me-1"></i> View Shop Profile
                    </a>
                ` : '<span></span>'}
                <button type="button" class="btn btn-outline-danger btn-sm rounded-pill px-3 ms-auto" id="btn-trigger-leave">
                    <i class="bi bi-box-arrow-left me-1"></i> Leave Queue
                </button>
            </div>
        `;

        myQueueContainer.innerHTML = html;

        // Wire "I'M HERE" button
        const btnHere = document.getElementById('btn-im-here-action');
        if (btnHere) {
            btnHere.addEventListener('click', async () => {
                try {
                    btnHere.disabled = true;
                    btnHere.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Confirming...';
                    await fetch(`/api/queue/${t.guestAccessToken}/arrive`, { method: 'POST' });
                    await pollTickets();
                } catch (e) {
                    console.error('Arrival confirm error:', e);
                }
            });
        }

        // Wire "Leave Queue" button
        const btnLeave = document.getElementById('btn-trigger-leave');
        if (btnLeave) {
            btnLeave.addEventListener('click', () => {
                tokenToLeave = t.guestAccessToken;
                if (leaveModal) {
                    leaveModal.show();
                } else if (confirm('Are you sure you want to leave this queue?')) {
                    executeLeaveQueue(t.guestAccessToken);
                }
            });
        }

        // Handle 2-minute Countdown
        if (isCalled && t.callExpiresAt) {
            startCountdown(t.callExpiresAt);
        } else {
            clearInterval(countdownTimerInterval);
        }
    }

    function startCountdown(expiresAtIso) {
        clearInterval(countdownTimerInterval);
        const expiresTime = new Date(expiresAtIso).getTime();

        function updateDisplay() {
            const now = new Date().getTime();
            const diffSecs = Math.max(0, Math.floor((expiresTime - now) / 1000));
            const displayEl = document.getElementById('countdown-display');
            if (displayEl) {
                const m = Math.floor(diffSecs / 60);
                const s = diffSecs % 60;
                displayEl.innerText = `${m}:${s < 10 ? '0' : ''}${s}`;
            }

            if (diffSecs <= 0) {
                clearInterval(countdownTimerInterval);
                pollTickets();
            }
        }

        updateDisplay();
        countdownTimerInterval = setInterval(updateDisplay, 1000);
    }

    async function executeLeaveQueue(token) {
        try {
            await fetch(`/api/queue/${token}/leave`, { method: 'POST' });
        } catch (e) {}
        removeToken(token);
        if (leaveModal) leaveModal.hide();
        pollTickets();
    }

    // Modal confirm leave button
    const confirmLeaveBtn = document.getElementById('confirm-leave-btn');
    if (confirmLeaveBtn) {
        confirmLeaveBtn.addEventListener('click', () => {
            if (tokenToLeave) {
                executeLeaveQueue(tokenToLeave);
            }
        });
    }

    function renderFinishedState(t) {
        clearInterval(countdownTimerInterval);
        if (multiQueueTabs) multiQueueTabs.classList.add('d-none');
        if (emptyStateCard) emptyStateCard.classList.add('d-none');
        if (myQueueContainer) {
            myQueueContainer.classList.remove('d-none');
            const isServed = t.status === 'SERVED';
            myQueueContainer.innerHTML = `
                <div class="card card-custom p-5 text-center my-queue-card">
                    <div class="stat-widget-icon ${isServed ? 'bg-success text-white' : 'bg-secondary text-white'} mx-auto mb-3" style="width: 64px; height: 64px; font-size: 2rem;">
                        <i class="bi ${isServed ? 'bi-check-circle-fill' : 'bi-slash-circle'}"></i>
                    </div>
                    <h4 class="fw-bold mb-1">${escapeHtml(t.humanStatusMessage || t.statusDisplayName)}</h4>
                    <p class="text-muted small mb-4">Token <strong>${t.token}</strong> at ${escapeHtml(t.shopName)}</p>
                    <div class="d-flex justify-content-center gap-2">
                        <button type="button" class="btn btn-primary-custom px-4" data-action="open-scanner">
                            <i class="bi bi-qr-code-scan me-2"></i> Join Another Queue
                        </button>
                        <button type="button" class="btn btn-outline-secondary px-3" id="btn-dismiss-ticket">
                            Dismiss
                        </button>
                    </div>
                </div>
            `;
            document.getElementById('btn-dismiss-ticket')?.addEventListener('click', () => {
                removeToken(t.guestAccessToken);
                renderEmptyState();
            });
        }
    }

    function renderEmptyState() {
        clearInterval(countdownTimerInterval);
        if (multiQueueTabs) multiQueueTabs.classList.add('d-none');
        if (myQueueContainer) myQueueContainer.classList.add('d-none');
        if (emptyStateCard) emptyStateCard.classList.remove('d-none');
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.innerText = str;
        return div.innerHTML;
    }

    // Initialize and kick off polling
    initTokens();
    pollTickets();
    setInterval(pollTickets, 3500);
});
