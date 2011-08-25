package org.sms.blacklist.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
	
	private String LOGTAG = "SMS Blacklist"; 
	private String COUNTRY_CODE = "+86";
	private boolean debugLog = true;
	private boolean saveMessages = true;
	
	private String[] keysE;
	private String[] keysS;
	private String[] keysC;
	private String[] keysK;
	private String[] keysW;
	
	private long smsTimestamp;
	private String smsNumber;
	private String smsBody;
	
	private SQLiteDatabase rDatabase;
	private SQLiteDatabase mDatabase;

	@Override
	public void onReceive(Context context, Intent intent) {

		RulesDatabaseHelper rdh = new RulesDatabaseHelper(context);
		rDatabase = rdh.getReadableDatabase();
		
		MessagesDatabaseHelper mdh = new MessagesDatabaseHelper(context);
		mDatabase = mdh.getWritableDatabase();
		
		readRules();
		
		Bundle bundle = intent.getExtras();
		Object messages[] = (Object[]) bundle.get("pdus");
		SmsMessage smsMessage[] = new SmsMessage[messages.length];
		for (int n = 0; n < messages.length; n++) {
			smsMessage[n] = SmsMessage.createFromPdu((byte[]) messages[n]);
			smsTimestamp = smsMessage[n].getTimestampMillis();
			smsNumber = smsMessage[n].getOriginatingAddress();
			smsBody = smsMessage[n].getMessageBody();

			for (String keyW : keysW) {
				if (smsNumber.equals(keyW)||smsNumber.equals(COUNTRY_CODE+keyW)){
					Log.d(LOGTAG,"Message from "+smsNumber+" is in the Whitelist! Timestamp: "+String.valueOf(smsTimestamp)+" Content: "+smsBody);
					return;
				} 
			}

			for (String keyE : keysE){
				if (smsNumber.equals(keyE)||smsNumber.equals(COUNTRY_CODE+keyE)){
					blockMessage();
					return;
				}
			}
			for (String keyS : keysS){
				if (smsNumber.startsWith(keyS)||smsNumber.startsWith(COUNTRY_CODE+keyS)){
					blockMessage();
					return;
				}
			}
			for (String keyC : keysC) {
				if (smsNumber.contains(keyC)){
					blockMessage();
					return;
				}
			}
			for (String keyK : keysK) {
				if (smsBody.contains(keyK)){
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
		if (debugLog){
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(smsTimestamp);
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm");
			Log.d(LOGTAG,"Message from "+smsNumber+" Blocked! Logged: "+String.valueOf(saveMessages)+" Time : " + formatter.format(calendar.getTime())+ " Content: "+smsBody);
		}
		if (saveMessages){
			//Log blocked messages in database
			ContentValues insertValues = new ContentValues();
			insertValues.put("timestamp", smsTimestamp);
			insertValues.put("number", smsNumber);
			insertValues.put("body", smsBody);
			insertValues.put("unread", "true");
			mDatabase.insert("messages", "", insertValues);
		}
	}
	
	private void readRules(){	
		String[] columns={"rule"}; 
		
		Cursor cursorE=rDatabase.query("rules", columns, "type='"+Constants.TYPE_E+"' and enabled='true'", null, null, null, null);
		List<String> listE = new ArrayList<String>();
		cursorE.moveToFirst();
		while (!cursorE.isAfterLast()) { 
			String rule=cursorE.getString(0); 
			listE.add(rule);
			cursorE.moveToNext();
		  } 
		  cursorE.close();
		  keysE = listE.toArray(new String[listE.size()]);
		
		Cursor cursorS=rDatabase.query("rules", columns, "type='"+Constants.TYPE_S+"' and enabled='true'", null, null, null, null);
		List<String> listS = new ArrayList<String>();
		cursorS.moveToFirst();
		while (!cursorS.isAfterLast()) { 
			String rule=cursorS.getString(0); 
			listS.add(rule);
			cursorS.moveToNext(); 
		  } 
		  cursorS.close();
		  keysS = listS.toArray(new String[listS.size()]);
		  
	  Cursor cursorC=rDatabase.query("rules", columns, "type='"+Constants.TYPE_C+"' and enabled='true'", null, null, null, null);
		List<String> listC = new ArrayList<String>();
		cursorC.moveToFirst();
		while (!cursorC.isAfterLast()) { 
			String rule=cursorC.getString(0); 
			listC.add(rule);
			cursorC.moveToNext(); 
		  } 
		  cursorC.close();
		 keysC = listC.toArray(new String[listC.size()]);
			  
		Cursor cursorK=rDatabase.query("rules", columns, "type='"+Constants.TYPE_K+"' and enabled='true'", null, null, null, null);
		List<String> listK = new ArrayList<String>();
		cursorK.moveToFirst();
			while (!cursorK.isAfterLast()) { 
				String rule=cursorK.getString(0); 
				listK.add(rule);
				cursorK.moveToNext(); 
			}
		cursorK.close();
		keysK = listK.toArray(new String[listK.size()]);
			  
		Cursor cursorW=rDatabase.query("rules", columns, "type='"+Constants.TYPE_W+"' and enabled='true'", null, null, null, null);
		List<String> listW = new ArrayList<String>();
		cursorW.moveToFirst();
			while (!cursorW.isAfterLast()) { 
				String rule=cursorW.getString(0); 
				listW.add(rule);
				cursorW.moveToNext(); 
			  } 
		cursorW.close();
		keysW = listW.toArray(new String[listW.size()]);
		  
	}
	
	private class MessagesDatabaseHelper extends SQLiteOpenHelper {
			public MessagesDatabaseHelper(Context context) {
					super(context, "blocked_messages.db", null, 1);
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
				super(context, "block_rules.db", null, 1);
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