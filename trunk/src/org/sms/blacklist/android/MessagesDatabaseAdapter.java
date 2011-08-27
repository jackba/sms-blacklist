package org.sms.blacklist.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MessagesDatabaseAdapter {
	public static final String KEY_MESSAGEID = "_id";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_NUMBER = "number";
	public static final String KEY_BODY = "body";
	public static final String KEY_UNREAD = "unread";
	
	private final Context context; 
	
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public MessagesDatabaseAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}
		
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, Constants.DATABASES_NAME, null, Constants.DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(Constants.DATABASES_CREATE_RULES);
			db.execSQL(Constants.DATABASES_CREATE_MESSAGES);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(Constants.LOGTAG, "Upgrading database from version " + oldVersion 
					+ " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL(Constants.DATABASES_UPGRADE_RULES);
			db.execSQL(Constants.DATABASES_UPGRADE_MESSAGES);
			onCreate(db);
		}
	}	
	
	/* opens the database */
	public MessagesDatabaseAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	/* closes the database */
	public void close() {
		DBHelper.close();
	}
	
	/* insert a message into the database */
	public void insertMessage(long timestamp, String number, String body) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TIMESTAMP, timestamp);
		initialValues.put(KEY_NUMBER, number);
		initialValues.put(KEY_BODY, body);
		initialValues.put(KEY_UNREAD, "true");
		db.insert(Constants.DATABASES_TABLE_MESSAGES, null, initialValues);
	}

	/* deletes a message */
	public void deleteMessage(int messageId) {
		db.delete(Constants.DATABASES_TABLE_MESSAGES, KEY_MESSAGEID + "=" + messageId, null);
	}

	/* deletes all messages */
	public void deleteAllMessages() {
		db.delete(Constants.DATABASES_TABLE_MESSAGES, null, null);
	}
	
	/* retrieves all the messages */
	public Cursor getAllMessages() {
		return db.query(Constants.DATABASES_TABLE_MESSAGES, new String[] {KEY_MESSAGEID, KEY_TIMESTAMP, KEY_NUMBER, KEY_BODY, KEY_UNREAD}, null, null, null, null, "timestamp DESC");
	}

	/* retrieves a message */
	public Cursor getMessage(int messageId) throws SQLException {
		Cursor mCursor =
				db.query(true, Constants.DATABASES_TABLE_MESSAGES, new String[] {KEY_TIMESTAMP, KEY_NUMBER, KEY_BODY, KEY_UNREAD}, KEY_MESSAGEID + "=" + messageId, null, null, null, 	null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/* mark a message as read */
	public void markRead(int messageId) {
		ContentValues args = new ContentValues();
		args.put(KEY_UNREAD, "false");
		db.update(Constants.DATABASES_TABLE_MESSAGES, args, KEY_MESSAGEID + "=" + messageId, null);
	}
	
	/* mark all message as read */
	public void markAllRead() {
		ContentValues args = new ContentValues();
		args.put(KEY_UNREAD, "false");
		db.update(Constants.DATABASES_TABLE_MESSAGES, args, null, null);
	}
}