<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/fragments/taglibs.jspf" %>

<!DOCTYPE html>
<html lang="pt-BR">

<%@ include file="/WEB-INF/jsp/fragments/head.jspf" %>

<body
    data-page="morador-reservas"
    data-context-path="${ctx}"
>

<div class="app-shell">

    <%@ include file="/WEB-INF/jsp/fragments/sidebar.jspf" %>

    <div class="app-main">

        <%@ include file="/WEB-INF/jsp/fragments/topbar.jspf" %>

        <main class="page-content">

            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <section class="two-column-grid">

                <article class="card">

                    <div class="section-header">
                        <div>
                            <p class="eyebrow">Reservas</p>
                            <h2>Nova reserva</h2>
                        </div>
                    </div>

                    <form
                        method="post"
                        action="${ctx}/morador/reservas"
                        class="stack-form"
                    >

                        <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

                        <label class="field">

                            <span>Area comum</span>

                            <select
                                name="areaComumId"
                                id="areaComumId"
                                required
                            >

                                <option value="">
                                    Selecione uma area
                                </option>

                                <c:forEach
                                    var="area"
                                    items="${areasAtivas}"
                                >
                                    <option value="${area.id}">
                                        ${area.nome}
                                    </option>
                                </c:forEach>

                            </select>

                        </label>


                        <button
                            type="button"
                            class="btn btn-secondary"
                            id="abrirCalendarioBtn"
                        >
                            Ver horarios disponiveis para esta area
                        </button>


                        <label class="field">

                            <span>Inicio</span>

                            <input
                                type="datetime-local"
                                name="dataInicio"
                                id="dataInicioReserva"
                                required
                            >

                        </label>


                        <label class="field">

                            <span>Fim</span>

                            <input
                                type="datetime-local"
                                name="dataFim"
                                id="dataFimReserva"
                                required
                            >

                        </label>


                        <button
                            type="submit"
                            class="btn btn-primary"
                        >
                            Solicitar reserva
                        </button>

                    </form>

                </article>


                <article class="card">

                    <div class="section-header">

                        <div>
                            <p class="eyebrow">Historico</p>
                            <h2>Minhas reservas</h2>
                        </div>

                        <button
                            type="button"
                            class="btn btn-secondary"
                            id="abrirMinhasReservasCalendarioBtn"
                        >
                            Ver calendario
                        </button>

                    </div>


                    <c:choose>

                        <c:when test="${empty reservas}">

                            <div class="empty-state">
                                <h3>Nenhuma reserva encontrada</h3>
                                <p>
                                    As reservas solicitadas por voce
                                    aparecerao aqui.
                                </p>
                            </div>

                        </c:when>


                        <c:otherwise>

                            <div class="table-wrap">

                                <table class="data-table">

                                    <thead>
                                        <tr>
                                            <th>Area</th>
                                            <th>Inicio</th>
                                            <th>Fim</th>
                                            <th>Status</th>
                                            <th></th>
                                        </tr>
                                    </thead>

                                    <tbody>

                                        <c:forEach
                                            var="reserva"
                                            items="${reservas}"
                                        >

                                            <tr>

                                                <td>
                                                    ${reserva.areaComumNome}
                                                </td>

                                                <td>
                                                    ${reserva.dataHoraInicioFormatada}
                                                </td>

                                                <td>
                                                    ${reserva.dataHoraFimFormatada}
                                                </td>

                                                <td>
                                                    <span class="status-pill neutral">
                                                        ${reserva.status}
                                                    </span>

                                                    <c:if test="${reserva.status == 'NEGADA' && not empty reserva.motivoNegativa}">
                                                        <p class="reserva-motivo">
                                                            <strong>Motivo:</strong>
                                                            ${reserva.motivoNegativa}
                                                        </p>
                                                    </c:if>

                                                    <c:if test="${reserva.status == 'CANCELADA' && not empty reserva.motivoCancelamento}">
                                                        <p class="reserva-motivo">
                                                            <strong>Motivo:</strong>
                                                            ${reserva.motivoCancelamento}
                                                        </p>
                                                    </c:if>
                                                </td>

                                                <td class="cell-actions">

                                                    <div class="reservation-actions">

                                                        <c:if test="${reserva.status == 'SOLICITADA' || reserva.status == 'APROVADA'}">

                                                            <form
                                                                method="post"
                                                                action="${ctx}/morador/reservas/${reserva.id}/cancelar"
                                                                onsubmit="return confirm('Deseja cancelar esta reserva?');"
                                                            >
                                                                <%@ include file="/WEB-INF/jsp/fragments/csrf.jspf" %>

                                                                <input
                                                                    type="hidden"
                                                                    name="_method"
                                                                    value="patch"
                                                                >

                                                                <button
                                                                    type="submit"
                                                                    class="btn btn-secondary"
                                                                >
                                                                    Cancelar
                                                                </button>

                                                            </form>

                                                        </c:if>

                                                        <c:if test="${reserva.status == 'NEGADA' || reserva.status == 'CANCELADA'}">
                                                            <span>-</span>
                                                        </c:if>

                                                    </div>

                                                </td>

                                            </tr>

                                        </c:forEach>

                                    </tbody>

                                </table>

                            </div>

                        </c:otherwise>

                    </c:choose>

                </article>

            </section>

            <%@ include file="/WEB-INF/jsp/fragments/calendario.jspf" %>

            <%@ include file="/WEB-INF/jsp/fragments/calendario-minhas-reservas.jspf" %>


        </main>

    </div>

</div>

<%@ include file="/WEB-INF/jsp/fragments/scripts.jspf" %>

<script src="${ctx}/js/reservas.js"></script>


</body>
</html>