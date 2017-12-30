package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.IntroHelper;
import com.fsck.k9.mail.internet.Viewable;
import com.fsck.k9.message.html.HtmlConverter;

/**
 * Displays a welcome message when no accounts have been created yet.
 */
public class WelcomeMessage extends K9Activity{

    private ViewPager viewPager;
    private IntroHelper introhelper;
    private int[]  layouts;
    private TextView[] dots;
    private LinearLayout dotsLayout;
    private Button next;
    private Button skip;
    private Button imp;
    private ViewPagerAdapter viewPagerAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        introhelper = new IntroHelper(this);
        if(!introhelper.check()){
            introhelper.setFirst(false);
            Intent i = new Intent(WelcomeMessage.this,AccountCreator.class);
            startActivity(i);
            finish();
        }
        setContentView(R.layout.welcome_message);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        skip = (Button) findViewById(R.id.btn_skip);
        next = (Button) findViewById(R.id.btn_next);
        imp = (Button) findViewById(R.id.imp_set);


        layouts = new int[]{R.layout.welcome_splash,R.layout.welcome_splash_detail1,R.layout.welcome_splash_detail2,R.layout.welcomr_splash_end};

        addBottomDots(0);
        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(viewListener);

        skip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(WelcomeMessage.this,AccountSetupBasics.class);
                startActivity(i);
                finish();
            }
        });

        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = getItem(+1);
                if (current<layouts.length){
                    viewPager.setCurrentItem(current);
                }
                else {
                    Intent i = new Intent(WelcomeMessage.this,AccountSetupBasics.class);
                    startActivity(i);
                    finish();
                }
            }
        });

        imp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Accounts.importSettings(WelcomeMessage.this);
                finish();
            }
        });
    }

    private void addBottomDots(int position){

        dots = new TextView[layouts.length];
        dotsLayout.removeAllViews();
        for(int i=0;i<layouts.length;i++){
            dots[i]= new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(getResources().getColor(R.color.dot_light_screen));
        }
        if (dots.length>0)
            dots[position].setTextColor(getResources().getColor(R.color.dot_dark_screen));

    }

    private int getItem(int i){
        return viewPager.getCurrentItem()+i;
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

            addBottomDots(position);
            if (position==layouts.length-1){
                next.setText(R.string.proceed_button);
                skip.setVisibility(View.GONE);
            }
            else {
                next.setText(R.string.next_button);
                skip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void changeStatusBarColor(){}

    public class ViewPagerAdapter extends PagerAdapter{

        private LayoutInflater layoutInflater;

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = layoutInflater.inflate(layouts[position],container,false);
            container.addView(v);
            return v;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View v = (View) object;
            container.removeView(v);
        }
    }
}
