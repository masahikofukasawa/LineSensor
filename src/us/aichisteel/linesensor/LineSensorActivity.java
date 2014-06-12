/*
 * Copyright (C) 2014 Aichi Micro Intelligent Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * Distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.aichisteel.linesensor;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.hardware.usb.UsbManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import us.aichisteel.linesensor.R;

public class LineSensorActivity extends Activity {

	private LineSensorData sensorData = new LineSensorData();
	private static final String ACTION_USB_PERMISSION = "us.aichisteel.linesensor.USB_PERMISSION";

	Handler mHandler = new Handler();

	private static final String CR = "\r";
	private String stTransmit = CR;
	private String stStartCommand = "mes 0 200";
	private String stStopCommand = "q";

	private Physicaloid mSerial;

	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private Button btClearOffset;
	private StringBuilder mText = new StringBuilder();
	private int iBaudRate = 1250000;
	private RadioGroup rgSensorSelection;
	private RadioButton rbtPow;
	private RadioButton rbtX;
	private RadioButton rbtY;
	private RadioButton rbtZ;

	private boolean mRunningMainLoop;

	private XYMultipleSeriesRenderer mRenderer;
	private XYMultipleSeriesDataset dataset;
	private GraphicalView graphicalView;
	public final static int AXIS_ID_POWER = 0;
	public final static int AXIS_ID_X = 1;
	public final static int AXIS_ID_Y = 2;
	public final static int AXIS_ID_Z = 3;
	private int mAxisId = AXIS_ID_POWER;

	// Linefeed
	private final static String BR = System.getProperty("line.separator");

	private XYMultipleSeriesDataset buildDataset() {
		XYMultipleSeriesDataset sd = new XYMultipleSeriesDataset();
		XYSeries level = new XYSeries("Level");
		for (int i = 1; i <= 16; i++) {
			level.add(i, 0);
		}
		sd.addSeries(level);
		return sd;
	}

	private void setRenderer(XYMultipleSeriesRenderer renderer, int col) {
		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(col);
		r.setFillPoints(true);
		r.setChartValuesTextAlign(Align.RIGHT);
		r.setChartValuesTextSize(18);
		r.setDisplayChartValues(true);
		renderer.addSeriesRenderer(r);
	}

	private XYMultipleSeriesRenderer buildRenderer() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		renderer.setXAxisMin(0.5);
		renderer.setXAxisMax(16.5);
		renderer.setYAxisMin(-1000);
		renderer.setYAxisMax(1000);
		renderer.setXTitle("Channel");
		renderer.setYTitle("Level[mGauss]");
		renderer.setAxisTitleTextSize(20);
		renderer.setShowGridX(true);
		renderer.setShowLegend(false);
		renderer.setBarSpacing(0.1);
		renderer.setApplyBackgroundColor(true);

		renderer.setAxesColor(getResources().getColor(R.color.dark_blue));
		renderer.setLabelsColor(getResources().getColor(R.color.dark_blue));
		renderer.setXLabelsColor(getResources().getColor(R.color.dark_blue));
		renderer.setYLabelsColor(0, getResources().getColor(R.color.dark_blue));
		renderer.setBackgroundColor(getResources()
				.getColor(R.color.back_ground));
		renderer.setMarginsColor(getResources().getColor(R.color.margin));
		renderer.setGridColor(getResources().getColor(R.color.grid));

		renderer.setXLabels(0);
		renderer.setYLabels(12);
		renderer.setLabelsTextSize(18);
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		for (int ch = 1; ch <= 16; ch++) {
			renderer.addXTextLabel(ch, "" + String.valueOf(ch));
		}

		renderer.setMargins(new int[] { 40, 60, 10, 20 });

		// Zoom設定
		renderer.setZoomButtonsVisible(false);
		renderer.setZoomEnabled(false, true);
		renderer.setPanEnabled(false, true);
		renderer.setPanLimits(new double[] { -1, 17, -15000, 15000 });
		renderer.setZoomLimits(new double[] { -1, 17, -15000, 15000 });

		setRenderer(renderer, getResources().getColor(R.color.dark_blue));
		return renderer;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_linesensor);

		dataset = buildDataset();
		Log.d("LINESENSOR", "buildDataset");

		mRenderer = buildRenderer();
		Log.d("LINESENSOR", "buildRenderer");

		LinearLayout layout = (LinearLayout) findViewById(R.id.plot_area);
		graphicalView = ChartFactory.getBarChartView(getApplicationContext(),
				dataset, mRenderer, Type.DEFAULT);
		layout.addView(graphicalView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		Log.d("LINESENSOR", "addView");

		mRunningMainLoop = false;

		btStart = (Button) findViewById(R.id.btStart);
		btStop = (Button) findViewById(R.id.btStop);
		btOffset = (Button) findViewById(R.id.btOffset);
		btClearOffset = (Button) findViewById(R.id.btClearOffset);
		rgSensorSelection = (RadioGroup) findViewById(R.id.radioGroup1);
		rgSensorSelection.check(R.id.rd_pow);
		rbtPow = (RadioButton) findViewById(R.id.rd_pow);
		rbtX = (RadioButton) findViewById(R.id.rd_x);
		rbtY = (RadioButton) findViewById(R.id.rd_y);
		rbtZ = (RadioButton) findViewById(R.id.rd_z);
		btOffset.setVisibility(View.GONE);
		btClearOffset.setVisibility(View.GONE);
		rgSensorSelection.setVisibility(View.GONE);

		// get service
		mSerial = new Physicaloid(this);

		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		openUsbSerial();

		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startSensor();
			}
		});

		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopSensor();
			}
		});

		btOffset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setOffset();
			}
		});

		btClearOffset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clearOffset();
			}
		});

		rgSensorSelection
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						int id = AXIS_ID_POWER;
						switch (checkedId) {
						case R.id.rd_pow:
							id = AXIS_ID_POWER;
							break;
						case R.id.rd_x:
							id = AXIS_ID_X;
							break;
						case R.id.rd_y:
							id = AXIS_ID_Y;
							break;
						case R.id.rd_z:
							id = AXIS_ID_Z;
							break;
						}
						setSensor(id);
					}
				});
	}

	@Override
	public void onDestroy() {
		mSerial.close();
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void enableButtons(boolean enable) {
		btStart.setEnabled(enable);
		btStop.setEnabled(enable);
		btOffset.setEnabled(enable);
		btClearOffset.setEnabled(enable);
		rbtPow.setEnabled(enable);
		rbtX.setEnabled(enable);
		rbtY.setEnabled(enable);
		rbtZ.setEnabled(enable);

		int color;
		if (enable)
			color = getResources().getColor(R.color.dark_blue);
		else
			color = Color.LTGRAY;

		btStart.setTextColor(color);
		btStop.setTextColor(color);
		btOffset.setTextColor(color);
		btClearOffset.setTextColor(color);
		rbtPow.setTextColor(color);
		rbtX.setTextColor(color);
		rbtY.setTextColor(color);
		rbtZ.setTextColor(color);
	}

	private void openUsbSerial() {
		if (mSerial == null) {
			enableButtons(false);
			return;
		}

		if (!mSerial.isOpened()) {
			if (!mSerial.open()) {
				enableButtons(false);
				return;
			} else {
				mSerial.setConfig(new UartConfig(iBaudRate, 8, 1, 0, false,
						false));
				enableButtons(true);
			}
		}

		if (!mRunningMainLoop) {
			mainloop();
		}
	}

	private void mainloop() {
		mRunningMainLoop = true;
		new Thread(mLoop).start();
	}

	private Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			int len;
			byte[] rbuf = new byte[4096];

			for (;;) {
				len = mSerial.read(rbuf);

				if (len > 0) {
					sensorData.addData(rbuf, len);
					setSerialDataToTextView(rbuf, len);

					mHandler.post(new Runnable() {
						@Override
						public void run() {
							try {
								dataset.getSeriesAt(0).clear();
								for (int ch = 1; ch <= 16; ch++) {
									if (mAxisId == AXIS_ID_POWER) {
										dataset.getSeriesAt(0).add(ch,
												sensorData.getPower(ch));
									} else if (mAxisId == AXIS_ID_X) {
										dataset.getSeriesAt(0).add(ch,
												sensorData.getData(ch, 0));
									} else if (mAxisId == AXIS_ID_Y) {
										dataset.getSeriesAt(0).add(ch,
												sensorData.getData(ch, 1));
									} else if (mAxisId == AXIS_ID_Z) {
										dataset.getSeriesAt(0).add(ch,
												sensorData.getData(ch, 2));
									}
								}
								graphicalView.repaint();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (!mRunningMainLoop) {
					return;
				}
			}
		}
	};

	boolean lastDataIs0x0D = false;

	void setSerialDataToTextView(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			// "\r":CR(0x0D) "\n":LF(0x0A)
			if (rbuf[i] == 0x0D) {
				mText.append(BR);
			} else if (rbuf[i] == 0x0A) {
				mText.append(BR);
			} else if ((rbuf[i] == 0x0D) && (rbuf[i + 1] == 0x0A)) {
				mText.append(BR);
				i++;
			} else if (rbuf[i] == 0x0D) {
				// case of rbuf[last] == 0x0D and rbuf[0] == 0x0A
				lastDataIs0x0D = true;
			} else if (lastDataIs0x0D && (rbuf[0] == 0x0A)) {
				mText.append(BR);
				lastDataIs0x0D = false;
			} else if (lastDataIs0x0D && (i != 0)) {
				// only disable flag
				lastDataIs0x0D = false;
				i--;
			} else {
				mText.append((char) rbuf[i]);
			}
		}
	}

	private void closeUsbSerial() {
		mSerial.close();
	}

	protected void onNewIntent(Intent intent) {
		openUsbSerial();
	};

	private void detachedUi() {
		enableButtons(false);
	}

	private String changeEscapeSequence(String in) {
		String out = new String();
		try {
			out = unescapeJava(in);
		} catch (IOException e) {
			return "";
		}

		out = out + stTransmit;
		return out;
	}

	private void startSensor() {
		openUsbSerial();
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStartCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			sensorData.initData();
		} else {
		}
	}

	private void stopSensor() {
		if (mSerial.isOpened()) {
			String strWrite = changeEscapeSequence(stStopCommand);
			mSerial.write(strWrite.getBytes(), strWrite.length());
			sensorData.initData();
		} else {
		}
		closeUsbSerial();
	}

	private void setSensor(int id) {
		mAxisId = id;
	}

	private void setOffset() {
		sensorData.setOffset();
	}

	private void clearOffset() {
		sensorData.clearOffset();
	}

	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				if (!mSerial.isOpened()) {
					openUsbSerial();
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				detachedUi();
				mSerial.close();
				finish();
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					if (!mSerial.isOpened()) {
						openUsbSerial();
					}
				}
				if (!mRunningMainLoop) {
					mainloop();
				}
			}
		}
	};

	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu planeMenu = menu.addSubMenu(Menu.NONE, 0, Menu.NONE,
				"Data Type");
		planeMenu.add(Menu.NONE, 1, Menu.NONE, "Power");
		planeMenu.add(Menu.NONE, 2, Menu.NONE, "X-Axis");
		planeMenu.add(Menu.NONE, 3, Menu.NONE, "Y-Axis");
		planeMenu.add(Menu.NONE, 4, Menu.NONE, "Z-Axis");
		planeMenu.setGroupCheckable(Menu.NONE, true, true);
		planeMenu.findItem(1).setChecked(true);

		menu.add(Menu.NONE, 20, Menu.NONE, "Set Offset");
		menu.add(Menu.NONE, 21, Menu.NONE, "Clear Offset");
		menu.add(Menu.NONE, 30, Menu.NONE, "About");

		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == 1) {
			setSensor(AXIS_ID_POWER);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 2) {
			setSensor(AXIS_ID_X);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 3) {
			setSensor(AXIS_ID_Y);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 4) {
			setSensor(AXIS_ID_Z);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 20) {
			setOffset();
		} else if (id == 21) {
			clearOffset();
		} else if (id == 30) {
			((TextView) new AlertDialog.Builder(LineSensorActivity.this)
			.setTitle("About")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setMessage(
							Html.fromHtml("<p>AMI LineSensor Application<br>"
									+ "<a href=\"http://www.aichi-mi.com\">Aichi Micro Intelligent Corporation</a></p>"
									+ "<p>This software includes the following works that are distributed in the Apache License 2.0.<br>"
									+ " - Physicaloid Library<br>"
									+ " - Achartengine 1.1.0</p>"
									))
			.show()
			.findViewById(android.R.id.message))
			.setMovementMethod(LinkMovementMethod.getInstance());

		} else {
		}
		return false;
	}

	/**
	 * <p>
	 * Unescapes any Java literals found in the <code>String</code> to a
	 * <code>Writer</code>.
	 * </p>
	 *
	 * <p>
	 * For example, it will turn a sequence of <code>'\'</code> and
	 * <code>'n'</code> into a newline character, unless the <code>'\'</code> is
	 * preceded by another <code>'\'</code>.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> string input has no effect.
	 * </p>
	 *
	 * @param out
	 *            the <code>String</code> used to output unescaped characters
	 * @param str
	 *            the <code>String</code> to unescape, may be null
	 * @throws IllegalArgumentException
	 *             if the Writer is <code>null</code>
	 * @throws IOException
	 *             if error occurs on underlying Writer
	 */
	private String unescapeJava(String str) throws IOException {
		if (str == null) {
			return "";
		}
		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);

		StringBuilder strout = new StringBuilder();
		boolean hadSlash = false;
		boolean inUnicode = false;
		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow
				unicode.append(ch);
				if (unicode.length() == 4) {
					// unicode now contains the four hex digits
					// which represents our unicode character
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						strout.append((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						// throw new
						// NestableRuntimeException("Unable to parse unicode value: "
						// + unicode, nfe);
						throw new IOException("Unable to parse unicode value: "
								+ unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					strout.append('\\');
					break;
				case '\'':
					strout.append('\'');
					break;
				case '\"':
					strout.append('"');
					break;
				case 'r':
					strout.append('\r');
					break;
				case 'f':
					strout.append('\f');
					break;
				case 't':
					strout.append('\t');
					break;
				case 'n':
					strout.append('\n');
					break;
				case 'b':
					strout.append('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;
				}
				default:
					strout.append(ch);
					break;
				}
				continue;
			} else if (ch == '\\') {
				hadSlash = true;
				continue;
			}
			strout.append(ch);
		}
		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			strout.append('\\');
		}
		return new String(strout.toString());
	}
}
