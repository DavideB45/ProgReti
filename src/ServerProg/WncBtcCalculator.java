package ServerProg;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class WncBtcCalculator{

    private long lastIteration;
    private float wncValue;
    private boolean usable;

    String httpsURLQuota = "https://www.random.org/quota/?format=plain";
    String httpsURLDecimal = "https://www.random.org/decimal-fractions/?num=1&dec=10&col=2&format=plain&rnd=new";
    java.net.URL myUrlQuota;
    java.net.URL myUrlDecimal;

    public WncBtcCalculator() {
        this.wncValue = 1;
        this.lastIteration = 0;
        try {
            myUrlQuota = new URL(httpsURLQuota);
            myUrlDecimal = new URL(httpsURLDecimal);
            usable = true;
        } catch (MalformedURLException e) {
            usable = false;
        }
    }

    /**
     * @param wnc amount to change
     * @return the value of WNC in BTC or -1 if random.org unusable
     */
    public synchronized float WNCtoBTC(float wnc){
        if(!usable){
            return -1;
        }
        if (lastIteration < System.currentTimeMillis() - 1000){

            try {
                HttpsURLConnection connQuota = (HttpsURLConnection)myUrlQuota.openConnection();
                BufferedReader quotaIn = new BufferedReader(new InputStreamReader(connQuota.getInputStream()));
                int quota = Integer.parseInt(quotaIn.readLine());
                if (quota < 100){
                    usable = false;
                    return -1;
                } else {
                    HttpsURLConnection connRand = (HttpsURLConnection) myUrlDecimal.openConnection();
                    BufferedReader randIn = new BufferedReader(new InputStreamReader(connRand.getInputStream()));
                    float change = Float.parseFloat(randIn.readLine());
                    wncValue = change;
                    lastIteration = System.currentTimeMillis();
                    return wnc*change;
                }
            } catch (IOException e) {
                return -1;
            }

        } else {
            return wnc*wncValue;
        }
    }
}
