package com.bazarooma.tools.sshfileuploader;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loads and holds configuration
 * @author george georgovassilis
 *
 */
public class Configuration extends Properties{
	
	public Configuration(String path) throws IOException{
		super();
		Reader reader = new FileReader(path);
		load(reader);
		reader.close();
	}
	
	private int getIntProperty(String name) {
		try {
			return Integer.parseInt(getProperty(name));
		} catch (Exception e) {
			throw new MissingPropertyException("Missing or non-nummeric property "+name);
		}
	}

	private String getStringProperty(String name) {
		String s = getProperty(name);
		if (s == null)
			throw new MissingPropertyException("Missing property "+name);
		return s;
	}
	
	public String getUserName() {
		return getStringProperty("userName");
	}


	public int getInterval() {
		return getIntProperty("INTERVAL");
	}

	public int getPauseAfterNewProcess() {
		return getIntProperty("PAUSE_AFTER_NEW_PROCSS");
	}

	public String getServer() {
		return getStringProperty("server");
	}

	public int getMaxProcesses() {
		return getIntProperty("MAXPROCESSES");
	}

	public int getPartSize() {
		return 1024 * getIntProperty("PARTSIZE_KB");
	}

	public int getMaxFailures() {
		return getIntProperty("MAX_FAILURES");
	}

	public String getScpExecutable() {
		return getStringProperty("scp");
	}

	public String getSshExecutable() {
		return getStringProperty("ssh");
	}
	
	public String getTmp(){
		return getStringProperty("tmpDir");
	}
	
	public String getLogging(){
		return getStringProperty("logging");
	}

	public String getSshArgs(){
		return getStringProperty("sshArgs");
	}


}
