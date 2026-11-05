package br.com.valueprojects.gamification.contribuicao.domain;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fonte de aleatoriedade do desempate (cenário 3), injetável no domínio.
 *
 * <p>Antes o domínio fazia {@code new Random()} internamente, o que tornava o
 * sorteio impossível de controlar em teste. Com esta abstração a regra "empate
 * se resolve por sorteio" continua no domínio, mas o teste decide quem "sorteia".
 */
@FunctionalInterface
public interface Sorteador {

    /** @return um inteiro em {@code [0, limite)}. */
    int sortear(int limite);

    static Sorteador aleatorio() {
        return limite -> ThreadLocalRandom.current().nextInt(limite);
    }
}
