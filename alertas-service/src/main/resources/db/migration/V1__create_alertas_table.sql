-- Schema exclusivo do microsservico de alertas.
-- gestante_id e origem_id sao referencias logicas ao servico principal: nao existe
-- chave estrangeira entre bancos, o que mantem os dois servicos independentes.

CREATE TABLE alertas (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    gestante_id BIGINT NOT NULL,
    gestante_nome VARCHAR(160) NOT NULL,
    origem_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensagem TEXT NOT NULL,
    data_referencia DATE,
    semana_gestacional_referencia INTEGER,
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP,
    data_leitura TIMESTAMP,
    data_resolucao TIMESTAMP,
    CONSTRAINT uk_alerta_origem_tipo UNIQUE (gestante_id, origem_id, tipo)
);

CREATE INDEX idx_alertas_gestante ON alertas (gestante_id);
CREATE INDEX idx_alertas_gestante_status ON alertas (gestante_id, status);

ALTER TABLE alertas ADD CONSTRAINT ck_alertas_tipo
    CHECK (tipo IN ('PROCEDIMENTO_ATRASADO', 'PROCEDIMENTO_PENDENTE', 'JANELA_PROXIMA', 'REVISAO_SOLICITADA'));

ALTER TABLE alertas ADD CONSTRAINT ck_alertas_prioridade
    CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA'));

ALTER TABLE alertas ADD CONSTRAINT ck_alertas_status
    CHECK (status IN ('ABERTO', 'LIDO', 'RESOLVIDO', 'CANCELADO'));

-- Um alerta encerrado precisa registrar quando foi encerrado.
ALTER TABLE alertas ADD CONSTRAINT ck_alertas_resolucao
    CHECK (status NOT IN ('RESOLVIDO', 'CANCELADO') OR data_resolucao IS NOT NULL);

-- Um alerta lido precisa registrar quando foi lido.
ALTER TABLE alertas ADD CONSTRAINT ck_alertas_leitura
    CHECK (status <> 'LIDO' OR data_leitura IS NOT NULL);

COMMENT ON TABLE alertas IS 'Alertas derivados do checklist pre-natal, mantidos pelo microsservico gestarafeto-alertas';
COMMENT ON COLUMN alertas.origem_id IS 'Id do item de checklist no servico principal (referencia logica, sem FK)';
COMMENT ON COLUMN alertas.gestante_nome IS 'Nome replicado no momento da avaliacao para evitar chamada de volta ao servico principal';
