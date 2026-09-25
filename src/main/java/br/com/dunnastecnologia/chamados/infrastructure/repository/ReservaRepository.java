package br.com.dunnastecnologia.chamados.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    // Busca todas as reservas de um morador específico (para a listagem do Morador)
    List<Reserva> findByMoradorIdOrderByDataInicioDesc(UUID moradorId);

    // Consulta de Conflito: Verifica se existe alguma reserva APROVADA na mesma área que se sobrepõe ao intervalo solicitado
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.areaComum.id = :areaComumId
          AND r.status = :statusAprovada
          AND r.dataInicio < :dataFim
          AND r.dataFim > :dataInicio
    """)
    boolean existeConflitoAprovado(
        @Param("areaComumId") UUID areaComumId,
        @Param("dataInicio") LocalDateTime dataInicio,
        @Param("dataFim") LocalDateTime dataFim,
        @Param("statusAprovada") StatusReserva statusAprovada
    );

    // Busca todas as reservas ativas/pendentes de uma determinada área em um período (para visualização no calendário/agenda)
    List<Reserva> findByAreaComumIdAndDataInicioBetween(UUID areaComumId, LocalDateTime inicio, LocalDateTime fim);
}