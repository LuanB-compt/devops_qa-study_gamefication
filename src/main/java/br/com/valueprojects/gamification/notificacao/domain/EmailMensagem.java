package br.com.valueprojects.gamification.notificacao.domain;

import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

/**
 * E-mail pronto para envio.
 *
 * @param idempotencyKey id do evento de origem. Vai no cabeçalho {@code Message-ID} ao
 *                       provedor: uma reentrega (entrega pelo menos uma vez) chega com a
 *                       MESMA chave e pode ser descartada por quem a recebe.
 */
public record EmailMensagem(UUID idempotencyKey, String para, String assunto, String corpo) {

    /**
     * O aluno ainda não tem e-mail no modelo (AC1). O endereço é derivado do nome por
     * convenção, até existir um campo próprio. Trocar isto não afeta o resto do fluxo.
     */
    static final String DOMINIO_DOS_ALUNOS = "alunos.gamification.local";

    public static EmailMensagem paraCursoConcluido(UUID eventoId, CursoConcluido evento) {
        StringBuilder corpo = new StringBuilder()
                .append("Olá, ").append(evento.alunoNome()).append("!\n\n")
                .append("Você concluiu o curso \"").append(evento.cursoNome()).append("\" ")
                .append("com média ").append(String.format(Locale.ROOT, "%.1f", evento.media())).append(".\n")
                .append("Cursos creditados: ").append(evento.cursosGanhos())
                .append(" (total: ").append(evento.cursosConcluidosTotal()).append(").\n");
        if (evento.virouPremium()) {
            corpo.append("Parabéns! Você agora é Premium e ganhou 3 moedas.\n");
        }
        return new EmailMensagem(eventoId, enderecoDe(evento.alunoNome()),
                "Curso concluído: " + evento.cursoNome(), corpo.toString());
    }

    /** "José da Silva" -> "jose.da.silva@alunos.gamification.local". */
    static String enderecoDe(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String local = semAcento.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", ".")
                .replaceAll("^\\.|\\.$", "");
        return local + "@" + DOMINIO_DOS_ALUNOS;
    }
}
