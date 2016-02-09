package jp.co.etlab.map.WimCurrent;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.FrameLayout1, new PlaceholderFragment(this)).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment implements SensorEventListener,
	 LocationListener,
	 ActionBar.OnNavigationListener {
		private SensorManager mSensorManager;   // センサマネージャ
	    private Sensor mAccelerometer;  // 加速度センサ
	    private Sensor mMagneticField;  // 磁気センサ
	    private TextView txtDegree;
	    private TextView txtDistance;
	    private TextView txtTodeg;
	    private TextView txtArrow;
		private ImageView imgArrow;
		private float nowDist;

	    static Context context;
		@SuppressWarnings("unused")
		private double az2;
	    private double s12;
		private double az1;
	    //private LocationClient mLocationClient = null;

	    LocationManager locationManager;
	    List<String> _allProvider;
	    Location _location;
	    Location _beforeLocation;
	    String _thisPlaceName="";

		public PlaceholderFragment(Context context) {
			if (context != null) {
				PlaceholderFragment.context = context;
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.activity_main, container,
					false);

            //mLocationClient = new LocationClient(PlaceholderFragment.context, this, this);

	        //システムサービスのLOCATION_SERVICEからLocationManager objectを取得
	        locationManager = (LocationManager) PlaceholderFragment.context.getSystemService(Context.LOCATION_SERVICE);
	        //可能なプロバイダーをリスト取得
	        _allProvider =  locationManager.getAllProviders();
	        //2時間以内のできるだけ正確な位置を取得
	        _beforeLocation = checkKnownLocation(_allProvider,1000*60*120);
	        if(_beforeLocation != null){
	    		_location = _beforeLocation;
	    		// TODO 自動生成されたメソッド・スタブ
	    		//String strLat = String.valueOf(_location.getLatitude());
	    		//String strLng = String.valueOf(_location.getLongitude());
	        }

			this.txtDegree = (TextView) rootView.findViewById(R.id.degree);
			this.txtDistance = (TextView) rootView.findViewById(R.id.distance);
			this.txtTodeg = (TextView) rootView.findViewById(R.id.todeg);
			this.txtArrow = (TextView) rootView.findViewById(R.id.toarrow);
			this.imgArrow = (ImageView)rootView.findViewById(R.id.imageView);

			//txtDegree.setText("test");
	       	//this.context = getActivity();
			imgArrow.setImageResource(R.drawable.aroow2);
			nowDist = 0.0f;

	       	//２点間距離計算クラス
	       	GeoDist geo = new GeoDist();
	        double lon1 = _location.getLongitude();	//ここ
	        double lat1 = _location.getLatitude();	//ここ
	        double lon2 = 39.816667;	//メッカ
	        double lat2 = 21.416667;	//メッカ

			double val[] = new double[3];
			val = geo.solve_inverse_problem(
					geo.deg2rad(lon1), geo.deg2rad(lat1),
					geo.deg2rad(lon2), geo.deg2rad(lat2));
			this.s12 = val[0];
			this.az1 = geo.rad2deg(val[1]);
			this.az2 = geo.rad2deg(val[2]);

			// センサーを取り出す
	        this.mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
	        this.mAccelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	        this.mMagneticField = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

	        return rootView;
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO 自動生成されたメソッド・スタブ

	        // センサーごとの処理
	        if (event.sensor==this.mAccelerometer){
	        // 加速度センサー
	            this.mAccelerometerValue = event.values.clone();
	        }
	        // 磁気センサー
	        else if (event.sensor== this.mMagneticField) {
//	        else  {
	            this.mMagneticFieldValue = event.values.clone();
	            this.mValidMagneticFiled = true;
	        }

	        // 値が更新された角度を出す準備ができた
	        if (this.mValidMagneticFiled) {
	            // 方位を出すための変換行列
	            float[] rotate = new float[16]; // 傾斜行列？
	            float[] inclination = new float[16];    // 回転行列

	            // うまいこと変換行列を作ってくれるらしい
	            SensorManager.getRotationMatrix(
	                    rotate, inclination,
	                    this.mAccelerometerValue,
	                    this.mMagneticFieldValue);

	            // 方向を求める
	            float[] orientation = new float[3];
	            //ときどき取得に失敗するが無視する
	            try{
		            this.getOrientation(rotate, orientation);
	            } catch (NullPointerException e) {
	            	//nop
	            }

	            // デグリー角に変換する
	            float degreeDir = (float)Math.toDegrees(orientation[0]);
	            //View v = this.infla.inflate(R.layout.activity_main, this.contain, false);

	            //Log.i("onSensorChanged", "角度:" + degreeDir);
	            //txtDegree = (TextView)findViewById(R.id.degree);
	            //txtDegree = (TextView)v.findViewById(R.id.degree);
	            //txtDegree.setText(Integer.toString((int)degreeDir));
	            txtDegree.setText(getString(R.string.orientation_name)+getHoui(degreeDir));
	            txtDistance.setText(getString(R.string.makkah_distance)+String.format("%1$,.2fKm",s12/1000.0));//m->Km
	            txtTodeg.setText(getString(R.string.makkah_orientation)+getHoui((float)az1));
	            txtArrow.setText(getArrow(degreeDir,(float)az1));

				//imgView

				ImageView tmp=(ImageView)imgArrow;
				RotateAnimation anim = new RotateAnimation((float)az1-nowDist,degreeDir,tmp.getWidth()/2,tmp.getHeight()/2);
				anim.setDuration(3000);
				anim.setFillAfter(true);
				tmp.startAnimation(anim);
				nowDist = degreeDir;
				Log.d("whereismakkah1:az1",String.valueOf(degreeDir));
	        }
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onPause(){
	        super.onPause();
			locationManager.removeUpdates(PlaceholderFragment.this);
	        //if (mLocationClient != null) {
	            // Google Play Servicesを切断
	        //    mLocationClient.disconnect();
	        //}
	        // リスナーの登録解除
	        this.mSensorManager.unregisterListener(this);
		}
	    @Override
		public
		 void onResume() {
	        super.onResume();
	    	//mLocationClient.connect();
	     	locationManager.removeUpdates(PlaceholderFragment.this);
	     	requestUpdates(_allProvider);
	        // リスナーの登録
	        this.mSensorManager.registerListener(
	                this, this.mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	        this.mSensorManager.registerListener(
	                this, this.mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
	    }

	    // 加速度センサの値
	    private float[] mAccelerometerValue = new float[3];
	    // 磁気センサの値
	    private float[] mMagneticFieldValue = new float[3];
	    // 磁気センサの更新がすんだか
	    private boolean mValidMagneticFiled = false;

	    //16方位に変換する
	    private String getHoui(float degree) {
	    	String houi="";

	    	if(((degree> -11.25) && (degree<=0)) ||
	    			((degree>0) && (degree<=11.25))){
	    		houi = getString(R.string.north_str);
	    	}else if((degree> 11.25) && (degree<=33.75)) {
	    		houi = getString(R.string.north_north_east_str);
	    	}else if((degree> 33.75) && (degree<=56.25)) {
	    		houi = getString(R.string.north_east_str);
	    	}else if((degree> 56.25) && (degree<=78.75)) {
	    		houi = getString(R.string.east_north_east_str);
	    	}else if((degree> 78.75) && (degree<=101.25)) {
	    		houi = getString(R.string.east_str);
	    	}else if((degree> 101.25) && (degree<=123.75)) {
	    		houi = getString(R.string.east_south_east_str);
	    	}else if((degree> 123.75) && (degree<=146.25)) {
	    		houi = getString(R.string.east_south_str);
	    	}else if((degree> 146.25) && (degree<=168.75)) {
	    		houi = getString(R.string.south_south_east_str);
	    	}else if(((degree> 168.75) && (degree<=180.0)) ||
	    			((degree< -168.75) && (degree>=-180.0))) {
	    		houi = getString(R.string.south_str);
	    	}else if((degree< -11.25) && (degree>=-33.75)) {
	    		houi = getString(R.string.north_north_west_str);
	    	}else if((degree< -33.75) && (degree>=-56.25)) {
	    		houi = getString(R.string.west_north_str);
	    	}else if((degree< -56.25) && (degree>=-78.75)) {
	    		houi = getString(R.string.west_north_west_str);
	    	}else if((degree< -78.75) && (degree>=-101.25)) {
	    		houi = getString(R.string.west_str);
	    	}else if((degree< -101.25) && (degree>=-123.75)) {
	    		houi = getString(R.string.west_south_west_str);
	    	}else if((degree< -123.75) && (degree>=-146.25)) {
	    		houi = getString(R.string.south_west_str);
	    	}else if((degree< -146.25) && (degree>=-168.75)) {
	    		houi = getString(R.string.south_south_west_str);
	    	}
	    	return houi;
	    }

	    //矢印に変換する
	    private String getArrow(float nowDire, float setDire) {
	    	String arrow="";

	    	float divDire;

	    	nowDire = get180to360(nowDire);
	    	setDire = get180to360(setDire);
	    	divDire = nowDire - setDire;

	    	//if (divDire > 180.0) {
	    	//	divDire = (float)(divDire*(float)(-1.0))+(float)180.0;
	    	//} else if (divDire < -180.0) {
	    	//	divDire = (float)(divDire*(float)(-1.0))-(float)180.0;
	    	//}
	    	divDire = get360to180(divDire);

	    	if(((divDire> -5.0) && (divDire<=0.0)) ||
	    			((divDire>0.0) && (divDire<=5.0))){
	    		arrow = "صحيح";
	    	}else if((divDire> 5.0) && (divDire<=90.0)) {
	    		arrow = "<";
	    	}else if((divDire> 90.0) && (divDire<=180.0)) {
	    		arrow = "<<";

	    	}else if((divDire< -5.0) && (divDire>=-90.0)) {
	    		arrow = ">";
	    	}else if((divDire< -90.0) && (divDire>=-180.0)) {
	    		arrow = ">>";
	    	}
	    	return arrow;
	    	//return arrow+String.valueOf(nowDire)+"/"+String.valueOf(setDire)+"/"+String.valueOf(divDire);
	    }

	    // 0-180はそのままで -1--179は 181-379にする
	    public float get180to360(float source) {
	    	float compData;

	    	//1-180 nop
	    	if (source < 0.0) compData = 360 + source;
	    	else compData = source;

	    	return compData;
	    }

	    public float get360to180(float source) {
	    	float compData;

	    	//1-180 nop
	    	if (source > 180.0) compData = source - (float)360.0;
	    	else if (source < -180.0) compData = (float)360.0 + source;
	    	else compData = source;

	    	return compData;
	    }
	    // ////////////////////////////////////////////////////////////
	    // 画面が回転していることを考えた方角の取り出し
	    public void getOrientation(float[] rotate, float[] out) {

	        // ディスプレイの回転方向を求める(縦もちとか横持ちとか)
	    	Display disp = getActivity().getWindowManager().getDefaultDisplay();
	        //Display disp = this.getWindowManager().getDefaultDisplay();
	        // ↓コレを使うためにはAPIレベルを8にする必要がある
	        int dispDir = disp.getRotation();

	        // 画面回転してない場合はそのまま
	        if (dispDir == Surface.ROTATION_0) {
	            SensorManager.getOrientation(rotate, out);

	            // 回転している
	        } else {

	            float[] outR = new float[16];

	            // 90度回転
	            if (dispDir == Surface.ROTATION_90) {
	                SensorManager.remapCoordinateSystem(
	                        rotate, SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X, outR);
	                // 180度回転
	            } else if (dispDir == Surface.ROTATION_180) {
	                float[] outR2 = new float[16];

	                SensorManager.remapCoordinateSystem(
	                        rotate, SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X, outR2);
	                SensorManager.remapCoordinateSystem(
	                        outR2, SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X, outR);
	                // 270度回転
	            } else if (dispDir == Surface.ROTATION_270) {
	                SensorManager.remapCoordinateSystem(
	                        outR, SensorManager.AXIS_MINUS_Y,SensorManager.AXIS_MINUS_X, outR);
	            }
	            SensorManager.getOrientation(outR, out);
	        }
	    }

	    //指定の時間以内でもっとも、精度の高いものを取得する
	    private Location checkKnownLocation(List<String> allProvider ,long time){
	        Location rlocation = null;
	        for(int i = 0 ; i < allProvider.size() ; i++){
	            Location tmplocation = locationManager.getLastKnownLocation(allProvider.get(i));
	            if(tmplocation != null){
	                if(tmplocation.getTime() > System.currentTimeMillis() - time){
	                    if(rlocation == null){
	                        rlocation = tmplocation;
	                    }else if(rlocation.getAccuracy() > tmplocation.getAccuracy()){
	                        rlocation = tmplocation;
	                    }
	                }
	            }
	        }
	        return rlocation;
	    }

	    //使用可能なプロバイダのupdateの依頼をする
	    private void requestUpdates(List<String> allProvider){
	        for(int i = 0 ; i < allProvider.size() ; i++){	//１時間か100m
	        	locationManager.requestLocationUpdates(allProvider.get(i), 1000*60*60, 100, this);
	        }
	    }

		@Override
		public boolean onNavigationItemSelected(int arg0, long arg1) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		@Override
		public void onLocationChanged(Location arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO 自動生成されたメソッド・スタブ

		}
	}
}
