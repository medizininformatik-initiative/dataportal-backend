package de.medizininformatikinitiative.dataportal.backend.terminology.es.repository;

import de.medizininformatikinitiative.dataportal.backend.terminology.es.model.OntologyItemDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@ConditionalOnExpression("${app.elastic.enabled}")
public interface OntologyItemEsRepository extends ElasticsearchRepository<OntologyItemDocument, String> {}