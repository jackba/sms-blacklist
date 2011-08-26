package org.sms.blacklist.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OpenMessage extends Activity {
	
	private Button mDelete;
	
	private int messageId;
	private TextView mNumber;
	private TextView mBody;
	private TextView mDate;
	
	private SQLiteDatabase mDatabase;
	private Cursor mCursor;
	
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		final Intent intent = getIntent();
		messageId = intent.getIntExtra("messageId", 0);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
		setContentView(R.layout.open_message);

		MessagesDatabaseHelper mhelper = new MessagesDatabaseHelper(this);
		mDatabase = mhelper.getWritableDatabase();
		mCursor = mDatabase.query("messages", new String[] {"timestamp", "number", "body"}, "_id="+String.valueOf(messageId), null, null, null, null);
		mCursor.moveToFirst();

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(mCursor.getLong(0));
		DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm");
		String messageDate = formatter.format(calendar.getTime());
		String messageNumber = mCursor.getString(1);
		String messageBody = mCursor.getString(2);

		mNumber = (TextView) findViewById(R.id.number);
		mBody = (TextView) findViewById(R.id.message);
		mDate = (TextView) findViewById(R.id.date);
		
		mNumber.setText(messageNumber);
		mDate.setText(messageDate);
		mBody.setText(messageBody);
		
		mDelete = (Button) findViewById(R.id.delete);
		mDelete.setOnClickListener(mOnDeleteClickListener);

		((Button)findViewById(R.id.ok)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ContentValues cv = new ContentValues();
				cv.put("unread", "false");
				mDatabase.update("messages", cv, "_id="+String.valueOf(messageId), null);
				mDatabase.close();
				setResult(RESULT_OK);
				finish();
			}
		});
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
		
	private View.OnClickListener mOnDeleteClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			new AlertDialog.Builder(OpenMessage.this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.delete)
			.setMessage(getString(R.string.delete_message_confirm))
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int i) {
					mDatabase.delete("messages", "_id="+String.valueOf(messageId), null);
					mDatabase.close();
					setResult(RESULT_OK);
					finish();
				}
			})
			.setNegativeButton(R.string.cancel , new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int i) {
				}
			})
			.show();
		}
	};
}