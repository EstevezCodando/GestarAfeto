package estevezalvarez.GestarAfeto.deploy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste basico de implantacao: valida, sem precisar de cluster, as regras que fazem a diferenca
 * em producao (limites de recurso, probes, usuario nao privilegiado, coerencia entre Services
 * e workloads, ausencia de segredos versionados).
 *
 * <p>Complementa o {@code kubeconform} do CI, que valida o <i>esquema</i> dos manifests; aqui
 * se valida a <i>politica</i> do projeto.</p>
 */
class ManifestosKubernetesTest {

    private static final Path RAIZ = Path.of("").toAbsolutePath();
    private static final List<Map<String, Object>> RECURSOS = new ArrayList<>();

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void carregar() throws IOException {
        List<Path> arquivos = new ArrayList<>();
        for (String dir : new String[] {"k8s/base", "observability"}) {
            try (Stream<Path> s = Files.list(RAIZ.resolve(dir))) {
                s.filter(p -> p.toString().endsWith(".yaml"))
                    .filter(p -> !p.getFileName().toString().equals("kustomization.yaml"))
                    .filter(p -> !p.getFileName().toString().startsWith("loki-config"))
                    .forEach(arquivos::add);
            }
        }
        assertFalse(arquivos.isEmpty(), "nenhum manifest encontrado a partir de " + RAIZ);
        for (Path arquivo : arquivos) {
            try (Reader r = Files.newBufferedReader(arquivo)) {
                for (Object doc : new Yaml().loadAll(r)) {
                    if (doc instanceof Map<?, ?> m) {
                        RECURSOS.add((Map<String, Object>) m);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ utilitarios

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object o) {
        return o instanceof Map<?, ?> ? (Map<String, Object>) o : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object o) {
        return o instanceof List<?> ? (List<Map<String, Object>>) o : List.of();
    }

    private static String tipo(Map<String, Object> r) {
        return String.valueOf(r.get("kind"));
    }

    private static String nome(Map<String, Object> r) {
        return String.valueOf(mapa(r.get("metadata")).get("name"));
    }

    private static List<Map<String, Object>> deTipo(String... tipos) {
        Set<String> aceitos = Set.of(tipos);
        return RECURSOS.stream().filter(r -> aceitos.contains(tipo(r))).collect(Collectors.toList());
    }

    private static Map<String, Object> podSpec(Map<String, Object> workload) {
        return mapa(mapa(mapa(workload.get("spec")).get("template")).get("spec"));
    }

    private static List<Map<String, Object>> containers(Map<String, Object> workload) {
        return lista(podSpec(workload).get("containers"));
    }

    // ------------------------------------------------------------------------ testes

    @Test
    void existemOsWorkloadsEsperados() {
        Set<String> nomes = deTipo("Deployment", "StatefulSet").stream()
            .map(ManifestosKubernetesTest::nome).collect(Collectors.toSet());

        assertTrue(nomes.containsAll(Set.of(
            "gestarafeto-app", "gestarafeto-alertas", "gestarafeto-frontend",
            "postgres-core", "postgres-alertas", "rabbitmq", "jaeger", "loki", "grafana")), nomes.toString());
    }

    @Test
    void todoContainerDeclaraRequestsELimitsDeCpuEMemoria() {
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            for (Map<String, Object> c : containers(w)) {
                Map<String, Object> recursos = mapa(c.get("resources"));
                for (String grupo : new String[] {"requests", "limits"}) {
                    Map<String, Object> g = mapa(recursos.get(grupo));
                    assertNotNull(g.get("cpu"), nome(w) + ": falta " + grupo + ".cpu");
                    assertNotNull(g.get("memory"), nome(w) + ": falta " + grupo + ".memory");
                }
            }
        }
    }

    @Test
    void todoContainerTemReadinessELivenessProbe() {
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            for (Map<String, Object> c : containers(w)) {
                assertNotNull(c.get("readinessProbe"), nome(w) + ": sem readinessProbe");
                assertNotNull(c.get("livenessProbe"), nome(w) + ": sem livenessProbe");
            }
        }
    }

    @Test
    void servicosJavaTemStartupProbeParaSubidaLentaDaJvm() {
        for (Map<String, Object> w : deTipo("Deployment")) {
            if (nome(w).equals("gestarafeto-app") || nome(w).equals("gestarafeto-alertas")) {
                assertNotNull(containers(w).get(0).get("startupProbe"), nome(w));
            }
        }
    }

    @Test
    void nenhumPodRodaComoRootNemEscalaPrivilegios() {
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            Map<String, Object> pod = mapa(podSpec(w).get("securityContext"));
            assertEquals(Boolean.TRUE, pod.get("runAsNonRoot"), nome(w) + ": runAsNonRoot");
            for (Map<String, Object> c : containers(w)) {
                Map<String, Object> sc = mapa(c.get("securityContext"));
                assertEquals(Boolean.FALSE, sc.get("allowPrivilegeEscalation"), nome(w));
                assertEquals(List.of("ALL"), mapa(sc.get("capabilities")).get("drop"), nome(w));
            }
        }
    }

    /**
     * Regressao de um defeito real: sem isso o Kubernetes injeta GESTARAFETO_ALERTAS_PORT=tcp://...,
     * que o Spring tenta converter para inteiro em server.port e a aplicacao nao sobe.
     */
    @Test
    void servicLinksDesligadosEmTodosOsPods() {
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            assertEquals(Boolean.FALSE, podSpec(w).get("enableServiceLinks"), nome(w));
        }
    }

    @Test
    void imagensProprietariasUsamTagMarcadoraEAsDeTerceirosSaoFixadas() {
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            for (Map<String, Object> c : containers(w)) {
                String imagem = String.valueOf(c.get("image"));
                assertTrue(imagem.contains(":"), nome(w) + ": imagem sem tag: " + imagem);
                assertFalse(imagem.endsWith(":latest"), nome(w) + ": tag latest e proibida");
                if (imagem.startsWith("gestarafeto/")) {
                    assertTrue(imagem.endsWith(":0.0.0"),
                        nome(w) + ": a tag real e definida pelo overlay (kustomize images), nao no base");
                }
            }
        }
    }

    @Test
    void autoscalersApontamParaDeploymentsExistentesComMinimoDeDuasReplicas() {
        Set<String> deployments = deTipo("Deployment").stream()
            .map(ManifestosKubernetesTest::nome).collect(Collectors.toSet());

        List<Map<String, Object>> hpas = deTipo("HorizontalPodAutoscaler");
        assertEquals(2, hpas.size());
        for (Map<String, Object> hpa : hpas) {
            Map<String, Object> spec = mapa(hpa.get("spec"));
            assertTrue(deployments.contains(String.valueOf(mapa(spec.get("scaleTargetRef")).get("name"))), nome(hpa));
            int min = (Integer) spec.get("minReplicas");
            int max = (Integer) spec.get("maxReplicas");
            assertTrue(min >= 2 && max > min, nome(hpa) + ": " + min + ".." + max);
        }
    }

    @Test
    void todoServiceSelecionaAlgumWorkload() {
        List<Map<String, String>> rotulosDosPods = new ArrayList<>();
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            Map<String, Object> labels = mapa(mapa(mapa(w.get("spec")).get("template")).get("metadata"));
            Map<String, String> m = new java.util.HashMap<>();
            mapa(labels.get("labels")).forEach((k, v) -> m.put(k, String.valueOf(v)));
            rotulosDosPods.add(m);
        }
        for (Map<String, Object> svc : deTipo("Service")) {
            Map<String, String> seletor = new java.util.HashMap<>();
            mapa(mapa(svc.get("spec")).get("selector")).forEach((k, v) -> seletor.put(k, String.valueOf(v)));
            assertFalse(seletor.isEmpty(), nome(svc));
            assertTrue(rotulosDosPods.stream().anyMatch(l -> l.entrySet().containsAll(seletor.entrySet())),
                "Service sem workload correspondente: " + nome(svc));
        }
    }

    @Test
    void urlsDoConfigMapApontamParaServicesQueExistem() {
        Set<String> services = deTipo("Service").stream()
            .map(ManifestosKubernetesTest::nome).collect(Collectors.toSet());
        Map<String, Object> dados = deTipo("ConfigMap").stream()
            .filter(c -> nome(c).equals("gestarafeto-config"))
            .map(c -> mapa(c.get("data"))).findFirst().orElseThrow();

        assertTrue(services.contains("postgres-core") && String.valueOf(dados.get("GESTARAFETO_DB_URL")).contains("postgres-core"));
        assertTrue(String.valueOf(dados.get("GESTARAFETO_ALERTAS_DB_URL")).contains("postgres-alertas"));
        assertTrue(services.contains(String.valueOf(dados.get("GESTARAFETO_RABBIT_HOST"))));
        assertTrue(String.valueOf(dados.get("GESTARAFETO_ALERTAS_URL")).contains("gestarafeto-alertas"));
        assertTrue(services.contains("gestarafeto-alertas"));
    }

    @Test
    void nenhumSegredoEVersionadoEOsReferenciadosSaoCriadosPeloScript() throws IOException {
        assertTrue(deTipo("Secret").isEmpty(), "Secret nao pode ser versionado");

        String script = Files.readString(RAIZ.resolve("scripts/k8s-up.sh"));
        Set<String> chavesReferenciadas = new java.util.TreeSet<>();
        for (Map<String, Object> w : deTipo("Deployment", "StatefulSet")) {
            for (Map<String, Object> c : containers(w)) {
                for (Map<String, Object> env : lista(c.get("env"))) {
                    Map<String, Object> ref = mapa(mapa(env.get("valueFrom")).get("secretKeyRef"));
                    if (!ref.isEmpty()) {
                        chavesReferenciadas.add(String.valueOf(ref.get("name")) + "/" + ref.get("key"));
                    }
                }
            }
        }
        assertFalse(chavesReferenciadas.isEmpty());
        for (String chave : chavesReferenciadas) {
            String campo = chave.substring(chave.indexOf('/') + 1);
            assertTrue(script.contains("--from-literal=" + campo + "="),
                "k8s-up.sh nao cria o segredo " + chave);
        }
    }

    @Test
    void bancosERabbitTemVolumePersistente() {
        for (String nome : new String[] {"postgres-core", "postgres-alertas", "rabbitmq", "loki"}) {
            Map<String, Object> sts = deTipo("StatefulSet").stream()
                .filter(w -> nome(w).equals(nome)).findFirst().orElseThrow();
            assertFalse(lista(mapa(sts.get("spec")).get("volumeClaimTemplates")).isEmpty(), nome);
        }
    }

    @Test
    void existeNetworkPolicyDeNegacaoPadraoNoNamespaceDaAplicacao() {
        assertTrue(deTipo("NetworkPolicy").stream().anyMatch(p -> nome(p).equals("default-deny-ingress")));
    }
}
