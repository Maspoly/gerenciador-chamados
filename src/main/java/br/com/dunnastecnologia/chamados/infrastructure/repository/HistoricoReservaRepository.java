package br.com.dunnastecnologia.chamados.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.dunnastecnologia.chamados.domain.model.HistoricoReserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;

@Repository
public interface HistoricoReservaRepository extends JpaRepository<HistoricoReserva, UUID> {

    Optional<HistoricoReserva> findTopByReservaIdAndStatusNovoOrderByDataAlteracaoDesc(
        UUID reservaId,
        StatusReserva statusNovo
);
}