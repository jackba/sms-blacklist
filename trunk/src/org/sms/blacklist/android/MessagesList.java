package org.sms.blacklist.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MessagesList extends ListActivity {
	
	private LayoutInflater mInflater;
	private ListView mListView;
	private Cursor mCursor;
	private MessagesAdapter mAdapter;
	private MessagesDatabaseAdapter mDatabaseAdapter;
	
	private static final int rulesList = Menu.FIRST + 1;
	private static final int markAllRead = Menu.FIRST + 2;
	private static final int clearMessages = Menu.FIRST + 3;
	private static final int contextOpen = 1;
	private static final int contextMark = 2;
	private static final int contextDelete = 3;
	private static final int contextCancel = 4;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messages_list);

		mListView = (ListView) findViewById(android.R.id.list);
		mInflater = getLayoutInflater();
		
		refreshCursor();
		
	  //Listview clicked
		mListView.setOnItemClickListener(
			new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//Execute the selected script
					mCursor.moveToPosition(position);
					int messageId = mCursor.getInt(0);
					openMessage(messageId);
				}
			}
		);
		
		mListView.setOnCreateContextMenuListener(
				new OnCreateContextMenuListener() {
					public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
						menu.add(0, contextOpen, 0, getString(R.string.open));
						menu.add(0, contextMark, 0, getString(R.string.mark_read));
						menu.add(0, contextDelete, 0, getString(R.string.delete));
						menu.add(0, contextCancel, 0, getString(R.string.cancel));
					}
				}
			);
	}
	
	@Override
	public void onResume() {
		refreshCursor();
		super.onResume();
	}
	
	@Override
	public void onStop() {
		mCursor.close();
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		mCursor.close();
		super.onDestroy();
	}
	
	@Override 
	public boolean onContextItemSelected(MenuItem item) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			int itemId = Integer.valueOf(String.valueOf(info.position));
			mCursor.moveToPosition(itemId);
			int messageId = mCursor.getInt(0);
			
			switch(item.getItemId()) {
				case contextOpen:
					openMessage(messageId);
					break;
				case contextMark:
					markRead(messageId);
					break;
				case contextDelete:
					deleteMessage(messageId);
					break;
				case contextCancel:
					break;
			}
		return super.onContextItemSelected(item);
	}
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {

			menu.setQwertyMode(true);
			MenuItem itemRule = menu.add(0, rulesList, 0, getString(R.string.rules_list));
			{
			  itemRule.setAlphabeticShortcut('r');
			  itemRule.setIcon(android.R.drawable.ic_menu_edit);
			}
			MenuItem itemMarkAll = menu.add(0, markAllRead, 0, getString(R.string.mark_all_read));
			{
				itemMarkAll.setAlphabeticShortcut('m');
				itemMarkAll.setIcon(R.drawable.ic_menu_enable_rules);
			}
			MenuItem itemClear = menu.add(0, clearMessages, 0, getString(R.string.clear_messages));
			{
				itemClear.setAlphabeticShortcut('c');
				itemClear.setIcon(android.R.drawable.ic_menu_delete);
			}
			return super.onCreateOptionsMenu(menu);
	}
	
	/** when menu button option selected */
	@Override 
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case rulesList:
				rulesList();
				break;
			case markAllRead:
				markAllRead();
				break;
			case clearMessages:
				clearMessages();
				break;
		}
			return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != RESULT_OK) {
			return;
		}
		if (requestCode == Constants.RESULT_OPEN) {
			if (resultCode == RESULT_OK) {
				refreshCursor();
			}
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
					View view = mInflater.inflate(R.layout.message_item_list, null);
					bindView(view, context, cursor);
					return view;
			}
	}
	
	private void refreshCursor() {
		mDatabaseAdapter = new MessagesDatabaseAdapter(this);
		mDatabaseAdapter.open();
		mCursor = mDatabaseAdapter.getAllMessages();
		mAdapter = new MessagesAdapter(this, mCursor);
		setListAdapter(mAdapter);
		mDatabaseAdapter.close();
	}
	
	private void rulesList() {
		Intent i = new Intent(this, RulesList.class);
		startActivity(i);
	}
	
	private void openMessage(int messageId) {
		Intent i = new Intent(MessagesList.this, OpenMessage.class);
		i.putExtra("messageId", messageId);
		startActivityForResult(i, Constants.RESULT_OPEN);
	}
	
	private void deleteMessage(final int messageId) {
		new AlertDialog.Builder(MessagesList.this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.delete)
		.setMessage(getString(R.string.delete_message_confirm))
		.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
				mDatabaseAdapter = new MessagesDatabaseAdapter(MessagesList.this);
				mDatabaseAdapter.open();
				mDatabaseAdapter.deleteMessage(messageId);
				mDatabaseAdapter.close();
				refreshCursor();
			}
		})
		.setNegativeButton(R.string.cancel , new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
			}
		})
		.show();
	}
	
	private void markRead(int messageId){
		mDatabaseAdapter = new MessagesDatabaseAdapter(this);
		mDatabaseAdapter.open();
		mDatabaseAdapter.markRead(messageId);
		mDatabaseAdapter.close();
		refreshCursor();
	}
	
	private void markAllRead() {
		mDatabaseAdapter = new MessagesDatabaseAdapter(this);
		mDatabaseAdapter.open();
		mDatabaseAdapter.markAllRead();
		mDatabaseAdapter.close();
		refreshCursor();
	}
	
	private void clearMessages() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle(R.string.clear_messages)
		.setMessage(getString(R.string.clear_message_confirm))
		.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
				mDatabaseAdapter = new MessagesDatabaseAdapter(MessagesList.this);
				mDatabaseAdapter.open();
				mDatabaseAdapter.deleteAllMessages();
				mDatabaseAdapter.close();
				refreshCursor();
			}
		})
		.setNegativeButton(R.string.cancel , new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface di, int i) {
			}
		})
		.show();
	}
}