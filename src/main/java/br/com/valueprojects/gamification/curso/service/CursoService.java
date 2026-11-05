package br.com.valueprojects.gamification.curso.service;

import br.com.valueprojects.gamification.aluno.domain.Aluno;
import br.com.valueprojects.gamification.aluno.domain.ConclusaoDeCurso;
import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.aluno.service.AlunoPort;
import br.com.valueprojects.gamification.aluno.vo.Curso;
import br.com.valueprojects.gamification.curso.domain.CursoConcluido;
import br.com.valueprojects.gamification.evento.service.EventoPublisherPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service da conclusão de curso (cenário 5). Só orquestra:
 * carrega o aluno (com o estado real), pede ao domínio que conclua o curso, salva e
 * registra o evento. O bônus de +3 cursos e a promoção a Premium com 12 cursos são
 * regras de {@link Aluno#concluirCurso(Curso, double)}, não deste serviço.
 *
 * <p><b>Fronteira transacional (ASR-6):</b> conclusão, recompensa e a intenção de
 * notificar são confirmadas ou desfeitas JUNTAS, na transação deste método. Nada aqui
 * chama o consumidor (e-mail): o evento só é gravado na outbox, e quem o entrega é o
 * dispatcher, depois do commit. Assim a recompensa continua imediata e uma falha de
 * notificação não consegue desfazê-la.
 */
@Service
public class CursoService {

    private final CursoPort cursoPort;
    private final AlunoPort alunoPort;
    private final EventoPublisherPort eventoPublisher;

    public CursoService(CursoPort cursoPort, AlunoPort alunoPort, EventoPublisherPort eventoPublisher) {
        this.cursoPort = cursoPort;
        this.alunoPort = alunoPort;
        this.eventoPublisher = eventoPublisher;
    }

    @Transactional
    public AlunoResponse concluirCurso(String alunoNome, String cursoNome, double media) {
        Aluno aluno = alunoPort.obterOuCriar(alunoNome);
        Curso curso = cursoPort.buscarOuRegistrar(cursoNome);
        ConclusaoDeCurso conclusao = aluno.concluirCurso(curso, media);
        AlunoResponse resposta = alunoPort.salvar(aluno);
        // Último passo, ainda dentro da transação: se salvar falhou, o evento nem nasce;
        // se publicar falhar, a recompensa também é desfeita (nunca fato sem evento).
        eventoPublisher.publicar(CursoConcluido.TIPO, CursoConcluido.de(aluno, conclusao));
        return resposta;
    }
}
