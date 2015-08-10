package com.example.gpsdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerDragListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.CoordinateConverter;

import org.apache.http.util.EncodingUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;

public class MainActivity extends Activity implements LocationListener, OnClickListener,
        BDLocationListener, OnMapClickListener, OnMarkerDragListener, OnGetGeoCoderResultListener {
    private static final String TAG="GpsActivity";
    private String mMockProviderName = LocationManager.GPS_PROVIDER;
    private Button bt_Ok;
    private Button bt_Fake;
    private Button bt_Recording;
    private Button bt_Stop;
    private EditText et_LoopNum;
    private EditText editText;
    private LocationManager lm;
    private LocationManager mylm;
    private double latitude = 31.3029742, longitude = 120.6097126;// 默认常州
    private Thread thread;// 需要一个线程一直刷新
    private Boolean RUN = true;
    private TextView tv_location;
    private static int flag = 0; //确认按钮点击标识

    boolean isFirstLoc = true;// 是否首次定位
    // 定位相关
    private LocationClient mLocClient;
    private LocationMode mCurrentMode;// 定位模式
    private BitmapDescriptor mCurrentMarker;// 定位图标
    private MapView mMapView;
    private BaiduMap mBaiduMap;

    // 初始化全局 bitmap 信息，不用时及时 recycle
    private BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
    private Marker mMarker;
    private LatLng curLatlng;
    private GeoCoder mSearch;

    // 需要循环读取的Loaction List
    private List<GPXLocation> GPXLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iniView();
        iniListner();
        iniData();
    }

    /**
     * iniView 界面初始化
     * 
     */
    private void iniView() {
        editText = (EditText) findViewById(R.id.editText);
        bt_Ok = (Button) findViewById(R.id.bt_Ok);
        bt_Fake = (Button) findViewById(R.id.fake);
        bt_Recording = (Button) findViewById(R.id.recording);
        bt_Stop = (Button) findViewById(R.id.stop);
        et_LoopNum = (EditText) findViewById(R.id.LoopNum);
        tv_location = (TextView) findViewById(R.id.tv_location);
        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
    }

    /**
     * iniListner 接口初始化
     * 
     */
    private void iniListner() {
        bt_Ok.setOnClickListener(this);
        bt_Fake.setOnClickListener(this);
        bt_Recording.setOnClickListener(this);
        bt_Stop.setOnClickListener(this);
        mLocClient.registerLocationListener(this);
        mBaiduMap.setOnMapClickListener(this);
        mBaiduMap.setOnMarkerDragListener(this);

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);
    }

    /**
     * iniData 数据初始化
     * 
     */
    private void iniData() {
        inilocation();
        initLocationData();
        iniMap();
    }

    /**
     * initLocationData 初始化Loaction List
     */
    private void initLocationData() {
        //InputStream in = MainActivity.class.getResourceAsStream("lushu.gpx");
        try {
            String xmlContext = FileUnitFromSDCard.readFileSdcardFile("/mnt/sdcard/GPX/1.xml");
            InputStream is = InputStreamUtils.StringTOInputStream(xmlContext);
            //InputStream is = getResources().getAssets().open("perhalfsecond.xml");
            GPXLocation = ReadXML.readXML(is);
            //2^Y(X-1)+1
            //AddAvgLocationInfo();
            //AddAvgLocationInfo();
            //AddAvgLocationInfo();
            //AddAvgLocationInfo();
            //AddAvgLocationInfo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * iniMap 初始化地图
     * 
     */
    private void iniMap() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        mCurrentMode = LocationMode.NORMAL;
        // 缩放
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(14.0f);
        mBaiduMap.setMapStatus(msu);

        mBaiduMap.setMyLocationConfigeration(new MyLocationConfiguration(mCurrentMode, true,
                mCurrentMarker));
        mLocClient.setLocOption(option);
        mLocClient.start();
        initOverlay();
    }

    /**
     * initOverlay 设置覆盖物，这里就是地图上那个点
     * 
     */
    private void initOverlay() {
        LatLng ll = new LatLng(latitude, longitude);
        OverlayOptions oo = new MarkerOptions().position(ll).icon(bd).zIndex(9).draggable(true);
        mMarker = (Marker) (mBaiduMap.addOverlay(oo));
    }

    /**
     * inilocation 初始化 位置模拟
     * 
     */
    private void inilocation() {


        // 获取位置管理服务
        mylm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mylm.addTestProvider(mMockProviderName, false, true, false, false, true, true,
                true, 0, 5);
        mylm.setTestProviderEnabled(mMockProviderName, true);
        // 设置监听器，自动更新的最小时间为间隔N秒(1秒为1*1000，这样写主要为了方便)或最小位移变化超过N米
        mylm.requestLocationUpdates(mMockProviderName, 0, 0, this);


        lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //判断GPS是否正常启动
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
            //返回开启GPS导航设置界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent,0);
            return;
        }

        //为获取地理位置信息时设置查询条件
        String bestProvider = lm.getBestProvider(getCriteria(), true);
        //获取位置信息
        //如果不设置查询要求，getLastKnownLocation方法传人的参数为LocationManager.GPS_PROVIDER
        Location location= lm.getLastKnownLocation(bestProvider);
        updateView(location);
        //监听状态
        lm.addGpsStatusListener(listener);
        //绑定监听，有4个参数
        //参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
        //参数2，位置信息更新周期，单位毫秒
        //参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
        //参数4，监听
        //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新

        // 1秒更新一次，或最小位移变化超过1米更新一次；
        //注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中sleep(10000);然后执行handler.sendMessage(),更新位置
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }

    //位置监听
    private LocationListener locationListener=new LocationListener() {

        /**
         * 位置信息变化时触发
         */
        public void onLocationChanged(Location location) {
            updateView(location);
            Log.i(TAG, "时间："+location.getTime());
            Log.i(TAG, "经度："+location.getLongitude());
            Log.i(TAG, "纬度："+location.getLatitude());
            Log.i(TAG, "海拔："+location.getAltitude());
        }

        /**
         * GPS状态变化时触发
         */
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                //GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.i(TAG, "当前GPS状态为可见状态");
                    break;
                //GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(TAG, "当前GPS状态为服务区外状态");
                    break;
                //GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        /**
         * GPS开启时触发
         */
        public void onProviderEnabled(String provider) {
            Location location=lm.getLastKnownLocation(provider);
            updateView(location);
        }

        /**
         * GPS禁用时触发
         */
        public void onProviderDisabled(String provider) {
            updateView(null);
        }


    };

    //状态监听
    GpsStatus.Listener listener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                //第一次定位
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(TAG, "第一次定位");
                    break;
                //卫星状态改变
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.i(TAG, "卫星状态改变");
                    //获取当前状态
                    GpsStatus gpsStatus=lm.getGpsStatus(null);
                    //获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    //创建一个迭代器保存所有卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    System.out.println("搜索到："+count+"颗卫星");
                    break;
                //定位启动
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i(TAG, "定位启动");
                    break;
                //定位结束
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i(TAG, "定位结束");
                    break;
            }
        };
    };

    /**
     * 实时更新文本内容
     *
     * @param location
     */
    private void updateView(Location location){
        if(location!=null){
            editText.setText("设备位置信息\n\n经度：");
            editText.append(String.valueOf(location.getLongitude()));
            editText.append("\n纬度：");
            editText.append(String.valueOf(location.getLatitude()));
        }else{
            //清空EditText对象
            editText.getEditableText().clear();
        }
    }

    /**
     * 返回查询条件
     * @return
     */
    private Criteria getCriteria(){
        Criteria criteria=new Criteria();
        //设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        //设置是否要求速度
        criteria.setSpeedRequired(false);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(false);
        //设置是否需要方位信息
        criteria.setBearingRequired(false);
        //设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }
    /**
     * setLocation 设置GPS的位置
     * 
     */
    private void setLocation(double longitude, double latitude) {
        Location location = new Location(mMockProviderName);
        location.setTime(System.currentTimeMillis());
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(2.0f);
        location.setAccuracy(3.0f);

        mylm.setTestProviderLocation(mMockProviderName, location);
    }


    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        Log.i("gps", String.format("location: x=%s y=%s", lat, lng));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        thisFinish();
    }

    @Override
    protected void onDestroy() {
        RUN = false;
        thread = null;

        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        bd.recycle();
        super.onDestroy();
    }

    private void thisFinish() {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle("提示");
        build.setMessage("退出后，将不再提供定位服务，继续退出吗？");
        build.setPositiveButton("确认", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        build.setNeutralButton("最小化", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                moveTaskToBack(true);
            }
        });
        build.setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        build.show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
        case R.id.bt_Ok:
            FakeLocation();
            break;

            case R.id.fake:
                latitude = curLatlng.latitude;
                longitude = curLatlng.longitude;
                Fake(GPXLocation);

                break;
            case R.id.recording:
                inilocation();
                RecordLocation();
                break;
            case R.id.stop:
                RUN = false;
                break;
        }
    }

    /**
     * 定位SDK监听函数
     */
    @Override
    public void onReceiveLocation(BDLocation location) {
        // map view 销毁后不在处理新接收的位置
        if (location == null || mMapView == null) {
            return;
        }

        if (isFirstLoc) {
            isFirstLoc = false;
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            setCurrentMapLatLng(ll);
        }
    }

    @Override
    public void onMapClick(LatLng arg0) {
        setCurrentMapLatLng(arg0);
    }

    @Override
    public boolean onMapPoiClick(MapPoi arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * setCurrentMapLatLng 设置当前坐标
     */
    private void setCurrentMapLatLng(LatLng arg0) {
        curLatlng = arg0;
        mMarker.setPosition(arg0);

        // 设置地图中心点为这是位置
        LatLng ll = new LatLng(arg0.latitude, arg0.longitude);
        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
        mBaiduMap.animateMapStatus(u);

        // 根据经纬度坐标 找到实地信息，会在接口onGetReverseGeoCodeResult中呈现结果
        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(arg0));
    }

    /**
     * onMarkerDrag 地图上标记拖动结束
     */
    @Override
    public void onMarkerDrag(Marker arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * onMarkerDragEnd 地图上标记拖动结束
     */
    @Override
    public void onMarkerDragEnd(Marker marker) {
        setCurrentMapLatLng(marker.getPosition());
    }

    /**
     * onMarkerDragStart 地图上标记拖动开始
     */
    @Override
    public void onMarkerDragStart(Marker arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * onGetGeoCodeResult 搜索（根据实地信息-->经纬坐标）
     */
    @Override
    public void onGetGeoCodeResult(GeoCodeResult arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * onGetReverseGeoCodeResult 搜索（根据坐标-->实地信息）
     */
    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "抱歉，未能找到结果", Toast.LENGTH_LONG).show();
            return;
        }

        tv_location.setText(String.format("伪造位置：%s", result.getAddress()));
    }

    public  void Fake(List<GPXLocation> myFake)
    {
        RUN=true;
        // 开启线程，一直显示GPS坐标运动，但并不将GPS信息传给设备
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int i =0;
                while (RUN) {
                    try {
                        Thread.sleep(500);
                        i++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(i<GPXLocation.size()) {
                        latitude = Double.parseDouble(GPXLocation.get(i).getlat());
                        longitude = Double.parseDouble(GPXLocation.get(i).getlon());
                        LatLng myLL = new LatLng(latitude,longitude);
                        curLatlng = myLL;
                        mMarker.setPosition(myLL);
                        // 设置地图中心点为这是位置
                        LatLng ll = new LatLng(myLL.latitude, myLL.longitude);
                        MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                        mBaiduMap.animateMapStatus(u);

                        // 根据经纬度坐标 找到实地信息，会在接口onGetReverseGeoCodeResult中呈现结果
                        mSearch.reverseGeoCode(new ReverseGeoCodeOption().location(myLL));
                    }
                    else {
                        RUN=Boolean.FALSE;
                    }
                }
            }
        });
        thread.start();
    }

    public void FakeLocation()
    {
        final int LoopNum = Integer.parseInt(et_LoopNum.getText().toString());
        RUN=true;
        // 开启线程，一直修改GPS坐标
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //开始前先暂停5秒
                try{Thread.sleep(5000);}
                catch (InterruptedException e)
                {e.printStackTrace();}
                int i =0;
                while (RUN) {
                    try {
                        Thread.sleep(100);//78
                        i++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //循环
                    for (int loop = 0; loop < LoopNum; loop++) {
                        if (i < GPXLocation.size()) {
                            latitude = Double.parseDouble(GPXLocation.get(i).getlat());
                            longitude = Double.parseDouble(GPXLocation.get(i).getlon());
                        } else {
                            RUN=Boolean.FALSE;
                        }

                        setLocation(longitude, latitude);
                    }
                }
            }
        });
        thread.start();
    }

    public void RecordLocation()
    {
        RUN=true;
        try {
            FileUnitFromSDCard.NewDir("/mnt/sdcard/GPX/");
        } catch (Exception e) {
            e.printStackTrace();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<GPXLocation> recordLocation = new ArrayList<GPXLocation>();
                SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");//设置日期格式
                String filename="/mnt/sdcard/GPX/" + df.format(new Date());
                int i =0;
                while (RUN) {
                    try {
                        Thread.sleep(100);
                        getCriteria();
                        String bestProvider = lm.getBestProvider(getCriteria(), true);
                        Location location = lm.getLastKnownLocation(bestProvider); // 通过GPS获取位置
                        recordLocation.add(updateToNewLocation(location));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    StringBuilder strLoc = new StringBuilder();
                    strLoc.append("<GPX>\n");
                    for(int j =0;j<recordLocation.size();j++) {

                        strLoc.append("<trkpt lat=\"" + recordLocation.get(j).getlat() + "\" lon=\"" + recordLocation.get(j).getlon() + "\">\n" +
                                "                <time>" + recordLocation.get(j).gettime() + "</time>\n" +
                                "            </trkpt>\n");

                    }
                    strLoc.append("</GPX>\n");
                    FileUnitFromSDCard.writeFileSdcardFile(filename, strLoc.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void AddAvgLocationInfo()
    {
        // GPX文件中 point 不足时，用于计算中间点
        List<GPXLocation> avgGpxInfo = new ArrayList<com.example.gpsdemo.GPXLocation>();
        for(int i=0;i<GPXLocation.size();i++)
        {
            GPXLocation per = new GPXLocation();
            GPXLocation now = new GPXLocation();
            GPXLocation next = new GPXLocation();
            if(i==GPXLocation.size()-1)
            {
                per=GPXLocation.get(i);
                avgGpxInfo.add(per);
            }
            else
            {
                per=GPXLocation.get(i);
                next=GPXLocation.get(i+1);
                Double nowlat = (Double.parseDouble(per.getlat())+Double.parseDouble(next.getlat()))/2;
                Double nowlon = (Double.parseDouble(per.getlon())+Double.parseDouble(next.getlon()))/2;
                now.setlat("" + nowlat);
                now.setlon("" + nowlon);
                avgGpxInfo.add(per);
                avgGpxInfo.add(now);
            }
        }
        if(avgGpxInfo.size()>GPXLocation.size())
        {
            GPXLocation.clear();
            GPXLocation=avgGpxInfo;
        }
    }

    private com.example.gpsdemo.GPXLocation updateToNewLocation(Location location) {
        com.example.gpsdemo.GPXLocation nowLocation = new GPXLocation();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String sDateTime = df.format(location.getTime());

        nowLocation.settime(sDateTime);
        nowLocation.setlat("" + location.getLatitude());
        nowLocation.setlon("" + location.getLongitude());
        return nowLocation;
    }
}
