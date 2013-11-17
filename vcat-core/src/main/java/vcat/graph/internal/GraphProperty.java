package vcat.graph.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a method is a getter for a value that should be used as a property when including it in Graphviz
 * output. The {@link value()} is used as the key in the property map.
 * 
 * @author Peter Schlömer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GraphProperty {

	/**
	 * @return The name the property should have in the Graphviz output.
	 */
	String value();

}
