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

public class LineSensorData {
	public class LineSensor {
		private double[][] mag = new double[3][16];

		public LineSensor() {
			super();
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 16; j++) {
					mag[i][j] = 0.0;
				}
			}
		}

		public LineSensor(LineSensor s) {
			super();
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 16; j++) {
					mag[i][j] = s.mag[i][j];
				}
			}
		}

		public double getMag(int ch, int axis) {
			return mag[axis][ch - 1];
		}

		public void setMag(int ch, int axis, double val) {
			mag[axis][ch - 1] = val;
		}

		public int calcPow(int ch) {
			int ret;
			try {
				ret = (int) Math.sqrt(mag[0][ch - 1] * mag[0][ch - 1]
						+ mag[1][ch - 1] * mag[1][ch - 1] + mag[2][ch - 1]
						* mag[2][ch - 1]);
			} catch (Exception e) {
				ret = 0;
			}
			return ret;
		}
	}

	private LineSensor mSensorData = new LineSensor();
	private LineSensor mOffsetData = new LineSensor();
	private LineSensor mLastValue = new LineSensor();
	private StringBuilder mText = new StringBuilder();
	public static final int SENSOR_SPS = 5; // [SPS]

	public LineSensorData() {
		super();
	}

	public double getData(int ch, int axis) {
		return mSensorData.getMag(ch, axis) - mOffsetData.getMag(ch, axis);
	}

	public void clearOffset() {
		for (int axis = 0; axis < 3; axis++) {
			for (int ch = 1; ch <= 16; ch++) {
				mOffsetData.setMag(ch, axis, 0);
			}
		}
	}

	public void setOffset() {
		for (int axis = 0; axis < 3; axis++) {
			for (int ch = 1; ch <= 16; ch++) {
				mOffsetData.setMag(ch, axis, mSensorData.getMag(ch, axis));
			}
		}
	}

	public void initData() {
		mText.setLength(0);
	}

	public double getPower(int ch) {
		LineSensor pow = new LineSensor();
		for (int axis = 0; axis < 3; axis++) {
			pow.setMag(ch, axis,
					mSensorData.getMag(ch, axis) - mOffsetData.getMag(ch, axis));
		}
		return pow.calcPow(ch);
	}

	private int ch_index = 0;
	private int axis_index = 0;

	public void addData(byte[] rbuf, int len) {
		for (int i = 0; i < len; i++) {
			if (rbuf[i] >= '0' && rbuf[i] <= '9') {
				mText.append((char) rbuf[i]);
			} else if (rbuf[i] == '-') {
				mText.append((char) rbuf[i]);
			} else if ((rbuf[i] == ',')
					| (rbuf[i] == '\r' && ch_index == 15 && axis_index == 2)) {
				mLastValue.mag[axis_index][ch_index] = (double) Double
						.parseDouble(mText.toString());
				mText.setLength(0);

				axis_index++;
				if (axis_index > 2) {
					axis_index = 0;
					ch_index++;
					if (ch_index > 15) {
						ch_index = 0;
					}
				}
			} else if (rbuf[i] == '|') {
				mSensorData = new LineSensor(mLastValue);
				ch_index = 0;
				axis_index = 0;
				mText.setLength(0);
			}
		}
	}
}
