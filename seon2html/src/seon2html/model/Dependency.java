package seon2html.model;

/* Represents the Dependency relation from one Package to another. */
public class Dependency {
	private Package	source;
	private Package	target;
	private String	description;
	private String	level;

	public Dependency(Package source, Package target, String description, String level) {
		this.source = source;
		this.target = target;
		this.description = description;
		this.level = level;
	}

	public Package getSource() {
		return this.source;
	}

	public Package getTarget() {
		return this.target;
	}

	public String getDescription() {
		return this.description;
	}

	public String getLevel() {
		return this.level;
	}

	public String toString() {
		return "D: " + source.getName() + "-->" + target.getName();
	}

}