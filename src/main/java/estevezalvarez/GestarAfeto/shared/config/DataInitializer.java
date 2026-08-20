package estevezalvarez.GestarAfeto.shared.config;

import estevezalvarez.GestarAfeto.procedimento.domain.ProcedimentoPreNatal;
import estevezalvarez.GestarAfeto.procedimento.domain.TipoProcedimento;
import estevezalvarez.GestarAfeto.procedimento.domain.TrimestreGestacional;
import estevezalvarez.GestarAfeto.procedimento.repository.ProcedimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("dev-h2")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProcedimentoRepository procedimentoRepository;

    @Override
    public void run(String... args) {
        if (procedimentoRepository.count() > 0) return;

        procedimentoRepository.saveAll(List.of(
            proc("Primeira Consulta de Pré-Natal", "Consulta inicial de acompanhamento gestacional", TipoProcedimento.CONSULTA, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Tipagem Sanguínea e Fator Rh", "Exame laboratorial para tipagem sanguínea", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Hemograma Completo", "Avaliação de células do sangue", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Glicemia de Jejum", "Rastreamento para diabetes gestacional", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Exame de Urina (EAS)", "Urinálise e sedimentoscopia", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Urocultura", "Cultura de urina para identificar infecções", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Teste para Sífilis (VDRL)", "Rastreamento para sífilis", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Teste Rápido HIV", "Rastreamento para HIV", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Teste Rápido Hepatite B", "Rastreamento para hepatite B", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Teste Rápido Hepatite C", "Rastreamento para hepatite C", TipoProcedimento.EXAME, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Ultrassonografia Obstétrica 1T", "Ultrassom do primeiro trimestre (11-13 semanas)", TipoProcedimento.ULTRASSONOGRAFIA, TrimestreGestacional.PRIMEIRO, 11, 13, true),
            proc("Avaliação de Pressão Arterial", "Monitoramento periódico de pressão arterial", TipoProcedimento.CONSULTA, null, 1, 40, true),
            proc("Avaliação de Peso", "Monitoramento periódico do ganho de peso", TipoProcedimento.CONSULTA, null, 1, 40, true),
            proc("Ultrassonografia Morfológica", "Ultrassom morfológico do segundo trimestre", TipoProcedimento.ULTRASSONOGRAFIA, TrimestreGestacional.SEGUNDO, 20, 24, true),
            proc("Teste de Tolerância à Glicose (TOTG)", "Diagnóstico de diabetes gestacional", TipoProcedimento.EXAME, TrimestreGestacional.SEGUNDO, 24, 28, true),
            proc("Vacina dTpa", "Vacina difteria, tétano e coqueluche acelular", TipoProcedimento.VACINA, TrimestreGestacional.TERCEIRO, 27, 36, true),
            proc("Vacina Influenza", "Vacina contra influenza (gripe sazonal)", TipoProcedimento.VACINA, null, 1, 40, true),
            proc("Vacina Hepatite B", "Vacina contra hepatite B, quando indicada", TipoProcedimento.VACINA, TrimestreGestacional.PRIMEIRO, 1, 13, false),
            proc("Orientação sobre Sinais de Alerta", "Educação sobre situações de emergência", TipoProcedimento.ORIENTACAO, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Orientação sobre Alimentação", "Educação nutricional durante a gestação", TipoProcedimento.ORIENTACAO, TrimestreGestacional.PRIMEIRO, 1, 13, true),
            proc("Orientação sobre Aleitamento Materno", "Educação em saúde sobre amamentação", TipoProcedimento.ORIENTACAO, TrimestreGestacional.TERCEIRO, 28, 40, true),
            proc("Planejamento do Parto", "Discussão e elaboração do plano de parto", TipoProcedimento.CONSULTA, TrimestreGestacional.TERCEIRO, 34, 40, true),
            proc("Consulta de Retorno", "Consultas periódicas de acompanhamento", TipoProcedimento.CONSULTA, null, 1, 40, true)
        ));
    }

    private ProcedimentoPreNatal proc(String nome, String descricao, TipoProcedimento tipo,
            TrimestreGestacional trimestre, Integer semanaInicial, Integer semanaFinal, boolean obrigatorio) {
        return ProcedimentoPreNatal.builder()
            .nome(nome).descricao(descricao).tipo(tipo)
            .trimestreRecomendado(trimestre)
            .semanaInicialRecomendada(semanaInicial)
            .semanaFinalRecomendada(semanaFinal)
            .obrigatorio(obrigatorio).ativo(true)
            .build();
    }
}
