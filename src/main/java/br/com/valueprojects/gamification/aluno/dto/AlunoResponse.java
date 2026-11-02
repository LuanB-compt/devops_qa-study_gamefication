package br.com.valueprojects.gamification.aluno.dto;

public record AlunoResponse(Long id, String nome, int cursosConcluidos, int moedas, String plano) {
}
