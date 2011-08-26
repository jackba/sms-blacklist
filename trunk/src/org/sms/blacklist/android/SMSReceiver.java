package org.sms.blacklist.android;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {
	
	private String[] keysBlacklist;
	private String[] keysWhitelist;
	private String[] keysKeyword;
	
	private long smsTimestamp;
	private String smsNumber;
	private String smsBody;
	
	private static final String PATTERN_LINE_START = "^" ;
	private static final String PATTERN_LINE_END = "$" ;
	private static final char[] META_CHARACTERS = { '$', '^', '[', ']', '(', ')', '{', '}', '|', '+', '.', '\\' };
	
	private SQLiteDatabase rDatabase;
	private SQLiteDatabase mDatabase;

	@Override
	public void onReceive(Context context, Intent intent) {

		RulesDatabaseHelper rhelper = new RulesDatabaseHelper(context);
		rDatabase = rhelper.getReadableDatabase();
		
		MessagesDatabaseHelper mhelper = new MessagesDatabaseHelper(context);
		mDatabase = mhelper.getWritableDatabase();
		
		readRules();
		
		Bundle bundle = intent.getExtras();
		Object messages[] = (Object[]) bundle.get("pdus");
		SmsMessage smsMessage[] = new SmsMessage[messages.length];
		for (int n = 0; n < messages.length; n++) {
			smsMessage[n] = SmsMessage.createFromPdu((byte[]) messages[n]);
			smsTimestamp = smsMessage[n].getTimestampMillis();
			smsNumber = smsMessage[n].getOriginatingAddress();
			smsBody = smsMessage[n].getMessageBody();

			for (String keyWhitelist : keysWhitelist) {
				if (wildcardMatch(keyWhitelist, smsNumber)){
					return;
				} 
			}

			for (String keyBlacklist : keysBlacklist){
				if (wildcardMatch(keyBlacklist, smsNumber)){
					blockMessage();
					return;
				}
			}
			for (String keyKeyword : keysKeyword) {
				if (wildcardMatch(keyKeyword, smsBody)){
					blockMessage();
					return;
				}
			}			
		}
		rDatabase.close();
		mDatabase.close();
	}
	
	private void blockMessage(){
		this.abortBroadcast();
			//Log blocked messages in database
			ContentValues insertValues = new ContentValues();
			insertValues.put("timestamp", smsTimestamp);
			insertValues.put("number", smsNumber);
			insertValues.put("body", smsBody);
			insertValues.put("unread", "true");
			mDatabase.insert("messages", "", insertValues);
	}
	
	private void readRules(){	
		String[] columns={"rule"}; 
		
		Cursor cursorBlacklist=rDatabase.query("rules", columns, "type='"+Constants.TYPE_BLACKLIST+"' and enabled='true'", null, null, null, null);
		List<String> listBlacklist = new ArrayList<String>();
		cursorBlacklist.moveToFirst();
		while (!cursorBlacklist.isAfterLast()) { 
			String rule=cursorBlacklist.getString(0); 
			listBlacklist.add(rule);
			cursorBlacklist.moveToNext();
		  } 
		  cursorBlacklist.close();
		  keysBlacklist = listBlacklist.toArray(new String[listBlacklist.size()]);
		
		Cursor cursorWhitelist=rDatabase.query("rules", columns, "type='"+Constants.TYPE_WHITELIST+"' and enabled='true'", null, null, null, null);
		List<String> listWhitelist = new ArrayList<String>();
		cursorWhitelist.moveToFirst();
		while (!cursorWhitelist.isAfterLast()) { 
			String rule=cursorWhitelist.getString(0); 
			listWhitelist.add(rule);
			cursorWhitelist.moveToNext(); 
		  } 
		  cursorWhitelist.close();
		  keysWhitelist = listWhitelist.toArray(new String[listWhitelist.size()]);
		  
	  Cursor cursorKeyword=rDatabase.query("rules", columns, "type='"+Constants.TYPE_KEYWORD+"' and enabled='true'", null, null, null, null);
		List<String> listKeyword = new ArrayList<String>();
		cursorKeyword.moveToFirst();
		while (!cursorKeyword.isAfterLast()) { 
			String rule=cursorKeyword.getString(0); 
			listKeyword.add(rule);
			cursorKeyword.moveToNext(); 
		  } 
		  cursorKeyword.close();
		 keysKeyword = listKeyword.toArray(new String[listKeyword.size()]);

	}

    public static boolean wildcardMatch(String pattern, String str) {
        pattern = convertToRegexPattern(pattern);
        return Pattern.matches(pattern, str);
    }

    private static String convertToRegexPattern(String wildcardString) {
        String result = PATTERN_LINE_START ;
        char[] chars = wildcardString.toCharArray() ;
        for (char ch : chars) {
            if (Arrays.binarySearch(META_CHARACTERS, ch)>=0) {
                result += "\\" + ch ;
                continue ;
            }
            switch (ch) {
                case '*':
                    result += ".*";
                    break;
                case '?':
                    result += ".{0,1}";
                    break;
                default:
                    result += ch;
            }
        }
        result += PATTERN_LINE_END ;
        return result;
    }
	
	private class MessagesDatabaseHelper extends SQLiteOpenHelper {
			public MessagesDatabaseHelper(Context context) {
					super(context, "messages.sqlite", null, 1);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
					String scripts = "create table messages (_id integer primary key, timestamp long not null, number text not null, body text, unread text not null);";
					db.execSQL(scripts);
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			}
	}
	
	private class RulesDatabaseHelper extends SQLiteOpenHelper {
		public RulesDatabaseHelper(Context context) {
				super(context, "rules.sqlite", null, 1);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
				String scripts = "create table rules (_id integer primary key, rule text not null, type integer not null, enabled text not null);";
				db.execSQL(scripts);
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}
}