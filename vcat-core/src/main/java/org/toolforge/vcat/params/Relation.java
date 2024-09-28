package org.toolforge.vcat.params;

import org.jspecify.annotations.Nullable;

public enum Relation {

    Category, Subcategory;

    @Nullable
    public static Relation valueOfIgnoreCase(String name) {
        for (Relation format : values()) {
            if (format.name().equalsIgnoreCase(name)) {
                return format;
            }
        }
        return null;
    }

}
