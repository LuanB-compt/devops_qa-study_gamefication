package br.com.valueprojects.gamification.curso.entity;

import br.com.valueprojects.gamification.aluno.vo.Curso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representação persistida (JPA) de um {@link br.com.valueprojects.gamification.domain.Curso}.
 */
@Entity
@Table(name = "curso")
public class CursoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    protected CursoEntity() {
        // exigido pelo JPA
    }

    public CursoEntity(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
