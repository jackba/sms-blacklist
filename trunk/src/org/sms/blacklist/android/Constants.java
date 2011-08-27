package org.sms.blacklist.android;

public class Constants {
	public final static String LOGTAG = "SMS Blacklist";
	
	public final static String DATABASES_NAME = "databases.sqlite";
	public static final int DATABASE_VERSION = 1;
	public final static String DATABASES_TABLE_RULES = "rules";
	public final static String DATABASES_TABLE_MESSAGES = "messages";
	public final static String DATABASES_CREATE_RULES = "CREATE TABLE rules (_id INTEGER PRIMARY KEY, rule TEXT NOT NULL, type INTEGER NOT NULL, enabled TEXT NOT NULL)";
	public final static String DATABASES_CREATE_MESSAGES = "CREATE TABLE messages (_id INTEGER PRIMARY KEY, timestamp LONG NOT NULL, number TEXT NOT NULL, body TEXT, unread TEXT NOT NULL)";
	public final static String DATABASES_UPGRADE_RULES = "DROP TABLE IF EXISTS rules";
	public final static String DATABASES_UPGRADE_MESSAGES = "DROP TABLE IF EXISTS messages";
	
	public final static int TYPE_BLOCKED_NUMBER = 0;
	public final static int TYPE_TRUSTED_NUMBER = 1;
	public final static int TYPE_BLOCKED_KEYWORD = 2;
	public final static int TYPE_ONLY_TRUSTED_NUMBER = 3;
	
	public final static int RESULT_EDIT = 0;
	public final static int RESULT_OPEN = 1;

}