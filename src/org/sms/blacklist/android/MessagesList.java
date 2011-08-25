package org.sms.blacklist.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MessagesList extends ListActivity {
	
	LayoutInflater mInflater;
	ListView mListView;
	Cursor mCursor;
	SQLiteDatabase mDatabase;
	MessagesAdapter mAdapter;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messages_list);
		
		mListView = (ListView) findViewById(android.R.id.list);
		mInflater = getLayoutInflater();
        
		MessagesDatabaseHelper mdh = new MessagesDatabaseHelper(this);
		mDatabase = mdh.getWritableDatabase();

		refreshCursor();
        
      //Listview clicked
		mListView.setOnItemClickListener(
			new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//Execute the selected script
					mCursor.moveToPosition(position);
					int MessageId = mCursor.getInt(0);
					Log.d("SMS Blacklist","Item "+String.valueOf(MessageId)+" Clicked");
				}
			}
		);
	}
	
	@Override
    public void onResume() {
	    refreshCursor();
	    super.onResume();
    }
	
	public void onStop() {
		mCursor.close();
		super.onStop();
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
	
	/** Cursor adapter */
	public class MessagesAdapter extends CursorAdapter {

			public MessagesAdapter(Context context, Cursor c) {
					super(context, c);
			}

			@Override
			public void bindView(View view, Context context, Cursor cursor) {

				TextView mNumber = (TextView) view.findViewById(R.id.message_number);
				TextView mBody = (TextView) view.findViewById(R.id.message_body);
				TextView mDate = (TextView) view.findViewById(R.id.message_date);

				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(cursor.getLong(1));
				DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm");
				String messageDate = formatter.format(calendar.getTime());
				
				String messageNumber = cursor.getString(2);
				String messageBody = cursor.getString(3);
				String messageUnread = cursor.getString(4);

				if(messageUnread.equals("true")) {
					mNumber.setTypeface(null, Typeface.BOLD);
				} else {
					mNumber.setTypeface(null, Typeface.NORMAL);
					mNumber.setTextColor(Color.GRAY);
					}
				
				mNumber.setText(messageNumber);
				mBody.setText(messageBody);
				mDate.setText(messageDate);
			}

	   
			public View newView(Context context, Cursor cursor, ViewGroup parent) {
					View view = mInflater.inflate(R.layout.messages_items, null);
					bindView(view, context, cursor);
					return view;
			}
	}
	
	private void refreshCursor() {
        mCursor = mDatabase.query(false, "messages", new String[] { "_id", "timestamp", "number", "body", "unread" }, null, null, null, null, null, null);
        mAdapter = new MessagesAdapter(this, mCursor);
        setListAdapter(mAdapter);
	}
}