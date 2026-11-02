package br.com.valueprojects.gamification.curso.controller;

import br.com.valueprojects.gamification.aluno.dto.AlunoResponse;
import br.com.valueprojects.gamification.curso.dto.ConcluirCursoRequest;
import br.com.valueprojects.gamification.curso.service.CursoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conclusão de cursos por um aluno (cenário 4).
 */
@RestController
@RequestMapping("/api/alunos/{alunoNome}/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @PostMapping
    public AlunoResponse concluirCurso(@PathVariable String alunoNome, @RequestBody ConcluirCursoRequest request) {
        return cursoService.concluirCurso(alunoNome, request.cursoNome(), request.media());
    }
}
