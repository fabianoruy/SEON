package seon2html.model;

import java.util.ArrayList;
import java.util.List;

import com.change_vision.jude.api.inf.model.IPackage;

/* Package representing an Ontology/Subontology. */
public class Ontology extends Package {
	private String			fullName;
	private String			shortName;
	private List<Concept>	concepts;

	public static enum OntoLevel {
		FOUNDATIONAL(1), CORE(1), DOMAIN(2);

		private final int value;

		private OntoLevel(final int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public Ontology(String name, String fullName, String shortName, String definition, PackType type, int order, IPackage astahPack) {
		super(name, definition, type, order, astahPack);
		this.fullName = fullName;
		this.shortName = shortName;
		this.concepts = new ArrayList<Concept>();
	}

	public String getFullName() {
		return this.fullName;
	}

	public String getShortName() {
		return this.shortName;
	}

	public List<Concept> getConcepts() {
		return this.concepts;
	}

	public void addConcept(Concept concept) {
		this.concepts.add(concept);
	}

	public List<Relation> getRelations() {
		return Relation.getRelationsByOntology(this);
	}

	/* Equals method. Comparing ontology full name. */
	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			Ontology ontology = (Ontology) obj;
			// TODO: find a better comparing criterion.
			if (fullName.equals(ontology.fullName)) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		// return "[" + type + "] " + name + " ("+ shortName +"): [" + definition + "]";
		return super.toString();
	}
}