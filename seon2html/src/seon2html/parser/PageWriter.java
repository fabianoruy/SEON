package seon2html.parser;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
//import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;

import seon2html.model.*;
import seon2html.model.Package;
import seon2html.model.Package.PackType;
import seon2html.model.Ontology.OntoLevel;
import seon2html.model.Diagram.DiagType;

/* Responsible for generate the HTML pages from the objects model. */
public class PageWriter {
	private static List<Ontology>	ontologies	= new ArrayList<Ontology>();
	private int						figCount;

	/* Generates all the HTML Seon Pages. */
	public void generateSeonPages(Package seon) {
		System.out.println("\n# Writing the HTML Pages");
		// Generating the content pages
		generateContentPages(seon);
		Collections.sort(ontologies);

		// TODO: Remove
		checkRelations();

		// Generating the main Seon page
		generateSeonPage(seon);

		// Generating the Menu page
		generateMenuPage();

		// Generating the information pages
		generateNetworkGraph(seon);
		generateSearchBase();
		generateStatsPage();

		// Copying the static page files
		recoverStaticPages();
	}

	/* Reads the Seon Model and generates the HTML Pages. */
	private void generateContentPages(Package superpack) {
		// Reaching the Ontologies
		for (Package pack : superpack.getPacks()) {
			if (pack.getType() == PackType.ONTOLOGY) {
				// Generating the Ontologies' Pages
				generateOntologyPage((Ontology) pack);
				ontologies.add((Ontology) pack);
			} else {
				// Recursive call
				generateContentPages(pack);
			}
		}
	}

	/* Prepare the main SEON diagram and put it in the SEON Page. */
	private void generateSeonPage(Package seon) {
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Seon.html");

		// Replacing the tags for the actual values
		Diagram diagram = seon.getDiagrams().get(0); // suposing only one package diagram here.
		html = html.replace("@networkImage", parseImage(diagram));

		// Writing the HTML page
		Utils.stringToFile("./page/SEON.html", html);
	}

	/* Creates a Network view (graph) from the Ontologies and dependencies. */
	private void generateNetworkGraph(Package seon) {
		System.out.println("# Creating the Network Graph");
		// Reading the HTML template
		String code = Utils.fileToString("./resources/Template.NetworkCode.js");

		// Excluding non-leveled ontologies
		List<Ontology> nwOntos = new ArrayList<Ontology>();
		for (Ontology onto : ontologies) {
			if (onto.getLevel() != null) {
				nwOntos.add(onto);
			}
		}

		// Building the ontology nodes
		String nodes = "";
		int diff = 100;
		double factor = 1.25;
		int id = 0;
		for (Ontology ontology : nwOntos) {
			String name = ontology.getShortName();
			int level = 3; // domain
			String color = "#e1e1d0"; // neutral
			if (ontology.getLevel() == OntoLevel.DOMAIN) {
				color = "#ffff99"; // yellow
			} else if (ontology.getLevel() == OntoLevel.CORE) {
				level = 2;
				color = "#99ff99"; // green
			} else if (ontology.getLevel() == OntoLevel.FOUNDATIONAL) {
				level = 1;
				color = "#99ffff"; // blue
			} else {
				continue;
			}
			// {data: {id:'0', name:'UFO', dim:92, level:1, color:'#99ffff'}},
			nodes += "  {data: {id:'" + id + "', name:'" + name + "', dim:" + (diff + (int) (factor * ontology.getAllConcepts().size())) + ", level:" + level + ", color:'" + color
					+ "'}},\n";
			id++;
		}
		// neutral nodes (examples)
		nodes += "  {data: {id:'" + (id++) + "', name:'DocO', dim:" + diff + ", level:3, color:'#e1e1d0'}},\n";
		// nodes += " {data: {id:'" + (id++) + "', name:'RSMO', dim:" + diff + ", level:3, color:'#e1e1d0'}},\n";
		// for (int i = 0; i < 2; i++) {
		// nodes += " {data: {id:'" + (id + i) + "', name:'DO" + (i + 1) + "', dim:" + diff + ", level:3,
		// color:'#e1e1d0'}},\n";
		// }

		// Building the dependency edges
		int[][] depMatrix = buildDependenciesMatrix(nwOntos);
		int max = 0;
		for (int i = 0; i < depMatrix.length; i++) {
			for (int j = 0; j < depMatrix.length; j++) {
				if (i != j && depMatrix[i][j] > max) max = depMatrix[i][j]; // finding the max relations for normalizing
			}
		}

		String edges = "";
		id = 0;
		for (int i = 0; i < depMatrix.length; i++) {
			for (int j = 0; j < depMatrix.length; j++) {
				int weight = depMatrix[i][j];
				if (i != j && weight > 0) {
					int thickness = (int) Math.ceil((20.0 / max) * weight); // normalizing thickness (max:20)
					// {data: {id:'e0', thickness:20, weight:33, source:'1', target:'0'}},
					edges += "  {data: {id:'e" + id + "', thickness:" + thickness + ", weight:" + weight + ", source:'" + i + "', target:'" + j + "'}},\n";
					id++;
					System.out.println(nwOntos.get(i).getShortName() + " --> " + nwOntos.get(j).getShortName() + " (" + weight + ")");
				}
			}
		}

		// Replacing the tags
		code = code.replace("@nodes", nodes);
		code = code.replace("@edges", edges);

		// Writing the JS code
		Utils.stringToFile("./page/networkCode.js", code);
	}

	/* Builds the Dependencies' matrix (using concepts relations - better way). */
	private int[][] buildDependenciesMatrix(List<Ontology> ontos) {
		int[][] matrix = new int[ontos.size()][ontos.size()];
		// for each ontology
		for (int i = 0; i < ontos.size(); i++) {
			// System.out.println(ontos.get(i));
			// get the concepts and their relations (including generalizations)
			for (Concept concept : ontos.get(i).getAllConcepts()) {
				// find the ontology of each relation target
				for (Relation relation : Relation.getRelationsBySource(concept)) {
					int j = ontos.indexOf(relation.getTarget().getMainOntology());
					// increase a relation between the ontologies.
					matrix[i][j] += 1; // factor 1
					if (ontos.get(i).getLevel().getValue() < relation.getTarget().getMainOntology().getLevel().getValue()) {
						System.out.println("!! (" + ontos.get(i).getName() + "-->" + relation.getTarget().getMainOntology().getName() + ") " + relation);
					}
				}
				// find the ontology of each generalization
				for (Concept general : concept.getGeneralizations()) {
					int j = ontos.indexOf(general.getMainOntology());
					// increase a generalization between the ontologies.
					matrix[i][j] += 1; // factor 1
				}
			}
		}
		return matrix;
	}

	/* Builds the Dependencies' matrix (using dependency levels - simple way). */
	@Deprecated
	private int[][] buildDependenciesMatrix0(List<Ontology> ontos) {
		int[][] matrix = new int[ontos.size()][ontos.size()];
		// for each ontology
		for (int i = 0; i < ontos.size(); i++) {
			// get their dependencies
			for (Dependency depend : ontos.get(i).getDependencies()) {
				int weight = 1;
				if (depend.getLevel().equals("High")) weight = 6;
				if (depend.getLevel().equals("Medium")) weight = 4;
				if (depend.getLevel().equals("Low")) weight = 2;
				int j = ontos.indexOf(depend.getTarget());
				// set the dependence weight between the ontologies.
				matrix[i][j] = weight;
			}
		}
		return matrix;
	}

	/* Reads the Ontologies names and creates the Menu page. */
	private void generateMenuPage() {
		String MENULINE = "<p><a href=\"@onto.html\">@ontology</a></p>";
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Menu.html");

		// Replacing the tags for the actual values
		String core = "";
		String domain = "";
		String found = "";
		for (Ontology ontology : ontologies) {
			OntoLevel level = ontology.getLevel();
			if (level != null) {
				String line = MENULINE;
				line = line.replace("@ontology", ontology.getShortName() + " - " + ontology.getFullName());
				line = line.replace("@onto", ontology.getShortName());
				if (level == OntoLevel.FOUNDATIONAL) found += line + "\n";
				else if (level == OntoLevel.CORE) core += line + "\n";
				else if (level == OntoLevel.DOMAIN) domain += line + "\n";
				// other level: ignore
			}
		}
		html = html.replace("@foundOntology", found);
		html = html.replace("@coreOntologies", core);
		html = html.replace("@domainOntologies", domain);
		html = html.replace("@version", "SEON Version " + SeonParser.VERSION);
		html = html.replace("@date", (new Date()).toString());

		// Writing the HTML page
		Utils.stringToFile("./page/menu.html", html);
	}

	/* Reads the Network and creates the Stats page. */
	private void generateStatsPage() {
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Stats.html");

		// Replacing the tags for the actual values
		String found = "";
		String core = "";
		String domain = "";
		int[] ontoDeps = new int[ontologies.size()];
		int[] ontoGens = new int[ontologies.size()];
		// Sorting the Ontologies by level and size.
		// Collections.sort(ontologies, Ontology.getLevelComparator());

		for (Ontology ontology : ontologies) {
			int relats = 0;
			int depends = 0;
			int generals = 0;
			Arrays.fill(ontoDeps, 0);
			Arrays.fill(ontoGens, 0);
			OntoLevel level = ontology.getLevel();
			List<Concept> concepts = ontology.getAllConcepts();
			for (Concept concept : concepts) {
				for (Relation relation : Relation.getRelationsBySource(concept)) {
					Ontology ontoTarget = relation.getTarget().getMainOntology();
					if (ontology.equals(ontoTarget)) {
						relats++;
					} else {
						depends++;
						ontoDeps[ontologies.indexOf(ontoTarget)]++;
					}
				}
				for (Concept general : concept.getGeneralizations()) {
					Ontology ontoTarget = general.getMainOntology();
					if (!ontology.equals(ontoTarget)) {
						generals++;
						ontoGens[ontologies.indexOf(ontoTarget)]++;
					}
				}
			}
			String allDeps = "";
			if (depends > 0) {
				allDeps = "(";
				for (int i = 0; i < ontoDeps.length; i++) {
					if (ontoDeps[i] > 0) {
						allDeps += ontologies.get(i).getShortName() + ":" + ontoDeps[i] + ", ";
					}
				}
				allDeps = allDeps.substring(0, allDeps.length() - 2) + ")";
			}
			String allGens = "";
			if (generals > 0) {
				allGens = "(";
				for (int i = 0; i < ontoGens.length; i++) {
					if (ontoGens[i] > 0) {
						allGens += ontologies.get(i).getShortName() + ":" + ontoGens[i] + ", ";
					}
				}
				allGens = allGens.substring(0, allGens.length() - 2) + ")";
			}

			String stats = "<div class=\"ontology\"><p style=\"margin:1px; font: bold 80% sans-serif\">" + ontology.getShortName() + " - " + ontology.getFullName() + "</p>\n";
			stats += "<code>" + concepts.size() + " concepts<br/>\n";
			stats += relats + " internal relations<br/>\n";
			stats += generals + " external generalizations " + allGens + "<br/>\n";
			stats += depends + " external dependencies " + allDeps + "</code>\n</div>\n";

			if (level != null) {
				if (level == OntoLevel.FOUNDATIONAL) found += stats;
				else if (level == OntoLevel.CORE) core += stats;
				else if (level == OntoLevel.DOMAIN) domain += stats;
				// other level: ignore
			}
		}
		html = html.replace("@foundOntology", found);
		html = html.replace("@coreOntologies", core);
		html = html.replace("@domainOntologies", domain);
		html = html.replace("@date", (new Date()).toString());

		// Writing the HTML page
		Utils.stringToFile("./page/NetworkStats.html", html);
	}

	/* Prints the Ontologies' pages. */
	private void generateOntologyPage(Ontology onto) {
		// Reading the HTML template
		String html = Utils.fileToString("./resources/Template.Page.html");

		///// Replacing the tags for the actual values /////
		// Page Introduction
		html = html.replace("@title", onto.getFullName() + " (" + onto.getShortName() + ")");
		html = html.replace("@description", formatDescription(onto.getDefinition()));

		// Ontology Dependencies
		html = html.replace("@ontologyDependencies", generateDependenciesTable(onto));

		// Models Sections (subpackages/subontologies)
		figCount = 1;
		String ontoDiags = generateDiagramStructures(onto); // ontology root diagrams
		String ontoPacks = generateSectionStructures(onto, "3."); // subpackages and their diagrams
		html = html.replace("@sectionContent", ontoDiags + ontoPacks);

		// Concepts Table
		html = html.replace("@conceptDefinitions", generateConceptsTable(onto));

		// Detailed Concepts List
		html = html.replace("@detailedConcepts", generateDetailedConcepts(onto));

		// Ontology Short Name and Generation Time
		html = html.replace("@onto", onto.getShortName());
		html = html.replace("@date", (new Date()).toString());

		// Writing the HTML page
		Utils.stringToFile("./page/" + onto.getShortName() + ".html", html);
	}

	/* Generates the file with the concepts and definitions for the Search. */
	private void generateSearchBase() {
		String conceptsHash = "var concepts = {\n";
		List<Concept> concepts = Concept.getAllConcepts();
		Collections.sort(concepts);
		for (Concept concept : concepts) {
			String definition = concept.getDefinition().replaceAll("\'", "").replaceAll("\n", ". ");
			conceptsHash += "'" + concept.getName() + "': {'def': '" + definition + "', 'ref': '" + concept.getReference() + "_detail'},\n";
		}
		conceptsHash += "};";
		Utils.stringToFile("./page/ConceptsHash.js", conceptsHash);
		// TODO: improve sorting
		// TODO: find bad chars
	}

	/* Generates the lines of the dependencies table. */
	private String generateDependenciesTable(Ontology onto) {
		String DEPENDSLINE = "<tr><td><a href=\"@onto.html\">@ontology</a></td><td>@description</td><td style=\"text-align:center\">@level</td></tr>";
		String dependsTable = "";
		for (Dependency depend : onto.getDependencies()) {
			Ontology supplier = (Ontology) depend.getTarget();
			String line = DEPENDSLINE;
			line = line.replace("@ontology", supplier.getShortName() + " - " + supplier.getFullName());
			line = line.replace("@onto", supplier.getShortName());
			line = line.replace("@description", depend.getDescription().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>"));
			line = line.replace("@level", depend.getLevel());
			dependsTable += line + "\n";
		}
		return dependsTable;
	}

	/* Generates the sections' structures of an ontology. */
	private String generateSectionStructures(Package superpack, String snum) {
		String SECTIONSTRUCT = "\n<h3><a name=\"@sectionref\">@snum @section</a></h3>\n<p align=\"justify\">@intro</p>\n@packdiagrams";
		int num = 1;
		String sectionStructures = "";
		List<Package> packs = superpack.getPacks();
		Collections.sort(packs);
		for (Package pack : packs) {
			String struct = SECTIONSTRUCT;
			struct = struct.replace("@snum", snum + num + ". ");
			struct = struct.replace("@sectionref", pack.getLabel() + "_section");
			struct = struct.replace("@section", pack.getName());
			struct = struct.replace("@intro", formatDescription(pack.getDefinition()));
			struct = struct.replace("@packdiagrams", generateDiagramStructures(pack));
			int diff = pack.getDeepLevel() - pack.getMainOntology().getDeepLevel();
			if (diff > 1) {
				struct = struct.replace("h3>", "h" + (2 + diff) + ">");
			}
			// recursive call
			sectionStructures += struct + generateSectionStructures(pack, (snum + num + "."));
			num++;
		}
		return sectionStructures;
	}

	/* Generates the diagrams' structures of a single package. */
	private String generateDiagramStructures(Package pack) {
		String DIAGRAMSTRUCT = "<p>@intro</p>\n<p align=\"center\">@image</p>\n<p align=\"center\"><b>@flabel</b></p>\n<p align=\"justify\">@description</p>\n";
		String diagramStructs = "";
		for (Diagram diag : pack.getDiagrams()) {
			String name = diag.getName();
			String introText;
			String labelText;
			String image;
			if (diag.getType() == DiagType.CONCEPTUALMODEL) {
				introText = "conceptual model of the " + name;
				labelText = name + " conceptual model";
				if (pack.getType() == PackType.SUBONTOLOGY) {
					introText = introText + " subontology";
				}
				image = parseImage(diag);
			} else if (diag.getType() == DiagType.PACKAGE) {
				introText = "packages of the " + name;
				labelText = name;
				image = parseImage(diag);
			} else if (diag.getType() == DiagType.OTHER) {
				introText = name;
				labelText = name;
				image = "<IMG src=\"images/" + diag.getName() + ".png\">";
			} else { // Type == IGNORE
				continue; // do nothing
			}

			String struct = DIAGRAMSTRUCT;
			struct = struct.replace("@intro", "Figure " + figCount + " presents the " + introText + ".");
			struct = struct.replace("@flabel", "Figure " + figCount + ". " + labelText + ".");
			struct = struct.replace("@diagram", name);
			struct = struct.replace("@image", image);
			struct = struct.replace("@description", formatDescription(diag.getDescription()));
			figCount++;
			diagramStructs += struct;
		}
		return diagramStructs;
	}

	/* Creates the html IMG code (defining dimensions) and the MAP code. */
	private String parseImage(Diagram diagram) {
		String image = "<IMG src=\"images/@diagram.png\" width=\"@width\" class=\"map\" usemap=\"#@diagram\">";
		IDiagram aDiagram = diagram.getAstahDiagram();
		image = image.replace("@diagram", diagram.getName());
		image = image.replace("@width", String.valueOf(Math.round(aDiagram.getBoundRect().getWidth())));
		return image + parseMap(diagram);
	}

	/* Reads the elements positions and creates the MAP code. This method reads the Astah model becouse the presentation
	 * information is not considered in the SEON Model. */
	private String parseMap(Diagram diagram) {
		String AREA = "\n<area shape=\"rect\" coords=\"@coords\" href=\"@reference\" title=\"@definition\">";
		String mapcode = "<MAP NAME=\"" + diagram.getName() + "\">";
		IDiagram aDiagram = diagram.getAstahDiagram();
		try {
			// For Conceptual Model diagrams
			if (diagram.getType() == DiagType.CONCEPTUALMODEL) {
				// Getting each Concept (Class) in the diagram and its position.
				for (IPresentation present : aDiagram.getPresentations()) {
					if (present instanceof INodePresentation && present.getType().equals("Class")) {
						INodePresentation node = (INodePresentation) present;
						Concept concept = Concept.getConceptByFullName(((IClass) node.getModel()).getFullName("::"));
						// area for the whole node
						String area = AREA;
						area = area.replace("@coords", getMapCoords(node, aDiagram.getBoundRect()));
						area = area.replace("@reference", concept.getReference());
						area = area.replace("@definition", concept.getDefinition()); // TODO: add the namespace and
																						// concept name
						mapcode += area;
						// TODO
						// area for the over square (to the source package)
						// if (!diagram.getPack().equals(concept.getOntology())) {
						// String areaOver = AREA;
						// areaOver = areaOver.replace("@coords", getMapCoordsOver(node, aDiagram.getBoundRect()));
						// areaOver = areaOver.replace("@reference", concept.getOntology().getReference() + "_section");
						// mapcode += areaOver;
						// }
					}
				}
				// For Package diagrams
			} else if (diagram.getType() == DiagType.PACKAGE) {
				// Selecting only the packages for ordering
				List<IPresentation> presentations = new ArrayList<IPresentation>();
				for (IPresentation present : aDiagram.getPresentations()) {
					if (present.getType().equals("Package")) presentations.add(present);
				}
				// Compares the number of packages to the root (deepless). It is used for showing the deeper MAPs in the
				// last.
				Comparator<IPresentation> comp = new Comparator<IPresentation>() {
					public int compare(IPresentation p1, IPresentation p2) {
						String fname = ((IPackage) p1.getModel()).getFullName(":");
						String fnamed = ((IPackage) p1.getModel()).getFullName("::");
						int packs1 = fnamed.length() - fname.length();
						fname = ((IPackage) p2.getModel()).getFullName(":");
						fnamed = ((IPackage) p2.getModel()).getFullName("::");
						int packs2 = fnamed.length() - fname.length();
						return (packs2 - packs1);
					}
				};
				Collections.sort(presentations, comp);
				// Getting each Package in the diagram and its position (ordered by deep).
				for (IPresentation present : presentations) {
					INodePresentation node = (INodePresentation) present;
					Package pack = Package.getPackageByFullName(((IPackage) node.getModel()).getFullName("::"));
					String area = AREA;
					area = area.replace("@coords", getMapCoords(node, aDiagram.getBoundRect()));
					area = area.replace("@reference", pack.getReference() + "_section");
					area = area.replace("@definition", "");
					mapcode += area;
				}
			}
		} catch (InvalidUsingException e) {
			e.printStackTrace();
		}
		return mapcode + "</MAP>";
	}

	/* Returns the String Coords of a html image MAP. */
	private String getMapCoords(INodePresentation node, Rectangle2D adjust) {
		int x = (int) Math.round(node.getLocation().getX() - adjust.getX());
		int y = (int) Math.round(node.getLocation().getY() - adjust.getY());
		int w = (int) Math.round(node.getWidth());
		int h = (int) Math.round(node.getHeight());
		return "" + x + "," + y + "," + (x + w) + "," + (y + h);
	}

	/* Returns the String Coords of a html image MAP (Left Square). */
	// private String getMapCoordsOver(INodePresentation node, Rectangle2D adjust) {
	// int size = 8;
	// int x = (int) Math.round(node.getLocation().getX() - adjust.getX());
	// int y = (int) Math.round(node.getLocation().getY() - adjust.getY()) - size;
	// int w = size;
	// int h = size;
	// return "" + x + "," + y + "," + (x + w) + "," + (y + h);
	// }

	/* Copies all the (static) files from resources directory to the SEON page directory. */
	private void recoverStaticPages() {
		String source = "./resources/static/";
		String target = "./page/";
		try {
			// static files
			int count = 0;
			File dir = new File(source);
			System.out.println("\nCopying all files in " + dir.getPath() + " to " + target);
			List<File> files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, null);
			for (File file : files) {
				File dest = new File(target + file.getName());
				FileUtils.copyFile(file, dest); // copies the files
				System.out.print(++count + " ");
			}
			// replacing the top page if it is a instable version
			if (!SeonParser.STABLE) {
				File file = new File(target + "top.instable.html");
				File dest = new File(target + "top.html");
				FileUtils.copyFile(file, dest); // copies the file
			}

			// static images
			source += "images/";
			target += "images/";
			dir = new File(source);
			files = (List<File>) FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, null);
			for (File file : files) {
				File dest = new File(target + file.getName());
				FileUtils.copyFile(file, dest); // copies the images
				System.out.print(++count + " ");
			}
			System.out.println("");
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}

	/* Generates the lines of the concepts table. */
	public String generateConceptsTable(Ontology onto) {
		String CONCEPTLINE = "<tr>\n<td><a name=\"@reference\">@concept</a>\n<a href=#@reference_detail><IMG src=\"images/plus-4-16.ico\"></a></td>\n<td>@definition\n<br/>@example</td>\n</tr>";
		List<Concept> concepts = onto.getAllConcepts();
		Collections.sort(concepts);
		String conceptsTable = "";
		for (Concept concept : concepts) {
			String line = CONCEPTLINE;
			String name = concept.getName();
			if (onto.getLevel() == OntoLevel.FOUNDATIONAL) name = "<i>" + name + "</i>";
			else if (onto.getLevel() == OntoLevel.CORE) name = "<b><i>" + name + "</i></b>";
			else if (onto.getLevel() == OntoLevel.DOMAIN) name = "<b>" + name + "</b>";
			line = line.replace("@concept", name);
			line = line.replace("@reference", concept.getLabel());
			line = line.replace("@definition", concept.getDefinition().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>"));
			String example = "";
			if (concept.getExample() != null) {
				example = "Ex.:<i>" + concept.getExample() + "</i>";
			}
			line = line.replace("@example", example);
			conceptsTable += line + "\n";
		}
		return conceptsTable;
	}

	/* Generates the detailed description of the Concepts. */
	private String generateDetailedConcepts(Ontology onto) {
		String DETAILITEM = "<a name=\"@reference_detail\"><hr></a><table border=\"1\" cellpadding=\"8\" style=\"border:black\">\n<tr><td style=\"text-align:center\">@stereotype<strong>@concept<strong></td></tr>\n</table><br/>\n<b>@fullName</b><br/>\n@generals<br/><br/>\n@definition<br/>\n@example<br/>\n<br/>\n@relations<br/>\n<br/><br/>";
		List<Concept> concepts = onto.getAllConcepts();
		Collections.sort(concepts);
		String detailedConcepts = "";
		// for each concept
		for (Concept concept : concepts) {
			String item = DETAILITEM;
			// main information
			String ster = concept.getStereotype();
			if (!ster.isEmpty()) {
				ster = "<code>&lt&lt" + ster + "&gt&gt</code><br/>\n";
			} else {
				if (!onto.getShortName().equals("UFO")) System.out.println("*" + concept + " <none>");
			}
			item = item.replace("@reference", concept.getLabel());
			item = item.replace("@stereotype", ster);
			item = item.replace("@concept", concept.getName());
			item = item.replace("@fullName", concept.getFullName());
			item = item.replace("@definition", concept.getDefinition().replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>"));
			String example = "";
			if (concept.getExample() != null) {
				example = "Ex.:<i>" + concept.getExample() + "</i>";
			}
			item = item.replace("@example", example);

			// generalizations
			List<Concept> generalizations = concept.getGeneralizations();
			String generals[] = new String[generalizations.size()];
			for (int i = 0; i < generalizations.size(); i++) {
				generals[i] = generalizations.get(i).getFullName();
			}
			item = item.replace("@generals", "Specializes " + Arrays.deepToString(generals).replace("[", "").replace("]", ""));

			// relations
			List<Relation> relations = Relation.getRelationsByConcept(concept);
			String relats = "";
			if (!relations.isEmpty()) {
				relats = "Relations:<br/>\n<code>";
				for (Relation relation : relations) {
					Ontology ontoSource = relation.getSource().getMainOntology();
					Ontology ontoTarget = relation.getTarget().getMainOntology();
					if (SeonParser.STABLE || ontoSource.getLevel().getValue() >= ontoTarget.getLevel().getValue()) {
						relats += relation.toString() + "<br/>\n";
					} else {
						relats += "<span style='color:red' title='Relation to a lower level (" + ontoSource.getName() + "-->" + ontoTarget.getName() + ")'>" + relation.toString()
								+ "</span><br/>\n";
					}
				}
				relats += "</code>";
			}
			item = item.replace("@relations", relats);

			detailedConcepts += item + "\n\n";
		}
		return detailedConcepts;
	}

	/** Formats a description to appear in a HTML page. */
	private String formatDescription(String description) {
		String text = "<b style='color:red'>No definition in Astah file</b>";
		if (!description.isEmpty() || SeonParser.STABLE) {
			text = description.replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", "<br/>");
		}
		return text;
	}

	private void checkRelations() {
		// TODO: Decide if while modeling we want:
		// 1 - Adjust the relation direction, making the imported allways the target (bad idea for both
		// imported concepts)
		// 2 - Force the source to be the in the package: start the relation in a concept from the
		// package, then change it for the right concept. (requires attention on modeling)
		System.out.println("\n# Printing Imported-target Relations (target != ontology).");
		int count = 0;
		for (Ontology onto : ontologies) {
			// System.out.println(onto);
			for (Relation relation : onto.getRelations()) {
				if (!relation.getTarget().getMainOntology().equals(onto)) {
					System.out.println("{" + onto.getName() + "} " + relation);
					count++;
				}
			}
		}
		System.out.println(count + " Relations\n");

		System.out.println("\n# Printing Imported-source Relations (source != ontology).");
		count = 0;
		for (Ontology onto : ontologies) {
			// System.out.println(onto);
			for (Relation relation : onto.getRelations()) {
				if (!relation.getSource().getMainOntology().equals(onto)) {
					System.out.println("{" + onto.getName() + "} " + relation);
					count++;
				}
			}
		}
		System.out.println(count + " Relations\n");
	}

	/* Replaces all occurrences of concepts' names in the text for the formated concepts' names. */
	@Deprecated
	private String formatFromConcepts(String text) {
		String formattedText = text;
		if (text != null && !text.isEmpty()) {
			// Creating a list of all concepts ordered by name lenght, to avoid mismaching in the text
			List<Concept> concepts = new ArrayList<Concept>(Concept.getAllConcepts());
			Comparator<Concept> comp = new Comparator<Concept>() {
				public int compare(Concept c1, Concept c2) {
					return c2.getName().length() - c1.getName().length();
				}
			};
			Collections.sort(concepts, comp);

			// Formating the text
			String open = "";
			String close = "";
			for (Concept concept : concepts) {
				if (concept.getOntology().getLevel() == OntoLevel.FOUNDATIONAL) { // italic for Foundational
					open = "<i>";
					close = "</i>";
				} else if (concept.getOntology().getLevel() == OntoLevel.CORE) { // bold italic for Core
					open = "<b><i>";
					close = "</i></b>";
				} else if (concept.getOntology().getLevel() == OntoLevel.DOMAIN) { // bold for Domain
					open = "<b>";
					close = "</b>";
				}

				// Pattern pattern = Pattern.compile(concept.getName() + "\\s");
				// System.out.println("Pattern: " + pattern);
				// Matcher match = pattern.matcher(formattedText);
				// String replace = open + concept.getName() + close;
				// int delay = 0;
				// while (match.find()) {
				// int start = match.start() + delay;
				// int end = start + concept.getName().length() + delay;
				// delay += (open+close).length();
				// formattedText = formattedText.substring(0, start) + replace + formattedText.substring(end);
				// }
				formattedText = formattedText.replace(concept.getName() + "\\s", (open + concept.getName() + close + " "));
			}
		}
		return formattedText;
	}

}