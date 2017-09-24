package seon2html.parser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import seon2html.model.Concept;
import seon2html.model.Ontology.OntoLevel;
import seon2html.model.Package;
import seon2html.model.Relation;

/* Responsible for generate the OWL file from the objects model. */
public class OwlWriter {
	private static final boolean	FOUND		= false;
	private static final boolean	CORE		= true;
	private static final boolean	DOMAIN		= true;
//	private static final boolean	FOUND		= false;
//	private static final boolean	CORE		= false;
//	private static final boolean	DOMAIN		= true;

	private OWLOntologyManager		manager		= OWLManager.createOWLOntologyManager();
	private OWLDataFactory			factory;
	private OWLOntology				ontology;
	private final String			nameSpace	= "dev.nemo.inf.ufes.br/seon/SEON.owl#";

	/* Generates all the HTML Seon Pages. */
	public void generateSeonOwl(Package seon) {

		// Creating the OWL ontology
		System.out.println("\n# Creating the OWL Ontology");
		try {
			factory = manager.getOWLDataFactory();
			ontology = manager.createOntology(IRI.create(nameSpace));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}

		// Generating the OWL classes
		generateClasses();

		// Generating the Generalizations
		generateGeneralizations();

		// Generating the Relations
		generateRelations();

		// Writing the OWL file
		OutputStream output;
		try {
			output = new FileOutputStream("./page/SEON.owl");
			manager.saveOntology(ontology, output);
		} catch (FileNotFoundException | OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}

	/* Reads the Seon Model and generates the OWL Classes. */
	private void generateClasses() {
		System.out.print("Generating the OWL Classes: ");
		int count = 0;
		// Reaching the Concepts
		List<Concept> concepts = Concept.getAllConcepts();

		// Creating the Classes statements
		for (Concept concept : concepts) {
			if (isIncluded(concept)) {
				OWLClass owlCls = getOwlClass(concept);
				OWLDeclarationAxiom declarationAxiom = factory.getOWLDeclarationAxiom(owlCls);
				manager.addAxiom(ontology, declarationAxiom);
				count++;

				// ontology annotation
				OWLAnnotationValue value = factory.getOWLLiteral(concept.getMainOntology().getShortName());
				OWLAnnotation annotation = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(IRI.create(nameSpace + "ontology")), value);
				OWLAxiom axiom = factory.getOWLAnnotationAssertionAxiom(owlCls.getIRI(), annotation);
				manager.applyChange(new AddAxiom(ontology, axiom));

				// stereotype annotation
				if (!concept.getStereotype().isEmpty()) {
					value = factory.getOWLLiteral(concept.getStereotype());
					annotation = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(IRI.create(nameSpace + "stereotype")), value);
					axiom = factory.getOWLAnnotationAssertionAxiom(owlCls.getIRI(), annotation);
					manager.applyChange(new AddAxiom(ontology, axiom));
				}

				// definition comment
				String comment = concept.getDefinition().replaceAll("\\<[^>]*>", "").replaceAll("\"", "");
				comment = Normalizer.normalize(comment, Normalizer.Form.NFD);
				comment = comment.replaceAll("[^\\p{ASCII}]", "").replaceAll("(\\r\\n|\\n\\r|\\r|\\n)", " ");
				OWLAnnotation commentAnno = factory.getOWLAnnotation(factory.getRDFSComment(), factory.getOWLLiteral(comment));
				axiom = factory.getOWLAnnotationAssertionAxiom(owlCls.getIRI(), commentAnno);
				manager.applyChange(new AddAxiom(ontology, axiom));
			}
		}
		System.out.println(count);
	}

	/* Reads the Seon Model and generates the OWL Generalizations. */
	private void generateGeneralizations() {
		System.out.print("Generating the OWL Classes Generalizations: ");
		int count = 0;
		// Reaching the Concepts
		List<Concept> concepts = Concept.getAllConcepts();

		// Creating the Generalizations statements
		for (Concept concept : concepts) {
			if (isIncluded(concept)) {
				List<Concept> generals = concept.getGeneralizations();
				for (Concept general : generals) {
					if (isIncluded(general)) {
						OWLClass son = getOwlClass(concept);
						OWLClass father = getOwlClass(general);
						OWLAxiom axiom = factory.getOWLSubClassOfAxiom(son, father);
						manager.applyChange(new AddAxiom(ontology, axiom));
						count++;
					}
				}
			}
		}
		System.out.println(count);
	}

	/* Reads the Seon Model and generates the OWL Relations (Object Properties). */
	private void generateRelations() {
		System.out.print("Generating the OWL Object Properties: ");
		int count = 0;

		List<Relation> relations = Relation.getAllRelations();
		for (Relation relation : relations) {
			if (isIncluded(relation.getSource()) && isIncluded(relation.getTarget())) {
				String name = relation.getName();
				if (name.isEmpty() && relation.isComposition()) {
					name = "composed of";
				}
				name = name.replace('/', '_') + "." + relation.getSource().getName() + "+" + relation.getTarget().getName();
				OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(nameSpace + name.replace(' ', '_')));

				// source and destination classes of the relation
				OWLClass src = getOwlClass(relation.getSource());
				OWLClass dst = getOwlClass(relation.getTarget());

				// Set domain and range from the property
				manager.applyChange(new AddAxiom(ontology, factory.getOWLObjectPropertyDomainAxiom(prop, src)));
				manager.applyChange(new AddAxiom(ontology, factory.getOWLObjectPropertyRangeAxiom(prop, dst)));
				count++;
			}
		}
		System.out.println(count);
	}

	/* Creates/recovers a OWL Class from a Concept. */
	private OWLClass getOwlClass(Concept concept) {
		String name = (concept.getMainOntology().getShortName() + "::" + concept.getName()).replace(' ', '_');
		return factory.getOWLClass(IRI.create(nameSpace + name));
	}

	/* Verifies if a concept will be included in the OWL. */
	private boolean isIncluded(Concept concept) {
		OntoLevel level = concept.getMainOntology().getLevel();
		if (level == OntoLevel.FOUNDATIONAL) return FOUND;
		if (level == OntoLevel.CORE) return CORE;
		if (level == OntoLevel.DOMAIN) return DOMAIN;
		return false;
	}

}