
package sysnetlab.android.sdc.sensor.audio;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AudioRecordSettingDataSource {

    private SQLiteDatabase database;
    private AudioRecordSettingDBHelper dbHelper;
    private String[] allColumns = {
            AudioRecordSettingDBHelper.COLUMN_NAME_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_SAMPLING_RATE,
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_RES_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_RES_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_RES_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_MIN_BUFFER_SIZE
    };
    private String[] audioSourceColumns = {
            AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_RES_ID
    };
    private String[] audioChannelInColumns = {
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_RES_ID
    };
    private String[] audioEncodingColumns = {
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_ID,
            AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_RES_ID
    };
    private String[] audioSamplingRateColumn = {
            AudioRecordSettingDBHelper.COLUMN_NAME_SAMPLING_RATE
    };
    private String[] audioMinBufferSizeColumn = {
            AudioRecordSettingDBHelper.COLUMN_NAME_MIN_BUFFER_SIZE
    };
    private String[] statusColumns = {
            AudioRecordSettingDBHelper.COLUMN_NAME_STATUS
    };

    public AudioRecordSettingDataSource(Context context) {
        dbHelper = new AudioRecordSettingDBHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    private boolean insertAudioRecordParameter(AudioRecordParameter audioRecordParam) {
        ContentValues values = new ContentValues();

        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_SAMPLING_RATE,
                audioRecordParam.getSamplingRate());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_ID, audioRecordParam
                .getChannel().getChannelId());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_RES_ID, audioRecordParam
                .getChannel().getChannelNameResId());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_ID, audioRecordParam
                .getEncoding().getEncodingId());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_RES_ID, audioRecordParam
                .getEncoding().getEncodingNameResId());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID, audioRecordParam
                .getSource().getSourceId());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_RES_ID, audioRecordParam
                .getSource().getSourceNameResId());
        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_MIN_BUFFER_SIZE,
                audioRecordParam.getMinBufferSize());

        if (database.insert(AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS, null, values) == -1) {
            return false;
        } else {
            return true;
        }
    }

    private boolean makeDataSourceReady() {
        ContentValues values = new ContentValues();

        values.put(AudioRecordSettingDBHelper.COLUMN_NAME_STATUS, 1);

        if (database.insert(AudioRecordSettingDBHelper.TABLE_AUDIORECORDDISCOVERSTATUS, null,
                values) == -1) {
            return false;
        } else {
            return true;
        }
    }

    private boolean addAllAudioRecordParameters(List<AudioRecordParameter> params) {
        database.beginTransaction();
        for (AudioRecordParameter p : params) {
            if (!insertAudioRecordParameter(p)) {
                database.endTransaction();
                return false;
            }
        }

        if (!makeDataSourceReady()) {
            database.endTransaction();
            return false;
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        return true;
    }

    public boolean prepareDataSource() {
        List<AudioRecordParameter> params = AudioSensorHelper.getValidRecordingParameters();
        return addAllAudioRecordParameters(params);
    }

    public List<AudioRecordParameter> getAllAudioRecordParameters() {
        List<AudioRecordParameter> params = new ArrayList<AudioRecordParameter>();

        Cursor cursor = database.query(AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            AudioRecordParameter p = cursorToAudioRecordParameter(cursor);
            params.add(p);
            cursor.moveToNext();
        }

        cursor.close();

        Log.i("SensorDataCollector",
                "AudioRecordSettingDataSource::getAllAudioRecodParameters() returns "
                        + params.size() + " parameters");
        return params;
    }

    public List<AudioChannelIn> getAllAudioChannelIns() {
        List<AudioChannelIn> listAudioChannelIn = new ArrayList<AudioChannelIn>();

        Cursor cursor = database.query(true, AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                audioChannelInColumns, null, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            AudioChannelIn c = cursorToAudioChannelIn(cursor);
            listAudioChannelIn.add(c);
            cursor.moveToNext();
        }

        cursor.close();

        return listAudioChannelIn;
    }

    public List<AudioSource> getAllAudioSources() {
        List<AudioSource> listAudioSources = new ArrayList<AudioSource>();

        Cursor cursor = database.query(true, AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                audioSourceColumns, null, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            AudioSource s = cursorToAudioSource(cursor);
            listAudioSources.add(s);
            cursor.moveToNext();
        }

        cursor.close();

        return listAudioSources;
    }

    public List<AudioChannelIn> getAllAudioChannelIns(AudioSource source) {
        List<AudioChannelIn> listChannels = new ArrayList<AudioChannelIn>();

        String selection = AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID + "="
                + source.getSourceId();
        Cursor cursor = database.query(true, AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                audioChannelInColumns, selection, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            AudioChannelIn c = cursorToAudioChannelIn(cursor);
            listChannels.add(c);
            cursor.moveToNext();
        }

        cursor.close();

        return listChannels;
    }

    public List<AudioEncoding> getAllAudioEncodings(AudioSource source, AudioChannelIn channel) {
        List<AudioEncoding> listEncodings = new ArrayList<AudioEncoding>();

        String selection = AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID + "="
                + source.getSourceId() + " and "
                + AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_ID + "="
                + channel.getChannelId();

        Cursor cursor = database.query(true, AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                audioEncodingColumns, selection, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            AudioEncoding e = cursorToAudioEncoding(cursor);
            listEncodings.add(e);
            cursor.moveToNext();
        }

        cursor.close();

        return listEncodings;
    }

    public List<Integer> getAllSamplingRates(AudioSource source, AudioChannelIn channel,
            AudioEncoding encoding) {
        List<Integer> listSamplingRates = new ArrayList<Integer>();

        String selection = AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID + "="
                + source.getSourceId() + " and "
                + AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_ID + "="
                + channel.getChannelId() + " and "
                + AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_ID + "="
                + encoding.getEncodingId();

        Cursor cursor = database.query(true, AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                audioSamplingRateColumn, selection, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int r = cursorToSamplingRate(cursor);
            listSamplingRates.add(r);
            cursor.moveToNext();
        }

        cursor.close();

        return listSamplingRates;
    }

    public int getMinBufferSize(AudioSource source, AudioChannelIn channel, AudioEncoding encoding,
            int samplingRate) {
        String selection = AudioRecordSettingDBHelper.COLUMN_NAME_AUDIO_SOURCE_ID + "="
                + source.getSourceId() + " and "
                + AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_IN_ID + "="
                + channel.getChannelId() + " and "
                + AudioRecordSettingDBHelper.COLUMN_NAME_CHANNEL_ENCODING_ID + "="
                + encoding.getEncodingId() + " and "
                + AudioRecordSettingDBHelper.COLUMN_NAME_SAMPLING_RATE + "=" + samplingRate;

        Cursor cursor = database.query(true, AudioRecordSettingDBHelper.TABLE_AUDIORECORDSETTINGS,
                audioMinBufferSizeColumn, selection, null, null, null, null, null);

        int minBufferSize = -1;

        cursor.moveToFirst();
        if (!cursor.isAfterLast()) {
            minBufferSize = cursorToMinBufferSize(cursor);
        }

        cursor.close();

        return minBufferSize;
    }

    public boolean isDataSourceReady() {

        Cursor cursor = database.query(AudioRecordSettingDBHelper.TABLE_AUDIORECORDDISCOVERSTATUS,
                statusColumns, null, null, null, null, null);

        cursor.moveToFirst();

        int status = 0;
        if (!cursor.isAfterLast()) {
            status = cursor.getInt(0);
        }

        cursor.close();

        if (status > 0) {
            return true;
        } else {
            return false;
        }
    }

    private AudioRecordParameter cursorToAudioRecordParameter(Cursor cursor) {
        AudioRecordParameter param = new AudioRecordParameter();

        param.setSamplingRate(cursor.getInt(1));
        param.setChannel(new AudioChannelIn(cursor.getInt(2), cursor.getInt(3)));
        param.setEncoding(new AudioEncoding(cursor.getInt(4), cursor.getInt(5)));
        param.setSource(new AudioSource(cursor.getInt(6), cursor.getInt(7)));
        param.setMinBufferSize(cursor.getInt(8));

        return param;
    }

    private AudioChannelIn cursorToAudioChannelIn(Cursor cursor) {
        return new AudioChannelIn(cursor.getInt(0), cursor.getInt(1));
    }

    private AudioSource cursorToAudioSource(Cursor cursor) {
        return new AudioSource(cursor.getInt(0), cursor.getInt(1));
    }

    private AudioEncoding cursorToAudioEncoding(Cursor cursor) {
        return new AudioEncoding(cursor.getInt(0), cursor.getInt(1));
    }

    private int cursorToSamplingRate(Cursor cursor) {
        return cursor.getInt(0);
    }

    private int cursorToMinBufferSize(Cursor cursor) {
        return cursor.getInt(0);
    }
}
