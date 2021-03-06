package com.mihovilic.android.led.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Switch;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class LedSettingsActivity extends ListActivity implements OnItemClickListener, OnItemLongClickListener, OnCheckedChangeListener
{
	public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR = "notification_light_pulse_default_color";

	public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON = "notification_light_pulse_default_led_on";

	public static final String NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF = "notification_light_pulse_default_led_off";

	public static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE = "notification_light_pulse_custom_enable";

	public static final String NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES = "notification_light_pulse_custom_values";

	private static final int MENU_ADD = 0;

	private static final int DIALOG_APPS = 0;

	private static final int DIALOG_EDIT = 1;

	private List<PackageInfo> mInstalledPackages;

	private PackageManager mPackageManager;

	private LayoutInflater mLayoutInflater;

	private List<Application> mApplications;

	private ApplicationAdapter mListAdapter;

	private int mTimeOff;

	private int mTimeOn;

	private int mColor;

	private ScreenReceiver mReceiver = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// Set settings separation headers
		((TextView) findViewById(R.id.default_category)).setText(R.string.category_default);
		((TextView) findViewById(R.id.applications_category)).setText(R.string.category_applications);

		((Switch) findViewById(R.id.checkedViewCustom)).setOnCheckedChangeListener(this);

		mLayoutInflater = getLayoutInflater();
		mPackageManager = getPackageManager();

		mInstalledPackages = mPackageManager.getInstalledPackages(0);

		mApplications = new ArrayList<LedSettingsActivity.Application>();
		mListAdapter = new ApplicationAdapter(this, R.layout.application_item, mApplications);

		setListAdapter(mListAdapter);

		getListView().setOnItemClickListener(this);
		getListView().setOnItemLongClickListener(this);

		refreshDefault();
		refreshCustomEnabled();
		refreshCustomApplications();
	}

	private void refreshCustomEnabled()
	{
		boolean customEnabled = Settings.System.getInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE, 0) == 1;

		((Switch) findViewById(R.id.checkedViewCustom)).setChecked(customEnabled);

		findViewById(android.R.id.list).setEnabled(customEnabled);
	}

	private void refreshDefault()
	{
		mColor = Settings.System.getInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, 0xFFFFFF);
		mTimeOn = Settings.System.getInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, 1000);
		mTimeOff = Settings.System.getInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, 1000);

		View def = findViewById(R.id.default_color);

		((TextView) def.findViewById(R.id.textViewName)).setText(R.string.default_value);
		((TextView) def.findViewById(R.id.textViewPackage)).setText(getResources().getString(R.string.default_package, mapTimeValue(mTimeOn),
				mapTimeValue(mTimeOff).toLowerCase()));
		((View) def.findViewById(R.id.textViewColorValue)).setBackgroundColor(0xFF000000 + mColor);
		// ((TextView)
		// def.findViewById(R.id.textViewTimeOnValue)).setText(mapTimeValue(mTimeOn));
		// ((TextView)
		// def.findViewById(R.id.textViewTimeOffValue)).setText(mapTimeValue(mTimeOff));

		def.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				// Open an edit dialog, not setting the "application" property
				// tells it that we are
				// changing the default notification settings

				Bundle bundle = new Bundle();
				bundle.putInt("color", mColor);
				bundle.putInt("timeon", mTimeOn);
				bundle.putInt("timeoff", mTimeOff);

				showDialog(DIALOG_EDIT, bundle);
			}
		});
	}

	private void refreshCustomApplications()
	{
		mApplications.clear();

		String customValues = Settings.System.getString(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

		if(customValues == null) customValues = "";

		String[] customs = customValues.split("\\|", -1);

		for(String custom : customs)
		{
			String[] app = custom.split("=", -1);
			if(app.length != 2) continue;

			String[] values = app[1].split(";", -1);
			if(values.length != 3) continue;

			try
			{
				mApplications.add(new Application(app[0], Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2])));
			}
			catch(NumberFormatException e)
			{
			}
		}

		mListAdapter.notifyDataSetChanged();
	}

	public void addCustomApplication(String packageName)
	{
		String customValues = Settings.System.getString(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

		if(customValues == null) customValues = "";

		customValues = customValues.trim();

		if(customValues.length() != 0 && !customValues.endsWith("|")) customValues += "|";

		customValues += packageName + "=0;-1;-1";

		Settings.System.putString(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, customValues);

		refreshCustomApplications();
	}

	public void removeCustomApplication(String packageName)
	{
		String customValues = Settings.System.getString(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES);

		if(customValues == null) customValues = "";

		StringBuilder newValues = new StringBuilder();

		for(String custom : customValues.split("\\|", -1))
		{
			String[] app = custom.split("=", -1);
			if(app.length != 2) continue;

			if(app[0].equals(packageName)) continue;

			if(newValues.length() != 0) newValues.append("|");

			newValues.append(custom);
		}

		Settings.System.putString(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, newValues.toString());

		refreshCustomApplications();
	}

	private String mapTimeValue(Integer time)
	{
		if(time == Application.DEFAULT_TIME) return getString(R.string.default_time);

		String[] time_names = getResources().getStringArray(R.array.time_names);
		String[] time_values = getResources().getStringArray(R.array.time_values);

		for(int i = 0; i < time_values.length; i++)
		{
			if(Integer.decode(time_values[i]).equals(time)) return time_names[i];
		}

		return getString(R.string.custom_time);
	}

	private String mapColorValue(Integer color)
	{
		if(color == Application.DEFAULT_COLOR) return getString(R.string.default_color);

		String[] color_names = getResources().getStringArray(R.array.color_names);
		String[] color_values = getResources().getStringArray(R.array.color_values);

		for(int i = 0; i < color_values.length; i++)
		{
			if(Integer.decode(color_values[i]).equals(color)) return color_names[i];
		}

		return getString(R.string.custom_color);
	}

	class Application
	{
		public static final int DEFAULT_COLOR = 0; // Zero (black) is a
													// nonsensical light color

		public static final int DEFAULT_TIME = -1; // Minus one is a nonsensical
													// time value

		public String name;

		public Integer color;

		public Integer timeon;

		public Integer timeoff;

		public Application(String name, Integer color, Integer timeon, Integer timeoff)
		{
			this.name = name;
			this.color = color;
			this.timeon = timeon;
			this.timeoff = timeoff;
		}
	}

	class ApplicationAdapter extends ArrayAdapter<Application>
	{
		public ApplicationAdapter(Context context, int resource, List<Application> applications)
		{
			super(context, resource, applications);
		}

		@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			ApplicationViewHolder holder;

			if(view == null)
			{
				view = mLayoutInflater.inflate(R.layout.application_item, null);

				holder = new ApplicationViewHolder();
				holder.name = (TextView) view.findViewById(R.id.textViewName);
				holder.pkg = (TextView) view.findViewById(R.id.textViewPackage);
				holder.color = view.findViewById(R.id.textViewColorValue);
				// holder.timeon = (TextView)
				// view.findViewById(R.id.textViewTimeOnValue);
				// holder.timeoff = (TextView)
				// view.findViewById(R.id.textViewTimeOffValue);

				view.setTag(holder);
			}
			else
				holder = (ApplicationViewHolder) view.getTag();

			String name = getItem(position).name;

			try
			{
				PackageInfo info = getPackageManager().getPackageInfo(name, PackageManager.GET_META_DATA);
				name = (String) info.applicationInfo.loadLabel(getPackageManager());
			}
			catch(NameNotFoundException e)
			{
			}

			Application item = getItem(position);

			holder.name.setText(name);
			// holder.pkg.setText(item.name);
			holder.pkg.setText(getResources().getString(R.string.default_package, mapTimeValue(item.timeon), mapTimeValue(item.timeoff).toLowerCase()));

			if(item.color == Application.DEFAULT_COLOR)
				holder.color.setVisibility(View.GONE);
			else
			{
				holder.color.setBackgroundColor(0xFF000000 + item.color);
				holder.color.setVisibility(View.VISIBLE);
			}

			/*
			 * if (item.timeon == Application.DEFAULT_TIME)
			 * holder.timeon.setVisibility(View.INVISIBLE); else {
			 * holder.timeon.setText(mapTimeValue(item.timeon));
			 * holder.timeon.setVisibility(View.VISIBLE); }
			 * 
			 * if (item.timeoff == Application.DEFAULT_TIME)
			 * holder.timeoff.setVisibility(View.INVISIBLE); else {
			 * holder.timeoff.setText(mapTimeValue(item.timeoff));
			 * holder.timeoff.setVisibility(View.VISIBLE); }
			 */

			return view;
		}
	}

	static class ApplicationViewHolder
	{
		TextView name;

		TextView pkg;

		View color;

		// TextView timeon;

		// TextView timeoff;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3)
	{
		final Application item = (Application) getListView().getItemAtPosition(position);

		final Bundle bundle = new Bundle();
		bundle.putString("application", item.name);
		bundle.putInt("color", item.color);
		bundle.putInt("timeon", item.timeon);
		bundle.putInt("timeoff", item.timeoff);

		showDialog(DIALOG_EDIT, bundle);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long arg3)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Action");
		builder.setMessage("Do you want to test or delete the specified application settings?");
		builder.setNegativeButton("Test", new AlertDialog.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1)
			{
				final Application item = (Application) getListView().getItemAtPosition(position);

				if(mReceiver != null) LedSettingsActivity.this.unregisterReceiver(mReceiver);

				mReceiver = new ScreenReceiver(item.timeon, item.timeoff, item.color);
				IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
				filter.addAction(Intent.ACTION_SCREEN_ON);
				LedSettingsActivity.this.registerReceiver(mReceiver, filter);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(LedSettingsActivity.this);

				builder.setTitle("Test");
				builder.setMessage("Turn the phone screen off to see the selected notification in action.");
				builder.setPositiveButton(android.R.string.ok, null);
				builder.show();
			}
		});
		builder.setPositiveButton("Delete", new AlertDialog.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1)
			{
				showDeleteDialog((Application) getListView().getItemAtPosition(position));
			}
		});

		builder.show();
		return true;
	}

	public void showDeleteDialog(final Application item)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Delete");
		builder.setMessage("Are you sure you want to delete this application settings?");
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1)
			{
				removeCustomApplication(item.name);
			}
		});

		builder.show();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE, ((Switch) findViewById(R.id.checkedViewCustom)).isChecked() ? 1
				: 0);
		refreshCustomEnabled();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, MENU_ADD, 0, R.string.menu_add).setIcon(android.R.drawable.ic_menu_add)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case MENU_ADD:
				showDialog(DIALOG_APPS, null);
				return true;
		}
		return false;
	}

	@Override
	public Dialog onCreateDialog(int id, final Bundle bundle)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final Dialog dialog;
		switch(id)
		{
			case DIALOG_APPS:
				final ListView list = new ListView(this);
				PackageAdapter adapter = new PackageAdapter(mInstalledPackages);
				list.setAdapter(adapter);
				adapter.update();

				builder.setTitle(R.string.choose_app);
				builder.setView(list);

				dialog = builder.create();

				list.setOnItemClickListener(new OnItemClickListener()
				{
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id)
					{
						// Add empty application definition, the user will be
						// able to edit it later
						PackageItem info = (PackageItem) parent.getItemAtPosition(position);
						addCustomApplication(info.packageName);
						dialog.cancel();
					}
				});
				break;

			case DIALOG_EDIT:
				final boolean defaul = bundle.getString("application") == null;

				final View layout = mLayoutInflater.inflate(R.layout.edit_dialog, null);

				Spinner spinner = (Spinner) layout.findViewById(R.id.spinnerColors);

				ColorSpinnerAdapter colorAdapter = new ColorSpinnerAdapter(R.array.color_names, R.array.color_values, !defaul, bundle.getInt("color"));

				spinner.setAdapter(colorAdapter);

				spinner.setSelection(colorAdapter.getColorPosition(bundle.getInt("color")));

				spinner = (Spinner) layout.findViewById(R.id.spinnerTimeOn);

				TimeSpinnerAdapter timeAdapter = new TimeSpinnerAdapter(R.array.time_names, R.array.time_values, !defaul, bundle.getInt("timeon"));

				spinner.setAdapter(timeAdapter);

				spinner.setSelection(timeAdapter.getTimePosition(bundle.getInt("timeon")));

				spinner = (Spinner) layout.findViewById(R.id.spinnerTimeOff);

				timeAdapter = new TimeSpinnerAdapter(R.array.time_names, R.array.time_values, !defaul, bundle.getInt("timeoff"));

				spinner.setAdapter(timeAdapter);

				spinner.setSelection(timeAdapter.getTimePosition(bundle.getInt("timeoff")));

				if(defaul)
					builder.setTitle(R.string.edit_default);
				else
					builder.setTitle(R.string.edit_app);

				builder.setView(layout);

				builder.setNegativeButton(android.R.string.cancel, null);
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						updateValues(bundle.getString("application"),
								((Pair<String, Integer>) ((Spinner) layout.findViewById(R.id.spinnerColors)).getSelectedItem()).second,
								((Pair<String, Integer>) ((Spinner) layout.findViewById(R.id.spinnerTimeOn)).getSelectedItem()).second,
								((Pair<String, Integer>) ((Spinner) layout.findViewById(R.id.spinnerTimeOff)).getSelectedItem()).second);
					}
				});

				dialog = builder.create();

				// TODO Hack to make the dialog properly refresh it's data upon
				// each invocation.
				// This should not be needed if we use DialogFragment
				dialog.setOnDismissListener(new OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						removeDialog(DIALOG_EDIT);
					}
				});
				break;

			default:
				dialog = null;
				break;
		}

		return dialog;
	}

	/**
	 * Updates the default or application specific notification settings.
	 * 
	 * @param application
	 *            Package name of aplication specific settings to update, if
	 *            "null" update the default settings.
	 * @param color
	 * @param timeon
	 * @param timeoff
	 */
	protected void updateValues(String application, Integer color, Integer timeon, Integer timeoff)
	{
		if(application == null)
		{
			Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_DEFAULT_COLOR, color);
			Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_ON, timeon);
			Settings.System.putInt(getContentResolver(), NOTIFICATION_LIGHT_PULSE_DEFAULT_LED_OFF, timeoff);
			refreshDefault();
			return;
		}

		for(Application app : mApplications)
		{
			if(!app.name.equals(application)) continue;

			app.color = color;
			app.timeon = timeon;
			app.timeoff = timeoff;
		}

		StringBuilder builder = new StringBuilder();

		for(Application a : mApplications)
		{
			if(builder.length() != 0) builder.append("|");

			builder.append(a.name).append("=").append(a.color).append(";").append(a.timeon).append(";").append(a.timeoff);
		}

		Settings.System.putString(getContentResolver(), NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES, builder.toString());
		refreshCustomApplications();
	}

	class TimeSpinnerAdapter extends BaseAdapter implements SpinnerAdapter
	{
		private ArrayList<Pair<String, Integer>> times;

		public TimeSpinnerAdapter(int timeNamesResource, int timeValuesResource, boolean defaul)
		{
			times = new ArrayList<Pair<String, Integer>>();

			if(defaul)
			{
				// Add the Default entry first
				times.add(new Pair<String, Integer>(getResources().getString(R.string.default_time), Application.DEFAULT_TIME));
			}

			String[] time_names = getResources().getStringArray(R.array.time_names);
			String[] time_values = getResources().getStringArray(R.array.time_values);

			for(int i = 0; i < time_values.length; ++i)
				times.add(new Pair<String, Integer>(time_names[i], Integer.decode(time_values[i])));
		}

		/**
		 * This constructor apart from taking a usual time entry array takes the
		 * currently configured time value which might cause the addition of a
		 * "Custom" time entry in the spinner in case this time value does not
		 * match any of the predefined ones in the array.
		 * 
		 * @param timesArrayResource
		 *            The time entry array
		 * @param customTime
		 *            Current time value that might be one of the predefined
		 *            values or a totaly custom value
		 */
		public TimeSpinnerAdapter(int timeNamesResource, int timeValuesResource, boolean defaul, Integer customTime)
		{
			this(timeNamesResource, timeValuesResource, defaul);

			// Check if we also need to add the custom value entry
			if(getTimePosition(customTime) == -1) times.add(new Pair<String, Integer>(getResources().getString(R.string.custom_time), customTime));
		}

		/**
		 * Will return the position of the spinner entry with the specified
		 * time. Returns -1 if there is no such entry.
		 * 
		 * @param time
		 *            Time in ms
		 * @return Position of entry with given time or -1 if not found.
		 */
		public int getTimePosition(Integer time)
		{
			for(int position = 0; position < getCount(); ++position)
				if(getItem(position).second.equals(time)) return position;

			return -1;
		}

		@Override
		public int getCount()
		{
			return times.size();
		}

		@Override
		public Pair<String, Integer> getItem(int position)
		{
			return times.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			if(view == null) view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.time_item, null);

			Pair<String, Integer> entry = getItem(position);

			((TextView) view.findViewById(R.id.textViewName)).setText(entry.first);

			return view;
		}
	}

	class ColorSpinnerAdapter extends BaseAdapter implements SpinnerAdapter
	{
		private ArrayList<Pair<String, Integer>> colors;

		public ColorSpinnerAdapter(int colorNamesResource, int colorValuesResource, boolean defaul)
		{
			colors = new ArrayList<Pair<String, Integer>>();

			if(defaul)
			{
				// Add the Default entry first
				colors.add(new Pair<String, Integer>(getResources().getString(R.string.default_color), Application.DEFAULT_COLOR));
			}

			String[] color_names = getResources().getStringArray(R.array.color_names);
			String[] color_values = getResources().getStringArray(R.array.color_values);

			for(int i = 0; i < color_values.length; ++i)
				colors.add(new Pair<String, Integer>(color_names[i], Integer.decode(color_values[i])));
		}

		/**
		 * This constructor apart from taking a usual color entry array takes
		 * the currently configured color value which might cause the addition
		 * of a "Custom" color entry in the spinner in case this color value
		 * does not match any of the predefined ones in the array.
		 * 
		 * @param colorsArrayResource
		 *            The time entry array
		 * @param customColor
		 *            Current color value that might be one of the predefined
		 *            values or a totaly custom value
		 */
		public ColorSpinnerAdapter(int colorNamesResource, int colorValuesResource, boolean defaul, Integer customColor)
		{
			this(colorNamesResource, colorValuesResource, defaul);

			if(getColorPosition(customColor) == -1) colors.add(new Pair<String, Integer>(getResources().getString(R.string.custom_color), customColor));
		}

		/**
		 * Will return the position of the spinner entry with the specified
		 * color value. Returns -1 if there is no such entry.
		 * 
		 * @param color
		 *            Color value
		 * @return Position of entry with given color or -1 if not found.
		 */
		public int getColorPosition(Integer color)
		{
			for(int position = 0; position < getCount(); ++position)
				if(getItem(position).second.equals(color)) return position;

			return -1;
		}

		@Override
		public int getCount()
		{
			return colors.size();
		}

		@Override
		public Pair<String, Integer> getItem(int position)
		{
			return colors.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			ColorViewHolder holder;

			if(view == null)
			{
				view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.color_item, null);

				holder = new ColorViewHolder();
				holder.color = (TextView) view.findViewById(R.id.textViewColor);
				holder.name = (TextView) view.findViewById(R.id.textViewName);

				view.setTag(holder);
			}
			else
				holder = (ColorViewHolder) view.getTag();

			Pair<String, Integer> entry = getItem(position);

			if(entry.second == Application.DEFAULT_COLOR)
			{
				holder.color.setVisibility(View.GONE);
			}
			else
			{
				holder.color.setTextColor(0xFF000000 + entry.second);
				holder.color.setVisibility(View.VISIBLE);
			}

			holder.name.setText(entry.first);

			return view;
		}
	}

	static class ColorViewHolder
	{
		TextView color;

		TextView name;
	}

	class PackageItem implements Comparable<PackageItem>
	{
		CharSequence title;

		String packageName;

		Drawable icon;

		@Override
		public int compareTo(PackageItem another)
		{
			return this.title.toString().compareTo(another.title.toString());
		}
	}

	class PackageAdapter extends BaseAdapter
	{

		protected List<PackageInfo> mInstalledPackageInfo;

		protected List<PackageItem> mInstalledPackages = new LinkedList<PackageItem>();

		private void reloadList()
		{
			final Handler handler = new Handler();
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized(mInstalledPackages)
					{
						mInstalledPackages.clear();
						for(PackageInfo info : mInstalledPackageInfo)
						{
							final PackageItem item = new PackageItem();
							ApplicationInfo applicationInfo = info.applicationInfo;
							item.title = applicationInfo.loadLabel(mPackageManager);
							item.icon = applicationInfo.loadIcon(mPackageManager);
							item.packageName = applicationInfo.packageName;
							handler.post(new Runnable()
							{

								@Override
								public void run()
								{
									int index = Collections.binarySearch(mInstalledPackages, item);
									if(index < 0)
									{
										index = -index - 1;
										mInstalledPackages.add(index, item);
									}
									notifyDataSetChanged();
								}
							});
						}
					}
				}
			}).start();
		}

		public PackageAdapter(List<PackageInfo> installedPackagesInfo)
		{
			mInstalledPackageInfo = installedPackagesInfo;
		}

		public void update()
		{
			reloadList();
		}

		@Override
		public int getCount()
		{
			return mInstalledPackages.size();
		}

		@Override
		public PackageItem getItem(int position)
		{
			return mInstalledPackages.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return mInstalledPackages.get(position).packageName.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			PackageViewHolder holder;
			if(convertView != null)
			{
				holder = (PackageViewHolder) convertView.getTag();
			}
			else
			{
				convertView = mLayoutInflater.inflate(R.layout.package_item, null, false);

				holder = new PackageViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.textViewTitle);
				holder.summary = (TextView) convertView.findViewById(R.id.textViewSummary);
				holder.icon = (ImageView) convertView.findViewById(R.id.imageViewIcon);

				convertView.setTag(holder);
			}

			PackageItem applicationInfo = getItem(position);

			if(holder.title != null)
			{
				holder.title.setText(applicationInfo.title);
			}
			if(holder.summary != null)
			{
				holder.summary.setText(applicationInfo.packageName);
			}
			if(holder.icon != null)
			{
				Drawable loadIcon = applicationInfo.icon;
				holder.icon.setImageDrawable(loadIcon);
				holder.icon.setAdjustViewBounds(true);
				holder.icon.setMaxHeight(72);
				holder.icon.setMaxWidth(72);
			}
			return convertView;
		}
	}

	static class PackageViewHolder
	{
		TextView title;

		TextView summary;

		ImageView icon;
	}

	public class ScreenReceiver extends BroadcastReceiver
	{

		protected int timeon;
		protected int timeoff;
		protected int color;

		public ScreenReceiver(int timeon, int timeoff, int color)
		{
			this.timeon = timeon;
			this.timeoff = timeoff;
			this.color = color;
		}

		@Override
		public void onReceive(Context context, Intent intent)
		{
			if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
			{
				Notification.Builder builder = new Notification.Builder(LedSettingsActivity.this);

				builder.setAutoCancel(true);
				builder.setLights(color, timeon, timeoff);

				((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, builder.getNotification());
				
			}
			else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			{
				((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
				
				LedSettingsActivity.this.runOnUiThread(new Runnable() {
				    public void run() {
				        unregisterReceiver(mReceiver);
				        mReceiver = null;
				    }
				});
			}

		}

	}

}
