-- ADR-004 (Desafio 4): tabela da outbox de eventos.
-- Em dev/homol o Hibernate cria a tabela (ddl-auto=update). Em PROD (ddl-auto=validate)
-- este script precisa ser aplicado ANTES do deploy; o projeto não usa Flyway/Liquibase.
-- Tipos escolhidos para passar na validação do Hibernate 6 sobre PostgreSQL.

CREATE TABLE IF NOT EXISTS evento_outbox (
    id                    uuid          NOT NULL PRIMARY KEY,
    tipo                  varchar(100)  NOT NULL,
    payload               varchar(4000) NOT NULL,
    criado_em             timestamp(6) with time zone NOT NULL,
    status                varchar(20)   NOT NULL,   -- PENDENTE | ENVIADO | FALHOU
    tentativas            integer       NOT NULL,
    proxima_tentativa_em  timestamp(6) with time zone NOT NULL,
    ultimo_erro           varchar(500)
);

-- Sustenta a consulta do dispatcher: pendentes cuja hora de tentar já chegou.
CREATE INDEX IF NOT EXISTS idx_evento_outbox_status_proxima
    ON evento_outbox (status, proxima_tentativa_em);
