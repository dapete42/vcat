package org.toolforge.vcat.params;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public class TitleNamespaceParam implements Serializable {

    @Serial
    private static final long serialVersionUID = -8613491816971451993L;

    private final int namespace;

    private final String title;

    public TitleNamespaceParam(String title) {
        this(title, 0);
    }

    public TitleNamespaceParam(String title, int namespace) {
        this.title = title;
        this.namespace = namespace;
    }

}
