package seon2html.parser;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/* Provides text facilities. */
public class Utils {

	/* Reads a File and returns it as a String. */
	public static String fileToString(String filename) {
		String text = null;
		try {
			text = FileUtils.readFileToString(new File(filename), "UTF-8");
			//text = FileUtils.readFileToString(new File(Utils.class.getResource(filename).toURI()), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return text;
	}

	/* Writes a String to a File. */
	public static void stringToFile(String filename, String text) {
		try {
			FileUtils.write(new File(filename), text, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void waitFor(int times, long millis) {
		System.out.print("Waiting (" + times + "*" + millis + ") ");
		try {
			for (int i = 0; i < times; i++) {
				Thread.sleep(millis); // 1000 milliseconds is one second.
				System.out.print(".");
			}
			System.out.println("");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/* Formats the XML elements' texts. */
	public static String parseText0(String text) {
		String result = text.replace('+', ' ');
		result = result.replace("%2B", "+");
		result = result.replace("%2C", ",");
		result = result.replace("%2F", "/");
		result = result.replace("%3A", ":");
		result = result.replace("%3B", ";");
		result = result.replace("%27", "'");
		result = result.replace("%28", "(");
		result = result.replace("%29", ")");
		result = result.replace("%5B", "[");
		result = result.replace("%5D", "]");
		result = result.replace("%22", "\"");
		result = result.replace("%0A", "<br>"); // "\n"
		result = result.replace("%40", "@");
		return result;
	}

}