/**
 * Shop Owner Live Queue Management & Dashboard Real-Time Polling
 */
document.addEventListener('DOMContentLoaded', () => {
    // Top KPI Elements
    const statWaiting = document.getElementById('stat-waiting');
    const statServing = document.getElementById('stat-serving');
    const statAvgWait = document.getElementById('stat-avg-wait');
    const statServedToday = document.getElementById('stat-served-today');

    // Hero "Now Serving" Elements
    const heroToken = document.getElementById('hero-token');
    const heroCustomerName = document.getElementById('hero-customer-name');
    const heroStatusBadge = document.getElementById('hero-status-badge');
    const heroTimerSection = document.getElementById('hero-timer-section');
    const heroTimer = document.getElementById('hero-timer');

    // Action Buttons
    const btnFinishAndNext = document.getElementById('btn-finish-and-next');
    const btnCallNext = document.getElementById('btn-call-next');
    const btnServeCurrent = document.getElementById('btn-serve-current');
    const btnSkipCurrent = document.getElementById('btn-skip-current');
    const btnCancelCurrent = document.getElementById('btn-cancel-current');
    const btnToggleQueue = document.getElementById('btn-toggle-queue');

    // Queue Table Body
    const queueTableBody = document.getElementById('queue-table-body');
    const emptyQueueAlert = document.getElementById('empty-queue-alert');

    // Walk-In Modal Form
    const walkInForm = document.getElementById('walk-in-form');
    const btnSubmitWalkIn = document.getElementById('btn-submit-walkin');

    let currentEntryId = null;
    let currentStatus = null;
    let currentSecondsRemaining = 0;
    let heroCountdownInterval = null;

    // Toast helper
    function showToast(message, isError = false) {
        const toastEl = document.getElementById('dashboard-toast');
        const toastBody = document.getElementById('toast-message');
        if (!toastEl || !toastBody) return;

        toastBody.innerText = message;
        toastEl.className = `toast align-items-center text-white border-0 ${isError ? 'bg-danger' : 'bg-success'}`;
        const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
        toast.show();
    }

    async function pollDashboard() {
        try {
            const [statsRes, queueRes] = await Promise.all([
                fetch('/api/shop/dashboard/stats'),
                fetch('/api/shop/queue/live')
            ]);

            if (statsRes.ok) {
                const statsData = await statsRes.json();
                if (statsData.success && statsData.data) {
                    updateStatsUI(statsData.data);
                }
            }

            if (queueRes.ok) {
                const queueData = await queueRes.json();
                if (queueData.success && queueData.data) {
                    updateQueueTableUI(queueData.data);
                }
            }
        } catch (e) {
            console.warn('Dashboard poll error:', e);
        }
    }

    function updateStatsUI(stats) {
        if (statWaiting) statWaiting.innerText = stats.customersWaiting;
        if (statServing) statServing.innerText = stats.currentServingToken;
        if (statAvgWait) statAvgWait.innerText = `${stats.averageWaitMinutes} min`;
        if (statServedToday) statServedToday.innerText = stats.servedToday;

        currentEntryId = stats.currentEntryId;
        currentStatus = stats.currentStatus;

        if (heroToken) heroToken.innerText = stats.currentServingToken;
        if (heroCustomerName) heroCustomerName.innerText = stats.currentCustomerName !== '--' ? `Customer: ${stats.currentCustomerName}` : 'Counter is free';

        if (heroStatusBadge) {
            heroStatusBadge.innerText = stats.currentStatus;
            heroStatusBadge.className = 'badge';
            if (stats.currentStatus === 'CALLED') {
                heroStatusBadge.classList.add('badge-called');
            } else if (stats.currentStatus === 'SERVING') {
                heroStatusBadge.classList.add('badge-serving');
            } else {
                heroStatusBadge.classList.add('bg-secondary');
            }
        }

        // Timer for CALLED status
        if (stats.currentStatus === 'CALLED') {
            heroTimerSection?.classList.remove('d-none');
            currentSecondsRemaining = stats.currentSecondsRemaining;
            startHeroTimer();
        } else {
            heroTimerSection?.classList.add('d-none');
            clearInterval(heroCountdownInterval);
        }

        // Button availability
        if (btnFinishAndNext) {
            btnFinishAndNext.disabled = stats.isPaused;
        }

        if (btnCallNext) {
            btnCallNext.disabled = stats.isPaused || stats.currentStatus === 'SERVING';
        }

        if (btnServeCurrent) {
            btnServeCurrent.disabled = !stats.currentEntryId || (stats.currentStatus !== 'SERVING' && stats.currentStatus !== 'CALLED');
        }

        if (btnSkipCurrent) {
            btnSkipCurrent.disabled = !stats.currentEntryId;
        }

        if (btnCancelCurrent) {
            btnCancelCurrent.disabled = !stats.currentEntryId;
        }

        // Toggle Queue Pause/Resume
        if (btnToggleQueue) {
            if (stats.isPaused) {
                btnToggleQueue.className = 'btn btn-outline-success';
                btnToggleQueue.innerHTML = '<i class="bi bi-play-circle me-1"></i> Resume Queue';
                btnToggleQueue.dataset.action = 'resume';
            } else {
                btnToggleQueue.className = 'btn btn-outline-warning';
                btnToggleQueue.innerHTML = '<i class="bi bi-pause-circle me-1"></i> Pause Queue';
                btnToggleQueue.dataset.action = 'pause';
            }
        }
    }

    function startHeroTimer() {
        clearInterval(heroCountdownInterval);
        updateHeroTimerDisplay();

        heroCountdownInterval = setInterval(() => {
            currentSecondsRemaining--;
            if (currentSecondsRemaining <= 0) {
                clearInterval(heroCountdownInterval);
                updateHeroTimerDisplay();
                pollDashboard();
            } else {
                updateHeroTimerDisplay();
            }
        }, 1000);
    }

    function updateHeroTimerDisplay() {
        if (!heroTimer) return;
        const mins = Math.floor(Math.max(0, currentSecondsRemaining) / 60);
        const secs = Math.max(0, currentSecondsRemaining) % 60;
        heroTimer.innerText = `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }

    function updateQueueTableUI(items) {
        if (!queueTableBody) return;

        if (!items || items.length === 0) {
            queueTableBody.innerHTML = '';
            emptyQueueAlert?.classList.remove('d-none');
            return;
        }

        emptyQueueAlert?.classList.add('d-none');

        let html = '';
        items.forEach(item => {
            const isRowCalled = item.status === 'CALLED';
            const isRowServing = item.status === 'SERVING';
            const rowClass = isRowCalled ? 'row-called' : (isRowServing ? 'row-serving' : '');

            let badgeClass = 'badge-waiting';
            if (item.status === 'CALLED') badgeClass = 'badge-called';
            if (item.status === 'SERVING') badgeClass = 'badge-serving';

            html += `
                <tr class="${rowClass}">
                    <td>
                        <span class="fw-bold fs-5 text-primary">${item.tokenNumber}</span>
                        ${item.isWalkIn ? '<span class="badge bg-light text-dark border ms-1" style="font-size:0.7rem;">Walk-In</span>' : ''}
                    </td>
                    <td>
                        <div class="fw-semibold">${escapeHtml(item.customerName)}</div>
                        <small class="text-muted">${escapeHtml(item.phone)}</small>
                    </td>
                    <td class="text-muted">${item.joinedTime}</td>
                    <td>${item.peopleAhead > 0 ? item.peopleAhead : '<span class="text-success fw-bold">Next</span>'}</td>
                    <td>${item.estimatedWaitMinutes} min</td>
                    <td><span class="badge ${badgeClass}">${item.statusDisplayName}</span></td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            ${item.status === 'WAITING' ? `
                                <button class="btn btn-outline-primary btn-action-call" data-id="${item.id}" title="Call Next">
                                    <i class="bi bi-megaphone"></i> Call
                                </button>
                            ` : ''}
                            ${item.canServe ? `
                                <button class="btn btn-success btn-action-serve" data-id="${item.id}" title="Mark as Served">
                                    <i class="bi bi-check-lg"></i> Serve
                                </button>
                            ` : ''}
                            ${item.canSkip ? `
                                <button class="btn btn-outline-warning btn-action-skip" data-id="${item.id}" title="Skip Customer">
                                    <i class="bi bi-skip-forward"></i> Skip
                                </button>
                            ` : ''}
                            ${item.canCancel ? `
                                <button class="btn btn-outline-danger btn-action-cancel" data-id="${item.id}" title="Cancel Customer">
                                    <i class="bi bi-x"></i>
                                </button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        });

        queueTableBody.innerHTML = html;
        bindRowActions();
    }

    function escapeHtml(text) {
        if (!text) return '';
        return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }

    function bindRowActions() {
        document.querySelectorAll('.btn-action-serve').forEach(btn => {
            btn.onclick = () => performAction(`/api/shop/queue/${btn.dataset.id}/serve`);
        });
        document.querySelectorAll('.btn-action-skip').forEach(btn => {
            btn.onclick = () => performAction(`/api/shop/queue/${btn.dataset.id}/skip`);
        });
        document.querySelectorAll('.btn-action-cancel').forEach(btn => {
            btn.onclick = () => {
                if (confirm('Cancel this customer ticket?')) {
                    performAction(`/api/shop/queue/${btn.dataset.id}/cancel`);
                }
            };
        });
    }

    async function performAction(url) {
        try {
            const res = await fetch(url, { method: 'POST' });
            const data = await res.json();
            if (data.success) {
                showToast(data.message);
                pollDashboard();
            } else {
                showToast(data.message, true);
            }
        } catch (e) {
            showToast('Failed to perform action.', true);
        }
    }

    // Top Action Handlers
    if (btnFinishAndNext) {
        btnFinishAndNext.addEventListener('click', async () => {
            btnFinishAndNext.disabled = true;
            try {
                const res = await fetch('/api/shop/queue/finish-and-next', { method: 'POST' });
                const data = await res.json();
                if (data.success) {
                    showToast(data.message);
                    pollDashboard();
                } else {
                    showToast(data.message, true);
                }
            } catch (e) {
                showToast('Unable to advance queue.', true);
            } finally {
                btnFinishAndNext.disabled = false;
            }
        });
    }

    if (btnCancelCurrent) {
        btnCancelCurrent.addEventListener('click', () => {
            if (currentEntryId && confirm('Cancel this customer ticket?')) {
                performAction(`/api/shop/queue/${currentEntryId}/cancel`);
            }
        });
    }

    if (btnCallNext) {
        btnCallNext.addEventListener('click', async () => {
            btnCallNext.disabled = true;
            try {
                const res = await fetch('/api/shop/queue/next', { method: 'POST' });
                const data = await res.json();
                if (data.success) {
                    showToast(data.message);
                    pollDashboard();
                } else {
                    showToast(data.message, true);
                }
            } catch (e) {
                showToast('Unable to call next customer.', true);
            } finally {
                btnCallNext.disabled = false;
            }
        });
    }

    if (btnServeCurrent) {
        btnServeCurrent.addEventListener('click', () => {
            if (currentEntryId) {
                performAction(`/api/shop/queue/${currentEntryId}/serve`);
            }
        });
    }

    if (btnSkipCurrent) {
        btnSkipCurrent.addEventListener('click', () => {
            if (currentEntryId) {
                performAction(`/api/shop/queue/${currentEntryId}/skip`);
            }
        });
    }

    if (btnToggleQueue) {
        btnToggleQueue.addEventListener('click', () => {
            const action = btnToggleQueue.dataset.action;
            performAction(`/api/shop/queue/${action}`);
        });
    }

    // Walk-In Customer Submission
    if (btnSubmitWalkIn && walkInForm) {
        btnSubmitWalkIn.addEventListener('click', async () => {
            const nameInput = document.getElementById('walkin-name');
            const phoneInput = document.getElementById('walkin-phone');

            if (!nameInput || !nameInput.value.trim()) {
                alert('Please enter customer name');
                return;
            }

            btnSubmitWalkIn.disabled = true;
            try {
                const res = await fetch('/api/shop/queue/walk-in', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        customerName: nameInput.value.trim(),
                        phone: phoneInput?.value.trim() || ''
                    })
                });
                const data = await res.json();
                if (data.success) {
                    showToast(data.message);
                    nameInput.value = '';
                    if (phoneInput) phoneInput.value = '';
                    const modalEl = document.getElementById('walkInModal');
                    if (modalEl) {
                        const modal = bootstrap.Modal.getInstance(modalEl);
                        modal?.hide();
                    }
                    pollDashboard();
                } else {
                    showToast(data.message, true);
                }
            } catch (e) {
                showToast('Failed to add walk-in customer.', true);
            } finally {
                btnSubmitWalkIn.disabled = false;
            }
        });
    }

    // Initial poll and set repeating interval (every 3.5 seconds)
    pollDashboard();
    setInterval(pollDashboard, 3500);
});
