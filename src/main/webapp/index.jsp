<% String pageTitle = "Skip the Waiting Room"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>

<div class="container-narrow text-center" style="padding-top: 20px;">
    <div class="pill pill-called mb-0" style="margin-bottom: 16px;">No more standing in line</div>
    <h1 style="font-size: 2.1rem;">Join the queue from your phone.<br>Walk in right when it's your turn.</h1>
    <p class="text-muted" style="margin-top: 14px;">
        Grab a virtual token at any registered clinic, salon, or repair shop,
        see your live position and estimated wait, and get called automatically -
        no more standing around a counter.
    </p>

    <div class="btn-row" style="justify-content: center; margin-top: 26px;">
        <a href="${pageContext.request.contextPath}/shops" class="btn btn-amber">Find a Queue to Join</a>
        <a href="${pageContext.request.contextPath}/login.jsp" class="btn btn-outline">I run a shop</a>
    </div>
</div>

<div class="container mt-24" style="max-width: 760px;">
    <div class="stat-row">
        <div class="stat-box">
            <div class="stat-value">01</div>
            <div class="stat-label">Pick a shop and join its queue remotely</div>
        </div>
        <div class="stat-box">
            <div class="stat-value">02</div>
            <div class="stat-label">See your token number and estimated wait update live</div>
        </div>
        <div class="stat-box">
            <div class="stat-value">03</div>
            <div class="stat-label">Walk in when you're called - miss it and you're auto-skipped</div>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
