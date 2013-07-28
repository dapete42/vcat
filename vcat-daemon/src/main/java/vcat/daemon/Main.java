package vcat.daemon;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import vcat.VCatException;
import vcat.VCatRenderer;
import vcat.VCatRenderer.RenderedFileInfo;
import vcat.graphviz.Graphviz;
import vcat.graphviz.GraphvizJNI;

public class Main {

	public static void main(String[] args) throws VCatException {

		// Configuration, hardcoded
		final File tmpDir = new File("./cache");
		final File inDir = new File("./in");
		final File outDir = new File("./out");
		inDir.mkdirs();
		outDir.mkdirs();

		// Call Graphviz using JNI
		final Graphviz graphviz = new GraphvizJNI();
		// Create renderer
		final VCatRenderer vCatRenderer = new VCatRenderer(tmpDir, graphviz);

		// Files put in inDir must be named *.json
		final FileFilter jsonFilter = FileFilterUtils.suffixFileFilter(".json");

		// Main loop; repeat forever
		while (true) {
			// Get file list
			final File[] jsonFiles = inDir.listFiles(jsonFilter);
			// Start render process for all files
			for (File jsonFile : jsonFiles) {
				renderJsonFile(jsonFile, outDir, vCatRenderer);
			}
			// Sleep for a while
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Ignore, should never occur
			}
		}

	}

	private static void fillParametersFromJson(HashMap<String, String[]> parameterMap, File inFile)
			throws VCatException {
		final JSONObject json;

		try (InputStreamReader reader = new FileReader(inFile)) {
			json = new JSONObject(new JSONTokener(reader));
		} catch (IOException e) {
			throw new VCatException("Error reading job input file '" + inFile.getAbsolutePath() + "'", e);
		} catch (JSONException e) {
			throw new VCatException("Error parsing json in job input file '" + inFile.getAbsolutePath() + "'", e);
		}

		JSONArray jsonArrayNames = json.names();

		try {
			for (int i = 0; i < jsonArrayNames.length(); i++) {
				final String jsonArrayName = jsonArrayNames.getString(i);
				final JSONArray jsonArray = json.getJSONArray(jsonArrayName);
				final String[] parameterArray = new String[jsonArray.length()];
				for (int j = 0; j < jsonArray.length(); j++) {
					parameterArray[j] = jsonArray.getString(j);
				}
				parameterMap.put(jsonArrayName, parameterArray);
			}
		} catch (JSONException e) {
			throw new VCatException("Error parsing json in job input file '" + inFile.getAbsolutePath() + "'", e);
		}
	}

	private static void handleError(final File outFileTemp, final File outFileHeadersTemp, final VCatException e) {
		final String errorMessage = e.getMessage();
		System.err.println(errorMessage);
		e.printStackTrace();
		writeErrorFiles(outFileTemp, outFileHeadersTemp, errorMessage);
	}

	protected static void renderJsonFile(final File jsonFile, final File outDir, final VCatRenderer vCatRenderer) {

		final String inName = jsonFile.getName();
		final String baseName = inName.substring(0, inName.length() - ".json".length());

		final File outFile = new File(outDir, baseName + ".out");
		final File outFileTemp = new File(outDir, baseName + ".out.tmp");
		final File outFileHeaders = new File(outDir, baseName + ".headers");
		final File outFileHeadersTemp = new File(outDir, baseName + ".headers.tmp");

		outFile.delete();
		outFileTemp.delete();
		outFileHeaders.delete();
		outFileHeadersTemp.delete();

		final HashMap<String, String[]> parameterMap = new HashMap<String, String[]>();

		try {
			fillParametersFromJson(parameterMap, jsonFile);
		} catch (VCatException e) {
			handleError(outFileTemp, outFileHeadersTemp, e);
			jsonFile.delete();
			return;
		}

		jsonFile.delete();

		Thread t = new Thread() {

			@Override
			public void run() {
				super.run();
				RenderedFileInfo renderedFileInfo;
				try {
					renderedFileInfo = vCatRenderer.render(parameterMap);
					System.out.println("Results in " + renderedFileInfo.getFile().getAbsolutePath());
					writeResultFiles(outFileTemp, outFileHeadersTemp, renderedFileInfo);
					if (outFileTemp.exists()) {
						outFileTemp.renameTo(outFile);
					}
					if (outFileHeadersTemp.exists()) {
						outFileHeadersTemp.renameTo(outFileHeaders);
					}
				} catch (VCatException e) {
					handleError(outFileTemp, outFileHeadersTemp, e);
				}
				System.out.println("Finished thread " + this.getName());
			}

			@Override
			public void start() {
				super.start();
				System.out.println("Started thread " + this.getName());
			}
		};
		t.start();

	}

	/**
	 * Write headers and contents with a plain text error message to temporary files.
	 * 
	 * @param outFileTemp
	 *            Temporary file for output data
	 * @param outFileHeadersTemp
	 *            Temporary file for output headers
	 * @param errorMessage
	 *            Error message
	 */
	private static void writeErrorFiles(final File outFileTemp, final File outFileHeadersTemp, final String errorMessage) {

		// Write output headers to temporary file
		try (final FileOutputStream tmpOutputStream = new FileOutputStream(outFileHeadersTemp);
				final OutputStreamWriter tmpWriter = new OutputStreamWriter(tmpOutputStream, "UTF-8");
				final BufferedWriter headerWriter = new BufferedWriter(tmpWriter)) {
			// Content-type
			headerWriter.write("Content-type: text/plain\n");
		} catch (Exception e) {
			// Ignore, if this fails as well, there is nothing we can do
		}

		// Write output data (error message) to temporary file
		try (final FileOutputStream tmpOutputStream = new FileOutputStream(outFileTemp);
				final OutputStreamWriter tmpWriter = new OutputStreamWriter(tmpOutputStream, "UTF-8");
				final BufferedWriter outWriter = new BufferedWriter(tmpWriter)) {
			// Content-type
			outWriter.write(errorMessage);
			outWriter.write('\n');
		} catch (Exception e) {
			// Ignore, if this fails as well, there is nothing we can do
		}

	}

	/**
	 * Write headers and contents with rendered results to temporary files.
	 * 
	 * @param outFileTemp
	 *            Temporary file for output data
	 * @param outFileHeadersTemp
	 *            Temporary file for output headers
	 * @param renderedFileInfo
	 *            Information returned from vCat rendering
	 * @throws VCatException
	 *             If any other exceptions occur
	 */
	private static void writeResultFiles(final File outFileTemp, final File outFileHeadersTemp,
			final RenderedFileInfo renderedFileInfo) throws VCatException {

		// Write output data to temporary file by copying rendered file
		try {
			FileUtils.copyFile(renderedFileInfo.getFile(), outFileTemp);
		} catch (IOException e) {
			throw new VCatException("Error copying output file to '" + outFileTemp.getAbsolutePath() + "'", e);
		}

		// Write output headers to temporary file
		try (final FileOutputStream tmpOutputStream = new FileOutputStream(outFileHeadersTemp);
				final OutputStreamWriter tmpWriter = new OutputStreamWriter(tmpOutputStream, "UTF-8");
				final BufferedWriter headerWriter = new BufferedWriter(tmpWriter)) {

			// Content-type, as returned from rendering process
			String contentType = renderedFileInfo.getMimeType();
			headerWriter.write("Content-type: ");
			headerWriter.write(contentType);
			headerWriter.write('\n');

			// Content-length, using length of temporary output file already written
			long length = outFileTemp.length();
			if (length < Integer.MAX_VALUE) {
				headerWriter.write("Content-length: ");
				headerWriter.write(Long.toString(length));
				headerWriter.write('\n');
			}

			// Content-disposition, to determine filename of returned contents
			String filename = renderedFileInfo.getFile().getName();
			headerWriter.write("Content-disposition: ");
			headerWriter.write("filename=\"");
			headerWriter.write(filename);
			headerWriter.write("\"\n");

		} catch (Exception e) {
			throw new VCatException("Error writing headers to '" + outFileTemp.getAbsolutePath() + "'", e);
		}

	}

}
