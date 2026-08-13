<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<% String pageTitle = "Dashboard"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container">

    <div class="card-title-row">
        <div>
            <h1 style="font-size: 1.6rem; margin-bottom: 6px;"><c:out value="${shop.shopName}"/></h1>
            <span class="pill ${shop.open ? 'pill-served' : 'pill-skipped'}">
                <c:out value="${shop.open ? 'Open' : 'Closed'}"/>
            </span>
        </div>
        <a href="${pageContext.request.contextPath}/display?shopId=${shop.shopId}" target="_blank" class="btn btn-outline">
            Open Public Display
        </a>
    </div>

    <div class="stat-row">
        <div class="stat-box">
            <div class="stat-value" id="waitingCountStat"><c:out value="${fn:length(waitingList)}"/></div>
            <div class="stat-label">waiting now</div>
        </div>
        <div class="stat-box">
            <div class="stat-value"><c:out value="${servedToday}"/></div>
            <div class="stat-label">served today</div>
        </div>
        <div class="stat-box">
            <div class="stat-value"><c:out value="${skippedToday}"/></div>
            <div class="stat-label">no-shows today</div>
        </div>
    </div>

    <div class="card">
        <h3>Now serving</h3>
        <c:choose>
            <c:when test="${not empty currentToken}">
                <div style="display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px;">
                    <div>
                        <div style="font-family: var(--font-mono); font-size: 2.4rem; font-weight: 700; color: var(--amber-dim);">
                            #<c:out value="${currentToken.tokenNumber}"/>
                        </div>
                        <div class="text-muted"><c:out value="${currentToken.customerName}"/></div>
                    </div>
                    <div class="btn-row">
                        <form action="${pageContext.request.contextPath}/callNext" method="post">
                            <button class="btn btn-primary" type="submit">Serve &amp; Call Next</button>
                        </form>
                        <form action="${pageContext.request.contextPath}/skip" method="post">
                            <button class="btn btn-danger" type="submit">Skip (No-show)</button>
                        </form>
                    </div>
                </div>
                <div class="grace-bar"><div class="grace-bar-fill" id="graceBarFill" style="width: 100%;"></div></div>
                <div class="text-muted" id="graceText" style="font-size: 0.8rem; margin-top: 6px;">
                    Calculating grace period&hellip;
                </div>
            </c:when>
            <c:otherwise>
                <p class="text-muted">Nobody is currently being served.</p>
                <form action="${pageContext.request.contextPath}/callNext" method="post">
                    <button class="btn btn-amber" type="submit" ${fn:length(waitingList) == 0 ? 'disabled' : ''}>
                        Call Next
                    </button>
                </form>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h3>Waiting (<c:out value="${fn:length(waitingList)}"/>)</h3>
        <c:choose>
            <c:when test="${empty waitingList}">
                <p class="text-muted mb-0">Nobody's waiting right now.</p>
            </c:when>
            <c:otherwise>
                <ul class="queue-list">
                    <c:forEach var="t" items="${waitingList}">
                        <li>
                            <span>#<c:out value="${t.tokenNumber}"/> &mdash; <c:out value="${t.customerName}"/></span>
                            <span class="text-muted"><c:out value="${t.customerPhone}"/></span>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h3>Shop settings</h3>
        <c:if test="${param.error == 'settings'}">
            <div class="alert alert-error">Enter a valid service time between 1 and 240 minutes.</div>
        </c:if>
        <form action="${pageContext.request.contextPath}/updateSettings" method="post">
            <div class="field">
                <label for="avgServiceTime">Average time per customer (minutes)</label>
                <input type="number" id="avgServiceTime" name="avgServiceTime" min="1" max="240"
                       value="${shop.avgServiceTimeMins}" required>
            </div>
            <div class="checkbox-row field">
                <input type="checkbox" id="isOpen" name="isOpen" ${shop.open ? 'checked' : ''}>
                <label for="isOpen" style="margin-bottom: 0;">Accepting new tokens</label>
            </div>
            <button type="submit" class="btn btn-outline">Save settings</button>
        </form>
    </div>

</div>

<script>
(function () {
    var contextPath = "${pageContext.request.contextPath}";
    var shopId = <c:out value="${shop.shopId}"/>;

    // Keep in sync with TokenDAO.GRACE_PERIOD_MINUTES
    var GRACE_TOTAL_MS = 5 * 60 * 1000;

    var graceDeadlineMs = <c:choose>
        <c:when test="${not empty currentToken and not empty currentToken.graceDeadline}"><c:out value="${currentToken.graceDeadline.time}"/></c:when>
        <c:otherwise>0</c:otherwise>
    </c:choose>;

    var initialCurrentTokenNumber = <c:choose>
        <c:when test="${not empty currentToken}"><c:out value="${currentToken.tokenNumber}"/></c:when>
        <c:otherwise>null</c:otherwise>
    </c:choose>;

    var initialWaitingCount = <c:out value="${fn:length(waitingList)}"/>;

    function tickGraceBar() {
        if (!graceDeadlineMs) return;
        var fill = document.getElementById('graceBarFill');
        var text = document.getElementById('graceText');
        if (!fill || !text) return;

        var remaining = graceDeadlineMs - Date.now();
        if (remaining <= 0) {
            fill.style.width = '0%';
            text.textContent = 'Grace period expired - this will auto-skip shortly.';
            return;
        }
        var pct = Math.max(0, Math.min(100, (remaining / GRACE_TOTAL_MS) * 100));
        fill.style.width = pct + '%';
        text.textContent = 'Auto-skips in ' + Math.ceil(remaining / 1000) + 's if not served.';
    }

    // Detects changes made by the background auto-skip worker (or another
    // device/tab) and refreshes so the dashboard never goes stale.
    function pollForChanges() {
        fetch(contextPath + '/api/queueStatus?shopId=' + shopId)
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (data) {
                if (!data || data.error) return;
                var waitingCount = data.waitingTokenNumbers ? data.waitingTokenNumbers.length : 0;
                if (data.currentTokenNumber !== initialCurrentTokenNumber || waitingCount !== initialWaitingCount) {
                    location.reload();
                }
            })
            .catch(function (err) { console.error('Queue poll failed', err); });
    }

    setInterval(tickGraceBar, 1000);
    setInterval(pollForChanges, 5000);
    tickGraceBar();
})();
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
