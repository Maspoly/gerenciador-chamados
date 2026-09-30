document.addEventListener("DOMContentLoaded", () => {
    const calendarioTitulo = document.getElementById("calendarioTitulo");

    const abrirCalendarioBtn = document.getElementById("abrirCalendarioBtn");

    const fecharCalendarioBtn = document.getElementById("fecharCalendarioBtn");

    const calendarioModal = document.getElementById("calendarioModal");

    const areaComumSelect = document.getElementById("areaComumId");

    const mesAnteriorBtn = document.getElementById("mesAnteriorBtn");

    const proximoMesBtn = document.getElementById("proximoMesBtn");

    const mesAnoCalendario = document.getElementById("mesAnoCalendario");

    const diasCalendario = document.getElementById("diasCalendario");

    const dataSelecionadaTitulo = document.getElementById("dataSelecionadaTitulo");

    const horariosDiaConteudo = document.getElementById("horariosDiaConteudo");

    const ctx = document.body.dataset.contextPath || "";

    let dataAtualCalendario = new Date();

    let reservasDisponibilidade = [];

    // =========================
    // ABRIR CALENDARIO
    // =========================

    abrirCalendarioBtn.addEventListener("click", async () => {
        if (!areaComumSelect.value) {
            alert("Selecione uma area comum primeiro.");

            return;
        }

        const nomeArea = areaComumSelect.options[areaComumSelect.selectedIndex].text;

        calendarioTitulo.textContent = "Horarios - " + nomeArea;

        dataAtualCalendario = new Date();

        await carregarDisponibilidade();

        renderizarCalendario();

        calendarioModal.showModal();
    });

    // =========================
    // FECHAR
    // =========================

    fecharCalendarioBtn.addEventListener("click", () => {
        calendarioModal.close();
    });

    // =========================
    // MES ANTERIOR
    // =========================

    mesAnteriorBtn.addEventListener("click", async () => {
        dataAtualCalendario.setMonth(dataAtualCalendario.getMonth() - 1);

        await carregarDisponibilidade();

        renderizarCalendario();
    });

    // =========================
    // PROXIMO MES
    // =========================

    proximoMesBtn.addEventListener("click", async () => {
        dataAtualCalendario.setMonth(dataAtualCalendario.getMonth() + 1);

        await carregarDisponibilidade();

        renderizarCalendario();
    });

    // =========================
    // RENDERIZAR CALENDARIO
    // =========================

    function renderizarCalendario() {
        const ano = dataAtualCalendario.getFullYear();

        const mes = dataAtualCalendario.getMonth();

        const primeiroDiaSemana = new Date(ano, mes, 1).getDay();

        const totalDias = new Date(ano, mes + 1, 0).getDate();

        mesAnoCalendario.textContent = dataAtualCalendario.toLocaleDateString("pt-BR", {
            month: "long",
            year: "numeric",
        });

        diasCalendario.innerHTML = "";

        // =========================
        // ESPACOS ANTES DO DIA 1
        // =========================

        for (let i = 0; i < primeiroDiaSemana; i++) {
            const vazio = document.createElement("div");

            vazio.classList.add("reserva-calendar-day", "empty");

            diasCalendario.appendChild(vazio);
        }

        // =========================
        // DIAS DO MES
        // =========================

        for (let dia = 1; dia <= totalDias; dia++) {
            const botaoDia = document.createElement("button");

            botaoDia.type = "button";

            botaoDia.textContent = dia;

            botaoDia.classList.add("reserva-calendar-day");

            const reservasDoDia = buscarReservasDoDia(ano, mes, dia);

            const possuiAprovada = reservasDoDia.some(reserva => reserva.status === "APROVADA");

            const possuiSolicitada = reservasDoDia.some(reserva => reserva.status === "SOLICITADA");

            if (possuiAprovada) {
                botaoDia.classList.add("has-reservation");
            }

            if (possuiSolicitada) {
                botaoDia.classList.add("has-request");
            }

            botaoDia.addEventListener("click", () => {
                mostrarHorariosDoDia(ano, mes, dia);
            });

            diasCalendario.appendChild(botaoDia);
        }
    }

    // =========================
    // FORMATAR DATA PARA SPRING
    // =========================

    function formatarDataParaBackend(data) {
        const ano = data.getFullYear();

        const mes = String(data.getMonth() + 1).padStart(2, "0");

        const dia = String(data.getDate()).padStart(2, "0");

        return `${ano}-${mes}-${dia}T00:00:00`;
    }

    // =========================
    // BUSCAR DISPONIBILIDADE
    // =========================

    async function carregarDisponibilidade() {
        const areaComumId = areaComumSelect.value;

        const ano = dataAtualCalendario.getFullYear();

        const mes = dataAtualCalendario.getMonth();

        const inicio = formatarDataParaBackend(new Date(ano, mes, 1));

        const fim = formatarDataParaBackend(new Date(ano, mes + 1, 1));

        const url =
            `${ctx}/morador/reservas/disponibilidade` +
            `?areaComumId=${encodeURIComponent(areaComumId)}` +
            `&inicio=${encodeURIComponent(inicio)}` +
            `&fim=${encodeURIComponent(fim)}`;

        const resposta = await fetch(url);

        if (!resposta.ok) {
            throw new Error("Nao foi possivel carregar a disponibilidade.");
        }

        reservasDisponibilidade = await resposta.json();
    }

    // =========================
    // VERIFICAR SOBREPOSICAO
    // COM UM DIA
    // =========================

    function reservaSobrepoeDia(reserva, ano, mes, dia) {
        const inicioDia = new Date(ano, mes, dia, 0, 0, 0, 0);

        const fimDia = new Date(ano, mes, dia + 1, 0, 0, 0, 0);

        const inicioReserva = new Date(reserva.dataHoraInicio);

        const fimReserva = new Date(reserva.dataHoraFim);

        return inicioReserva < fimDia && fimReserva > inicioDia;
    }

    // =========================
    // RESERVAS DE UM DIA
    // =========================

    function buscarReservasDoDia(ano, mes, dia) {
        return reservasDisponibilidade.filter(reserva => reservaSobrepoeDia(reserva, ano, mes, dia));
    }

    // =========================
    // MOSTRAR HORARIOS DO DIA
    // =========================

    function mostrarHorariosDoDia(ano, mes, dia) {
        const reservasDoDia = buscarReservasDoDia(ano, mes, dia);

        dataSelecionadaTitulo.textContent = `Disponibilidade - ${dia}/${mes + 1}/${ano}`;

        if (reservasDoDia.length === 0) {
            horariosDiaConteudo.innerHTML = "<p>Nenhum horario ocupado ou pendente neste dia.</p>";

            return;
        }

        horariosDiaConteudo.innerHTML = reservasDoDia
            .map(reserva => {
                const inicio = new Date(reserva.dataHoraInicio);

                const fim = new Date(reserva.dataHoraFim);

                const inicioFormatado = inicio.toLocaleString("pt-BR", {
                    day: "2-digit",
                    month: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                });

                const fimFormatado = fim.toLocaleString("pt-BR", {
                    day: "2-digit",
                    month: "2-digit",
                    hour: "2-digit",
                    minute: "2-digit",
                });

                const statusTexto =
                    reserva.status === "APROVADA" ? "Ocupado" : "Solicitacao pendente - horario ainda disponivel";

                return `
                            <div class="reserva-calendar-item">

                                <strong>
                                    ${inicioFormatado}
                                    -
                                    ${fimFormatado}
                                </strong>

                                <div>
                                    ${statusTexto}
                                </div>

                            </div>
                        `;
            })
            .join("");
    }
});
document.addEventListener("DOMContentLoaded", () => {
    const abrirBtn = document.getElementById("abrirMinhasReservasCalendarioBtn");

    const modal = document.getElementById("minhasReservasCalendarioModal");

    const fecharBtn = document.getElementById("fecharMinhasReservasCalendarioBtn");

    const mesAnteriorBtn = document.getElementById("mesAnteriorMinhasReservasBtn");

    const proximoMesBtn = document.getElementById("proximoMesMinhasReservasBtn");

    const mesAnoTitulo = document.getElementById("mesAnoMinhasReservasCalendario");

    const diasContainer = document.getElementById("diasMinhasReservasCalendario");

    const dataTitulo = document.getElementById("dataMinhasReservasTitulo");

    const conteudoDia = document.getElementById("minhasReservasDiaConteudo");

    if (!abrirBtn || !modal || !fecharBtn || !diasContainer) {
        return;
    }

    const ctx = document.body.dataset.contextPath || "";

    let mesAtual = new Date();

    let minhasReservas = [];

    // =========================
    // ABRIR
    // =========================

    abrirBtn.addEventListener("click", async () => {
        mesAtual = new Date();

        await carregarMinhasReservas();

        renderizarCalendarioMinhasReservas();

        modal.showModal();
    });

    // =========================
    // FECHAR
    // =========================

    fecharBtn.addEventListener("click", () => {
        modal.close();
    });

    // =========================
    // MES ANTERIOR
    // =========================

    mesAnteriorBtn.addEventListener("click", () => {
        mesAtual.setMonth(mesAtual.getMonth() - 1);

        renderizarCalendarioMinhasReservas();
    });

    // =========================
    // PROXIMO MES
    // =========================

    proximoMesBtn.addEventListener("click", () => {
        mesAtual.setMonth(mesAtual.getMonth() + 1);

        renderizarCalendarioMinhasReservas();
    });

    // =========================
    // BUSCAR RESERVAS
    // =========================

    async function carregarMinhasReservas() {
        const resposta = await fetch(`${ctx}/morador/reservas/minhas/calendario`);

        if (!resposta.ok) {
            throw new Error("Nao foi possivel carregar suas reservas.");
        }

        minhasReservas = await resposta.json();
    }

    // =========================
    // RENDERIZAR CALENDARIO
    // =========================

    function renderizarCalendarioMinhasReservas() {
        const ano = mesAtual.getFullYear();

        const mes = mesAtual.getMonth();

        const primeiroDiaSemana = new Date(ano, mes, 1).getDay();

        const totalDias = new Date(ano, mes + 1, 0).getDate();

        mesAnoTitulo.textContent = mesAtual.toLocaleDateString("pt-BR", {
            month: "long",
            year: "numeric",
        });

        diasContainer.innerHTML = "";

        // Espacos antes do primeiro dia

        for (let i = 0; i < primeiroDiaSemana; i++) {
            const vazio = document.createElement("div");

            vazio.classList.add("reserva-calendar-day", "empty");

            diasContainer.appendChild(vazio);
        }

        // Dias do mes

        for (let dia = 1; dia <= totalDias; dia++) {
            const botao = document.createElement("button");

            botao.type = "button";
            botao.textContent = dia;

            botao.classList.add("reserva-calendar-day");

            const reservasDoDia = buscarMinhasReservasDoDia(ano, mes, dia);

            if (reservasDoDia.length > 0) {
                botao.classList.add("has-own-reservation");

                botao.title =
                    reservasDoDia.length === 1 ? "1 reserva neste dia" : `${reservasDoDia.length} reservas neste dia`;
            }

            botao.addEventListener("click", () => {
                mostrarMinhasReservasDoDia(ano, mes, dia);
            });

            diasContainer.appendChild(botao);
        }

        dataTitulo.textContent = "Selecione um dia";

        conteudoDia.innerHTML = "<p>Clique em um dia para visualizar suas reservas.</p>";
    }

    // =========================
    // SOBREPOSICAO COM O DIA
    // =========================

    function reservaSobrepoeDia(reserva, ano, mes, dia) {
        const inicioDia = new Date(ano, mes, dia, 0, 0, 0, 0);

        const fimDia = new Date(ano, mes, dia + 1, 0, 0, 0, 0);

        const inicioReserva = new Date(reserva.dataHoraInicio);

        const fimReserva = new Date(reserva.dataHoraFim);

        return inicioReserva < fimDia && fimReserva > inicioDia;
    }

    // =========================
    // RESERVAS DO DIA
    // =========================

    function buscarMinhasReservasDoDia(ano, mes, dia) {
        return minhasReservas
            .filter(reserva => reservaSobrepoeDia(reserva, ano, mes, dia))
            .sort((a, b) => new Date(a.dataHoraInicio) - new Date(b.dataHoraInicio));
    }

    // =========================
    // MOSTRAR RESERVAS DO DIA
    // =========================

    function mostrarMinhasReservasDoDia(ano, mes, dia) {
        const reservasDoDia = buscarMinhasReservasDoDia(ano, mes, dia);

        dataTitulo.textContent = `Minhas reservas - ${String(dia).padStart(2, "0")}/${String(mes + 1).padStart(2, "0")}/${ano}`;

        conteudoDia.innerHTML = "";

        if (reservasDoDia.length === 0) {
            const mensagem = document.createElement("p");

            mensagem.textContent = "Voce nao possui reservas neste dia.";

            conteudoDia.appendChild(mensagem);

            return;
        }

        reservasDoDia.forEach(reserva => {
            const item = document.createElement("div");

            item.classList.add("reserva-calendar-item");

            // AREA

            const area = document.createElement("strong");

            area.textContent = reserva.areaComumNome;

            // PERIODO

            const periodo = document.createElement("div");

            periodo.classList.add("reserva-calendar-period");

            const inicio = new Date(reserva.dataHoraInicio);

            const fim = new Date(reserva.dataHoraFim);

            periodo.textContent = `${formatarDataHora(inicio)} - ${formatarDataHora(fim)}`;

            // STATUS

            const status = document.createElement("span");

            status.classList.add("reserva-calendar-status", `status-${reserva.status.toLowerCase()}`);

            status.textContent = reserva.status;

            item.appendChild(area);
            item.appendChild(periodo);
            item.appendChild(status);

            conteudoDia.appendChild(item);
        });
    }

    // =========================
    // FORMATAR DATA/HORA
    // =========================

    function formatarDataHora(data) {
        return data.toLocaleString("pt-BR", {
            day: "2-digit",
            month: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    }
});
