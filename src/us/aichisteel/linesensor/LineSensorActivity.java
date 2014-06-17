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

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import us.aichisteel.amisensor.*;
import us.aichisteel.linesensor.R;

public class LineSensorActivity extends Activity implements AMISensorInterface {

	private LineSensor mSensor;
	private Button btStart;
	private Button btStop;
	private Button btOffset;
	private Button btClearOffset;
	private RadioGroup rgSensorSelection;
	private RadioButton rbtPow;
	private RadioButton rbtX;
	private RadioButton rbtY;
	private RadioButton rbtZ;

	private XYMultipleSeriesRenderer mRenderer;
	private XYMultipleSeriesDataset dataset;
	private GraphicalView graphicalView;

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

		mRenderer = buildRenderer();

		LinearLayout layout = (LinearLayout) findViewById(R.id.plot_area);
		graphicalView = ChartFactory.getBarChartView(getApplicationContext(),
				dataset, mRenderer, Type.DEFAULT);
		layout.addView(graphicalView, new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

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

		mSensor = new LineSensor(this);
		mSensor.initialize(this);
		enableButtons(mSensor.isReady());

		btStart.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.startSensor();
			}
		});

		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.stopSensor();
			}
		});

		btOffset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.setOffset();
			}
		});

		btClearOffset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSensor.clearOffset();
			}
		});

		rgSensorSelection
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						int id = LineSensor.AXIS_ID_POWER;
						switch (checkedId) {
						case R.id.rd_pow:
							id = LineSensor.AXIS_ID_POWER;
							break;
						case R.id.rd_x:
							id = LineSensor.AXIS_ID_X;
							break;
						case R.id.rd_y:
							id = LineSensor.AXIS_ID_Y;
							break;
						case R.id.rd_z:
							id = LineSensor.AXIS_ID_Z;
							break;
						}
						mSensor.setSensorAxis(id);
					}
				});
	}

	protected void onDestroy() {
		mSensor.finalize(this);
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onNewIntent(Intent intent) {
		mSensor.initialize(this);
		enableButtons(mSensor.isReady());
	}

	@Override
	public void attachedSensor() {
		enableButtons(mSensor.isReady());
	}

	@Override
	public void detachedSensor() {
		enableButtons(mSensor.isReady());
		finish();
	}

	@Override
	public void dataReady() {
		dataset.getSeriesAt(0).clear();
		for (int ch = 1; ch <= 16; ch++) {
			if (mSensor.getSensorAxis() == LineSensor.AXIS_ID_POWER) {
				dataset.getSeriesAt(0).add(ch, mSensor.getPower(ch));
			} else if (mSensor.getSensorAxis() == LineSensor.AXIS_ID_X) {
				dataset.getSeriesAt(0).add(ch, mSensor.getData(ch, 0));
			} else if (mSensor.getSensorAxis() == LineSensor.AXIS_ID_Y) {
				dataset.getSeriesAt(0).add(ch, mSensor.getData(ch, 1));
			} else if (mSensor.getSensorAxis() == LineSensor.AXIS_ID_Z) {
				dataset.getSeriesAt(0).add(ch, mSensor.getData(ch, 2));
			}
		}
		graphicalView.repaint();
	}

	@Override
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == 1) {
			mSensor.setSensorAxis(LineSensor.AXIS_ID_POWER);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 2) {
			mSensor.setSensorAxis(LineSensor.AXIS_ID_X);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 3) {
			mSensor.setSensorAxis(LineSensor.AXIS_ID_Y);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 4) {
			mSensor.setSensorAxis(LineSensor.AXIS_ID_Z);
			item.setChecked(item.isChecked() ? false : true);
		} else if (id == 20) {
			mSensor.setOffset();
		} else if (id == 21) {
			mSensor.clearOffset();
		} else if (id == 30) {
			((TextView) new AlertDialog.Builder(LineSensorActivity.this)
					.setTitle("About")
					.setIcon(android.R.drawable.ic_dialog_info)
					.setMessage(
							Html.fromHtml("<p>AMI LineSensor Application<br>"
									+ "<a href=\"http://www.aichi-mi.com\">Aichi Micro Intelligent Corporation</a></p>"
									+ "<p>This software includes the following works that are distributed in the Apache License 2.0.<br>"
									+ " - Physicaloid Library<br>"
									+ " - Achartengine 1.1.0</p>")).show()
					.findViewById(android.R.id.message))
					.setMovementMethod(LinkMovementMethod.getInstance());

		}
		return false;
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
			color = Color.GRAY;

		btStart.setTextColor(color);
		btStop.setTextColor(color);
		btOffset.setTextColor(color);
		btClearOffset.setTextColor(color);
		rbtPow.setTextColor(color);
		rbtX.setTextColor(color);
		rbtY.setTextColor(color);
		rbtZ.setTextColor(color);
	}
}
