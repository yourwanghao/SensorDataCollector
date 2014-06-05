
package sysnetlab.android.sdc.datastore;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import sysnetlab.android.sdc.datacollector.DeviceInformation;
import sysnetlab.android.sdc.datacollector.Experiment;
import sysnetlab.android.sdc.datacollector.Note;
import sysnetlab.android.sdc.datacollector.Tag;
import sysnetlab.android.sdc.datacollector.TaggingAction;
import sysnetlab.android.sdc.sensor.AbstractSensor;
import sysnetlab.android.sdc.sensor.AndroidSensor;
import sysnetlab.android.sdc.sensor.SensorUtilsSingleton;
import android.util.Log;

/**
 * Assumptions The application allows a single experiment being run. No two
 * experiments are allowed to run concurrently.
 */
public class DbxSimpleFileStore extends AbstractStore {
    private final String DIR_PREFIX = "exp";
    private final String DEFAULT_DATAFILE_PREFIX = "sdc";
    private String mParentPath;
    private String mNewExperimentPath;

    private int mNextExperimentNumber;
    private int mNextChannelNumber;
    
    private DbxFileSystem mDbxFs; 

    public DbxSimpleFileStore() throws RuntimeException {
    	
        Log.i("SensorDataCollector",
                "entering DbxSimpleFileStore.constructor() ...");

        mParentPath = "/SensorData";
        DbxAccountManager dbxAcctMgr = StoreSingleton.getDbxAccountManager();
        try {
			mDbxFs = DbxFileSystem.forAccount(dbxAcctMgr.getLinkedAccount());
		} catch (Unauthorized e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        //mDbxFs.createFolder(new DbxPath(mParentPath));
        

        String pathPrefix = mParentPath + "/" + DIR_PREFIX;
        DecimalFormat f = new DecimalFormat("00000");
        String path = "";

        mNextExperimentNumber = 0;
        try {
        	do {
        		mNextExperimentNumber++;
        		path = pathPrefix + f.format(mNextExperimentNumber);
        	} while (mDbxFs.exists(new DbxPath(path)));
        } catch(DbxException e) {
            throw new RuntimeException(
                    "DbxSimpleFileStore::DbxSimpleFileStore(): failed to create directory "
                            + path);
        }
    }

    @Override
    public void setupNewExperimentStorage(Experiment experiment) throws RuntimeException {
        DecimalFormat f = new DecimalFormat("00000");
        mNewExperimentPath = mParentPath + "/" + DIR_PREFIX
                + f.format(mNextExperimentNumber);

        try {
        	mDbxFs.createFolder(new DbxPath(mNewExperimentPath));
        } catch (DbxException e) {
            throw new RuntimeException(
                    "SimpleFileStore::addExperiment(): failed to create directory "
                            + mNewExperimentPath);
        }
        mNextExperimentNumber++;
        mNextChannelNumber = 1;
        mChannels = new ArrayList<Channel>();
    }

    @Override
    public List<Experiment> listStoredExperiments() {
    	Log.i("DbxSimpleFileStore", "listStoredExperiments called");
        List<Experiment> listExperiments = new ArrayList<Experiment>();

        DecimalFormat f = new DecimalFormat("00000");
        for (int i = 1; i < mNextExperimentNumber; i++) {
            String dirName = DIR_PREFIX + f.format(i);
            String pathPrefix = mParentPath + "/" + dirName;

            Experiment experiment = loadExperiment(dirName, pathPrefix);
            if (experiment != null)
                listExperiments.add(experiment);
        }
        return listExperiments;
    }

    @Override
    public void writeExperimentMetaData(Experiment experiment) {
        String configFilePath = mNewExperimentPath + "/.experiment";
        DbxFile file;
    	try {
    		file = mDbxFs.create(new DbxPath(configFilePath));
    	}
    	catch(DbxException e) {
    		throw new RuntimeException(
                    "SimpleFileStore::writeExperimentMetaData(): failed to create .experiment file");
    	}

        PrintStream out;
        try {
            out = new PrintStream(new BufferedOutputStream(file.getWriteStream()));

            out.println(experiment.getName());
            out.println(experiment.getDateTimeCreatedAsString());
            out.println(experiment.getDateTimeDoneAsString());

            out.println(experiment.getTags().size());
            for (Tag tag : experiment.getTags()) {
                out.println(tag.getName());
                out.println(tag.getShortDescription());
                out.println(tag.getLongDescription());
            }

            out.println(experiment.getNotes().size());
            for (Note note : experiment.getNotes()) {
                out.println(note.getDateCreatedAsString());
                out.println(note.getNote());
            }

            out.println(experiment.getDeviceInformation().getManufacturer());
            out.println(experiment.getDeviceInformation().getModel());

            out.println(experiment.getSensors().size());
            for (AbstractSensor sensor : experiment.getSensors()) {
                out.println(sensor.getName());
                out.println(sensor.getMajorType());
                out.println(sensor.getMinorType());
                switch(sensor.getMajorType()) {
                    case AbstractSensor.ANDROID_SENSOR:
                        out.println(((AndroidSensor) sensor).getListener().getChannel().describe());
                        break;
                }
            }
            
            out.println(experiment.getTaggingActions().size());
            for (TaggingAction taggingAction : experiment.getTaggingActions()) {
                out.println(taggingAction.toString());
            }

            out.close();
        } catch (Exception e) {
            Log.e("SensorDataCollector",
                    "SimpleFileStore::writeExperimentMetaData(): failed to write to " +
                            configFilePath);
            e.printStackTrace();
        }
    }

    protected Experiment loadExperiment(String dirName, String parentDir) {
    	Log.v("DbxSimpleFileStore", "loadExperiment dirName: " + dirName + " parentDir: " + parentDir);
        String configFilePath = parentDir + "/.experiment";
        String name = null, dateTimeCreated = null;
        Experiment experiment = null;

        try {
            BufferedReader in;
            InputStreamReader isr;
            DbxFile file; 
            

            if (mDbxFs.exists(new DbxPath(configFilePath))) {
                String dateTimeDone;
                Log.v("DbxSimpleFileStore", "Exists.  Attempting to open: " + configFilePath);
                file = mDbxFs.open(new DbxPath(configFilePath));
                Log.v("DbxSimpleFileStore", "Opened: " + configFilePath);
                isr = new InputStreamReader(file.getReadStream());
                in = new BufferedReader(isr);
                Log.v("DbxSimpleFileStore", "BufferedReader created: " + configFilePath);

                name = in.readLine();
                dateTimeCreated = in.readLine();
                dateTimeDone = in.readLine();
                
                experiment = new Experiment();
                experiment.setName(name);
                experiment.setDateTimeCreatedFromString(dateTimeCreated);
                experiment.setDateTimeDoneFromString(dateTimeDone);

                int n;
                n = Integer.parseInt(in.readLine());
                if (n > 0) {
                    ArrayList<Tag> tags = new ArrayList<Tag>();

                    for (int i = 0; i < n; i++) {
                        String tagName = in.readLine();
                        String tagShortDesc = in.readLine();
                        String tagLongDesc = in.readLine();

                        Tag tag = new Tag(tagName, tagShortDesc, tagLongDesc);
                        tags.add(tag);
                    }
                    experiment.setTags(tags);
                }


                n = Integer.parseInt(in.readLine());
                if (n > 0) {
                    ArrayList<Note> notes = new ArrayList<Note>();

                    for (int i = 0; i < n; i++) {
                        String dateTime = in.readLine();
                        String noteText = in.readLine();

                        Note note = new Note(noteText);
                        note.setDateCreatedFromString(dateTime);
                        notes.add(note);
                    }
                    
                    experiment.setNotes(notes);
                }

                String make = in.readLine();
                String model = in.readLine();
                DeviceInformation deviceInfo = new DeviceInformation(make, model);

                experiment.setDeviceInformation(deviceInfo);

                ArrayList<AbstractSensor> lstSensors = new ArrayList<AbstractSensor>();
                int numSensors = Integer.parseInt(in.readLine());
                for (int i = 0; i < numSensors; i++) {
                    String sensorName = in.readLine();
                    int sensorMajorType = Integer.parseInt(in.readLine());
                    int sensorMinorType = Integer.parseInt(in.readLine());
                    String channelDescriptor = "";
                    switch (sensorMajorType) {
                        case AbstractSensor.ANDROID_SENSOR:
                            channelDescriptor = in.readLine();
                            break;
                    }

                    // TODO make sure channel is read-only
                    Channel channel = new SimpleFileChannel(channelDescriptor, Channel.READ_ONLY);
                    AbstractSensor sensor = SensorUtilsSingleton.getInstance().getSensor(sensorName,
                            sensorMajorType,
                            sensorMinorType, channel);
                    lstSensors.add(sensor);
                }
                experiment.setSensors(lstSensors);
                
                file.close();

                Log.i("SensorDataCollector",
                        "SimpleFileStore::loadExperiment(): load experiment("
                                + experiment.getName() + ", " + experiment.getDateTimeCreatedAsString()
                                + ") successfully.");
                return experiment;
            } else {
                file = mDbxFs.open(new DbxPath(parentDir));

                Date dateCreated = file.getInfo().modifiedTime;
                name = dirName;

                Log.w("SensorDataCollector",
                        "SimpleFileStore::loadExperiment(): no configuraiton file is found for " + name);
                file.close();
                return new Experiment(name, dateCreated);
            }
        } catch (NumberFormatException e) {
        	Log.i("DbxSimpleFileStore", e.toString());
            if (name != null && dateTimeCreated != null) {
                Log.w("SensorDataCollector",
                        "SimpleFileStore::loadExperiment(): Found an old configuration file for " + name);
                return null;
            }

            Log.e("SensorDataCollector", "SimpleFileStore::loadExperiment(): " +
                    "Failed to load the configuration file.");
            e.printStackTrace();

            return null;

        } catch (IOException e) {
        	Log.i("DbxSimpleFileStore", e.toString());
            if (name != null && dateTimeCreated != null) {
                Log.w("SensorDataCollector",
                        "SimpleFileStore::loadExperiment(): Found an old configuration file for " + name);
                return null;
            }

            Log.e("SensorDataCollector", "SimpleFileStore::loadExperiment(): " +
                    "Failed to load the configuration file.");
            e.printStackTrace();

            return null;
        } catch (RuntimeException e) {
        	Log.i("DbxSimpleFileStore", e.toString());
            Log.w("SensorDataCollector",
                    "SimpleFileStore::loadExperiment(): Found an old configuration file "
                            + experiment.getName() + ", " + experiment.getDateTimeCreatedAsString());
            return experiment;
        }
    }

    public class SimpleFileChannel extends AbstractStore.Channel {
        private PrintStream mOut;
        private BufferedReader mIn;
        private String mPath;
        private DbxFile mFile;

        protected SimpleFileChannel() {
            // prohibit from creating SimpleFileChannel object without an argument
        }

        public SimpleFileChannel(String path) throws FileNotFoundException {
            this(path, WRITE_ONLY);
        }
        
        public SimpleFileChannel(String path, int flags) throws FileNotFoundException {
        	try {
	            if (flags == READ_ONLY) {
	                mOut = null;
					if (mDbxFs.exists(new DbxPath(path))) {
						mFile = mDbxFs.open(new DbxPath(path));
						InputStreamReader isr = new InputStreamReader(mFile.getReadStream());
					    mIn = new BufferedReader(isr);
					} else {
					    throw new RuntimeException("SimpleFileChannel: cannot open file " + path);
					}
	            } else if (flags == WRITE_ONLY) {
	            	mFile = mDbxFs.create(new DbxPath(path));
	                mOut = new PrintStream(new BufferedOutputStream(
	                        mFile.getWriteStream()));
	                mIn = null;
	                mPath = path;
	            } else {
	                throw new RuntimeException(
	                        "SimpleFileChannel: encountered unsupported creation flag " + flags);
	            }
        	} catch (Exception e) {
        		throw new RuntimeException("SimpleFileChannel");
        	}
        }

        public void close() {
            if (mOut != null) {
            	mOut.close();
            	mFile.close();
            }
            if (mIn != null) {
                try {
                    mIn.close();
                    mFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("SensorDataCollector", "close error in SimpleFileStore::close().");
                }
            }
        }

        @Override
        public void write(String s) {
            mOut.print(s);
        }
        
        @Override
        public String read() {
            if (mIn != null) {
                try {
                    return mIn.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("SensorDataCollector", "read error in SimpleFileStore::read().");
                    return null;
                }
            } else {
                return null;
            }
        }


        @Override
        public void reset() {
            if (mIn != null) {
                try {
                    mIn.close();
                    mIn = new BufferedReader(new FileReader(mPath));
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("SensorDataCollector", "reset error in SimpleFileStore::mark().");              
                }
            }
        }        

        @Override
        public void open() {
        }

        public String describe() {
            // return the value passed to the constructor
            return mPath;
        }
    }

    @Override
    public Channel createChannel(String tag) {

        String path;

        if (tag == null || tag.trim().length() == 0) {
            DecimalFormat f = new DecimalFormat("00000");
            path = mNewExperimentPath + "/" + DEFAULT_DATAFILE_PREFIX
                    + f.format(mNextChannelNumber) + ".txt";
            mNextChannelNumber++;
        } else {
            path = mNewExperimentPath + "/" + tag.replace(' ', '_') + ".txt";
        }

        try {
            Channel channel;
            channel = new SimpleFileChannel(path);
            mChannels.add(channel);

            return channel;
        } catch (FileNotFoundException e) {
            Log.e("SensorDataCollector",
                    "SimpleFileStore::getChannel: cannot open the channel file: "
                            + path);
            e.printStackTrace();

            return null;
        }
    }

    @Override
    public void closeAllChannels() {
        for (Channel channel : mChannels) {
            channel.close();
        }
        // create new channel list and garbage-collect the old channel list
        mChannels = new ArrayList<Channel>();
    }

    public String getNewExperimentPath() {
        return mNewExperimentPath;
    }
    
    public int getNextExperimentNumber() {
        return mNextExperimentNumber;
    }
    
    public int getNextChannelNumber() {
        return mNextChannelNumber;
    }

}