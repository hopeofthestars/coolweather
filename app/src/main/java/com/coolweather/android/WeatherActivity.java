package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.County_Addition;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.R.attr.id;

public class WeatherActivity extends AppCompatActivity {
    private ImageView imageView;
    private int weatherNum=0,defNum;
    private int LEFT=1000,RIGHT=1001;
    private int STATE=1;
    private FragmentTransaction transaction;
    private OneFragment fragmentOne;
    private TwoFragment fragmentTwo;
    private float x1=0;
    private float x2=0;
    private int flag=3;
    private FrameLayout weather_layout;
    private ImageView bingPicImg;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageView=(ImageView)findViewById(R.id.beijing_pic);//背景图

        if(Build.VERSION.SDK_INT>=21){//如果版本号大于或等于21
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);//让状态栏也可以显示活动的布局
            getWindow().setStatusBarColor(Color.TRANSPARENT);//将状态栏设置为透明色
        }
        setContentView(R.layout.activity_weather);
        //weather_layout.setOnTouchListener(this);
        Log.d("TAG","onCreate");
        bingPicImg=(ImageView)findViewById(R.id.bing_pic_img);
        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic=preferences.getString("bing_pic",null);
        if(bingPic!=null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            lodimage();
        }
        initFragment();
        //加载背景图片
    }
    /*private boolean fling(MotionEvent e1,MotionEvent e2, MotionEvent e3,MotionEvent e4,float vlocity) {
        if (e1.getX() - e2.getX() > 100) {
            if (flag == 3) {
                weather_layout.setBackgroundResource(R.drawable.addbg);
                flag=4;
                return true;

            }
            if(flag==4){
                weather_layout.setBackgroundResource(R.drawable.bg);
                flag=5;
                return true;
            }
            if(flag==1){
                weather_layout.setBackgroundResource(R.drawable.addbg);
                flag=2;
                return true;
            }
            if(flag==2){
                weather_layout.setBackgroundResource(R.drawable.bg);
                flag=3;
                return true;
            }
        }*/

    @Override
    protected void onStart() {
        super.onStart();
        weatherNum=0;
        defNum=0;
        showFragment();
        Log.d("WeatherActivity","onStart");
    }
    private void initFragment(){
        fragmentOne=new OneFragment();
        fragmentTwo=new TwoFragment();
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.framelayout_weather,fragmentOne);
        transaction.add(R.id.framelayout_weather,fragmentTwo);
        transaction.hide(fragmentTwo);
        transaction.commit();
    }
    private void showFragment(){
        List<County_Addition> countyAdditions= DataSupport.findAll(County_Addition.class);
        for(County_Addition countyAddition:countyAdditions){
            weatherNum++;
            if(countyAddition.getDef()==true)
                defNum=weatherNum;
            requestWeather(countyAddition.getCountyWeatherId(),countyAddition,STATE);
        }
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {//左右滑动切换Fragment
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            x1=event.getX();
        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            x2=event.getX();
            if(x2-x1>100){
                changeFragment(LEFT);
            }else if(x1-x2>100){
                changeFragment(RIGHT);
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void changeFragment(int lr){
        transaction=getSupportFragmentManager().beginTransaction();
        if(lr==RIGHT){
            transaction.setCustomAnimations(R.anim.push_left_in,R.anim.push_left_out);
            if(defNum<weatherNum){
                defNum++;
                showFragment(transaction,defNum);
            }
        }else if(lr==LEFT){
            transaction.setCustomAnimations(R.anim.push_right_in,R.anim.push_right_out);
            if(defNum>1){
                defNum--;
                showFragment(transaction,defNum);
            }
        }
        transaction.commit();
    }
    private void showFragment(FragmentTransaction transaction,int num){
        int i=0;
        if(STATE==1){
            transaction.hide(fragmentOne);
            transaction.show(fragmentTwo);
            List<County_Addition> countyAdditions= DataSupport.findAll(County_Addition.class);
            for(County_Addition countyAddition:countyAdditions){
                i++;
                if(i==num){
                    fragmentTwo.showWeatherInfo(Utility.handleWeatherResponse(countyAddition.getCountyWeather()));
                    break;
                }
            }
            STATE=2;
        }else if(STATE==2){
            transaction.hide(fragmentTwo);
            List<County_Addition> countyAdditions= DataSupport.findAll(County_Addition.class);
            for(County_Addition countyAddition:countyAdditions){
                i++;
                if(i==num){
                    fragmentOne.showWeatherInfo(Utility.handleWeatherResponse(countyAddition.getCountyWeather()));
                    break;
                }
            }
            transaction.show(fragmentOne);
            STATE=1;
        }
    }
    private void lodimage(){
        final String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
               final String bingPic =response.body().string();
                SharedPreferences.Editor editor= PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
            @Override
            public void onFailure(Call call, IOException e) {
             e.printStackTrace();
            }
        });
    }
    public void requestWeather(final String weatherId, final County_Addition county_addition, final int frag_state){
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+
                "&key=63660528a9dc4f968243a7f0669cbe26";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            county_addition.setCountyWeather(responseText);
                            county_addition.save();
                            if(county_addition.getDef()==true){
                                if(frag_state==1)
                                    fragmentOne.showWeatherInfo(weather);
                                else if(frag_state==2)
                                    fragmentTwo.showWeatherInfo(weather);
                            }
                        }else {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
            public void onFailure(Call call, IOException e){
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

}
