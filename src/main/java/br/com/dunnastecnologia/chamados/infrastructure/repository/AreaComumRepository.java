package br.com.dunnastecnologia.chamados.infrastructure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import jakarta.persistence.LockModeType;

@Repository
public interface AreaComumRepository
        extends JpaRepository<AreaComum, UUID> {

    /*
     * Busca apenas as areas comuns ativas.
     * Utilizado principalmente nas consultas do morador.
     */
    List<AreaComum> findByAtivaTrue();


    /*
     * Busca uma area comum aplicando lock pessimista de escrita.
     *
     * Enquanto uma transacao possuir esse lock,
     * outra transacao que tentar bloquear a mesma area
     * devera aguardar.
     *
     * Isso sera usado durante a aprovacao das reservas
     * para impedir duas aprovacoes conflitantes
     * simultaneas para a mesma area.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM AreaComum a
        WHERE a.id = :areaComumId
    """)
    Optional<AreaComum> buscarPorIdComLock(
            @Param("areaComumId") UUID areaComumId
    );
}