package seon2html.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.change_vision.jude.api.inf.model.IPackage;

import seon2html.model.Ontology.OntoLevel;

/* Represents the model packages, including levels, ontologies and subontologies. */
public class Package implements Comparable<Package> {
	private static Map<IPackage, Package>	packageMap	= new HashMap<IPackage, Package>();
	private String							name;
	private String							definition;
	private PackType						type;
	private int								order;
	private Package							parent;
	private List<Package>					subpacks;
	private List<Dependency>				dependencies;
	private List<Diagram>					diagrams;
	private IPackage						astahPack;

	public static enum PackType {
		NETWORK, SUBNETWORK, LEVEL, PACKAGE, ONTOLOGY, SUBONTOLOGY, IGNORE
	}

	public Package(String name, String definition, PackType type, int order, IPackage astahPack) {
		this.name = name;
		this.definition = definition;
		this.type = type;
		this.order = order;
		this.subpacks = new ArrayList<Package>();
		this.dependencies = new ArrayList<Dependency>();
		this.diagrams = new ArrayList<Diagram>();
		this.astahPack = astahPack;
		packageMap.put(astahPack, this);
	}

	public static List<Package> getAllPackages() {
		return new ArrayList<Package>(packageMap.values());
	}

	public static Package getPackageByAstah(IPackage apack) {
		return packageMap.get(apack);
	}

	/* Returns the Package with the full name parameter ("::" separator). */
	public static Package getPackageByFullName(String fullName) {
		for (Package pack : packageMap.values()) {
			if (pack.getAstahPack().getFullName("::").equals(fullName)) {
				return pack;
			}
		}
		return null;
	}

	public static PackType getPackType(String type) {
		if (type != null) {
			switch (type) {
			case "Level":
				return PackType.LEVEL;
			case "Subnetwork":
				return PackType.SUBNETWORK;
			case "Package":
				return PackType.PACKAGE;
			case "Ontology":
				return PackType.ONTOLOGY;
			case "Subontology":
				return PackType.SUBONTOLOGY;
			case "Ignore":
				return PackType.IGNORE;
			}
		}
		return PackType.PACKAGE;
	}

	public String getName() {
		return this.name;
	}

	public String getDefinition() {
		return this.definition;
	}

	public PackType getType() {
		return this.type;
	}

	public int getOrder() {
		return this.order;
	}

	public void setParent(Package parent) {
		this.parent = parent;
	}

	public Package getParent() {
		return this.parent;
	}

	public List<Package> getPacks() {
		return this.subpacks;
	}

	public void addPack(Package pack) {
		this.subpacks.add(pack);
	}

	public List<Dependency> getDependencies() {
		return this.dependencies;
	}

	public void addDependency(Dependency depend) {
		this.dependencies.add(depend);
	}

	public List<Diagram> getDiagrams() {
		return this.diagrams;
	}

	public void addDiagram(Diagram diag) {
		this.diagrams.add(diag);
	}

	public IPackage getAstahPack() {
		return astahPack;
	}

	/* Get the Package's Level. */
	public OntoLevel getLevel() {
		Package pack = this;
		while (pack.type != PackType.LEVEL && pack.type != PackType.NETWORK) {
			pack = pack.parent;
		}
		if (pack.getName().contains("Foundational")) return OntoLevel.FOUNDATIONAL;
		else if (pack.getName().contains("Core")) return OntoLevel.CORE;
		else if (pack.getName().contains("Domain")) return OntoLevel.DOMAIN;
		return null;
	}

	public String getPath() {
		String path = "";
		Package pack = this;
		while (pack.parent != null) {
			path = pack.name + "/" + path;
			pack = pack.parent;
		}
		return path;
	}

	/* Returns the string used for labeling this package in the html. Ex.: SPO_Standard+Process+Structure */
	public String getLabel() {
		Ontology onto = this.getMainOntology();
		String sname = name.replace(' ', '+');
		if (onto != null) {
			sname = onto.getShortName() + "_" + sname;
		}
		return sname;
	}

	/* Returns the string used for referencing this package in the html. Ex.: SPO.html#SPO_Standard+Process+Structure */
	public String getReference() {
		Ontology onto = this.getMainOntology();
		String sname = this.name.replace(' ', '+');
		if (onto != null) {
			sname = onto.getShortName() + ".html#" + onto.getShortName() + "_" + sname;
		}
		return sname;
	}

	public Ontology getMainOntology() {
		Package pack = this;
		while (pack != null && pack.type != PackType.ONTOLOGY) {
			pack = pack.getParent();
		}
		return (Ontology) pack;
	}

	public int getDeepLevel() {
		int level = 0;
		Package pack = this;
		while (pack.parent != null) {
			pack = pack.parent;
			level++;
		}
		return level;
	}

	/* Gets all concepts of the Package, including subpackages. */
	public List<Concept> getAllConcepts() {
		List<Concept> concepts = new ArrayList<Concept>();
		if (this instanceof Ontology) {
			concepts.addAll(((Ontology) this).getConcepts());
		}
		for (Package pack : this.subpacks) {
			concepts.addAll(pack.getAllConcepts());
		}
		return concepts;
	}

	@Override
	public String toString() {
		// return "[" + type + "] " + name + ": " + definition;
		return "[" + type.toString().charAt(0) + "] " + name + " (" + this.getDeepLevel() + ")";
	}
	
	/* Equals method. Comparing the package full namespace. */
	@Override
	public boolean equals(Object obj) {
		if (obj != null) {
			Package pack = (Package) obj;
			if (astahPack.getFullNamespace("::").equals(pack.getAstahPack().getFullNamespace("::"))) {
				return true;
			}
		}
		return false;
	}


	@Override
	public int compareTo(Package pack) {
		return getStrength(this) - getStrength(pack);
	}

	/* Calculates the precedence of a package by level and informed order. */
	private int getStrength(Package pack) {
		int high, low = 0;
		if (pack.getLevel() == OntoLevel.DOMAIN) high = (int) Math.pow(100, 3);
		else if (pack.getLevel() == OntoLevel.CORE) high = (int) Math.pow(100, 2);
		else if (pack.getLevel() == OntoLevel.FOUNDATIONAL) high = (int) Math.pow(100, 1);
		else high = 0;
		low = pack.order;
		return high + low;
	}

}
