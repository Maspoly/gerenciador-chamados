package br.com.dunnastecnologia.chamados.infrastructure.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.application.UserCase.ReservaUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.HistoricoReserva;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.domain.model.Usuario;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.HistoricoReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.support.AuthenticatedUserValidator;

@Service
@Transactional(readOnly = true)
public class ReservaService implements ReservaUseCases {

    private static final ZoneId ZONA_HORARIA_RESERVAS =
            ZoneId.of("America/Sao_Paulo");

    private final ReservaRepository reservaRepository;
    private final AreaComumRepository areaComumRepository;
    private final MoradorRepository moradorRepository;
    private final HistoricoReservaRepository historicoReservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticatedUserValidator authenticatedUserValidator;

    public ReservaService(
            ReservaRepository reservaRepository,
            AreaComumRepository areaComumRepository,
            MoradorRepository moradorRepository,
            HistoricoReservaRepository historicoReservaRepository,
            UsuarioRepository usuarioRepository,
            AuthenticatedUserValidator authenticatedUserValidator
    ) {
        this.reservaRepository = reservaRepository;
        this.areaComumRepository = areaComumRepository;
        this.moradorRepository = moradorRepository;
        this.historicoReservaRepository = historicoReservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.authenticatedUserValidator = authenticatedUserValidator;
    }

    @Override
    @Transactional
    public Reserva solicitarReserva(
            AuthenticatedUser morador,
            UUID areaComumId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    ) {
        authenticatedUserValidator.assertMorador(morador);

        if (dataInicio == null || dataFim == null) {
            throw new IllegalArgumentException(
                    "As datas de início e fim devem ser informadas."
            );
        }

        if (!dataFim.isAfter(dataInicio)) {
            throw new IllegalArgumentException(
                    "O horário final deve ser posterior ao horário inicial."
            );
        }

        if (!dataInicio.isAfter(agora())) {
            throw new IllegalArgumentException(
                    "O horário inicial da reserva deve ser em uma data/hora futura."
            );
        }

        Morador moradorEntity = moradorRepository
                .findByIdAndAtivoTrue(morador.id())
                .orElseThrow(() ->
                        new IllegalArgumentException("Morador não encontrado.")
                );

        AreaComum area = areaComumRepository
                .findById(areaComumId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Área comum não encontrada.")
                );

        if (!area.getAtiva()) {
            throw new IllegalArgumentException(
                    "Esta área comum está desativada e não aceita novas solicitações."
            );
        }

        Reserva reserva = new Reserva();
        reserva.setMorador(moradorEntity);
        reserva.setAreaComum(area);
        reserva.setDataHoraInicio(dataInicio);
        reserva.setDataHoraFim(dataFim);
        reserva.setStatus(StatusReserva.SOLICITADA);

        Reserva reservaSalva = reservaRepository.save(reserva);

        registrarHistorico(
                reservaSalva,
                null,
                StatusReserva.SOLICITADA,
                moradorEntity,
                "Solicitação de reserva criada."
        );

        return reservaSalva;
    }

    @Override
    @Transactional
    public Reserva aprovarReserva(
            UUID reservaId,
            AuthenticatedUser administrador
    ) {
        authenticatedUserValidator.assertAdministrador(administrador);

        Reserva reserva = buscarPorId(reservaId);
        UUID areaComumId = reserva.getAreaComum().getId();

        // Serializa as decisões de aprovação da mesma área.
        areaComumRepository
                .buscarPorIdComLock(areaComumId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Área comum não encontrada.")
                );

        StatusReserva statusAnterior = reserva.getStatus();

        if (statusAnterior != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Apenas solicitações com estado SOLICITADA podem ser aprovadas."
            );
        }

        if (!reserva.getDataHoraInicio().isAfter(agora())) {
            throw new IllegalStateException(
                    "Não é possível aprovar uma reserva cujo horário inicial já ocorreu ou está em andamento."
            );
        }

        boolean existeConflito = reservaRepository.existeConflitoAprovado(
                areaComumId,
                reserva.getDataHoraInicio(),
                reserva.getDataHoraFim(),
                StatusReserva.APROVADA
        );

        if (existeConflito) {
            throw new IllegalStateException(
                    "Não foi possível aprovar: existe outra reserva aprovada em conflito com este horário."
            );
        }

        reserva.setStatus(StatusReserva.APROVADA);
        Reserva reservaAtualizada = reservaRepository.save(reserva);

        registrarHistorico(
                reservaAtualizada,
                statusAnterior,
                StatusReserva.APROVADA,
                buscarUsuarioAutenticado(administrador),
                "Reserva aprovada pelo administrador."
        );

        return reservaAtualizada;
    }

    @Override
    @Transactional
    public Reserva negarReserva(
            UUID reservaId,
            String motivo,
            AuthenticatedUser administrador
    ) {
        authenticatedUserValidator.assertAdministrador(administrador);

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "É obrigatório informar o motivo para negar a reserva."
            );
        }

        Reserva reserva = buscarPorId(reservaId);
        StatusReserva statusAnterior = reserva.getStatus();

        if (statusAnterior != StatusReserva.SOLICITADA) {
            throw new IllegalStateException(
                    "Apenas solicitações com estado SOLICITADA podem ser negadas."
            );
        }

        reserva.setStatus(StatusReserva.NEGADA);
        reserva.setMotivoNegativa(motivo.trim());

        Reserva reservaAtualizada = reservaRepository.save(reserva);

        registrarHistorico(
                reservaAtualizada,
                statusAnterior,
                StatusReserva.NEGADA,
                buscarUsuarioAutenticado(administrador),
                motivo.trim()
        );

        return reservaAtualizada;
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(
            UUID reservaId,
            String motivo,
            AuthenticatedUser usuarioLogado
    ) {
        if (authenticatedUserValidator.isAdministrador(usuarioLogado)) {
            authenticatedUserValidator.assertAdministrador(usuarioLogado);
        } else {
            authenticatedUserValidator.assertMorador(usuarioLogado);
        }

        boolean isAdmin =
                authenticatedUserValidator.isAdministrador(usuarioLogado);

        Reserva reserva;

        if (isAdmin) {
            reserva = buscarPorId(reservaId);
        } else {
            reserva = reservaRepository
                    .findByIdAndMoradorId(reservaId, usuarioLogado.id())
                    .orElseThrow(() ->
                            new SecurityException("Operacao nao permitida.")
                    );
        }

        StatusReserva statusAnterior = reserva.getStatus();

        if (statusAnterior != StatusReserva.SOLICITADA
                && statusAnterior != StatusReserva.APROVADA) {
            throw new IllegalStateException(
                    "Apenas reservas SOLICITADAS ou APROVADAS podem ser canceladas."
            );
        }

        if (!reserva.getDataHoraInicio().isAfter(agora())) {
            throw new IllegalStateException(
                    "Nao e possivel cancelar uma reserva cujo horario inicial ja ocorreu ou esta em andamento."
            );
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        Reserva reservaAtualizada = reservaRepository.save(reserva);

        String motivoHistorico =
                motivo == null || motivo.trim().isEmpty()
                        ? "Reserva cancelada."
                        : motivo.trim();

        registrarHistorico(
                reservaAtualizada,
                statusAnterior,
                StatusReserva.CANCELADA,
                buscarUsuarioAutenticado(usuarioLogado),
                motivoHistorico
        );

        return reservaAtualizada;
    }

    @Override
    public List<Reserva> listarMinhasReservas(
            AuthenticatedUser morador
    ) {
        authenticatedUserValidator.assertMorador(morador);

        return reservaRepository
                .findByMoradorIdOrderByDataCriacaoDesc(morador.id());
    }

    @Override
    public List<Reserva> listarTodas(
            AuthenticatedUser administrador
    ) {
        authenticatedUserValidator.assertAdministrador(administrador);

        return reservaRepository.findAllByOrderByDataCriacaoDesc();
    }

    public Reserva buscarPorId(UUID id) {
        return reservaRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Reserva não encontrada com o ID: " + id
                        )
                );
    }

    @Override
    public List<Reserva> listarAprovadasPorAreaEPeriodo(
            AuthenticatedUser morador,
            UUID areaComumId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        authenticatedUserValidator.assertMorador(morador);

        return reservaRepository.buscarAprovadasPorAreaEPeriodo(
                areaComumId,
                inicio,
                fim,
                StatusReserva.APROVADA
        );
    }

    @Override
    public List<Reserva> listarPorStatus(
            AuthenticatedUser administrador,
            StatusReserva status
    ) {
        authenticatedUserValidator.assertAdministrador(administrador);

        return reservaRepository
                .findByStatusOrderByDataCriacaoDesc(status);
    }

    @Override
    public List<Reserva> listarCalendarioAdministrador(
            AuthenticatedUser administrador,
            UUID areaComumId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        authenticatedUserValidator.assertAdministrador(administrador);

        if (areaComumId == null) {
            throw new IllegalArgumentException(
                    "A área comum deve ser informada."
            );
        }

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException(
                    "O período da consulta deve ser informado."
            );
        }

        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "O fim do período deve ser posterior ao início."
            );
        }

        areaComumRepository
                .findById(areaComumId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Área comum não encontrada."
                        )
                );

        return reservaRepository.buscarPorAreaEPeriodoEStatus(
                areaComumId,
                inicio,
                fim,
                List.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA,
                        StatusReserva.NEGADA,
                        StatusReserva.CANCELADA
                )
        );
    }

    @Override
    public String buscarMotivoCancelamento(
            AuthenticatedUser morador,
            UUID reservaId
    ) {
        authenticatedUserValidator.assertMorador(morador);

        reservaRepository
                .findByIdAndMoradorId(reservaId, morador.id())
                .orElseThrow(() ->
                        new SecurityException("Operacao nao permitida.")
                );

        return historicoReservaRepository
                .findTopByReservaIdAndStatusNovoOrderByDataAlteracaoDesc(
                        reservaId,
                        StatusReserva.CANCELADA
                )
                .map(HistoricoReserva::getMotivo)
                .orElse(null);
    }

    @Override
    public List<Reserva> listarDisponibilidadePorAreaEPeriodo(
            AuthenticatedUser morador,
            UUID areaComumId,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        authenticatedUserValidator.assertMorador(morador);

        if (areaComumId == null) {
            throw new IllegalArgumentException(
                    "A área comum deve ser informada."
            );
        }

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException(
                    "O período da consulta deve ser informado."
            );
        }

        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "O fim do período deve ser posterior ao início."
            );
        }

        AreaComum area = areaComumRepository
                .findById(areaComumId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Área comum não encontrada."
                        )
                );

        if (!area.getAtiva()) {
            throw new IllegalArgumentException(
                    "Esta área comum está desativada."
            );
        }

        return reservaRepository.buscarPorAreaEPeriodoEStatus(
                areaComumId,
                inicio,
                fim,
                List.of(
                        StatusReserva.SOLICITADA,
                        StatusReserva.APROVADA
                )
        );
    }

    private void registrarHistorico(
            Reserva reserva,
            StatusReserva statusAnterior,
            StatusReserva statusNovo,
            Usuario usuario,
            String motivo
    ) {
        HistoricoReserva historico = new HistoricoReserva();
        historico.setReserva(reserva);
        historico.setUsuario(usuario);
        historico.setStatusAnterior(statusAnterior);
        historico.setStatusNovo(statusNovo);
        historico.setMotivo(motivo);
        historico.setDataAlteracao(agora());

        historicoReservaRepository.save(historico);
    }

    private Usuario buscarUsuarioAutenticado(
            AuthenticatedUser usuario
    ) {
        return usuarioRepository
                .findByIdAndAtivoTrue(usuario.id())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario autenticado nao encontrado."
                        )
                );
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(ZONA_HORARIA_RESERVAS);
    }
}