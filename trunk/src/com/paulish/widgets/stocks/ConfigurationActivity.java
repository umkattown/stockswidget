package com.paulish.widgets.stocks;

import java.util.List;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class ConfigurationActivity extends Activity implements OnClickListener, OnItemClickListener {
	
	private final static String TAG = "paulish.ConfigurationActivity";

	private List<String> tickers;
	private ArrayAdapter<String> adapter;
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int updateInterval;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setTitle(R.string.editPortfolio);
		setContentView(R.layout.stocks_widget_portfolio_edit);
		findViewById(R.id.save).setOnClickListener(this);
		findViewById(R.id.updateInterval).setOnClickListener(this);
		Button btn = (Button)findViewById(R.id.cancel);
		btn.setText(android.R.string.cancel);
		btn.setOnClickListener(this);			
		
		final ListView tickersList = (ListView)findViewById(R.id.tickersList);
		registerForContextMenu(tickersList);		
		tickersList.setOnItemClickListener(this);
		
		// prepare the listview
		final Bundle extras = getIntent().getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
			tickers = Preferences.getPortfolio(this, appWidgetId);
			adapter = new ArrayAdapter<String>(this, R.layout.stocks_widget_portfolio_edit_list_item, tickers);
			adapter.add(getString(R.string.addTickerSymbol));
			tickersList.setAdapter(adapter);
			updateInterval = Preferences.getUpdateInterval(this);
		} else
			finish();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel: 
			finish(); 
			break;
		case R.id.save:
			savePreferences();
			Intent resultValue = new Intent();                    
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
            StocksProvider.loadFromYahooInBackgroud(appWidgetId);
            UpdateService.registerService(this);
            finish();            
			break;
		case R.id.updateInterval:
			editUpdateInterval();
		}						
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.tickersList) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			
			if (info.position != tickers.size() - 1) {			
				menu.setHeaderTitle(tickers.get(info.position));
				menu.add(Menu.NONE, 0, 0, R.string.openTickerSymbol);
				menu.add(Menu.NONE, 1, 1, R.string.editTickerSymbol);
				menu.add(Menu.NONE, 2, 2, R.string.deleteTickerSymbol);
				if (info.position > 0)
					menu.add(Menu.NONE, 3, 3, R.string.moveUp);
				if (info.position < tickers.size() - 2)
					menu.add(Menu.NONE, 4, 4, R.string.moveDown);
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final int menuItemIndex = item.getItemId();
		final int position = info.position;
		switch (menuItemIndex) {
		case 0:
			QuoteViewActivity.openForSymbol(this, tickers.get(position));
			break;
		case 1:
			editSymbol(position);
			break;
		case 2:
			tickers.remove(position);
			adapter.notifyDataSetChanged();
			break;
		case 3:
			if (position > 0) {
				final String curValue = tickers.get(position);
				tickers.set(position, tickers.get(position - 1));
				tickers.set(position - 1, curValue);
				adapter.notifyDataSetChanged();
			}
			break;
		case 4:
			if (position < tickers.size() - 2) {
				final String curValue = tickers.get(position);
				tickers.set(position, tickers.get(position + 1));
				tickers.set(position + 1, curValue);
				adapter.notifyDataSetChanged();
			}
			break;
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (position == tickers.size() - 1)
			editSymbol(-1);
		else
			QuoteViewActivity.openForSymbol(this, tickers.get(position));
	}
	
	protected void onNewIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			int position = intent.getIntExtra("position", -1);
			if (position == -1)
				adapter.insert(query, tickers.size() - 1);
			else 
				tickers.set(position, query);
			adapter.notifyDataSetChanged();
		}
	}
	
	private void editSymbol(final int position) {
		Intent search = new Intent(this, SymbolSearchActivity.class);
		search.setAction(Intent.ACTION_VIEW);
		search.putExtra("position", position);
		if (position != -1)
			search.putExtra("ticker", tickers.get(position));
	}
	
	private void editUpdateInterval() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		
		// temporary. an ugly piece of code :) Just for testing.
		final Resources res = getResources();
		final String[] updateIntervalEntryValues = res.getStringArray(R.array.stocks_update_interval_entryValues);
		final String strInterval = Integer.toString(updateInterval);
		int item = -1;
		for (int i = 0; i < updateIntervalEntryValues.length; i++)
			if (updateIntervalEntryValues[i].equals(strInterval)) {
				item = i;
				break;
			};

		
		alert.setSingleChoiceItems(res.getStringArray(R.array.stocks_update_interval_entries), item,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						updateInterval = Integer.parseInt(updateIntervalEntryValues[which]);
						dialog.cancel();
					}
				});
				
		alert.show();
	}
	
	private void savePreferences() {
		StringBuffer result = new StringBuffer();
		final int count = tickers.size();
		if (count > 1) {
			result.append(tickers.get(0));
			for (int i = 1; i < count - 1; i++) {
				result.append(",");
				result.append(tickers.get(i));
			}
		}
		Preferences.setPortfolio(this, appWidgetId, result.toString());
		Preferences.setUpdateInterval(this, updateInterval);
	}
}
