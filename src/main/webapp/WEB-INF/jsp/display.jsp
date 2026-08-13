<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Now Serving &middot; <c:out value="${shop.shopName}"/></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600&family=Roboto+Mono:wght@500;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<div class="board">
    <div class="board-shop-name"><c:out value="${shop.shopName}"/></div>

    <div id="closedNotice" class="hidden">
        <div class="board-label">Not accepting tokens right now</div>
    </div>

    <div id="servingBlock">
        <div class="board-label">Now Serving</div>
        <div class="board-number" id="currentNumber">&mdash;</div>
    </div>

    <div class="board-waiting" id="waitingLine">Loading queue&hellip;</div>
</div>

<script>
(function () {
    var contextPath = "${pageContext.request.contextPath}";
    var shopId = <c:out value="${shop.shopId}"/>;
    var lastNumber = null;

    function render(data) {
        var servingBlock = document.getElementById('servingBlock');
        var closedNotice = document.getElementById('closedNotice');
        var numberEl = document.getElementById('currentNumber');
        var waitingEl = document.getElementById('waitingLine');

        closedNotice.classList.toggle('hidden', data.open);
        servingBlock.classList.toggle('hidden', !data.open);

        if (data.open) {
            var text = data.currentTokenNumber ? ('#' + data.currentTokenNumber) : '\u2014';
            if (data.currentTokenNumber !== lastNumber) {
                numberEl.style.opacity = '0';
                setTimeout(function () {
                    numberEl.textContent = text;
                    numberEl.style.opacity = '1';
                }, 150);
                lastNumber = data.currentTokenNumber;
            }

            var waiting = data.waitingTokenNumbers || [];
            if (waiting.length === 0) {
                waitingEl.textContent = 'No one else waiting';
            } else {
                waitingEl.innerHTML = 'Up next: <strong>' +
                    waiting.slice(0, 6).map(function (n) { return '#' + n; }).join(', ') +
                    '</strong>' + (waiting.length > 6 ? ' + ' + (waiting.length - 6) + ' more' : '');
            }
        }
    }

    function poll() {
        fetch(contextPath + '/api/queueStatus?shopId=' + shopId)
            .then(function (res) { return res.ok ? res.json() : null; })
            .then(function (data) { if (data && !data.error) render(data); })
            .catch(function (err) { console.error('Display poll failed', err); });
    }

    poll();
    setInterval(poll, 4000);
})();
</script>

</body>
</html>
