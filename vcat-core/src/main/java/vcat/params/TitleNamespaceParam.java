package vcat.params;

import java.io.Serializable;

public class TitleNamespaceParam implements Serializable {

	private static final long serialVersionUID = -5233638242636432181L;

	private final int namespace;

	private final String title;

	public TitleNamespaceParam(String title) {
		this(title, 0);
	}

	public TitleNamespaceParam(String title, int namespace) {
		this.title = title;
		this.namespace = namespace;
	}

	public int getNamespace() {
		return namespace;
	}

	public String getTitle() {
		return title;
	}

}
