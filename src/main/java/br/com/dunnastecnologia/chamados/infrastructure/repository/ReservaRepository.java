package br.com.dunnastecnologia.chamados.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    List<Reserva> findByMoradorIdOrderByDataCriacaoDesc(
            UUID moradorId
    );

    List<Reserva> findAllByOrderByDataCriacaoDesc();

    List<Reserva> findByStatusOrderByDataCriacaoDesc(
            StatusReserva status
    );

    // Busca todas as reservas de um morador específico ordenadas por data
    List<Reserva> findByMoradorIdOrderByDataHoraInicioDesc(UUID moradorId);

    // Consulta de Conflito: Verifica se existe reserva APROVADA na mesma área com sobreposição
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
          AND r.status = :statusAprovada
          AND r.dataHoraInicio < :dataFim
          AND r.dataHoraFim > :dataInicio
    """)
    boolean existeConflitoAprovado(
        @Param("areaComumId") UUID areaComumId,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        @Param("statusAprovada") StatusReserva statusAprovada
    );

    @Query("""
        SELECT r FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
        AND r.status = :status
        AND r.dataHoraInicio < :fim
        AND r.dataHoraFim > :inicio
        ORDER BY r.dataHoraInicio
    """)
    List<Reserva> buscarAprovadasPorAreaEPeriodo(
            @Param("areaComumId") UUID areaComumId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusReserva status
    );

    // Busca reservas de uma determinada área em um período para o calendário
    List<Reserva> findByAreaComumIdAndDataHoraInicioBetween(UUID areaComumId, LocalDateTime inicio, LocalDateTime fim);
    List<Reserva> findByStatusOrderByDataHoraInicioDesc(StatusReserva status);

    @Query("""
        SELECT r
        FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
        AND r.status IN :status
        AND r.dataHoraInicio < :fim
        AND r.dataHoraFim > :inicio
        ORDER BY r.dataHoraInicio
    """)
    List<Reserva> buscarPorAreaEPeriodoEStatus(
            @Param("areaComumId") UUID areaComumId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") List<StatusReserva> status
    );

    Optional<Reserva> findByIdAndMoradorId(
        UUID reservaId,
        UUID moradorId
    );
}