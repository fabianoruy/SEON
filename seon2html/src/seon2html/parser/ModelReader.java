package seon2html.parser;

import java.io.IOException;
import java.util.List;
import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.exception.*;
import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

import seon2html.model.Package;
import seon2html.model.*;
import seon2html.model.Package.PackType;
import seon2html.model.Diagram.DiagType;

/* Responsible for reading the Astah file and create the Seon objects model. */
public class ModelReader {

	//////////////////// PARSERS ////////////////////
	/* Reads the Astah file and builds the Seon Model. */
	public Package parseAstah2Seon(String filename) {
		ProjectAccessor accessor = null;
		Package seon = null;
		try {
			// Accessing the astah model
			accessor = AstahAPI.getAstahAPI().getProjectAccessor();

			// Opening a project (name, true not to check model version, false not to lock a project file,
			// true to open a project file with the read only mode if the file is locked.)
			accessor.open(filename, true, false, true);
			IModel model = accessor.getProject();

			// Creating the network main package (seon).
			seon = new Package(model.getName(), model.getDefinition(), PackType.NETWORK, 0, (IPackage) model);

			// Reading the model information (Packages and Concepts, Depencencies, Generalizations, Relations)
			System.out.println("\n# Parsing the Network Packages and Concepts");
			parsePackages(seon);
			parseDependencies(seon);
			parseGeneralizations(Concept.getAllConcepts());
			parseRelations(Concept.getAllConcepts());

			// Reading the Diagrams' information
			parseDiagrams(seon);

		} catch (IOException | ClassNotFoundException | LicenseNotFoundException | ProjectNotFoundException | NonCompatibleException | ProjectLockedException e) {
			e.printStackTrace();
		} finally {
			accessor.close();
		}
		return seon;
	}

	/* Reads the packages recursivelly and creates the Package objects. */
	private void parsePackages(Package superpack) {
		System.out.println(superpack);
		Package pack = null;
		for (INamedElement elem : superpack.getAstahPack().getOwnedElements()) {
			// Parsing packages
			if (elem instanceof IPackage) {
				String givenType = elem.getTaggedValue("Type");
				if(givenType == null) System.out.println("No type defined: "+ elem.getName());
				PackType type = Package.getPackType(elem.getTaggedValue("Type"));
				if (type != PackType.IGNORE) {
					String packName = elem.getName();
					String fullName = elem.getTaggedValue("FullName");
					String shortName = elem.getTaggedValue("ShortName");
					String definition = elem.getDefinition();
					String sorder = elem.getTaggedValue("Order");
					int order = 100;
					if (!sorder.isEmpty()) order = Integer.valueOf(sorder);

					// Creates a Package or a Ontology
					if (type == PackType.LEVEL || type == PackType.PACKAGE || type == PackType.SUBNETWORK) {
						pack = new Package(packName, definition, type, order, (IPackage) elem);
					} else if (type == PackType.ONTOLOGY || type == PackType.SUBONTOLOGY) {
						pack = new Ontology(packName, fullName, shortName, definition, type, order, (IPackage) elem);
					}
					pack.setParent(superpack);
					superpack.addPack(pack);

					// Recursive call for subpacks
					parsePackages(pack);
				}

				// Parsing concepts
			} else if (elem instanceof IClass) {
				// Reading the classes and creating the Concepts
				String name = elem.getName();
				String definition = elem.getDefinition();
				String stereotype = "";
				if ((elem.getStereotypes()).length > 0) {
					stereotype = elem.getStereotypes()[0]; // only the first for while
				}
				Concept concept = new Concept(name, definition, stereotype, (IClass) elem);
				concept.setOntology((Ontology) superpack);
				((Ontology) superpack).addConcept(concept);
				System.out.println(concept);
			}
		}
	}

	/* Reads the Packages recursivelly and creates their Dependencies. */
	private void parseDependencies(Package superpack) {
		// Reading Dependencies
		for (IDependency elem : superpack.getAstahPack().getClientDependencies()) {
			Package client = Package.getPackageByAstah((IPackage) elem.getClient());
			Package supplier = Package.getPackageByAstah((IPackage) elem.getSupplier());
			String definition = elem.getDefinition();
			String level = elem.getTaggedValue("Level");

			Dependency depend = new Dependency(client, supplier, definition, level);
			client.addDependency(depend);
		}
		// Recursive call
		for (Package pack : superpack.getPacks()) {
			parseDependencies(pack);
		}
	}

	/* Reads the Concepts and sets up their generalizations. */
	private void parseGeneralizations(List<Concept> concepts) {
		for (Concept child : concepts) {
			// Reading and setting generalizations
			for (IGeneralization elem : child.getAstahClass().getGeneralizations()) {
				Concept parent = Concept.getConceptByAstah(elem.getSuperType());
				child.addGeneralization(parent);
			}
		}
	}

	/* Reads the Concepts and creates the Relations between them. */
	private void parseRelations(List<Concept> concepts) {
		for (Concept source : concepts) {
			// Reading and creating relations
			for (IAttribute attrib : source.getAstahClass().getAttributes()) {
				IAssociation assoc = attrib.getAssociation();
				if (assoc != null) { // it is an Association, not an Attribute
					IAttribute firstEnd = assoc.getMemberEnds()[0];
					IAttribute secondEnd = assoc.getMemberEnds()[1];
					// Selecting only the relations where this concept is source (not target).
					if (firstEnd.getType().equals(source.getAstahClass())) {
						String name = assoc.getName();
						String definition = assoc.getDefinition();
						String ster = null;
						if (assoc.getStereotypes().length > 0) {
							ster = assoc.getStereotypes()[0]; // only the first for while
						}
						boolean composition = (firstEnd.isComposite() || firstEnd.isAggregate());
						String smult = "";
						String tmult = "";
						if (firstEnd.getMultiplicity().length > 0) {
							smult = multiplicityToString(firstEnd.getMultiplicity()[0]);
						}
						if (secondEnd.getMultiplicity().length > 0) {
							tmult = multiplicityToString(secondEnd.getMultiplicity()[0]);
						}

						Concept target = Concept.getConceptByAstah(attrib.getType());
						Package pack = Package.getPackageByFullName(assoc.getFullNamespace("::"));
						// Creating the Relation object
						Relation relation = new Relation(name, definition, ster, composition, pack, source, target, smult, tmult);
					}
				} else {
					System.out.println("# Attribute: " + attrib.getName());
				}
			}
		}
	}

	/* Returns the multiplicity of an end in text format (n..m). */
	private String multiplicityToString(IMultiplicityRange imult) {
		int lower = imult.getLower();
		int upper = imult.getUpper();
		if (lower == IMultiplicityRange.UNDEFINED) return "";
		if (lower == IMultiplicityRange.UNLIMITED) return "*";
		if (upper == IMultiplicityRange.UNDEFINED) return lower + "";
		if (upper == IMultiplicityRange.UNLIMITED) return lower + "..*";
		return lower + ".." + upper;
	}

	/* Reads the astah Diagrams info and creates the Diagrams objects. */
	private void parseDiagrams(Package superpack) {
		for (IDiagram elem : superpack.getAstahPack().getDiagrams()) {
			String name = elem.getName();
			String definition = elem.getDefinition();
			DiagType type = Diagram.getDiagramType(elem.getTaggedValue("Type"));
			Diagram diagram = new Diagram(name, definition, type, elem);
			diagram.setPack(superpack);
			superpack.addDiagram(diagram);
			// System.out.println("DIAGRAM:" + diagram);
		}
		// Recursive call
		for (Package pack : superpack.getPacks()) {
			parseDiagrams(pack);
		}
	}

}