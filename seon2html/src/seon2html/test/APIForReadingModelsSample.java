package seon2html.test;

import java.io.IOException;

import com.change_vision.jude.api.inf.exception.LicenseNotFoundException;
import com.change_vision.jude.api.inf.exception.NonCompatibleException;
import com.change_vision.jude.api.inf.exception.ProjectLockedException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.model.INamedElement;
import com.change_vision.jude.api.inf.model.IOperation;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.AstahAPI;

/**
 * Sample source codes for obtaining package and class information of Astah models. 
 * Output package and class names in console.
 */
public class APIForReadingModelsSample {
	
	//installer
	private static final String PROJECT_PATH = "./resources/SEON.asta";
	//eclipse
	//private static final String PROJECT_PATH = "api_sample\\simpleRead\\SampleModel.asta";

    public static void main(String[] args) {
        try {
            System.out.println("Opening project...");

            ProjectAccessor prjAccessor = AstahAPI.getAstahAPI().getProjectAccessor();

            // Open a project
            // param 1 : Project name
            // param 2 : true not to check model version
            // param 3 : false not to lock a project file
            // param 4 : true to open a project file with the read only mode if the file is locked.
            prjAccessor.open(PROJECT_PATH, true, false, true);

            System.out.println("Printing the elements...");

            // Get a project model
            IModel project = prjAccessor.getProject();

            // Get all of packages and classes in the project
            printPackageInfo(project);

            // Close a project
            prjAccessor.close();

            System.out.println("Finished");

        } catch (LicenseNotFoundException e) {
            e.printStackTrace();
        } catch (ProjectNotFoundException e) {
            e.printStackTrace();
        } catch (ProjectLockedException e) {
            e.printStackTrace();
        } catch (NonCompatibleException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void printPackageInfo(IPackage iPackage) {
        // Display a package name
        System.out.println("Package name: " + iPackage.getName());
        // Diaplay a package definition
        System.out.println("Package definition: " + iPackage.getDefinition());

        // Display packages and classes
        INamedElement[] iNamedElements = iPackage.getOwnedElements();
        for (int i = 0; i < iNamedElements.length; i++) {
            if (iNamedElements[i] instanceof IPackage) {
                IPackage iChildPackage = (IPackage) iNamedElements[i];
                // Display a package
                printPackageInfo(iChildPackage);
            } else if (iNamedElements[i] instanceof IClass) {
                IClass iClass = (IClass) iNamedElements[i];
                // Display a class
                printClassInfo(iClass);
            }
        }
    }

    private static void printClassInfo(IClass iClass) {
        // Display a class name
        System.out.println("Class name: " + iClass.getName());
        // Display a class definition
        System.out.println("Class definition: " + iClass.getDefinition());

        // Display all attributes
        IAttribute[] iAttributes = iClass.getAttributes();
        for (int i = 0; i < iAttributes.length; i++) {
            printAttributeInfo(iAttributes[i]);
        }

        // Display all operations
        IOperation[] iOperations = iClass.getOperations();
        for (int i = 0; i < iOperations.length; i++) {
            printOperationInfo(iOperations[i]);
        }

        // Display inner class information
        IClass[] iClasses = iClass.getNestedClasses();
        for (int i = 0; i < iClasses.length; i++) {
            printClassInfo(iClasses[i]);
        }
    }

    private static void printOperationInfo(IOperation iOperation) {
        // Display an operation name
        System.out.println("Operation name: " + iOperation.getName());
        // Display a return type of operation
        System.out.println("Operation returnType: " + iOperation.getReturnTypeExpression());
        // Display an operation definition
        System.out.println("Operation definition: " + iOperation.getDefinition());
    }

    private static void printAttributeInfo(IAttribute iAttribute) {
        // Display an attribute name
        System.out.println("Attribute name: " + iAttribute.getName());
        // Display an attribute type
        System.out.println("Attribute type: " + iAttribute.getTypeExpression());
        // Display an attribute definition
        System.out.println("Attribute definition: " + iAttribute.getDefinition());
    }
}
