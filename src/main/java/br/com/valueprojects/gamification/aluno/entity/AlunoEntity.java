package br.com.valueprojects.gamification.aluno.entity;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representação persistida (JPA) de um {@link Aluno}. Guarda o estado que o
 * objeto de domínio não mantém sozinho entre requisições (cursos concluídos,
 * moedas, plano). A tradução entre esta entidade e o objeto de domínio é
 * responsabilidade do service — o domínio não conhece JPA.
 */
@Entity
@Table(name = "aluno")
public class AlunoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private int cursosConcluidos;

    @Column(nullable = false)
    private int moedas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Aluno.Plano plano;

    protected AlunoEntity() {
        // exigido pelo JPA
    }

    public AlunoEntity(String nome) {
        this.nome = nome;
        this.cursosConcluidos = 0;
        this.moedas = 0;
        this.plano = Aluno.Plano.BASICO;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public int getCursosConcluidos() {
        return cursosConcluidos;
    }

    public void setCursosConcluidos(int cursosConcluidos) {
        this.cursosConcluidos = cursosConcluidos;
    }

    public int getMoedas() {
        return moedas;
    }

    public void setMoedas(int moedas) {
        this.moedas = moedas;
    }

    public Aluno.Plano getPlano() {
        return plano;
    }

    public void setPlano(Aluno.Plano plano) {
        this.plano = plano;
    }
}
