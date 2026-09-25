package br.com.dunnastecnologia.chamados.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "areas_comuns")
@Getter
@Setter
public class AreaComum {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativa = Boolean.TRUE;
}