package org.sms.blacklist.android;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
	
	private String[] blockedNumbers;
	private String[] trustedNumbers;
	private String[] blockedKeywords;
	private String[] onlyTrustedNumbers;
	private String[] blockedNumbersRegexp;
	private String[] blockedKeywordsRegexp;
	
	private long timestamp;
	private String number;
	private String body = "";
	
	private MessagesDatabaseAdapter mDatabaseAdapter;
	private RulesDatabaseAdapter rDatabaseAdapter;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		String country_code = context.getString(R.string.country_code);
		
		readRules(context);
		
		Bundle bundle = intent.getExtras();
		Object messages[] = (Object[]) bundle.get("pdus");
		SmsMessage smsMessage[] = new SmsMessage[messages.length];
		
		for (int n = 0; n < messages.length; n++) {
			smsMessage[n] = SmsMessage.createFromPdu((byte[]) messages[n]);
			timestamp = smsMessage[n].getTimestampMillis();
			number = smsMessage[n].getOriginatingAddress();
			body += smsMessage[n].getDisplayMessageBody();
			}
		
		for (String trustedNumber : trustedNumbers) {
			try {
				if (wildcardMatch(trustedNumber, number.replaceFirst("\\+"+country_code, ""))){
					// nothing to do here
					return;
				} 
			} catch (RuntimeException e) {
				deleteIncorrectedRule(trustedNumber, Constants.TYPE_TRUSTED_NUMBER, context);
				}
		}
		for (String onlyTrustedNumber : onlyTrustedNumbers){
			try {
				if (!wildcardMatch(onlyTrustedNumber, number.replaceFirst("\\+"+country_code, ""))){
					blockMessage(context);
					return;
				} 
			} catch (RuntimeException e) {
				deleteIncorrectedRule(onlyTrustedNumber, Constants.TYPE_ONLY_TRUSTED_NUMBER, context);
				}
		}
		for (String blockedNumber : blockedNumbers){
			try {
				if (wildcardMatch(blockedNumber, number.replaceFirst("\\+"+country_code, ""))){
					blockMessage(context);
					return;
				}
			} catch (RuntimeException e) {
				deleteIncorrectedRule(blockedNumber, Constants.TYPE_BLOCKED_NUMBER, context);
				}
		}
		for (String blockedKeyword : blockedKeywords) {
			try {
				if (wildcardMatch(blockedKeyword, body)){
					blockMessage(context);
					return;
				}
			} catch (RuntimeException e) {
				deleteIncorrectedRule(blockedKeyword, Constants.TYPE_BLOCKED_KEYWORD, context);
				}
			}
		for (String blockedNumberRegexp : blockedNumbersRegexp) {
			try {
				if (Pattern.matches(blockedNumberRegexp, number.replaceFirst("\\+"+country_code, ""))){
					blockMessage(context);
					return;
				}
			} catch (RuntimeException e) {
				deleteIncorrectedRule(blockedNumberRegexp, Constants.TYPE_BLOCKED_NUMBER_REGEXP, context);
				}
			}
		for (String blockedKeywordRegexp : blockedKeywordsRegexp) {
			try {
				if (Pattern.matches(blockedKeywordRegexp, number)){
					blockMessage(context);
					return;
				}
			} catch (RuntimeException e) {
				deleteIncorrectedRule(blockedKeywordRegexp, Constants.TYPE_BLOCKED_KEYWORD_REGEXP, context);
				}
			}
		}
	
	private void blockMessage(Context context){
		abortBroadcast();
		mDatabaseAdapter = new MessagesDatabaseAdapter(context);
		mDatabaseAdapter.open();
		mDatabaseAdapter.insertMessage(timestamp, number, body);
		mDatabaseAdapter.close();
	}
	
	private void readRules(Context context){	
		
		rDatabaseAdapter = new RulesDatabaseAdapter(context);
		rDatabaseAdapter.open();
		
		Cursor cursor = rDatabaseAdapter.getAllRules("type='"+Constants.TYPE_BLOCKED_NUMBER+"' and enabled='true'");
		List<String> listBlockedNumber = new ArrayList<String>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) { 
			String rule=cursor.getString(1); 
			listBlockedNumber.add(rule);
			cursor.moveToNext();
		  } 
		  cursor.close();
		  blockedNumbers = listBlockedNumber.toArray(new String[listBlockedNumber.size()]);
		
		cursor=rDatabaseAdapter.getAllRules("type='"+Constants.TYPE_TRUSTED_NUMBER+"' and enabled='true'");
		List<String> listTrustedNumber = new ArrayList<String>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) { 
			String rule=cursor.getString(1); 
			listTrustedNumber.add(rule);
			cursor.moveToNext(); 
		  } 
		  cursor.close();
		  trustedNumbers = listTrustedNumber.toArray(new String[listTrustedNumber.size()]);
		  
	  cursor = rDatabaseAdapter.getAllRules("type='"+Constants.TYPE_BLOCKED_KEYWORD+"' and enabled='true'");
		List<String> listBlockedKeyword = new ArrayList<String>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) { 
			String rule=cursor.getString(1); 
			listBlockedKeyword.add(rule);
			cursor.moveToNext(); 
		  } 
		  cursor.close();
		 blockedKeywords = listBlockedKeyword.toArray(new String[listBlockedKeyword.size()]);
		 
	 cursor = rDatabaseAdapter.getAllRules("type='"+Constants.TYPE_ONLY_TRUSTED_NUMBER+"' and enabled='true'");
		List<String> listOnlyTrustedNumber = new ArrayList<String>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) { 
			String rule=cursor.getString(1); 
			listOnlyTrustedNumber.add(rule);
			cursor.moveToNext(); 
		  } 
		  cursor.close();
		 onlyTrustedNumbers = listOnlyTrustedNumber.toArray(new String[listOnlyTrustedNumber.size()]);

	 cursor = rDatabaseAdapter.getAllRules("type='"+Constants.TYPE_BLOCKED_NUMBER_REGEXP+"' and enabled='true'");
		List<String> listBlockedNumbersRegexp = new ArrayList<String>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) { 
			String rule=cursor.getString(1); 
			listBlockedNumbersRegexp.add(rule);
			cursor.moveToNext(); 
		  } 
		  cursor.close();
		  blockedNumbersRegexp = listBlockedNumbersRegexp.toArray(new String[listBlockedNumbersRegexp.size()]);
		 
	 cursor = rDatabaseAdapter.getAllRules("type='"+Constants.TYPE_BLOCKED_KEYWORD_REGEXP+"' and enabled='true'");
		List<String> listBlockedKeywordsRegexp = new ArrayList<String>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String rule=cursor.getString(1); 
			listBlockedKeywordsRegexp.add(rule);
			cursor.moveToNext(); 
		  }
		  cursor.close();
		  blockedKeywordsRegexp = listBlockedKeywordsRegexp.toArray(new String[listBlockedKeywordsRegexp.size()]);
		 
		 rDatabaseAdapter.close();
	}

	public void deleteIncorrectedRule(String rule, int type, Context context){
		rDatabaseAdapter = new RulesDatabaseAdapter(context);
		rDatabaseAdapter.open();
		Cursor cursor = rDatabaseAdapter.getAllRules("rule='"+rule+"' AND enabled='true'"+" AND type='"+String.valueOf(type)+"'");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			int ruleId = cursor.getInt(0);
			rDatabaseAdapter.deleteRule(ruleId);
			cursor.moveToNext(); 
		  }
		  cursor.close();
		rDatabaseAdapter.close();
		Log.e(Constants.LOGTAG, "Rule '"+rule+"' have SYNTAX ERROR! It has been DELETED!");
	}
	
	public boolean wildcardMatch(String wildcard, String string) {
        String regexp = wildcard.replaceAll("\\?", ".").replaceAll("\\*", ".*");
        return Pattern.matches(regexp, string);
    }
}