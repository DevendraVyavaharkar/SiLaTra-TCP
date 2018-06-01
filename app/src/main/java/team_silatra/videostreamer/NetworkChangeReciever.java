package team_silatra.videostreamer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Button;

public class NetworkChangeReciever extends BroadcastReceiver {
    Button startSendFeed;
    Activity sendFeedActivity;
    public NetworkChangeReciever(Activity _activity){
        this.sendFeedActivity = _activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();

        boolean isConnected = wifi != null&&wifi.isConnected() ;
        startSendFeed = (Button)this.sendFeedActivity.findViewById(R.id.sendFeedStartButton);
        if (!isConnected ) {
            startSendFeed.setEnabled(false);
            // Do something
        }
        else
        {
            startSendFeed.setEnabled(true);
        }
    }
}
