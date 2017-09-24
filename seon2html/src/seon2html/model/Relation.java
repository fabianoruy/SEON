package seon2html.model;

import java.util.ArrayList;
import java.util.List;

/* Represents the Relations between Concepts. */
public class Relation {
	private static List<Relation>	relationsList	= new ArrayList<Relation>();
	private String					name;
	private String					definition;
	private String					stereotype;
	private boolean					composition;
	private Package					pack;
	private Concept					source;
	private Concept					target;
	private String					sourceMult;
	private String					targetMult;

	public Relation(String name, String definition, String stereotype, boolean composition, Package pack, Concept source, Concept target, String smult, String tmult) {
		this.name = name;
		this.definition = definition;
		this.stereotype = stereotype;
		this.composition = composition;
		this.pack = pack;
		this.source = source;
		this.target = target;
		this.sourceMult = smult;
		this.targetMult = tmult;
		if (!relationsList.contains(this)) relationsList.add(this);
	}

	public static List<Relation> getAllRelations() {
		return relationsList;
	}

	public static List<Relation> getRelationsByConcept(Concept concept) {
		List<Relation> srelations = new ArrayList<Relation>();
		List<Relation> trelations = new ArrayList<Relation>();
		// Getting by source and target (separated for ordering, and to avoid self-relation repetition)
		for (Relation relation : relationsList) {
			if (relation.source.equals(concept)) {
				srelations.add(relation);
			} else if (relation.target.equals(concept)) {
				trelations.add(relation);
			}
		}
		srelations.addAll(trelations);
		return srelations;
	}

	public static List<Relation> getRelationsBySource(Concept concept) {
		List<Relation> relations = new ArrayList<Relation>();
		for (Relation relation : relationsList) {
			if (relation.source.equals(concept)) {
				relations.add(relation);
			}
		}
		return relations;
	}

	protected static List<Relation> getRelationsByOntology(Ontology onto) {
		List<Relation> relations = new ArrayList<Relation>();
		for (Relation relation : relationsList) {
			if (relation.pack != null) { //TODO solve how to set something != null.
				if (relation.pack.getMainOntology().equals(onto)) {
					relations.add(relation);
				}
			} else {
				System.out.println(relation + " has no parent!");
			}
		}
		return relations;
	}

	public String getName() {
		return this.name;
	}

	// public String getDefinition() {
	// return definition;
	// }
	//
	// public String getStereotype() {
	// return this.stereotype;
	// }

	public Concept getSource() {
		return this.source;
	}

	public Concept getTarget() {
		return this.target;
	}

	public boolean isComposition() {
		return composition;
	}

	// public String getSourceMult() {
	// return sourceMult;
	// }
	//
	// public String getTargetMult() {
	// return targetMult;
	// }

	/* Equals method. Comparing concepts and relation name. */
	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			Relation relation = (Relation) obj;
			// TODO: find a better comparing criterion.
			if (name.equals(relation.name) && source.equals(relation.source) && target.equals(relation.target)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		String rname = name;
		String smult = " ";
		String tmult = " ";
		String ster = "";
		String def = "";
		if (composition) rname = "<>--" + name;
		if (!sourceMult.isEmpty()) smult = " (" + sourceMult + ") ";
		if (!targetMult.isEmpty()) tmult = " (" + targetMult + ") ";
		if (stereotype != null) ster = "  &lt&lt" + stereotype + "&gt&gt";
		if (definition.length() > 0) def = " [" + def + "]";
		return source.getName() + smult + rname + tmult + target.getName() + ster + def;
	}

}