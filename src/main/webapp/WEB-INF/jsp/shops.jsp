<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% String pageTitle = "Find a Queue"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container">
    <h1 style="font-size: 1.6rem;">Find a queue to join</h1>
    <p class="text-muted">Pick a shop below to see the current wait and grab a token.</p>

    <c:if test="${param.error == 'closed'}">
        <div class="alert alert-error">That shop isn't accepting new tokens right now.</div>
    </c:if>
    <c:if test="${param.error == 'notfound'}">
        <div class="alert alert-error">We couldn't find that token. It may have expired.</div>
    </c:if>

    <c:choose>
        <c:when test="${empty shops}">
            <div class="card empty-state">
                No shops are open right now. Check back later, or
                <a href="${pageContext.request.contextPath}/register.jsp">register your own shop</a>.
            </div>
        </c:when>
        <c:otherwise>
            <div class="card" style="padding: 8px 24px;">
                <c:forEach var="shop" items="${shops}" varStatus="loop">
                    <div class="shop-card" style="${loop.last ? '' : 'border-bottom: 1px solid var(--border);'}">
                        <div>
                            <h3 class="mb-0"><c:out value="${shop.shopName}"/></h3>
                            <div class="category"><c:out value="${shop.category}"/></div>
                        </div>
                        <div style="text-align: right;">
                            <div class="meta">~<c:out value="${shop.avgServiceTimeMins}"/> min / customer</div>
                            <a class="btn btn-primary" style="margin-top: 8px;"
                               href="${pageContext.request.contextPath}/shop?id=${shop.shopId}">View &amp; Join</a>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
