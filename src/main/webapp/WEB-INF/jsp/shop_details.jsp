<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% String pageTitle = "Join the Queue"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container-narrow">
    <a href="${pageContext.request.contextPath}/shops" class="text-muted" style="font-size: 0.88rem;">&larr; All shops</a>

    <h1 style="font-size: 1.6rem; margin-top: 10px;"><c:out value="${shop.shopName}"/></h1>
    <p class="text-muted"><c:out value="${shop.category}"/></p>

    <c:if test="${param.error == 'name'}">
        <div class="alert alert-error">Please enter your name to join the queue.</div>
    </c:if>

    <div class="stat-row">
        <div class="stat-box">
            <div class="stat-value"><c:out value="${waitingCount}"/></div>
            <div class="stat-label">people currently waiting</div>
        </div>
        <div class="stat-box">
            <div class="stat-value">~<c:out value="${shop.avgServiceTimeMins}"/>m</div>
            <div class="stat-label">average time per customer</div>
        </div>
    </div>

    <c:choose>
        <c:when test="${shop.open}">
            <div class="card">
                <h3>Join the queue</h3>
                <form action="${pageContext.request.contextPath}/join" method="post">
                    <input type="hidden" name="shopId" value="${shop.shopId}">
                    <div class="field">
                        <label for="customerName">Your name</label>
                        <input type="text" id="customerName" name="customerName" required autofocus>
                    </div>
                    <div class="field">
                        <label for="customerPhone">Phone number (optional)</label>
                        <input type="tel" id="customerPhone" name="customerPhone" placeholder="For your own reference">
                    </div>
                    <button type="submit" class="btn btn-amber btn-block">Get my token</button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <div class="card empty-state">This shop isn't accepting new tokens right now.</div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
