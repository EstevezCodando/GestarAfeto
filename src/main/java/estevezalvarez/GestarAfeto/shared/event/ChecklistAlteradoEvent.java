package estevezalvarez.GestarAfeto.shared.event;

/**
 * Publicado pelo dominio de checklist sempre que o estado do checklist de uma gestante muda.
 *
 * <p>O evento carrega apenas o identificador: quem reage a ele busca os dados de que precisa.
 * Isso mantem o dominio de checklist sem qualquer conhecimento sobre alertas.</p>
 */
public record ChecklistAlteradoEvent(Long gestanteId) {
}
