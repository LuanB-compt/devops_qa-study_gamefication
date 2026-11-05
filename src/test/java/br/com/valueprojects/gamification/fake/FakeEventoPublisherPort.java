package br.com.valueprojects.gamification.fake;

import br.com.valueprojects.gamification.evento.service.EventoPublisherPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Publisher que só registra o que foi publicado, para testar o caso de uso sem outbox. */
public class FakeEventoPublisherPort implements EventoPublisherPort {

    public record Publicado(String tipo, Object payload) {
    }

    private final List<Publicado> publicados = new ArrayList<>();

    @Override
    public UUID publicar(String tipo, Object payload) {
        publicados.add(new Publicado(tipo, payload));
        return UUID.randomUUID();
    }

    public List<Publicado> publicados() {
        return publicados;
    }
}
