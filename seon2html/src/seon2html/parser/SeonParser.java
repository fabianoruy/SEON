package seon2html.parser;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import seon2html.model.Package;

/* Responsable for manage the parsing from XML reading to HTML writing. */
public class SeonParser {
	public static final String	PATH	= System.getProperty("user.dir").replace('\\', '/');
	public static String		VERSION;
	public static boolean		STABLE;

	/* Main method. */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		System.out.println("Working Path: " + PATH);
		// Reading the properties file
		Properties props = readProperties(PATH + "/parser.properties");
		String astahfile = PATH + "/" + props.getProperty("seon.file");
		VERSION = props.getProperty("seon.version");
		STABLE = Boolean.valueOf(props.getProperty("seon.stable"));

		// Setting the log file (only for the JAR execution)
		if (props.getProperty("log.output").equals("log")) {
			setLogOutput("SeonParserLog.log");
		}
		System.out.println("Properties: " + props);

		// Exporting images from astah and copying to the proper location for the page
		recoverAstahImages(props.getProperty("astah.location"), astahfile, props.getProperty("images.export").equals("auto"));

		// Reading the Astah file and building the Seon Model
		ModelReader reader = new ModelReader();
		Package seon = reader.parseAstah2Seon(astahfile);

		// Reading the Seon Model and generating the HTML
		PageWriter pwriter = new PageWriter();
		pwriter.generateSeonPages(seon);

		// Reading the Seon Model and generating the OWL file
		OwlWriter owriter = new OwlWriter();
		owriter.generateSeonOwl(seon);
		
		// Reading the Seon Model and generating data for graph
		GraphDataWriter gdwriter = new GraphDataWriter();
		gdwriter.generateDataFiles(seon);

		System.out.print("\nTHE END! (" + (System.currentTimeMillis() - start) / 1000.0 + "s)");
		// finishMessage();
	}

	private static void finishMessage() {
		int option = JOptionPane.showConfirmDialog(null, "SEON Page Generated! Would you like to open it now?", "SEON Parser", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION) {
			try {
				File htmlFile = new File(PATH + "/page/index.html");
				Desktop.getDesktop().browse(htmlFile.toURI());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* Read the Properties file. */
	private static Properties readProperties(String filename) {
		Properties props = new Properties();
		FileInputStream input;
		try {
			input = new FileInputStream(filename);
			props.load(input);
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return props;
	}

	/* Recover the astah PNG images (from astah file or exported html/images directory) to the SEON images directory. */
	private static void recoverAstahImages(String location, String astahfile, boolean export) {
		String exportPath = PATH + "/page/astahdoc/";
		File dir = new File(exportPath);
		if (!dir.exists()) dir.mkdirs();
		try {
			// Exporting images from the Astah file (using command line).
			if (export) {
				System.out.println("\n# Exporting images from Astah to " + exportPath);
				String command = location + "/astah-commandw.exe"; // command for exporting
				command += " -image cl"; // selecting only Class diagrams
				command += " -f " + astahfile; // defining input astah file
				command += " -o " + exportPath; // defining output directory
				System.out.println("$ " + command);

				long start = System.currentTimeMillis();
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
				System.out.print("[-] Time: " + (System.currentTimeMillis() - start) + " - ");

				// TODO: test images exportation in other machines/conditions.
				int files = 0;
				int before = 0;
				int diff = 0;
				while (files == 0 || diff > 0) {
					Utils.waitFor(3, 1000);
					files = FileUtils.listFiles(dir, new String[] { "png" }, true).size();
					diff = files - before;
					before = files;
					System.out.print("[" + files + "] Time: " + (System.currentTimeMillis() - start) + " - ");
				}
			}

			// Copying all .PNG files from the astahdoc directory to the SEON page images directory
			String target = PATH + "/page/images/";
			int count = 0;
			System.out.println("\nCopying all .PNG files in " + dir.getPath() + " and subdirectories to " + target);
			List<File> files = (List<File>) FileUtils.listFiles(dir, new String[] { "png" }, true);
			for (File file : files) {
				File dest = new File(target + file.getName());
				FileUtils.copyFile(file, dest); // copies the PNG files
				System.out.print(++count + " ");
			}
			System.out.println("");

			// Scheduling the Deletion of temporary astahdoc images directory
			if (export) {
				System.out.println("Deleting " + dir.getName());
				FileUtils.forceDeleteOnExit(dir);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/* Defines the output log file. */
	private static void setLogOutput(String logfile) {
		try {
			System.setOut(new PrintStream(logfile));
			System.out.println("SEON Parser log file - " + new java.util.Date());
			System.out.println("---------------------------------------------------\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}