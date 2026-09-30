package br.com.dunnastecnologia.chamados.infrastructure.controller.api;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.dunnastecnologia.chamados.application.UserCase.AreaComumUseCases;
import br.com.dunnastecnologia.chamados.domain.model.AreaComum;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.WebControllerSupport;
import br.com.dunnastecnologia.chamados.infrastructure.controller.web.form.AreaComumForm;

@Controller
@RequestMapping("/admin/areas-comuns")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminAreaComumApiController {

    private final AreaComumUseCases areaComumUseCases;
    private final WebControllerSupport support;

    public AdminAreaComumApiController(
            AreaComumUseCases areaComumUseCases,
            WebControllerSupport support
    ) {
        this.areaComumUseCases = areaComumUseCases;
        this.support = support;
    }

    @PostMapping
    public String cadastrar(
            Authentication authentication,
            @ModelAttribute AreaComumForm form,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);

        var area = new AreaComum();
        area.setNome(form.getNome());
        area.setDescricao(form.getDescricao());

        areaComumUseCases.salvar(currentUser, area);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Área comum cadastrada."
        );

        return "redirect:/admin/areas-comuns";
    }

    @PatchMapping("/{areaId}")
    public String editar(
            Authentication authentication,
            @PathVariable UUID areaId,
            @ModelAttribute AreaComumForm form,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);

        var area = areaComumUseCases.buscarPorId(areaId);

        area.setNome(form.getNome());
        area.setDescricao(form.getDescricao());

        areaComumUseCases.salvar(currentUser, area);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Área comum atualizada."
        );

        return "redirect:/admin/areas-comuns";
    }

    @PatchMapping("/{areaId}/desativar")
    public String desativar(
            Authentication authentication,
            @PathVariable UUID areaId,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);

        areaComumUseCases.desativar(currentUser, areaId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Área comum desativada."
        );

        return "redirect:/admin/areas-comuns";
    }

    @PatchMapping("/{areaId}/reativar")
    public String reativar(
            Authentication authentication,
            @PathVariable UUID areaId,
            RedirectAttributes redirectAttributes
    ) {
        var currentUser = support.authenticatedUser(authentication);

        areaComumUseCases.reativar(currentUser, areaId);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Área comum reativada."
        );

        return "redirect:/admin/areas-comuns";
    }
}