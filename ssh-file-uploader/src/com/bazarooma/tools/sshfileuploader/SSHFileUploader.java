package com.bazarooma.tools.sshfileuploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class SSHFileUploader {

	private final static int ERROR_MISSING_PROPERTY = 1;
	private final static int ERROR_WRONG_ARGUMENTS = 2;

	private static Logger log = Logger.getLogger(SSHFileUploader.class.getName());

	private static void exit(int errCode) {
		System.exit(errCode);
	}

	private Configuration configuration;
	private int parts;
	private String remotePath;
	private int[] partLog;
	private Map<Integer, Process> processes = new LinkedHashMap<Integer, Process>();

	private void loadConfiguration(String path) throws Exception {
		configuration = new Configuration(path);
	}

	public SSHFileUploader(String pathToProperties) throws Exception {
		loadConfiguration(pathToProperties);
	}

	private int getExitCode(Process p) {
		try {
			if (p == null)
				return -2;
			return p.exitValue();
		} catch (IllegalThreadStateException e) {
			return -1;
		}
	}

	private Process scp(File part) throws Exception {
		ProcessBuilder pb = new ProcessBuilder(configuration.getScpExecutable(), configuration.getSshArgs(), part.getAbsolutePath(), configuration.getUserName()
				+ "@" + configuration.getServer() + ":" + remotePath);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		Thread.sleep(configuration.getPauseAfterNewProcess());
		return p;
	}

	private int remoteMerge(String filename) throws Exception {
		String catCommand = "cat ";
		for (int i = 0; i <= parts; i++)
			catCommand += i + ".part ";
		catCommand += " > " + filename;
		String mergeCommand = "rm " + filename + ";" + catCommand + ";exit";
		ProcessBuilder pb = new ProcessBuilder(configuration.getSshExecutable(), configuration.getUserName() + "@" + configuration.getServer(),
				mergeCommand);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(p.getInputStream()));
		String s;
		while (null != (s = lnr.readLine()))
			log.info(s);
		int i = p.waitFor();
		return i;
	}

	private int split(File file, int partSize) throws Exception {
		FileInputStream fis = new FileInputStream(file);
		File baseDir = new File(configuration.getTmp());
		int partnr = 0;
		byte[] buffer = new byte[partSize];
		while (true) {
			int length = fis.read(buffer);
			write(new File(baseDir, "" + partnr + ".part"), buffer, length);
			if (length < partSize)
				break;
			partnr++;
		}
		fis.close();
		return partnr;
	}

	public void upload(File file, String remotePath) throws Exception {
		this.remotePath = remotePath;
		log.info("Splitting "+file.getName()+" into upload parts of "+configuration.getPartSize());
		int parts = split(file, configuration.getPartSize());
		this.parts = parts;
		log.info("Starting upload of "+parts+" parts.");
		uploadChunks(file);
		int failcount = -1;
		do {
			log.info("Merging "+parts+" parts.");
			failcount++;
			if (failcount > configuration.getMaxFailures()) {
				log.severe("Aborting because of failures during merge");
				return;
			}
		} while (0 != remoteMerge(file.getName()));
	}

	private void uploadChunks(File file) throws Exception {
		File base = file.getParentFile();
		for (int part = 0; part <= parts; part++) {
			processes.put(part, null);
		}
		while (!processes.isEmpty()) {
			if (getActiveUploaders() > 0)
				Thread.sleep(configuration.getInterval());
			int rerunsLeft = 1;
			int maxRunningProccesses = configuration.getMaxProcesses();
			for (Iterator<Map.Entry<Integer, Process>> ite = processes.entrySet().iterator(); ite.hasNext();) {
				Map.Entry<Integer, Process> entry = ite.next();
				Integer partNr = entry.getKey();
				Process p = entry.getValue();
				int exitCode = getExitCode(p);

				switch (exitCode) {
				case 0:
					File f = new File(base, "" + partNr);
					f.delete();
					ite.remove();
					logEnd(partNr);
					break;
				case -1:
					maxRunningProccesses--;
					// not ready yet;
					break;
				default:
					if (rerunsLeft == 0 || maxRunningProccesses == 0)
						continue;
					rerunsLeft--;
					maxRunningProccesses--;
					if (exitCode == -2)
						logStart(partNr);
					else {
						logRedo(partNr);
					}
					p = scp(new File(base, "" + partNr + ".part"));
					processes.put(partNr, p);
				}
			}
		}
	}

	private void initPartLog() {
		if (partLog == null) {
			partLog = new int[configuration.getMaxProcesses()];
			for (int i = 0; i < partLog.length; i++)
				partLog[i] = -1;
		}

	}

	private void substLog(int what, int with) {
		initPartLog();
		for (int i = 0; i < partLog.length; i++)
			if (partLog[i] == what) {
				partLog[i] = with;
				break;
			}
	}

	private void logStart(int partNr) {
		synchronized (this) {
			substLog(-1, partNr);
			printProgress();
		}
	}

	private void logEnd(int partNr) {
		synchronized (this) {
			substLog(partNr, -1);
			printProgress();
		}
	}

	private void logRedo(int partNr) {
	}

	private int getActiveUploaders() {
		synchronized (this) {
			initPartLog();
			int count = 0;
			for (int part : partLog)
				if (part != -1)
					count++;
			return count;
		}
	}

	private void printProgress() {
		if ("quiet".equals(configuration.getLogging()))
			return;
		String log = "|";
		for (int part : partLog) {
			if (part == -1)
				log += "   |";
			else {
				String n = "" + part;
				while (n.length() < 3)
					n = " " + n;
				log += n + "|";
			}
		}
		int percentageComplete = (parts - processes.size()) * 100 / parts;
		log += " %" + percentageComplete;
		System.out.println(log);
	}

	private void write(File target, byte[] buffer, int length) throws Exception {
		if (length < 1)
			return;
		FileOutputStream fos = new FileOutputStream(target);
		fos.write(buffer, 0, length);
		fos.flush();
		fos.close();
	}

	protected static void showUsage() {
		System.out
				.println("Usage: SSHFileUploader path_to_configuration_file file_to_upload target_directory_on_server");
		System.out.println();
		System.out.println("Usage example:");
		System.out
				.println("/home/jack/tools/sshfileuploader.properties ./photos/backup.zip /mnt/raid/backups/2013-06-13");
		System.out.println();
		System.out.println("For configuration file format check the example downloaded with this file.");
		System.out.println("For configuration file format check the example downloaded with this file.");
	}

	private static void handleException(Exception e) {
		if (e instanceof MissingPropertyException) {
			log.severe(e.getMessage());
			exit(ERROR_MISSING_PROPERTY);
		}
		e.printStackTrace(System.err);
	}

	public static void main(String[] args) throws Exception {
		try {
			if (args.length != 3) {
				showUsage();
				exit(ERROR_WRONG_ARGUMENTS);
			} else {
				String pathToProperties = args[0];
				String localPath = args[1];
				String remotePath = args[2];
				SSHFileUploader uploader = new SSHFileUploader(pathToProperties);
				uploader.upload(new File(localPath), remotePath);
			}
		} catch (Exception e) {
			handleException(e);
		}
	}

}
