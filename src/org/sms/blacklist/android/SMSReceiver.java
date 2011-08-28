package org.sms.blacklist.android;

import java.util.ArrayList;
import java.util.Arrays;
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
	private String body;
	
	private MessagesDatabaseAdapter mDatabaseAdapter;
	private RulesDatabaseAdapter rDatabaseAdapter;
	
	private static final String PATTERN_LINE_START = "^" ;
	private static final String PATTERN_LINE_END = "$" ;
	private static final char[] META_CHARACTERS = { '$', '^', '[', ']', '(', ')', '{', '}', '|', '+', '.', '\\' };

	@Override
	public void onReceive(Context context, Intent intent) {
		
		readRules(context);
		
		Bundle bundle = intent.getExtras();
		Object messages[] = (Object[]) bundle.get("pdus");
		SmsMessage smsMessage[] = new SmsMessage[messages.length];
		for (int n = 0; n < messages.length; n++) {
			smsMessage[n] = SmsMessage.createFromPdu((byte[]) messages[n]);
			timestamp = smsMessage[n].getTimestampMillis();
			number = smsMessage[n].getOriginatingAddress();
			body = smsMessage[n].getMessageBody();

			for (String trustedNumber : trustedNumbers) {
				if (wildcardMatch(trustedNumber, number)){
					// nothing to do here
					return;
				} 
			}
			for (String onlyTrustedNumber : onlyTrustedNumbers){
				if (!wildcardMatch(onlyTrustedNumber, number)){
					blockMessage(context);
					return;
				} 
			}
			for (String blockedNumber : blockedNumbers){
				if (wildcardMatch(blockedNumber, number)){
					blockMessage(context);
					return;
				}
			}
			for (String blockedKeyword : blockedKeywords) {
				if (wildcardMatch(blockedKeyword, body)){
					blockMessage(context);
					return;
				}
			}
			for (String blockedNumberRegexp : blockedNumbersRegexp) {
				try {
					if (Pattern.matches(blockedNumberRegexp, number)){
						blockMessage(context);
						return;
					}
				} catch (RuntimeException e) {
					disableIncorrectedRule(blockedNumberRegexp, Constants.TYPE_BLOCKED_NUMBER_REGEXP, context);
					}
				}
			for (String blockedKeywordRegexp : blockedKeywordsRegexp) {
				try {
					if (Pattern.matches(blockedKeywordRegexp, number)){
						blockMessage(context);
						return;
					}
				} catch (RuntimeException e) {
					disableIncorrectedRule(blockedKeywordRegexp, Constants.TYPE_BLOCKED_KEYWORD_REGEXP, context);
					}
				}
		}
	}
	
	private void blockMessage(Context context){
		this.abortBroadcast();
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

	public void disableIncorrectedRule(String rule, int type, Context context){
		rDatabaseAdapter = new RulesDatabaseAdapter(context);
		rDatabaseAdapter.open();
		Cursor cursor = rDatabaseAdapter.getAllRules("rule='"+rule+"' and enabled='true'");
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			int ruleId = cursor.getInt(0);
			rDatabaseAdapter.updateRule(ruleId, rule, type, "error");
			cursor.moveToNext(); 
		  }
		  cursor.close();
		rDatabaseAdapter.close();
		Log.e(Constants.LOGTAG, "Rule '"+rule+"' have SYNTAX ERROR! It will be DISABLED until corrected !");
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
}