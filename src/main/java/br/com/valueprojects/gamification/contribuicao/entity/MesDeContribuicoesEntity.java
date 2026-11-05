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
 * Representação persistida (JPA) de um
 * {@link br.com.valueprojects.gamification.contribuicao.domain.MesDeContribuicoes}.
 * As contribuições em si ficam em {@link ContribuicaoEntity}, ligadas a este
 * mês por chave estrangeira.
 *
 * <p>Guarda também o <b>resultado da apuração</b> (encerrado + vencedor). O
 * objeto de domínio é reconstruído a cada requisição e não carrega esse
 * estado; sem ele, encerrar o mês duas vezes concederia o curso duas vezes.
 */
@Entity
@Table(name = "mes_de_contribuicoes")
public class MesDeContribuicoesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String descricao;

    // DEFAULT explícito: com ddl-auto=update o Hibernate faz ALTER TABLE ADD COLUMN NOT NULL,
    // que falharia em bancos (dev/homol) que já têm meses cadastrados sem o default.
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean encerrado;

    // Nulo enquanto o mês está aberto, ou se foi encerrado sem nenhuma contribuição.
    @ManyToOne
    @JoinColumn(name = "vencedor_id")
    private AlunoEntity vencedor;

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

    public boolean isEncerrado() {
        return encerrado;
    }

    public AlunoEntity getVencedor() {
        return vencedor;
    }

    /**
     * Fecha o mês de forma irreversível. Falha se já estiver encerrado: o
     * invariante "um mês só é apurado uma vez" fica garantido na própria
     * entidade, e não depende de cada chamador lembrar de checar.
     */
    public void encerrarCom(AlunoEntity vencedor) {
        if (encerrado) {
            throw new IllegalStateException("Mês já encerrado: " + descricao);
        }
        this.encerrado = true;
        this.vencedor = vencedor;
    }
}
