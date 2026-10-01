package br.com.dunnastecnologia.chamados.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.domain.model.Administrador;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.domain.model.Morador;
import br.com.dunnastecnologia.chamados.domain.model.Reserva;
import br.com.dunnastecnologia.chamados.domain.model.StatusReserva;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AreaComumRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ReservaRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.UsuarioRepository;
import br.com.dunnastecnologia.chamados.infrastructure.service.ReservaService;

@SpringBootTest
@Testcontainers
class ReservaConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reservas_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("api.security.token.secret", () -> "test-only-secret-key-12345678901234567890");
    }

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AreaComumRepository areaComumRepository;

    @Autowired
    private MoradorRepository moradorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void duasAprovacoesConcorrentesNaoDevemPermitirDuasReservasAprovadasSobrepostas() throws Exception {
        AreaComum area = new AreaComum();
        area.setNome("Salão de Festas");
        area.setAtiva(Boolean.TRUE);
        final AreaComum areaPersistida = areaComumRepository.save(area);

        Administrador administrador = new Administrador();
        administrador.setNome("Admin");
        administrador.setEmail("admin.concorrencia@condominio.local");
        administrador.setSenha("secret");
        administrador.setAtivo(Boolean.TRUE);
        final Administrador administradorPersistido = (Administrador) usuarioRepository.save(administrador);

        Morador morador1 = new Morador();
        morador1.setNome("Morador 1");
        morador1.setEmail("morador1.concorrencia@condominio.local");
        morador1.setSenha("secret");
        morador1.setAtivo(Boolean.TRUE);
        final Morador moradorPersistido1 = moradorRepository.save(morador1);

        Morador morador2 = new Morador();
        morador2.setNome("Morador 2");
        morador2.setEmail("morador2.concorrencia@condominio.local");
        morador2.setSenha("secret");
        morador2.setAtivo(Boolean.TRUE);
        final Morador moradorPersistido2 = moradorRepository.save(morador2);

        LocalDateTime inicio = LocalDateTime.now().plusDays(5).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(2);

        Reserva reserva1 = new Reserva();
        reserva1.setAreaComum(areaPersistida);
        reserva1.setMorador(moradorPersistido1);
        reserva1.setDataHoraInicio(inicio);
        reserva1.setDataHoraFim(fim);
        reserva1.setStatus(StatusReserva.SOLICITADA);
        final Reserva reservaPersistida1 = reservaRepository.save(reserva1);

        Reserva reserva2 = new Reserva();
        reserva2.setAreaComum(areaPersistida);
        reserva2.setMorador(moradorPersistido2);
        reserva2.setDataHoraInicio(inicio.plusMinutes(30));
        reserva2.setDataHoraFim(fim.plusMinutes(30));
        reserva2.setStatus(StatusReserva.SOLICITADA);
        final Reserva reservaPersistida2 = reservaRepository.save(reserva2);

        final AuthenticatedUser adminUser = new AuthenticatedUser(administradorPersistido.getId(), administradorPersistido.getEmail(), "ROLE_ADMINISTRADOR");
        final java.util.UUID areaId = areaPersistida.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(2);

        Future<Boolean> t1 = executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                reservaService.aprovarReserva(reservaPersistida1.getId(), adminUser);
                return true;
            } catch (Exception ex) {
                return false;
            }
        });

        Future<Boolean> t2 = executor.submit(() -> {
            ready.countDown();
            start.await();
            try {
                reservaService.aprovarReserva(reservaPersistida2.getId(), adminUser);
                return true;
            } catch (Exception ex) {
                return false;
            }
        });

        ready.await();
        start.countDown();

        boolean aprovouPrimeira = t1.get();
        boolean aprovouSegunda = t2.get();
        executor.shutdown();

        List<Reserva> reservasAprovadas = reservaRepository.findAll().stream()
                .filter(reserva -> reserva.getStatus() == StatusReserva.APROVADA)
                .toList();

        assertEquals(1, reservasAprovadas.size(), "Exatamente uma reserva deve permanecer aprovada.");
        assertEquals(1, reservasAprovadas.stream().filter(reserva -> reserva.getAreaComum().getId().equals(areaId)).count());
        assertNotNull(reservasAprovadas.getFirst());
        assertEquals(1, (aprovouPrimeira ? 1 : 0) + (aprovouSegunda ? 1 : 0), "Uma das aprovações deve falhar por conflito real.");
    }
}
