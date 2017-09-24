package seon2html.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.change_vision.jude.api.inf.model.IClass;

/* Represents the Concepts with their definitions. */
public class Concept implements Comparable<Concept> {
	private static Map<IClass, Concept>	conceptMap	= new HashMap<IClass, Concept>();
	private String						name;
	private String						definition;
	private String						example;
	private String						stereotype;
	private Ontology					ontology;
	private List<Concept>				generalizations;
	// private List<Relation> relations;
	private IClass						astahClass;

	public Concept(String name, String definition, String stereotype, IClass astahClass) {
		this.name = name;
		this.parseDefintion(definition);
		this.stereotype = stereotype;
		this.generalizations = new ArrayList<Concept>();
		// this.relations = new ArrayList<Relation>();
		this.astahClass = astahClass;
		conceptMap.put(astahClass, this);
	}

	public static List<Concept> getAllConcepts() {
		return new ArrayList<Concept>(conceptMap.values());
	}

	public static Concept getConceptByAstah(IClass aclass) {
		return conceptMap.get(aclass);
	}

	/* Returns the Concept with the full name parameter ("::" separator). */
	public static Concept getConceptByFullName(String fullName) {
		for (Concept concept : conceptMap.values()) {
			if (concept.getAstahClass().getFullName("::").equals(fullName)) {
				return concept;
			}
		}
		return null;
	}

	public String getName() {
		return this.name;
	}

	public String getFullName() {
		return ontology.getMainOntology().getShortName() + "::" + this.name;
	}

	/* Returns the string used for labeling this concept in the html. Ex.: SPO_Artifact+Participation */
	public String getLabel() {
		return ontology.getMainOntology().getShortName() + "_" + name.replace(' ', '+');
	}

	/* Returns the string used for referencing this concept in the html. Ex.: SPO.html#SPO_Artifact+Participation */
	public String getReference() {
		String sname = ontology.getMainOntology().getShortName();
		return sname + ".html#" + sname + "_" + name.replace(' ', '+');
	}

	/* Returns the full path (w.r.t Astah) of the concept. */
	public String getPath() {
		return this.ontology.getPath() + this.name;
	}

	public String getDefinition() {
		return this.definition;
	}

	public String getExample() {
		return this.example;
	}

	public String getStereotype() {
		return this.stereotype;
	}

	public void setOntology(Ontology onto) {
		this.ontology = onto;
	}

	public Ontology getOntology() {
		return ontology;
	}

	public Ontology getMainOntology() {
		return ontology.getMainOntology();
	}

	public List<Concept> getGeneralizations() {
		return this.generalizations;
	}

	public void addGeneralization(Concept concept) {
		this.generalizations.add(concept);
	}

	public List<Relation> getRelations() {
		return Relation.getRelationsByConcept(this);
	}
	//
	// public void addRelation(Relation relation) {
	// this.relations.add(relation);
	// }

	public IClass getAstahClass() {
		return astahClass;
	}

	private void parseDefintion(String fullDefinition) {
		if (fullDefinition != null) {
			int atPos = fullDefinition.indexOf("@Ex.:");
			if (atPos >= 0) {
				this.definition = fullDefinition.substring(0, atPos - 1);
				this.example = fullDefinition.substring(atPos + 5);
			} else {
				this.definition = fullDefinition;
			}
		}
	}

	@Override
	public String toString() {
		// return "[CONCEPT] " + name + ": " + definition + " (" + example + ")";
		return "    C:" + name;
	}

	/* Equals method. Comparing name and ontology name. */
	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			Concept concept = (Concept) obj;
			// TODO: find a better comparing criterion.
			if (name.equals(concept.name) && ontology.equals(concept.ontology)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int compareTo(Concept o) {
		return this.name.compareTo(o.name);
	}

}
