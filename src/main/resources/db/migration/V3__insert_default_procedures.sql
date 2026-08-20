INSERT INTO procedimentos_prenatal
    (nome, descricao, tipo, trimestre_recomendado, semana_inicial_recomendada, semana_final_recomendada, obrigatorio, ativo)
SELECT nome, descricao, tipo, trimestre_recomendado, semana_inicial_recomendada, semana_final_recomendada, obrigatorio, ativo
FROM (VALUES
    ('Primeira Consulta de Pre-Natal', 'Consulta inicial de acompanhamento gestacional', 'CONSULTA', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Tipagem Sanguinea e Fator Rh', 'Exame laboratorial para tipagem sanguinea', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Hemograma Completo', 'Avaliacao de celulas do sangue', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Glicemia de Jejum', 'Rastreamento para diabetes gestacional', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Exame de Urina EAS', 'Uranalise e sedimentoscopia', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Urocultura', 'Cultura de urina para identificar infeccoes', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Teste para Sifilis VDRL', 'Rastreamento para sifilis', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Teste Rapido HIV', 'Rastreamento para HIV', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Teste Rapido Hepatite B', 'Rastreamento para hepatite B', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Teste Rapido Hepatite C', 'Rastreamento para hepatite C', 'EXAME', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Ultrassonografia Obstetrica 1T', 'Ultrassom do primeiro trimestre', 'ULTRASSONOGRAFIA', 'PRIMEIRO', 11, 13, TRUE, TRUE),
    ('Avaliacao de Pressao Arterial', 'Monitoramento periodico de pressao arterial', 'CONSULTA', NULL, 1, 40, TRUE, TRUE),
    ('Avaliacao de Peso', 'Monitoramento periodico do ganho de peso', 'CONSULTA', NULL, 1, 40, TRUE, TRUE),
    ('Ultrassonografia Morfologica', 'Ultrassom morfologico do segundo trimestre', 'ULTRASSONOGRAFIA', 'SEGUNDO', 20, 24, TRUE, TRUE),
    ('Teste de Tolerancia a Glicose TOTG', 'Diagnostico de diabetes gestacional', 'EXAME', 'SEGUNDO', 24, 28, TRUE, TRUE),
    ('Vacina dTpa', 'Vacina difteria, tetano e coqueluche acelular', 'VACINA', 'TERCEIRO', 27, 36, TRUE, TRUE),
    ('Vacina Influenza', 'Vacina contra influenza', 'VACINA', NULL, 1, 40, TRUE, TRUE),
    ('Vacina Hepatite B', 'Vacina contra hepatite B quando indicada', 'VACINA', 'PRIMEIRO', 1, 13, FALSE, TRUE),
    ('Orientacao sobre Sinais de Alerta', 'Educacao sobre situacoes de emergencia', 'ORIENTACAO', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Orientacao sobre Alimentacao', 'Educacao nutricional durante a gestacao', 'ORIENTACAO', 'PRIMEIRO', 1, 13, TRUE, TRUE),
    ('Orientacao sobre Aleitamento Materno', 'Educacao em saude sobre amamentacao', 'ORIENTACAO', 'TERCEIRO', 28, 40, TRUE, TRUE),
    ('Planejamento do Parto', 'Discussao e elaboracao do plano de parto', 'CONSULTA', 'TERCEIRO', 34, 40, TRUE, TRUE),
    ('Consulta de Retorno', 'Consultas periodicas de acompanhamento', 'CONSULTA', NULL, 1, 40, TRUE, TRUE)
) AS seed(nome, descricao, tipo, trimestre_recomendado, semana_inicial_recomendada, semana_final_recomendada, obrigatorio, ativo)
WHERE NOT EXISTS (
    SELECT 1 FROM procedimentos_prenatal p WHERE p.nome = seed.nome
);
