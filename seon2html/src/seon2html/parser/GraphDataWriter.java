package seon2html.parser;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import seon2html.model.Concept;
import seon2html.model.Ontology.OntoLevel;
import seon2html.model.Package;
import seon2html.model.Relation;

/* Responsible for generate the CSV files to plot a Network Graph of concepts and relations. */
public class GraphDataWriter {
	private static final boolean	FOUND			= false;
	private static final boolean	CORE			= true;
	private static final boolean	DOMAIN			= true;
	private static final boolean	RELATIONS		= true;
	private static final boolean	GENERALIZATIONS	= true;
	private static List<Concept>	concepts		= new ArrayList<Concept>();

	/* Generates all the HTML Seon Pages. */
	public void generateDataFiles(Package seon) {

		// Generating the nodes file
		generateNodesFile();

		// Generating the edges file
		generateEdgesFile();
	}

	/* Reads the SEON Concepts and generates the Nodes CSV File. */
	private void generateNodesFile() {
		System.out.print("\nGenerating the Nodes CSV File: ");
		PrintWriter writer;
		try {
			// Creating the file
			writer = new PrintWriter("./data/nodes.csv", "UTF-8");
			writer.println("Id;Label;Layer;Stereotype;CoreType;UFOType;Ontology");

			// Reaching the Concepts
			List<Concept> allConcepts = Concept.getAllConcepts();

			// Getting the concepts info
			int id = 1;
			for (Concept concept : allConcepts) {
				if (isIncluded(concept)) {
					String cid = String.format("%03d", id);
					String name = concept.getName();
					String layer = concept.getMainOntology().getLevel().name();
					String ster = concept.getStereotype();
					String core = "";
					String found = "";
					String onto = concept.getMainOntology().getShortName();
					//System.out.println(cid + ";" + name + ";" + layer + ";" + ster + ";" + core + ";" + found + ";" + onto);
					writer.println(cid + ";" + name + ";" + layer + ";" + ster + ";" + core + ";" + found + ";" + onto);
					concepts.add(concept);
					id++;
				}
			}
			System.out.println((id - 1) + " concepts");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/* Reads the SEON Concepts and generates the Edges CSV File. */
	private void generateEdgesFile() {
		System.out.print("Generating the Edges CSV File: ");
		PrintWriter writer;
		int count = 0;
		try {
			// Creating the file
			writer = new PrintWriter("./data/edges.csv", "UTF-8");
			writer.println("Source;Target;Type;Id;Relation");

			int id = 1;
			String type = "Directed";
			String reltype;;
			String cid;
			String sid;
			String tid;

			if (RELATIONS) {
				// Getting the concepts' relations info
				reltype = "Relation";
				List<Relation> relations = Relation.getAllRelations();
				for (Relation relation : relations) {
					Concept source = relation.getSource();
					Concept target = relation.getTarget();
					if (isIncluded(source) && isIncluded(target)) {
						cid = String.format("%03d", id);
						sid = String.format("%03d", concepts.indexOf(source) + 1);
						tid = String.format("%03d", concepts.indexOf(target) + 1);
						//System.out.println(sid + ";" + tid + ";" + type + ";" + cid + ";" + reltype);
						writer.println(sid + ";" + tid + ";" + type + ";" + cid + ";" + reltype);
						id++;
					}
				}
				count = id - 1;
				System.out.print(count + " relations; ");
			}

			if (GENERALIZATIONS) {
				// Getting the concepts' generalization info
				reltype = "Generalization";
				for (Concept concept : concepts) {
					sid = String.format("%03d", concepts.indexOf(concept) + 1);
					if (isIncluded(concept)) {
						List<Concept> generals = concept.getGeneralizations();
						for (Concept general : generals) {
							if (isIncluded(general)) {
								cid = String.format("%03d", id);
								tid = String.format("%03d", concepts.indexOf(general) + 1);
								//System.out.println(sid + ";" + tid + ";" + type + ";" + cid + ";" + reltype);
								writer.println(sid + ";" + tid + ";" + type + ";" + cid + ";" + reltype);
								id++;
							}
						}
					}
				}
				count = (id - 1) - count;
				System.out.println(count + " generalizations");
			}

			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/* Verifies if a concept will be included in the files. */
	private boolean isIncluded(Concept concept) {
		OntoLevel level = concept.getMainOntology().getLevel();
		if (level == OntoLevel.FOUNDATIONAL) return FOUND;
		if (level == OntoLevel.CORE) return CORE;
		if (level == OntoLevel.DOMAIN) return DOMAIN;
		return false;
	}

}