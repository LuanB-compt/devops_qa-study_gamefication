package br.com.valueprojects.gamification.contribuicao.entity;

import br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representação persistida (JPA) de um
 * {@link br.com.valueprojects.gamification.domain.MesDeContribuicoes}.
 * As contribuições em si ficam em {@link ContribuicaoEntity}, ligadas a este
 * mês por chave estrangeira.
 */
@Entity
@Table(name = "mes_de_contribuicoes")
public class MesDeContribuicoesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String descricao;

    protected MesDeContribuicoesEntity() {
        // exigido pelo JPA
    }

    public MesDeContribuicoesEntity(String descricao) {
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }
}
