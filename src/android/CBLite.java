package com.couchbase.cblite.phonegap;

import android.content.Context;
import android.os.Handler;
import com.couchbase.lite.*;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.javascript.JavaScriptReplicationFilterCompiler;
import com.couchbase.lite.javascript.JavaScriptViewCompiler;
import com.couchbase.lite.listener.Credentials;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.router.URLStreamHandlerFactory;
import com.couchbase.lite.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class CBLite extends CordovaPlugin {

    private static final int DEFAULT_LISTEN_PORT = 5984;
    private boolean initFailed = false;
    private int listenPort;
    private Credentials allowedCredentials;
    private Database db;

    /**
     * Constructor.
     */
    public CBLite() {
        super();
        System.out.println("CBLite() constructor called");
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        System.out.println("initialize() called");

        super.initialize(cordova, webView);
        initCBLite();

    }

    private void initCBLite() {
        try {

            allowedCredentials = new Credentials();

            URLStreamHandlerFactory.registerSelfIgnoreError();

            View.setCompiler(new JavaScriptViewCompiler());
            Database.setFilterCompiler(new JavaScriptReplicationFilterCompiler());

            Manager server = startCBLite(this.cordova.getActivity());

            listenPort = startCBLListener(DEFAULT_LISTEN_PORT, server, allowedCredentials);

            System.out.println("initCBLite() completed successfully");


        } catch (final Exception e) {
            e.printStackTrace();
            initFailed = true;
        }

    }

    @Override
    public boolean execute(String action, JSONArray args,
                           CallbackContext callback) {
        if (action.equals("getURL")) {
            try {

                if (initFailed == true) {
                    callback.error("Failed to initialize couchbase lite.  See console logs");
                    return false;
                } else {
                    String callbackRespone = String.format(
                            "http://%s:%s@localhost:%d/",
                            allowedCredentials.getLogin(),
                            allowedCredentials.getPassword(),
                            listenPort
                    );

                    callback.success(callbackRespone);

                    return true;
                }

            } catch (final Exception e) {
                e.printStackTrace();
                callback.error(e.getMessage());
            }
        }
        return false;
    }

    final Handler handler = new Handler();
    // Create the Handler object (on the main thread by default)
    final Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            final Database dataBase = db;
            try {
                dataBase.compact();
                handler.postDelayed(runnableCode, 1000*60 * 60 * 12);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            Log.d("Handlers", "autoCompact Called on main thread");
        }
    };

    // Run the above code block on the main thread after 2 seconds
    private void autoCompact() {

        // Define the code block to be executed


        handler.post(runnableCode);

    }

    private URL createSyncURL(boolean isEncrypted) {
        URL syncURL = null;
        String host = "http://root:root@13.76.101.187";
        String port = "5984";
        String dbName = "nhs_fsdev";
        try {
            syncURL = new URL(host + ":" + port + "/" + dbName);
        } catch (MalformedURLException me) {
            me.printStackTrace();
        }
        return syncURL;
    }

    private void startReplications() throws CouchbaseLiteException {
        Replication pull = this.db.createPullReplication(this.createSyncURL(false));
        Replication push = this.db.createPushReplication(this.createSyncURL(false));
        pull.setContinuous(true);
        push.setContinuous(true);
        pull.start();
        push.start();
    }

    protected Manager startCBLite(Context context) {
        Manager manager;
        try {
            Manager.enableLogging(Log.TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_CHANGE_TRACKER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_BLOB_STORE, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_LISTENER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_MULTI_STREAM_WRITER, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_REMOTE_REQUEST, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_ROUTER, Log.VERBOSE);
            DatabaseOptions options = new DatabaseOptions();
            String key = "password123456";
            options.setCreate(true);
            options.setStorageType(Manager.FORESTDB_STORAGE);
            options.setCreate(true);
            options.setEncryptionKey(key);
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            try {
                db = manager.openDatabase("fhs", options);
                startReplications();
                //autoCompact();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return manager;
    }

    private int startCBLListener(int listenPort, Manager manager, Credentials allowedCredentials) {

        LiteListener listener = new LiteListener(manager, listenPort, allowedCredentials);
        int boundPort = listener.getListenPort();
        Thread thread = new Thread(listener);
        thread.start();
        return boundPort;

    }

    public void onResume(boolean multitasking) {
        System.out.println("CBLite.onResume() called");
    }

    public void onPause(boolean multitasking) {
        System.out.println("CBLite.onPause() called");
    }


}
