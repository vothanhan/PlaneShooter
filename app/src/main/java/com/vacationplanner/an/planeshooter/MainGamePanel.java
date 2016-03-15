package com.vacationplanner.an.planeshooter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by An on 8/21/2015.
 */
public class MainGamePanel extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG=MainThread.class.getSimpleName();
    long start = System.currentTimeMillis();
    private MainThread thread;
    private boolean begin=true;
    private Ship mainship;
    private Context c;
    private MediaPlayer player;
    private int shootspace;
    private int FRAME=0;
    private int DEADFRAME=0;
    private int framepershoot=30;
    private Target target;
    Enemy enemy;
    ArrayList<Projectile> projectiles;
    ArrayList<SpriteAnimated> explosion;
    private Bitmap background;
    private Bitmap youlose;
    private Bitmap youwin;
    ArrayList<Bullet> bullets;
    Display display;
    DisplayMetrics metrics;
    private int n=0;
    private int n1=0;
    private MotionEvent event1;
    public MainGamePanel (Context context,int diff,int shiptype){
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);
        thread=new MainThread(getHolder(),this);
        c=context;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display=wm.getDefaultDisplay();
        metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        mainship=new Ship(BitmapFactory.decodeResource(getResources(), R.drawable.ship),display.getWidth()/2,display.getHeight(),10,100);
        mainship.setVisible(true);
        shootspace=mainship.getBitmap().getWidth()/4;
        player = MediaPlayer.create(context,R.raw.b);
        enemy=new Enemy(BitmapFactory.decodeResource(getResources(), R.drawable.enemy),display.getWidth()/2,0,2000,true,8);
        projectiles=new ArrayList<Projectile>();
        bullets = new ArrayList<Bullet>();
        background= BitmapFactory.decodeResource(getResources(), R.drawable.background);
        youwin=BitmapFactory.decodeResource(getResources(), R.drawable.youwin);
        youlose=BitmapFactory.decodeResource(getResources(), R.drawable.youlose);
        youwin=Bitmap.createScaledBitmap(youwin, metrics.widthPixels, metrics.heightPixels, true);
        youlose=Bitmap.createScaledBitmap(youlose, metrics.widthPixels, metrics.heightPixels, true);
        Bitmap bitmap = Bitmap.createScaledBitmap(background, metrics.widthPixels, metrics.heightPixels, true);
        background=bitmap;
        for(int i=0;i<100;++i)
        {
            Projectile proj=new Projectile(BitmapFactory.decodeResource(getResources(), R.drawable.bullet),mainship.getX()+shootspace,mainship.getY(),false,10,15);
            projectiles.add(proj);
            Bullet bullet=new Bullet(BitmapFactory.decodeResource(getResources(),R.drawable.bullet2),0,0,false,10,10,display.getWidth(),display.getHeight());
            bullets.add(bullet);
        }
        target=new Target(BitmapFactory.decodeResource(getResources(),R.drawable.mark),display.getWidth()/2,display.getHeight()-display.getHeight()/4);
        explosion = new ArrayList<SpriteAnimated>();
        for (int i=0;i<3;++i) {
            SpriteAnimated sprite = new SpriteAnimated(BitmapFactory.decodeResource(getResources(), R.drawable.explosion), 20, 20, 30, 47, 5, 12,false);
            explosion.add(sprite);
        }
        if(diff==0)
        {
            enemy.setHealth(200);
        }
        else if(diff==1)
        {
            enemy.setHealth(300);
            enemy.setSpeed(12);
            framepershoot=20;
            enemy.setBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.mediumship));
        }
        else if(diff==2)
        {
            enemy.setHealth(500);
            framepershoot=10;
            enemy.setSpeed(10);
            enemy.setBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.hardship));
        }
        if(shiptype==1)
        {
            mainship.setSpeed(13);
            for(int i=0;i<100;++i)
            {
                projectiles.get(i).setDamage(7);
            }
            mainship.setBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.fastship));
        }
        if(shiptype==2)
        {
            mainship.setSpeed(18);
            mainship.setHealth(10);
            mainship.setBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.speedship));
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }
    Handler handler = new Handler ();
    boolean pushingDown=false;
    Runnable repeater = new Runnable(){
        @Override
        public void run()
        {
            if(pushingDown){target.update((int)event1.getX(),(int)event1.getY());
                handler.postDelayed(this,10);
            }
        }
    };
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry=true;
        while(retry)
        {
            try {
                thread.join();
                retry=false;
            }catch(InterruptedException e)
            {
                //try shutting down the thread again
            }
        }
        thread.setRunning(false);
        thread.currentThread().interrupt();
        thread=null;
    }
    public void shot(Enemy enemy, Projectile proj, SpriteAnimated expl)
    {
        if(Math.abs(enemy.getX()-proj.getX())<enemy.getBitmap().getWidth()/2 && proj.getY()-enemy.getY()<enemy.getBitmap().getHeight()/2)
        {
            enemy.setHealth(enemy.getHealth()-proj.getDamage());
            if(enemy.getHealth()<=0)
            {
                player.start();
                expl.setX(enemy.getX());
                expl.setY(enemy.getY());
                expl.setVisible(true);
                enemy.setVisible(false);
            }
            proj.setVisible(false);
        }
    }
    public void getshot(Ship ship, Bullet bullet, SpriteAnimated expl)
    {
        if(Math.abs(ship.getX()-bullet.getX())<ship.getBitmap().getWidth()/2 && Math.abs(bullet.getY()-ship.getY())<ship.getBitmap().getHeight()/2)
        {
            ship.setHealth(ship.getHealth()-bullet.getDamage());
            if(ship.getHealth()<=0)
            {
                player.start();
                expl.setX(ship.getX());
                expl.setY(ship.getY());
                expl.setVisible(true);
                ship.setVisible(false);
            }
            bullet.setVisible(false);
        }
    }
    public void render(Canvas canvas)
    {
        canvas.drawBitmap(background,0,0,null);
        if(mainship.getVisible())
        {
            mainship.draw(canvas);
        }
        target.draw(canvas);
        for(int i=0;i<100;++i)
        {
            Projectile proj=projectiles.get(i);
            Bullet bullet=bullets.get(i);
            if(proj.getVisible()==true)
            {
                proj.draw(canvas);
            }
            if(bullet.getVisible()==true)
            {
                bullet.draw(canvas);
            }
        }
            if(enemy.getVisible()==true)
            {
                enemy.draw(canvas);
            }
        for(int i=0;i<3;++i){
            SpriteAnimated sprite = explosion.get(i);
            if(sprite.getVisible()==true){
                sprite.draw(canvas);
            }

        }
    }
    public void update(Canvas canvas)
    {

        if (mainship.getHealth()==0 || enemy.getHealth()==0)
        {
            ++DEADFRAME;
            if(DEADFRAME>=120)
            {
                thread.interrupt();
                if(mainship.getHealth()==0)
                {
                    canvas.drawBitmap(youlose,0,0,null);
                }
                else if (enemy.getHealth()<=0)
                {
                    canvas.drawBitmap(youwin,0,0,null);
                }
                Runnable runnable= new Runnable() {
                    @Override
                    public void run() {

                        ((Activity) c).finish();
                    }
                };
                postDelayed(runnable,3000);
            }
        }
        long curr = System.currentTimeMillis();
        if(curr-start>1000 && curr-start<3000)
        {
            enemy.update(display.getWidth()/2,display.getHeight()/6);
            mainship.update(display.getWidth()/2,display.getHeight()-display.getHeight()/4);
        }
        if(curr-start>=3000)
        {
            if(FRAME%20==0 && mainship.getHealth()>0)
            {
                projectiles.get(n).setX(mainship.getX());
                projectiles.get(n).setY(mainship.getY() - mainship.getBitmap().getHeight()/2);
                projectiles.get(n).setVisible(true);
                ++n;
                if(n==100)
                {
                    n=0;
                }
            }
            if(FRAME%framepershoot==0 && mainship.getHealth()>0 && enemy.getHealth()>0)
            {
                bullets.get(n1).setX(enemy.getX());
                bullets.get(n1).setY(enemy.getY() + enemy.getBitmap().getHeight() / 2);
                bullets.get(n1).setVisible(true);
                bullets.get(n1).setTarget(mainship.getX(),mainship.getY());
                ++n1;
                if(n1==100)
                {
                    n1=0;
                }
            }
            mainship.update(target.getX(),target.getY());
            enemy.evade(mainship.getX(),display.getWidth());
        }
        else
        {
            mainship.update(display.getWidth() / 2, display.getHeight() - display.getHeight() / 4);
        }
        for (int i=0;i<3;++i){
                if (explosion.get(i).getVisible()== true) {
                    explosion.get(i).update(System.currentTimeMillis());
            }
        }

        for(int i=0;i<100;++i)
        {
            if (projectiles.get(i).getVisible()==true)
            {
                projectiles.get(i).update();
            }
            if(bullets.get(i).getVisible()==true)
            {
                bullets.get(i).update();
            }
            if(enemy.getVisible() && projectiles.get(i).getVisible())
                shot(enemy,projectiles.get(i),explosion.get(1));
            if(mainship.getHealth()>0 && bullets.get(i).getVisible())
            {
                getshot(mainship, bullets.get(i), explosion.get(2));
            }
        }
        if(FRAME==60)
        {
            FRAME=0;
        }
        ++FRAME;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event)
    {

        if(event.getAction()==MotionEvent.ACTION_UP)
        {
            pushingDown=false;
        }
        if(event.getAction()==MotionEvent.ACTION_DOWN)
        {
            target.setX((int) event.getX());
            target.setY((int) event.getY());
        }
        if(event.getAction()==MotionEvent.ACTION_MOVE  && (Math.abs(mainship.getX()-event.getX())>=mainship.getSpeed() || Math.abs(mainship.getY()-event.getY())>=mainship.getSpeed()) && event.getX()>50 && event.getX()>0 && event.getY()>0 && event.getX()<display.getWidth() && event.getY()<display.getHeight())
        {
            target.setX((int) event.getX());
            target.setY((int) event.getY());
        }
        return true;
    }
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale++;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }
}
