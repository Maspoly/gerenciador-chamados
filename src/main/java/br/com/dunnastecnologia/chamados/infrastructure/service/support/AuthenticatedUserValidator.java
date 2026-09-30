package br.com.dunnastecnologia.chamados.infrastructure.service.support;

import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.dunnastecnologia.chamados.application.Security.AuthenticatedUser;
import br.com.dunnastecnologia.chamados.infrastructure.exception.UnauthorizedOperationException;
import br.com.dunnastecnologia.chamados.infrastructure.repository.AdministradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.ColaboradorRepository;
import br.com.dunnastecnologia.chamados.infrastructure.repository.MoradorRepository;

@Component
public class AuthenticatedUserValidator {

    private final AdministradorRepository administradorRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final MoradorRepository moradorRepository;

    public AuthenticatedUserValidator(
            AdministradorRepository administradorRepository,
            ColaboradorRepository colaboradorRepository,
            MoradorRepository moradorRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.moradorRepository = moradorRepository;
    }

    public void assertAdministrador(AuthenticatedUser user) {
        if (!isAdministrador(user) || !administradorRepository.existsByIdAndAtivoTrue(user.id())) {
            throw new UnauthorizedOperationException("Usuario autenticado nao possui perfil de administrador");
        }
    }

    public void assertColaborador(AuthenticatedUser user) {
        if (!isColaborador(user) || !colaboradorRepository.existsByIdAndAtivoTrue(user.id())) {
            throw new UnauthorizedOperationException("Usuario autenticado nao possui perfil de colaborador");
        }
    }

    public void assertMorador(AuthenticatedUser user) {
        if (!isMorador(user) || !moradorRepository.existsByIdAndAtivoTrue(user.id())) {
            throw new UnauthorizedOperationException("Usuario autenticado nao possui perfil de morador");
        }
    }

    public boolean isAdministrador(AuthenticatedUser user) {
        return hasRole(user, "ADMIN", "ADMINISTRADOR");
    }

    public boolean isColaborador(AuthenticatedUser user) {
        return hasRole(user, "COLABORADOR");
    }

    public boolean isMorador(AuthenticatedUser user) {
        return hasRole(user, "MORADOR");
    }

    private boolean hasRole(AuthenticatedUser user, String... acceptedRoles) {
        if (user == null || user.role() == null) {
            return false;
        }

        String normalizedRole = user.role().trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }

        for (String acceptedRole : acceptedRoles) {
            if (normalizedRole.equals(acceptedRole)) {
                return true;
            }
        }
        return false;
    }
}
