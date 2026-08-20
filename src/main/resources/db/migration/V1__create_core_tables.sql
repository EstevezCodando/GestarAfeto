CREATE TABLE gestantes (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(160) NOT NULL,
    data_nascimento DATE,
    telefone VARCHAR(30),
    email VARCHAR(160),
    data_ultima_menstruacao DATE,
    data_provavel_parto DATE,
    observacoes TEXT,
    data_cadastro TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP
);

CREATE TABLE procedimentos_prenatal (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    nome VARCHAR(180) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(40) NOT NULL,
    trimestre_recomendado VARCHAR(20),
    semana_inicial_recomendada INTEGER,
    semana_final_recomendada INTEGER,
    obrigatorio BOOLEAN NOT NULL DEFAULT TRUE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE itens_checklist_gestante (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    gestante_id BIGINT NOT NULL,
    procedimento_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_prevista DATE,
    data_realizacao DATE,
    observacao TEXT,
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP,
    CONSTRAINT fk_checklist_gestante
        FOREIGN KEY (gestante_id) REFERENCES gestantes(id) ON DELETE CASCADE,
    CONSTRAINT fk_checklist_procedimento
        FOREIGN KEY (procedimento_id) REFERENCES procedimentos_prenatal(id)
);

CREATE TABLE consultas_prenatal (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    gestante_id BIGINT NOT NULL,
    data_consulta DATE NOT NULL,
    semana_gestacional INTEGER,
    peso NUMERIC(5, 2),
    pressao_arterial VARCHAR(20),
    observacoes TEXT,
    data_registro TIMESTAMP NOT NULL,
    CONSTRAINT fk_consulta_gestante
        FOREIGN KEY (gestante_id) REFERENCES gestantes(id) ON DELETE CASCADE
);
