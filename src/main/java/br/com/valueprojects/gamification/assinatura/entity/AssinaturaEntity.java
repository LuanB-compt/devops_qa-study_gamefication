package br.com.valueprojects.gamification.assinatura.entity;

import br.com.valueprojects.gamification.aluno.entity.AlunoEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Representação persistida (JPA) de uma
 * {@link br.com.valueprojects.gamification.domain.Assinatura}. Atende o
 * cenário 2 do bdd.md (cancelamento por falta de pagamento no encerramento
 * do mês).
 */
@Entity
@Table(name = "assinatura")
public class AssinaturaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "aluno_id", nullable = false, unique = true)
    private AlunoEntity aluno;

    @Column(nullable = false)
    private boolean ativa;

    @Column(nullable = false)
    private boolean pagamentoRealizado;

    protected AssinaturaEntity() {
        // exigido pelo JPA
    }

    public AssinaturaEntity(AlunoEntity aluno) {
        this.aluno = aluno;
        this.ativa = true;
        this.pagamentoRealizado = false;
    }

    public Long getId() {
        return id;
    }

    public AlunoEntity getAluno() {
        return aluno;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public boolean isPagamentoRealizado() {
        return pagamentoRealizado;
    }

    public void setPagamentoRealizado(boolean pagamentoRealizado) {
        this.pagamentoRealizado = pagamentoRealizado;
    }
}
