package br.com.dunnastecnologia.chamados.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import br.com.dunnastecnologia.chamados.infrastructure.service.AreaComumService;
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
    private AreaComumService areaComumService;

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

    @Test
    void reservaSolicitadaNaoBloqueiaAprovacaoDeOutraReservaSobreposta() {
        AreaComum area = salvarArea("Area pendente");
        Administrador administrador = salvarAdministrador();
        Morador morador1 = salvarMorador();
        Morador morador2 = salvarMorador();
        LocalDateTime inicio = LocalDateTime.now().plusDays(8).withMinute(0).withSecond(0).withNano(0);

        Reserva solicitada1 = salvarReserva(area, morador1, inicio, inicio.plusHours(2));
        Reserva solicitada2 = salvarReserva(area, morador2, inicio.plusMinutes(30), inicio.plusHours(3));

        AuthenticatedUser adminUser = usuarioAdministrador(administrador);
        Reserva aprovada = reservaService.aprovarReserva(solicitada1.getId(), adminUser);

        assertEquals(StatusReserva.APROVADA, aprovada.getStatus());
        assertEquals(StatusReserva.SOLICITADA, reservaRepository.findById(solicitada2.getId()).orElseThrow().getStatus());
    }

    @Test
    void reservasSobrepostasDeAreasDiferentesPodemSerAprovadas() {
        AreaComum area1 = salvarArea("Area A");
        AreaComum area2 = salvarArea("Area B");
        Administrador administrador = salvarAdministrador();
        Morador morador1 = salvarMorador();
        Morador morador2 = salvarMorador();
        LocalDateTime inicio = LocalDateTime.now().plusDays(9).withMinute(0).withSecond(0).withNano(0);

        Reserva reserva1 = salvarReserva(area1, morador1, inicio, inicio.plusHours(2));
        Reserva reserva2 = salvarReserva(area2, morador2, inicio, inicio.plusHours(2));
        AuthenticatedUser adminUser = usuarioAdministrador(administrador);

        reservaService.aprovarReserva(reserva1.getId(), adminUser);
        reservaService.aprovarReserva(reserva2.getId(), adminUser);

        assertEquals(StatusReserva.APROVADA, reservaRepository.findById(reserva1.getId()).orElseThrow().getStatus());
        assertEquals(StatusReserva.APROVADA, reservaRepository.findById(reserva2.getId()).orElseThrow().getStatus());
    }

    @Test
    void desativarAreaPreservaReservaExistenteERejeitaNovaSolicitacao() {
        AreaComum area = salvarArea("Area desativada");
        Administrador administrador = salvarAdministrador();
        Morador morador = salvarMorador();
        LocalDateTime inicio = LocalDateTime.now().plusDays(10).withMinute(0).withSecond(0).withNano(0);
        Reserva existente = salvarReserva(area, morador, inicio, inicio.plusHours(2));
        AuthenticatedUser adminUser = usuarioAdministrador(administrador);
        AuthenticatedUser moradorUser = usuarioMorador(morador);

        areaComumService.desativar(adminUser, area.getId());

        Reserva recarregada = reservaRepository.findById(existente.getId()).orElseThrow();
        assertTrue(reservaRepository.existsById(existente.getId()));
        assertEquals(StatusReserva.SOLICITADA, recarregada.getStatus());
        assertEquals(Boolean.FALSE, areaComumRepository.findById(area.getId()).orElseThrow().getAtiva());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> reservaService.solicitarReserva(
                        moradorUser,
                        area.getId(),
                        inicio.plusDays(1),
                        inicio.plusDays(1).plusHours(2)
                )
        );
    }

    private AreaComum salvarArea(String nome) {
        AreaComum area = new AreaComum();
        area.setNome(nome + " " + java.util.UUID.randomUUID());
        area.setAtiva(Boolean.TRUE);
        return areaComumRepository.save(area);
    }

    private Administrador salvarAdministrador() {
        Administrador administrador = new Administrador();
        administrador.setNome("Admin");
        administrador.setEmail("admin." + java.util.UUID.randomUUID() + "@condominio.local");
        administrador.setSenha("secret");
        administrador.setAtivo(Boolean.TRUE);
        return (Administrador) usuarioRepository.save(administrador);
    }

    private Morador salvarMorador() {
        Morador morador = new Morador();
        morador.setNome("Morador");
        morador.setEmail("morador." + java.util.UUID.randomUUID() + "@condominio.local");
        morador.setSenha("secret");
        morador.setAtivo(Boolean.TRUE);
        return moradorRepository.save(morador);
    }

    private Reserva salvarReserva(
            AreaComum area,
            Morador morador,
            LocalDateTime inicio,
            LocalDateTime fim
    ) {
        Reserva reserva = new Reserva();
        reserva.setAreaComum(area);
        reserva.setMorador(morador);
        reserva.setDataHoraInicio(inicio);
        reserva.setDataHoraFim(fim);
        reserva.setStatus(StatusReserva.SOLICITADA);
        return reservaRepository.save(reserva);
    }

    private AuthenticatedUser usuarioAdministrador(Administrador administrador) {
        return new AuthenticatedUser(
                administrador.getId(),
                administrador.getEmail(),
                "ROLE_ADMINISTRADOR"
        );
    }

    private AuthenticatedUser usuarioMorador(Morador morador) {
        return new AuthenticatedUser(
                morador.getId(),
                morador.getEmail(),
                "ROLE_MORADOR"
        );
    }
}
