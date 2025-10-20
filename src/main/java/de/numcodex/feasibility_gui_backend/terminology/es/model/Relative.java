package de.numcodex.feasibility_gui_backend.terminology.es.model;

import lombok.Builder;
import org.springframework.data.elasticsearch.annotations.Field;

@Builder
public record Relative(
    Display display,
    boolean selectable,
    @Field(name = "contextualized_termcode_hash")
    String contextualizedTermcodeHash
) {
}
