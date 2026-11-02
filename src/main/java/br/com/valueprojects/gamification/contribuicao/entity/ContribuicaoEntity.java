package br.com.valueprojects.gamification.contribuicao.entity;

import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Representação persistida (JPA) de uma
 * {@link br.com.valueprojects.gamification.domain.Contribuicao}, associada
 * ao aluno que contribuiu e ao mês em que a contribuição ocorreu. A ordem de
 * inserção (id crescente) é usada pelo service para reconstruir o
 * {@link br.com.valueprojects.gamification.domain.MesDeContribuicoes} do
 * domínio na mesma sequência em que as contribuições foram registradas.
 */
@Entity
@Table(name = "contribuicao")
public class ContribuicaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private AlunoEntity aluno;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mes_id", nullable = false)
    private MesDeContribuicoesEntity mes;

    @Column(nullable = false)
    private double peso;

    protected ContribuicaoEntity() {
        // exigido pelo JPA
    }

    public ContribuicaoEntity(AlunoEntity aluno, MesDeContribuicoesEntity mes, double peso) {
        this.aluno = aluno;
        this.mes = mes;
        this.peso = peso;
    }

    public Long getId() {
        return id;
    }

    public AlunoEntity getAluno() {
        return aluno;
    }

    public MesDeContribuicoesEntity getMes() {
        return mes;
    }

    public double getPeso() {
        return peso;
    }
}
