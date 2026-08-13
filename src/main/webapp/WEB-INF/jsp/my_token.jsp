<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<% String pageTitle = "Your Token"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container-narrow">

    <div class="ticket">
        <div class="label">Your Token</div>
        <div class="token-number" id="tokenNumber">#<c:out value="${token.tokenNumber}"/></div>
        <div class="shop-name"><c:out value="${shop.shopName}"/></div>
        <span class="pill pill-${fn:toLowerCase(token.status)}" id="statusPill"><c:out value="${token.status}"/></span>
    </div>

    <div id="waitBlock" class="card mt-24 ${token.status == 'WAITING' ? '' : 'hidden'}">
        <div class="stat-row">
            <div class="stat-box">
                <div class="stat-value" id="peopleAhead"><c:out value="${peopleAhead}"/></div>
                <div class="stat-label">people ahead of you</div>
            </div>
            <div class="stat-box">
                <div class="stat-value"><span id="estimatedWait"><c:out value="${estimatedWait}"/></span>m</div>
                <div class="stat-label">estimated wait</div>
            </div>
        </div>
        <p class="text-muted mb-0" style="font-size: 0.85rem;">
            This page updates on its own every few seconds - no need to refresh.
            Keep it open and head over once you're called.
        </p>
    </div>

    <div id="calledBlock" class="card mt-24 ${token.status == 'CALLED' ? '' : 'hidden'}"
         style="text-align: center; border-color: var(--amber);">
        <h2 style="color: var(--amber-dim);">You're up!</h2>
        <p class="mb-0">Please head to the counter now.</p>
    </div>

    <div id="servedBlock" class="card mt-24 ${token.status == 'SERVED' ? '' : 'hidden'}" style="text-align: center;">
        <h2>Thanks for visiting</h2>
        <p class="mb-0 text-muted">This token has been marked as served.</p>
    </div>

    <div id="skippedBlock" class="card mt-24 ${(token.status == 'SKIPPED' or token.status == 'CANCELLED') ? '' : 'hidden'}"
         style="text-align: center;">
        <h2>Token no longer active</h2>
        <p class="text-muted">
            This token was skipped, most likely because the grace period passed
            before you arrived. You're welcome to grab a new one.
        </p>
        <a href="${pageContext.request.contextPath}/shop?id=${shop.shopId}" class="btn btn-outline">Join again</a>
    </div>

    <p class="text-center text-muted mt-24" style="font-size: 0.8rem;">
        Bookmark this page or note your token number - it's how we find you again.
    </p>
</div>

<script>
(function () {
    var tokenId = <c:out value="${token.tokenId}"/>;
    var contextPath = "${pageContext.request.contextPath}";
    var pollTimer = null;

    function show(id, visible) {
        var el = document.getElementById(id);
        if (el) el.classList.toggle('hidden', !visible);
    }

    function applyStatus(data) {
        document.getElementById('tokenNumber').textContent = '#' + data.tokenNumber;

        var pill = document.getElementById('statusPill');
        pill.textContent = data.status;
        pill.className = 'pill pill-' + data.status.toLowerCase();

        show('waitBlock', data.status === 'WAITING');
        show('calledBlock', data.status === 'CALLED');
        show('servedBlock', data.status === 'SERVED');
        show('skippedBlock', data.status === 'SKIPPED' || data.status === 'CANCELLED');

        if (data.status === 'WAITING') {
            document.getElementById('peopleAhead').textContent = data.peopleAhead;
            document.getElementById('estimatedWait').textContent = data.estimatedWaitMinutes;
        }

        if (data.status === 'SERVED' || data.status === 'SKIPPED' || data.status === 'CANCELLED') {
            if (pollTimer) {
                clearInterval(pollTimer);
                pollTimer = null;
            }
        }
    }

    function pollStatus() {
        fetch(contextPath + '/api/tokenStatus?tokenId=' + tokenId)
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (data) { if (data && !data.error) applyStatus(data); })
            .catch(function (err) { console.error('Status poll failed', err); });
    }

    pollTimer = setInterval(pollStatus, 4000);
    pollStatus();
})();
</script>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
