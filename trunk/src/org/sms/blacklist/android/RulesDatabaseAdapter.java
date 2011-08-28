package org.sms.blacklist.android;

import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class RulesDatabaseAdapter {
	public static final String KEY_RULEID = "_id";
	public static final String KEY_RULE = "rule";
	public static final String KEY_TYPE = "type";
	public static final String KEY_ENABLED = "enabled";
	
	private final Context context; 
	
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public RulesDatabaseAdapter(Context ctx) {
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
	public RulesDatabaseAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	/* closes the database */
	public void close() {
		DBHelper.close();
	}
	
	/* insert a rule into the database */
	public void insertRule(String rule, int type, String enabled) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_RULE, rule);
		initialValues.put(KEY_TYPE, type);
		initialValues.put(KEY_ENABLED, enabled);
		db.insert(Constants.DATABASES_TABLE_RULES, null, initialValues);
	}

	/* update a rule in the adtabase */
	public void updateRule(int ruleId, String rule, int type, String enabled) {
		ContentValues args = new ContentValues();
		args.put(KEY_RULE, rule);
		args.put(KEY_TYPE, type);
		args.put(KEY_ENABLED, enabled);
		db.update(Constants.DATABASES_TABLE_RULES, args, KEY_RULEID + "=" + ruleId, null);
	}
	
	/* deletes a rule */
	public void deleteRule(int ruleId) {
		db.delete(Constants.DATABASES_TABLE_RULES, KEY_RULEID + "=" + ruleId, null);
	}

	/* deletes all rules */
	public void deleteAllRules() {
		db.delete(Constants.DATABASES_TABLE_RULES, null, null);
	}
	
	/* retrieves all the rules */
	public Cursor getAllRules(String selection) {
		return db.query(Constants.DATABASES_TABLE_RULES, new String[] {KEY_RULEID, KEY_RULE, KEY_TYPE, KEY_ENABLED}, selection, null, null, null, null);
	}

	/* retrieves a rule */
	public Cursor getRule(int ruleId) throws SQLException {
		Cursor mCursor =
				db.query(true, Constants.DATABASES_TABLE_RULES, new String[] {KEY_RULE, KEY_TYPE, KEY_ENABLED}, KEY_RULEID + "=" + ruleId, null, null, null, 	null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/* enable a rule */
	public void enableRule(int ruleId) {
		ContentValues args = new ContentValues();
		args.put(KEY_ENABLED, "true");
		db.update(Constants.DATABASES_TABLE_RULES, args, KEY_RULEID + "=" + ruleId, null);
		Cursor cursor = getRule(ruleId);
		String rule = cursor.getString(0);
		int type = cursor.getInt(1);
		if (type == Constants.TYPE_BLOCKED_NUMBER_REGEXP||type == Constants.TYPE_BLOCKED_KEYWORD_REGEXP) {
			String rnumber = String.valueOf((int)(Math.random() * 10));
			try {
				Pattern.matches(rule, rnumber);
			} catch (RuntimeException e) {
				updateRule(ruleId, rule, type, "error");
			}
		}
		cursor.close();
	}
	
	/* disable a rule */
	public void disableRule(int ruleId) {
		ContentValues args = new ContentValues();
		args.put(KEY_ENABLED, "false");
		db.update(Constants.DATABASES_TABLE_RULES, args, KEY_RULEID + "=" + ruleId, null);
	}
	
	/* enable all rules */
	public void enableAllRules() {
		ContentValues args = new ContentValues();
		args.put(KEY_ENABLED, "true");
		db.update(Constants.DATABASES_TABLE_RULES, args, null, null);
		Cursor cursor = getAllRules(KEY_TYPE + "= '" + String.valueOf(Constants.TYPE_BLOCKED_NUMBER_REGEXP) + "' OR "+ KEY_TYPE +"= '" + String.valueOf(Constants.TYPE_BLOCKED_KEYWORD_REGEXP) +"'");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			int ruleId = cursor.getInt(0);
			String rule = cursor.getString(1);
			int type = cursor.getInt(2);
			String rnumber = String.valueOf((int)(Math.random() * 10));
			try {
				Pattern.matches(rule, rnumber);
			} catch (RuntimeException e) {
				updateRule(ruleId, rule, type, "error");
			}
			cursor.moveToNext(); 
		  }
		  cursor.close();
	}
	
	/* disable all rules */
	public void disableAllRules() {
		ContentValues args = new ContentValues();
		args.put(KEY_ENABLED, "false");
		db.update(Constants.DATABASES_TABLE_RULES, args, null, null);
	}
}