package sysnetlab.android.sdc.services;

import java.util.ArrayList;
import java.util.Iterator;

import sysnetlab.android.sdc.datacollector.AndroidSensorEventListener;
import sysnetlab.android.sdc.datacollector.Experiment;
import sysnetlab.android.sdc.datastore.StoreSingleton;
import sysnetlab.android.sdc.datastore.AbstractStore.Channel;
import sysnetlab.android.sdc.sensor.AbstractSensor;
import sysnetlab.android.sdc.sensor.AndroidSensor;
import sysnetlab.android.sdc.sensor.SensorDiscoverer;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;

public class RunExperimentService extends IntentService{

	private SensorManager mSensorManager;
	private int mCheckedSensors;
	private IBinder mBinder;
	
	public RunExperimentService() {
		super("runexperimentservice");
		mBinder=new RunExperimentBinder();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mCheckedSensors=0;
		
		Experiment experiment=intent.getParcelableExtra("experiment");
		StoreSingleton.getInstance().setupExperiment();
        Iterator<AbstractSensor> iter = SensorDiscoverer.discoverSensorList(this).iterator();
        ArrayList<AbstractSensor> lstSensors = new ArrayList<AbstractSensor>();

        while (iter.hasNext()) {
            AndroidSensor sensor = (AndroidSensor) iter.next();
            if (sensor.isSelected()) {
                Channel channel = StoreSingleton.getInstance().createChannel(sensor.getName());
                AndroidSensorEventListener listener =
                        new AndroidSensorEventListener(channel);
                sensor.setListener(listener);
                mSensorManager.registerListener(listener, (Sensor) sensor.getSensor(),
                        sensor.getSamplingInterval());
                lstSensors.add(sensor);
                mCheckedSensors++;
            }
        }
        experiment.setSensors(lstSensors);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class RunExperimentBinder extends Binder {
		public int getCheckedSensors(){
			return mCheckedSensors;
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Iterator<AbstractSensor> iter = SensorDiscoverer.discoverSensorList(this).iterator();
        while (iter.hasNext()) {
            AndroidSensor sensor = (AndroidSensor) iter.next();
            if (sensor.isSelected()) {
                AndroidSensorEventListener listener = sensor.getListener();
                mSensorManager.unregisterListener(listener);
            }
        }
	}
}