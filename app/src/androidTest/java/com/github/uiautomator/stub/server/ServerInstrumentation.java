package com.github.uiautomator.stub.server;

import android.content.Context;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import com.github.uiautomator.stub.AutomatorHttpServer;
import java.io.IOException;
import com.github.uiautomator.stub.AutomatorService;
import com.github.uiautomator.stub.AutomatorServiceImpl;
import com.github.uiautomator.stub.Log;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by BHB on 16-11-1.
 */

public class ServerInstrumentation {
    private static String TAG = "[AutomatorHttpServer]";
    private static ServerInstrumentation instance;
    private static Context context;
    private PowerManager.WakeLock wakeLock;
    private ServerThread serverThread;
    private int listenPort;
    private  boolean isStopServer;

    private ServerInstrumentation(int port){
        this.listenPort = port;
        if (!isValidPort(listenPort)) {
            throw new RuntimeException((TAG + " Invalid server listen port: " + listenPort));
        }

    }

    public static  synchronized ServerInstrumentation getInstance(Context ctx, int port){
        if (instance == null){
            context = ctx;
            instance = new ServerInstrumentation(port);
        }
        return instance;
    }

    private static boolean isValidPort(int port) {
        return port >= 9008 && port <= 65535;
    }

    public boolean isStopServer(){
        return isStopServer;
    }

    public void startServer() throws Exception {
        if (serverThread != null && serverThread.isAlive()) {
            Log.d("AutomatorHttpServer monitor thread is alive");
            Log.d("name: "+ serverThread.getName());
            Log.d("id: "+ serverThread.getId());
            return;
        }

        if(serverThread == null && isStopServer){
            throw new Exception(TAG + "AutomatorHttpServer monitor thread already stop running");
        }

        if (serverThread != null) {
            Log.d("Stopping AutomatorHttpServer monitor thread");
            stopServer();
        }

        serverThread = new ServerThread(this, this.listenPort);
        serverThread.start();
        //client to wait for server to up
        Log.d("New AutomatorHttpServer monitor thread is starting up");
    }


    public void stopServer(){
        try {
            if (wakeLock != null) {
                try {
                    wakeLock.release();
                }catch(Exception e){/* ignore */}
                wakeLock = null;
            }
            stopServerThread();

        } finally {
            instance = null;
        }
    }

    private void stopServerThread()  {
        if (serverThread == null) {
            return;
        }
        if (!serverThread.isAlive()) {
            serverThread = null;
            return;
        }

        Log.d("Stopping AutomatorHttpServer monitor thread");
        serverThread.stopLooping();
        serverThread.interrupt();
        try {
            serverThread.join();
        } catch (InterruptedException ignored) {
        }
        serverThread = null;
        isStopServer = true;
    }


    public ServerThread getWorkingThread(){
        return serverThread;
    }

    /***
     * Inner class
     * Init real AutomatorHttpServer
     * setup route for server
     */
    private class ServerThread extends Thread {
        private final AutomatorHttpServer targetServer;
        private ServerInstrumentation instrumentation;
        private Looper looper;

        public ServerThread(ServerInstrumentation instrumentation, int listenPort){
            this.instrumentation = instrumentation;
            targetServer = new AutomatorHttpServer(listenPort);
        }

        @Override
        public void run() {
            Looper.prepare();
            looper = Looper.myLooper();
            startServer();
            Looper.loop();
        }
        @Override
        public void interrupt() {
            targetServer.stop();
            super.interrupt();
        }

        public AutomatorHttpServer getServer() {
            return targetServer;
        }

        private void startServer() {
            // Get a wake lock to stop the cpu going to sleep
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AutomatorHttpServer");
            try {
                wakeLock.acquire();
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wakeUp();
            } catch (SecurityException e) {
                Log.d(e.toString());
            } catch (RemoteException e) {
                Log.d(e.toString());
            }

            try {
                //add route and start server
                Log.d("AutomatorHttpServer starting");
                targetServer.route("/jsonrpc/0", new JsonRpcServer(new ObjectMapper(), new AutomatorServiceImpl(), AutomatorService.class));
                targetServer.start();
                Log.d("AutomatorHttpServer started on port " + listenPort);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("Unable to start AutomatorHttpServer on port " + listenPort);
            }

        }
        public void stopLooping() {
            if (looper == null) {
                return;
            }
            looper.quit();
        }
        ////////// inner class define end
    }
}
