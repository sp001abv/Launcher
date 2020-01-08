package com.sp.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements Thread.UncaughtExceptionHandler {

    class App {
        Rect bounds = new Rect();
        int posX;
        int posY;
        String id;
        String name;
        Drawable icon;

        void onDraw(Canvas canvas, boolean text)
        {
            int width = canvas.getWidth() / POS_X;
            int height = canvas.getHeight() / POS_Y;
            int left = posX * width;
            int top = posY * height;
            bounds.set(left, top, left + width,  top + height);
            int w = 96;
            int h = w;
            int l = left + (width - w)/2;
            int t = top + height - h - (int)(paint.getTextSize() * 2);
            icon.setBounds(l, t, l + w, t + h);
            icon.draw(canvas);
            if (text)
                canvas.drawText(name, left + (width -paint.measureText(name)) /2, top + height - paint.getTextSize() / 2, paint);
        }
    }

    ArrayList<App> allApps;
    ArrayList<App> homeApps;
    ArrayList<App> showApps;
    HomeView view;
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final int POS_X = 7;
    static final int POS_Y = 5;
    static final String PACKAGE = String.valueOf(Character.PARAGRAPH_SEPARATOR);
    static final String VALUE = String.valueOf(Character.LINE_SEPARATOR);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPrefs();
        view = new HomeView(this);
        setContentView(view);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

    }

    @Override
    public void onBackPressed() {
        if (showApps != homeApps) {
            showApps = homeApps;
            view.postInvalidate();
        }
    }

    App getApp(String packageName)
    {
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName,  PackageManager.GET_META_DATA);
            Drawable icon  = getPackageManager().getApplicationIcon(info);
            App app = new App();
            app.id = packageName;
            app.icon = icon;
            app.name = getPackageManager().getApplicationLabel(info).toString();
            return app;

        } catch (Exception e){

        }
        return null;
    }

    void loadPrefs()
    {
        homeApps = new ArrayList<>();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        if (prefs.contains(PACKAGE)) {
            String[] packages = prefs.getString(PACKAGE, null).split(PACKAGE);
            for (String pack : packages) {
                String[] values = pack.split(VALUE);
                App app = getApp(values[0]);
                if (app != null ) {
                    app.posX = Integer.parseInt(values[1]);
                    app.posY = Integer.parseInt(values[2]);
                    homeApps.add(app);
                }
            }
        }
        if (homeApps.size() == 0) {
            App app = getApp(getPackageName());
            if (app != null) {
                app.posX = 0;
                app.posY = 0;
                homeApps.add(app);
            }
        }
        showApps = homeApps;
    }

    void savePrefs()
    {
        SharedPreferences.Editor prefs = getPreferences(Context.MODE_PRIVATE).edit();
        StringBuilder sb = new StringBuilder();
        for(App app : homeApps) {
            if (sb.length() > 0)
                sb.append(PACKAGE);
            sb.append(app.id);
            sb.append(VALUE);
            sb.append(app.posX);
            sb.append(VALUE);
            sb.append(app.posY);
        }
        prefs.putString(PACKAGE, sb.toString());
        prefs.apply();
    }

    class HomeView extends View implements View.OnClickListener
    {
        App eventApp;
        float eventX;
        float eventY;

        HomeView(Context context)  {
            super(context);
            paint.setTextSize(24);
            paint.setColor(Color.WHITE);
            setOnClickListener(this);
        }

        App getApp(int x, int y) {
            for (App app : showApps) {
                if (app.bounds.contains( x,  y)) {
                    return app;
                }
            }
            return null;
        }

        void launch(App app) {
            boolean all = false;
            if (app.id.equals(getPackageName())) {
                loadAllApps();
                all = true;
                postInvalidate();
            } else {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.id);
                startActivity(launchIntent);
            }
            showApps = all ? allApps : homeApps;
        }

        void bringEventAppToHome() {
            showApps = homeApps;
            for(App app : homeApps) {
                if (app.id.equals(eventApp.id)) {
                    eventApp = app;
                    return;
                }
            }
            App app = new App();
            app.id = eventApp.id;
            app.name = eventApp.name;
            app.icon = eventApp.icon;
            app.posX = eventApp.posX;
            app.posY = eventApp.posY;
            homeApps.add(app);
            eventApp = app;
        }

        void loadAllApps()
        {
            Intent i = new Intent(Intent.ACTION_MAIN, null);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> apps = pm.queryIntentActivities(i, 0);
            int x = 0;
            int y = 0;
            allApps = new ArrayList<App>();
            for(ResolveInfo ri:apps) {
                if (!ri.activityInfo.packageName.equals(getPackageName())) {
                    App app = new App();
                    allApps.add(app);
                    app.id = ri.activityInfo.packageName;
                    app.icon = ri.loadIcon(pm);
                    app.name = ri.loadLabel(pm).toString();
                    app.posX = x++;
                    app.posY = y;
                    if (x == POS_X) {
                        x = 0;
                        y++;
                    }
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for(App app : showApps)
                app.onDraw(canvas, showApps==allApps);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                eventX = event.getX();
                eventY = event.getY();
                eventApp = getApp((int)eventX, (int)eventY);
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP){
                if (eventApp != null && eventX == event.getX() && eventY == event.getY())
                    launch(eventApp);
            } else if (action == MotionEvent.ACTION_MOVE){
                if (eventApp != null) {
                    if (showApps == homeApps &&
                            (event.getX() > getWidth() - POS_X ||
                                    event.getY() >= getHeight() - POS_Y)) {
                        homeApps.remove(eventApp);
                        eventApp = null;
                        savePrefs();
                        postInvalidate();
                    }
                    else {
                        int posX = (int)(event.getX() * POS_X / getWidth());
                        int posY =  (int)(event.getY() * POS_Y / getHeight());
                        if (eventApp.posX != posX || eventApp.posY != posY) {
                            if (showApps == allApps)
                                bringEventAppToHome();
                            eventApp.posX = posX;
                            eventApp.posY = posY;
                            savePrefs();
                            postInvalidate();
                        }
                    }
                }
            }
            return super.onTouchEvent(event);
        }

        @Override
        public void onClick(View view) {

        }
    }
}
