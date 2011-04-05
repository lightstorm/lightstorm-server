package org.hyperion.rs2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Manages server scripts.
 * 
 * @author blakeman8192
 * 
 */
public class ScriptManager {

	/**
	 * The singleton of this class.
	 */
	private static final ScriptManager INSTANCE = new ScriptManager();

	/**
	 * Gets the ScriptManager singleton.
	 * 
	 * @return The ScriptManager singleton.
	 */
	public static ScriptManager getScriptManager() {
		return INSTANCE;
	}

	/**
	 * The ScriptEngineManager.
	 */
	private final ScriptEngineManager mgr;

	/**
	 * The JavaScript Engine.
	 */
	private final ScriptEngine jsEngine;

	/**
	 * The logger for this manager.
	 */
	private final Logger logger = Logger.getLogger(toString());

	/**
	 * Creates the script manager.
	 */
	private ScriptManager() {
		mgr = new ScriptEngineManager();
		jsEngine = mgr.getEngineByName("JavaScript");
		logger.info("Loading scripts...");
	}

	/**
	 * Invokes a JavaScript function.
	 * 
	 * @param identifier
	 *            The identifier of the function.
	 * @param args
	 *            The function arguments.
	 */
	public void invoke(String identifier, Object... args) {
		final Invocable invEngine = (Invocable) jsEngine;
		try {
			invEngine.invokeFunction(identifier, args);
		} catch (final NoSuchMethodException ex) {
			logger.log(Level.WARNING, "No such method: " + identifier, ex);
		} catch (final ScriptException ex) {
			logger.log(Level.WARNING, "ScriptException thrown!", ex);
		}
	}

	/**
	 * Loads JavaScript files into the JavaScript ScriptEngine from the argued
	 * path.
	 * 
	 * @param dirPath
	 *            The path of the directory to load the JavaScript source files
	 *            from.
	 */
	public void loadScripts(String dirPath) {
		final File dir = new File(dirPath);
		if (dir.exists() && dir.isDirectory()) {
			final File[] children = dir.listFiles();
			for (final File child : children) {
				if (child.isFile() && child.getName().endsWith(".js")) {
					try {
						jsEngine.eval(new InputStreamReader(
								new FileInputStream(child)));
					} catch (final ScriptException ex) {
						logger.log(Level.SEVERE, "Unable to load script!", ex);
					} catch (final FileNotFoundException ex) {
						logger.log(Level.SEVERE, "Unable to find script!", ex);
					}
				} else if (child.isDirectory()) {
					loadScripts(child.getPath());
				}
			}
		}
	}

}
