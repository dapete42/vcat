package org.toolforge.vcat.graph;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GroupRank {

    None("none"),
    Max("max"),
    Min("min"),
    Same("same");

    private final String value;

}
