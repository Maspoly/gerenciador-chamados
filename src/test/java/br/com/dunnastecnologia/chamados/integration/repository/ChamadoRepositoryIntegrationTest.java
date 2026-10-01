package br.com.dunnastecnologia.chamados.integration.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.dunnastecnologia.chamados.domain.model.Chamado;
import br.com.dunnastecnologia.chamados.domain.model.StatusChamado;
import br.com.dunnastecnologia.chamados.domain.model.TipoChamado;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ChamadoRepository;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ChamadoRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("chamados_test")
        .withUsername("postgres")
        .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void marcarChamadosAtrasadosDeveAtualizarStatusQuandoSlaJaEstiverExpirado() {
        StatusChamado solicitado = new StatusChamado();
        solicitado.setNome("Solicitado");
        solicitado.setInicialPadrao(Boolean.TRUE);
        entityManager.persist(solicitado);

        StatusChamado atrasado = new StatusChamado();
        atrasado.setNome("Atrasado");
        atrasado.setInicialPadrao(Boolean.FALSE);
        entityManager.persist(atrasado);

        TipoChamado tipoChamado = new TipoChamado();
        tipoChamado.setTitulo("Eletrica");
        tipoChamado.setPrazoHoras(1);
        entityManager.persist(tipoChamado);

        Chamado chamado = new Chamado();
        chamado.setDescricao("Luz apagada");
        chamado.setDataAbertura(LocalDateTime.now().minusHours(2));
        chamado.setStatus(solicitado);
        chamado.setTipoChamado(tipoChamado);
        entityManager.persist(chamado);

        entityManager.flush();
        entityManager.clear();

        int registrosAtualizados = chamadoRepository.marcarChamadosAtrasados();

        entityManager.clear();
        Chamado chamadoAtualizado = entityManager.find(Chamado.class, chamado.getId());

        assertEquals(1, registrosAtualizados);
        assertEquals("Atrasado", chamadoAtualizado.getStatus().getNome());
    }
}
