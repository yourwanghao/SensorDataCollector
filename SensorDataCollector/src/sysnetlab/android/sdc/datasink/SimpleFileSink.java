package sysnetlab.android.sdc.datasink;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sysnetlab.android.sdc.datacollector.Experiment;
import android.os.Environment;
import android.util.Log;

public class SimpleFileSink implements DataSink {
	String mParentPath; 
	String mPath;
	LinkedList<PrintStream> mPrintStreamList;
	
	public SimpleFileSink() {
		// TODO Auto-generated method stub
		Log.i("SensorDataCollector", "entering SimpleFileSink.constructor() ...");
		String parentPath = Environment.getExternalStorageDirectory().getPath();
		parentPath = parentPath + "/SensorData";
		File dataDir = new File(parentPath);
		if (!dataDir.exists()) {
			dataDir.mkdir();
		} 
		mParentPath = parentPath;
		
		mPrintStreamList = new LinkedList<PrintStream>();
	}
	
	@Override
	public PrintStream open(String filename) {
		PrintStream out = null;
		String path = mPath + "/" + filename;
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path)));
			mPrintStreamList.add(out);
		} catch (FileNotFoundException ex) {
			Log.e("SensorDataCollector", "Calling open:", ex);
		}
		return out;
	}

	@Override
	public void close() {
		while (!mPrintStreamList.isEmpty()) {
			PrintStream out = mPrintStreamList.remove();
			out.close();
		}
	}

	@Override
	public void createExperiment() {
		String pathPrefix = mParentPath + "/exp"; 
		DecimalFormat f = new DecimalFormat("00000");
		int i = 0;
		String path;
		File dataDir;
		do {
			i = i + 1;
			path = pathPrefix + f.format(i);
			dataDir = new File(path);
		} while (dataDir.exists());
		dataDir.mkdir();
		mPath = path;
		Log.i("SensorDataCollector", "path " + path + " does not exist and is created.");
	}

	@Override
	public List<Experiment> listExperiments() {
		List<Experiment> listExp = new ArrayList<Experiment>();
		
		File parentDir = new File(mParentPath);
		String[] experimentDirNames = parentDir.list(new FilenameFilter() {
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		
		for (String dirName : experimentDirNames)
		{
			listExp.add(new Experiment(dirName,"unknown"));
		}
		
		return listExp;
	}
}
