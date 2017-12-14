package mccode.qdup;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import org.codehaus.jackson.map.ObjectMapper;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;

import mccode.qdup.Utils.Client.ClientConnector;
import mccode.qdup.Utils.Listeners.ConnectListener;
import mccode.qdup.Utils.Server.ServerConnector;

public class MainActivity extends Activity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback
{
    private static final String CLIENT_ID = "dfa2a91d372d42db9cb74bed20fb5630";
    private static final String REDIRECT_URI = "mccode-qdup://callback";
    private static final String HOST = "spotidjrouter.access.ly";
    private static int CPORT = 16455;
    private static int SPORT = 16456;
    public static String key = "";
    public static ObjectMapper mapper = new ObjectMapper();
    public static Socket routerSocket;
    public static String responseToken = "";
    public static boolean stopped = false;
    private ServerConnector s;
    private ClientConnector c;
    private AuthenticationResponse aResponse = null;
    private boolean premium = true;

    private static boolean failedConnect = false;

    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 1337;
    public static Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(!failedConnect){
            setContentView(R.layout.activity_main);
            final CompoundButton serverOrClient = (CompoundButton) findViewById(R.id.serverOrClient);
            final Button confirmType = (Button) findViewById(R.id.confirmType);
            final EditText keySearch = (EditText) findViewById(R.id.key_search);
            final Button retry = (Button) findViewById(R.id.retry);
            final TextView error = (TextView) findViewById(R.id.errorConnect);
            error.setVisibility(View.GONE);
            retry.setVisibility(View.GONE);
            serverOrClient.setVisibility(View.GONE);
            confirmType.setVisibility(View.GONE);
            keySearch.setVisibility(View.GONE);
        }

        super.onCreate(savedInstanceState);

        logIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        setContentView(R.layout.activity_main);
        final Button retry = (Button) findViewById(R.id.retry);
        final CompoundButton serverOrClient = (CompoundButton) findViewById(R.id.serverOrClient);
        final Button confirmType = (Button) findViewById(R.id.confirmType);
        final EditText keySearch = (EditText) findViewById(R.id.key_search);
        final TextView error = (TextView) findViewById(R.id.errorConnect);
        final int colorPrimary = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
        final int colorFaded = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryClicked);
        final ViewGroup mainView = (ViewGroup) findViewById(R.id.mainView);

        if (requestCode == REQUEST_CODE) {
            aResponse = AuthenticationClient.getResponse(resultCode, intent);
            if (aResponse.getType() == AuthenticationResponse.Type.TOKEN) {
                retry.setVisibility(View.GONE);
                error.setVisibility(View.GONE);
                responseToken = aResponse.getAccessToken();
                Config playerConfig = new Config(this, aResponse.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
                final ConnectListener listener = new ConnectListener() {
                    @Override
                    public void onConnectSucceeded(ArrayList<String> result) {
                        //is checked means it is server, not is client
                        if(!result.get(0).equals("NA")) {
                            key = result.get(0);
                            if(serverOrClient.isChecked()){
                                Intent intent = new Intent(MainActivity.this, ServerActivity.class);
                                startActivity(intent);
                            }else{
                                Intent intent = new Intent(MainActivity.this, RequesterActivity.class);
                                startActivity(intent);
                            }
                        }
                    }
                };

                confirmType.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorPrimary, colorFaded);
                        colorAnimation.setDuration(250);
                        final ValueAnimator colorAnimationRev = ValueAnimator.ofObject(new ArgbEvaluator(), colorFaded, colorPrimary);
                        colorAnimationRev.setDuration(250);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                confirmType.setBackgroundColor((int) animator.getAnimatedValue());
                            }
                        });
                        colorAnimationRev.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                confirmType.setBackgroundColor((int) animator.getAnimatedValue());
                            }
                        });
                        colorAnimation.start();
                        colorAnimationRev.start();
                        if(serverOrClient.isChecked()){
                            s = new ServerConnector();
                            s.setOnConnectListener(listener);
                            //s.execute();
                            s.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }else{
                            key = keySearch.getText().toString();
                            c = new ClientConnector(key);
                            c.setOnConnectListener(listener);
                            //c.execute();
                            c.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                });
                if(premium) {
                    serverOrClient.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorPrimary, colorFaded);
                            colorAnimation.setDuration(250);
                            final ValueAnimator colorAnimationRev = ValueAnimator.ofObject(new ArgbEvaluator(), colorFaded, colorPrimary);
                            colorAnimationRev.setDuration(250);
                            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    serverOrClient.setBackgroundColor((int) animator.getAnimatedValue());
                                }
                            });
                            colorAnimationRev.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    serverOrClient.setBackgroundColor((int) animator.getAnimatedValue());
                                }
                            });
                            colorAnimation.start();
                            colorAnimationRev.start();
                            TransitionManager.beginDelayedTransition(mainView);
                            keySearch.setVisibility(serverOrClient.isChecked() ? View.GONE : View.VISIBLE);
                        }
                    });
                }else{
                    serverOrClient.setEnabled(false);
                }
                routerSocket = new Socket();
            }else{
                failedConnect = true;
                retry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorPrimary, colorFaded);
                        colorAnimation.setDuration(250);
                        final ValueAnimator colorAnimationRev = ValueAnimator.ofObject(new ArgbEvaluator(), colorFaded, colorPrimary);
                        colorAnimationRev.setDuration(250);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                retry.setBackgroundColor((int) animator.getAnimatedValue());
                            }
                        });
                        colorAnimationRev.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                retry.setBackgroundColor((int) animator.getAnimatedValue());
                            }
                        });
                        colorAnimation.start();
                        colorAnimationRev.start();
                        onCreate(null);
                    }
                });
                retry.setVisibility(View.VISIBLE);
                error.setVisibility(View.VISIBLE);
                serverOrClient.setVisibility(View.GONE);
                confirmType.setVisibility(View.GONE);
                keySearch.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
//        setContentView(R.layout.activity_main);
        //mPlayer.playUri(null, "spotify:track:7oK9VyNzrYvRFo7nQEYkWN", 0, 0);
//        final int colorBackground = ContextCompat.getColor(getApplicationContext(), R.color.background);
//        final int colorPrimary = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
//        final int colorFaded = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryClicked);
//        final CompoundButton serverOrClient = (CompoundButton) findViewById(R.id.serverOrClient);
//        final Button confirmType = (Button) findViewById(R.id.confirmType);
//        final EditText keySearch = (EditText) findViewById(R.id.key_search);
//        final ViewGroup mainView = (ViewGroup) findViewById(R.id.mainView);
//        final Button retry = (Button) findViewById(R.id.retry);
//        final TextView error = (TextView) findViewById(R.id.errorConnect);
//        retry.setVisibility(View.GONE);
//        error.setVisibility(View.GONE);
//        final ConnectListener listener = new ConnectListener() {
//            @Override
//            public void onConnectSucceeded(ArrayList<String> result) {
//                //is checked means it is server, not is client
//                if(!result.get(0).equals("NA")) {
//                    key = result.get(0);
//                    if(serverOrClient.isChecked()){
//                        Intent intent = new Intent(MainActivity.this, ServerActivity.class);
//                        startActivity(intent);
//                    }else{
//                        Intent intent = new Intent(MainActivity.this, RequesterActivity.class);
//                        startActivity(intent);
//                    }
//                }
//            }
//        };
//
//        confirmType.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorPrimary, colorFaded);
//                colorAnimation.setDuration(250);
//                final ValueAnimator colorAnimationRev = ValueAnimator.ofObject(new ArgbEvaluator(), colorFaded, colorPrimary);
//                colorAnimationRev.setDuration(250);
//                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        confirmType.setBackgroundColor((int) animator.getAnimatedValue());
//                    }
//                });
//                colorAnimationRev.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        confirmType.setBackgroundColor((int) animator.getAnimatedValue());
//                    }
//                });
//                colorAnimation.start();
//                colorAnimationRev.start();
//                if(serverOrClient.isChecked()){
//                    s = new ServerConnector();
//                    s.setOnConnectListener(listener);
//                    //s.execute();
//                    s.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                }else{
//                    key = keySearch.getText().toString();
//                    c = new ClientConnector(key);
//                    c.setOnConnectListener(listener);
//                    //c.execute();
//                    c.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                }
//            }
//        });
//
//        serverOrClient.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View v){
//                final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorPrimary, colorFaded);
//                colorAnimation.setDuration(250);
//                final ValueAnimator colorAnimationRev = ValueAnimator.ofObject(new ArgbEvaluator(), colorFaded, colorPrimary);
//                colorAnimationRev.setDuration(250);
//                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        serverOrClient.setBackgroundColor((int) animator.getAnimatedValue());
//                    }
//                });
//                colorAnimationRev.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(ValueAnimator animator) {
//                        serverOrClient.setBackgroundColor((int) animator.getAnimatedValue());
//                    }
//                });
//                colorAnimation.start();
//                colorAnimationRev.start();
//                TransitionManager.beginDelayedTransition(mainView);
//                keySearch.setVisibility(serverOrClient.isChecked() ? View.GONE : View.VISIBLE);
//            }
//        });
    }

    public void logIn(){
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        if(premium) {
            builder.setScopes(new String[]{"user-read-private", "streaming"});
        }else{

        }
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }
    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error error) {
        Log.d("MainActivity", "Login failed");
        if(error.toString().equals("kSpErrorNeedsPremium")){
            premium = false;
            logIn();
        }
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(aResponse!= null && aResponse.getType() == AuthenticationResponse.Type.TOKEN){
            routerSocket = new Socket();
        }
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread t: threadSet
             ) {
            System.out.println(t.getId() + ": " + t.getName() + "-" );
            for (StackTraceElement s :t.getStackTrace()
                 ) {
                System.out.println(s.toString());
            }
        }
    }

    public static String getHost(){
        return HOST;
    }

    public static int getCPort(){
        return CPORT;
    }

    public static int getSPort() { return SPORT;}
}