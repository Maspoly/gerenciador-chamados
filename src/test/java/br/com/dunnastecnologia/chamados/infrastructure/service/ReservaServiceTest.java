package br.com.dunnastecnologia.chamados.infrastructure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.HistoricoReserva;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.HistoricoReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AreaComumRepository areaComumRepository;

    @Mock
    private MoradorRepository moradorRepository;

    @Mock
    private HistoricoReservaRepository historicoReservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthenticatedUserValidator authenticatedUserValidator;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void solicitarReservaDeveCriarReservaComoSolicitadaERegistrarHistorico() {
        UUID moradorId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");
        Morador morador = new Morador();
        morador.setId(moradorId);
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.TRUE);

        LocalDateTime inicio = agora().plusDays(2);
        LocalDateTime fim = inicio.plusHours(2);

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(moradorRepository.findByIdAndAtivoTrue(moradorId)).thenReturn(Optional.of(morador));
        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoReservaRepository.save(any(HistoricoReserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = reservaService.solicitarReserva(moradorAutenticado, areaId, inicio, fim);

        assertNotNull(reserva);
        assertEquals(StatusReserva.SOLICITADA, reserva.getStatus());
        assertEquals(morador, reserva.getMorador());
        assertEquals(area, reserva.getAreaComum());
        verify(historicoReservaRepository).save(argThat(historico ->
                historico.getStatusAnterior() == null
                        && historico.getStatusNovo() == StatusReserva.SOLICITADA
                        && historico.getUsuario() == morador
                        && historico.getMotivo() != null
        ));
    }

    @Test
    void solicitarReservaDeveRejeitarQuandoAreaEstiverInativa() {
        UUID moradorId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");
        Morador morador = new Morador();
        morador.setId(moradorId);
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.FALSE);

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(moradorRepository.findByIdAndAtivoTrue(moradorId)).thenReturn(Optional.of(morador));
        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));

        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        moradorAutenticado,
                        areaId,
                        agora().plusDays(2),
                        agora().plusDays(2).plusHours(2)
                )
        );
    }

    @Test
    void solicitarReservaDeveRejeitarQuandoHorarioFinalNaoForPosteriorAoInicial() {
        UUID moradorId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        moradorAutenticado,
                        areaId,
                        agora().plusDays(1),
                        agora().plusDays(1)
                )
        );
    }

        @Test
        void solicitarReservaDeveRejeitarQuandoHorarioInicialJaOcorreu() {
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(
            UUID.randomUUID(),
            "morador@condominio.local",
            "ROLE_MORADOR"
        );

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);

        assertThrows(
            IllegalArgumentException.class,
            () -> reservaService.solicitarReserva(
                moradorAutenticado,
                UUID.randomUUID(),
                agora().minusMinutes(1),
                agora().plusHours(1)
            )
        );
        }

        @Test
        void solicitarReservaDeveRejeitarQuandoDatasNaoForemInformadas() {
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(
            UUID.randomUUID(),
            "morador@condominio.local",
            "ROLE_MORADOR"
        );

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);

        assertThrows(
            IllegalArgumentException.class,
            () -> reservaService.solicitarReserva(
                moradorAutenticado,
                UUID.randomUUID(),
                null,
                agora().plusHours(1)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> reservaService.solicitarReserva(
                moradorAutenticado,
                UUID.randomUUID(),
                agora().plusHours(1),
                null
            )
        );
        }

    @Test
    void aprovarReservaDeveAprovarSolicitacaoValidaERegistrarHistorico() {
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        Administrador usuarioAdmin = new Administrador();
        usuarioAdmin.setId(adminId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setDataHoraInicio(agora().plusDays(2));
        reserva.setDataHoraFim(agora().plusDays(2).plusHours(3));
        AreaComum area = new AreaComum();
        area.setId(areaId);
        reserva.setAreaComum(area);

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(areaComumRepository.buscarPorIdComLock(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.existeConflitoAprovado(areaId, reserva.getDataHoraInicio(), reserva.getDataHoraFim(), StatusReserva.APROVADA)).thenReturn(false);
        when(usuarioRepository.findByIdAndAtivoTrue(adminId)).thenReturn(Optional.of(usuarioAdmin));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoReservaRepository.save(any(HistoricoReserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva aprovada = reservaService.aprovarReserva(reservaId, administrador);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
        verify(historicoReservaRepository).save(argThat(historico ->
                historico.getStatusAnterior() == StatusReserva.SOLICITADA
                        && historico.getStatusNovo() == StatusReserva.APROVADA
                        && historico.getUsuario() == usuarioAdmin
        ));
    }

    @Test
    void aprovarReservaDeveRejeitarQuandoExisteConflitoAprovado() {
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setDataHoraInicio(agora().plusDays(2));
        reserva.setDataHoraFim(agora().plusDays(2).plusHours(2));
        AreaComum area = new AreaComum();
        area.setId(areaId);
        reserva.setAreaComum(area);

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(areaComumRepository.buscarPorIdComLock(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.existeConflitoAprovado(areaId, reserva.getDataHoraInicio(), reserva.getDataHoraFim(), StatusReserva.APROVADA)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> reservaService.aprovarReserva(reservaId, administrador));
    }

    @Test
    void aprovarReservaDeveRejeitarQuandoHorarioInicialJaOcorreuOuEstaEmAndamento() {
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setDataHoraInicio(agora().minusHours(1));
        reserva.setDataHoraFim(agora().plusHours(2));
        AreaComum area = new AreaComum();
        area.setId(areaId);
        reserva.setAreaComum(area);

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(areaComumRepository.buscarPorIdComLock(areaId)).thenReturn(Optional.of(area));

        assertThrows(IllegalStateException.class, () -> reservaService.aprovarReserva(reservaId, administrador));
    }

    @Test
    void negarReservaDeveRegistrarMotivoEHistorico() {
        UUID reservaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        Administrador usuarioAdmin = new Administrador();
        usuarioAdmin.setId(adminId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setDataHoraInicio(agora().plusDays(3));
        reserva.setDataHoraFim(agora().plusDays(3).plusHours(2));

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(usuarioRepository.findByIdAndAtivoTrue(adminId)).thenReturn(Optional.of(usuarioAdmin));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoReservaRepository.save(any(HistoricoReserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva negada = reservaService.negarReserva(reservaId, "Não houve disponibilidade no período.", administrador);

        assertEquals(StatusReserva.NEGADA, negada.getStatus());
        assertEquals("Não houve disponibilidade no período.", negada.getMotivoNegativa());
        verify(historicoReservaRepository).save(argThat(historico ->
                historico.getStatusAnterior() == StatusReserva.SOLICITADA
                        && historico.getStatusNovo() == StatusReserva.NEGADA
                        && historico.getMotivo().equals("Não houve disponibilidade no período.")
        ));
    }

    @Test
    void negarReservaDeveRejeitarMotivoVazio() {
        UUID reservaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.negarReserva(reservaId, "  ", administrador)
        );
    }

    @Test
    void cancelarReservaDevePermitirAdministradorCancelarSolicitacaoFutura() {
        UUID reservaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        Administrador usuarioAdmin = new Administrador();
        usuarioAdmin.setId(adminId);

        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.SOLICITADA);
        reserva.setDataHoraInicio(agora().plusDays(4));
        reserva.setDataHoraFim(agora().plusDays(4).plusHours(2));

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(authenticatedUserValidator.isAdministrador(administrador)).thenReturn(Boolean.TRUE);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(usuarioRepository.findByIdAndAtivoTrue(adminId)).thenReturn(Optional.of(usuarioAdmin));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoReservaRepository.save(any(HistoricoReserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cancelada = reservaService.cancelarReserva(reservaId, "Cancelamento solicitado.", administrador);

        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());
        verify(historicoReservaRepository).save(argThat(historico ->
                historico.getStatusAnterior() == StatusReserva.SOLICITADA
                        && historico.getStatusNovo() == StatusReserva.CANCELADA
        ));
    }

        @Test
        void cancelarReservaDevePermitirMoradorProprietarioCancelarReservaAprovada() {
        UUID reservaId = UUID.randomUUID();
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(
            moradorId,
            "morador@condominio.local",
            "ROLE_MORADOR"
        );
        Morador morador = new Morador();
        morador.setId(moradorId);
        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setDataHoraInicio(agora().plusDays(2));
        reserva.setDataHoraFim(agora().plusDays(2).plusHours(2));
        reserva.setMorador(morador);

        when(authenticatedUserValidator.isAdministrador(moradorAutenticado)).thenReturn(false);
        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(reservaRepository.findByIdAndMoradorId(reservaId, moradorId)).thenReturn(Optional.of(reserva));
        when(usuarioRepository.findByIdAndAtivoTrue(moradorId)).thenReturn(Optional.of(morador));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historicoReservaRepository.save(any(HistoricoReserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cancelada = reservaService.cancelarReserva(
            reservaId,
            "Cancelamento solicitado pelo morador.",
            moradorAutenticado
        );

        assertEquals(StatusReserva.CANCELADA, cancelada.getStatus());
        verify(reservaRepository).save(reserva);
        verify(historicoReservaRepository).save(argThat(historico ->
            historico.getStatusAnterior() == StatusReserva.APROVADA
                && historico.getStatusNovo() == StatusReserva.CANCELADA
                && historico.getUsuario() == morador
        ));
        }

        @Test
        void cancelarReservaDeveRejeitarQuandoEstadoJaForCancelada() {
        UUID reservaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(
            adminId,
            "admin@condominio.local",
            "ROLE_ADMINISTRADOR"
        );
        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setDataHoraInicio(agora().plusDays(2));
        reserva.setDataHoraFim(agora().plusDays(2).plusHours(2));

        when(authenticatedUserValidator.isAdministrador(administrador)).thenReturn(true);
        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));

        assertThrows(
            IllegalStateException.class,
            () -> reservaService.cancelarReserva(reservaId, "Cancelamento", administrador)
        );
        assertEquals(StatusReserva.CANCELADA, reserva.getStatus());
        org.mockito.Mockito.verify(reservaRepository, org.mockito.Mockito.never()).save(any(Reserva.class));
        }

    @Test
    void cancelarReservaDeveRejeitarQuandoMoradorNaoEhProprietario() {
        UUID reservaId = UUID.randomUUID();
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");

        when(authenticatedUserValidator.isAdministrador(moradorAutenticado)).thenReturn(false);
        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(reservaRepository.findByIdAndMoradorId(reservaId, moradorId)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> reservaService.cancelarReserva(reservaId, "Cancelamento", moradorAutenticado));
    }

    @Test
    void cancelarReservaDeveRejeitarQuandoHorarioJaComecou() {
        UUID reservaId = UUID.randomUUID();
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");
        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setDataHoraInicio(agora().minusHours(1));
        reserva.setDataHoraFim(agora().plusHours(2));
        reserva.setMorador(new Morador());
        reserva.getMorador().setId(moradorId);

        when(authenticatedUserValidator.isAdministrador(moradorAutenticado)).thenReturn(false);
        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(reservaRepository.findByIdAndMoradorId(reservaId, moradorId)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> reservaService.cancelarReserva(reservaId, "Cancelamento", moradorAutenticado));
    }

    @Test
    void listarMinhasReservasDeveRetornarSomenteReservasDoMorador() {
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(reservaRepository.findByMoradorIdOrderByDataCriacaoDesc(moradorId)).thenReturn(List.of(reserva));

        List<Reserva> reservas = reservaService.listarMinhasReservas(moradorAutenticado);

        assertEquals(1, reservas.size());
        assertEquals(List.of(reserva), reservas);
    }

    @Test
    void listarDisponibilidadePorAreaEPeriodoDeveConsiderarSolicitadasEAprovadas() {
        UUID moradorId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(moradorId, "morador@condominio.local", "ROLE_MORADOR");
        AreaComum area = new AreaComum();
        area.setId(areaId);
        area.setAtiva(Boolean.TRUE);
        LocalDateTime inicio = agora().plusDays(1);
        LocalDateTime fim = inicio.plusHours(3);

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(areaComumRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(reservaRepository.buscarPorAreaEPeriodoEStatus(areaId, inicio, fim, List.of(StatusReserva.SOLICITADA, StatusReserva.APROVADA))).thenReturn(List.of(new Reserva(), new Reserva()));

        List<Reserva> disponibilidade = reservaService.listarDisponibilidadePorAreaEPeriodo(moradorAutenticado, areaId, inicio, fim);

        assertEquals(2, disponibilidade.size());
    }

        @Test
        void buscarMotivoCancelamentoDeveRejeitarReservaDeOutroMorador() {
        UUID reservaId = UUID.randomUUID();
        UUID moradorId = UUID.randomUUID();
        AuthenticatedUser moradorAutenticado = new AuthenticatedUser(
            moradorId,
            "morador@condominio.local",
            "ROLE_MORADOR"
        );

        doNothing().when(authenticatedUserValidator).assertMorador(moradorAutenticado);
        when(reservaRepository.findByIdAndMoradorId(reservaId, moradorId)).thenReturn(Optional.empty());

        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> reservaService.buscarMotivoCancelamento(moradorAutenticado, reservaId)
        );

        assertEquals("Operacao nao permitida.", exception.getMessage());
        verify(historicoReservaRepository, org.mockito.Mockito.never())
            .findTopByReservaIdAndStatusNovoOrderByDataAlteracaoDesc(reservaId, StatusReserva.CANCELADA);
        }

    @Test
    void aprovarReservaDeveRejeitarQuandoStatusNaoEhSolicitada() {
        UUID reservaId = UUID.randomUUID();
        UUID areaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AuthenticatedUser administrador = new AuthenticatedUser(adminId, "admin@condominio.local", "ROLE_ADMINISTRADOR");
        Reserva reserva = new Reserva();
        reserva.setId(reservaId);
        reserva.setStatus(StatusReserva.NEGADA);
        reserva.setDataHoraInicio(agora().plusDays(2));
        reserva.setDataHoraFim(agora().plusDays(2).plusHours(2));
        AreaComum area = new AreaComum();
        area.setId(areaId);
        reserva.setAreaComum(area);

        doNothing().when(authenticatedUserValidator).assertAdministrador(administrador);
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(areaComumRepository.buscarPorIdComLock(areaId)).thenReturn(Optional.of(area));

        assertThrows(IllegalStateException.class, () -> reservaService.aprovarReserva(reservaId, administrador));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now();
    }
}
