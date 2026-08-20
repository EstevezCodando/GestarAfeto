ALTER TABLE itens_checklist_gestante
    ADD CONSTRAINT uk_checklist_gestante_procedimento UNIQUE (gestante_id, procedimento_id);

ALTER TABLE procedimentos_prenatal
    ADD CONSTRAINT ck_procedimento_tipo
        CHECK (tipo IN ('CONSULTA', 'EXAME', 'VACINA', 'ORIENTACAO', 'ULTRASSONOGRAFIA')),
    ADD CONSTRAINT ck_procedimento_trimestre
        CHECK (trimestre_recomendado IS NULL OR trimestre_recomendado IN ('PRIMEIRO', 'SEGUNDO', 'TERCEIRO')),
    ADD CONSTRAINT ck_procedimento_semana_inicial
        CHECK (semana_inicial_recomendada IS NULL OR semana_inicial_recomendada BETWEEN 1 AND 42),
    ADD CONSTRAINT ck_procedimento_semana_final
        CHECK (semana_final_recomendada IS NULL OR semana_final_recomendada BETWEEN 1 AND 42),
    ADD CONSTRAINT ck_procedimento_intervalo_semanas
        CHECK (
            semana_inicial_recomendada IS NULL
            OR semana_final_recomendada IS NULL
            OR semana_inicial_recomendada <= semana_final_recomendada
        );

ALTER TABLE itens_checklist_gestante
    ADD CONSTRAINT ck_checklist_status
        CHECK (status IN ('PENDENTE', 'REALIZADO', 'NAO_SE_APLICA', 'PRECISA_REVISAR')),
    ADD CONSTRAINT ck_checklist_realizado_tem_data
        CHECK (status <> 'REALIZADO' OR data_realizacao IS NOT NULL);

ALTER TABLE consultas_prenatal
    ADD CONSTRAINT ck_consulta_semana
        CHECK (semana_gestacional IS NULL OR semana_gestacional BETWEEN 1 AND 42),
    ADD CONSTRAINT ck_consulta_peso
        CHECK (peso IS NULL OR peso > 0);

CREATE INDEX idx_gestantes_nome ON gestantes (LOWER(nome));
CREATE INDEX idx_procedimentos_ativo ON procedimentos_prenatal (ativo);
CREATE INDEX idx_procedimentos_ativo_tipo ON procedimentos_prenatal (ativo, tipo);
CREATE INDEX idx_checklist_gestante ON itens_checklist_gestante (gestante_id);
CREATE INDEX idx_checklist_gestante_status ON itens_checklist_gestante (gestante_id, status);
CREATE INDEX idx_consultas_gestante_data_desc ON consultas_prenatal (gestante_id, data_consulta DESC);
