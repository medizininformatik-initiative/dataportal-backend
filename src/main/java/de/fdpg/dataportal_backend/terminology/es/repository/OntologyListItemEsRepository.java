package de.fdpg.dataportal_backend.terminology.es.repository;

import de.fdpg.dataportal_backend.terminology.es.model.OntologyListItemDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@ConditionalOnExpression("${app.elastic.enabled}")
public interface OntologyListItemEsRepository extends ElasticsearchRepository<OntologyListItemDocument, String> {
}
