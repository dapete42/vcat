package org.toolforge.vcat.params;

public enum Relation {

    Category, Subcategory;

    public static Relation valueOfIgnoreCase(String name) {
        for (Relation format : values()) {
            if (format.name().equalsIgnoreCase(name)) {
                return format;
            }
        }
        return null;
    }

}
