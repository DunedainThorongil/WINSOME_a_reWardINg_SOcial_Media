import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Classe che implementa il task del thread che si occupa di mandare una richiesta al url random.org per prendere il valore
 * della valuta del cambio per la conversione in wincoin
 */
public class RichiestaRandomORG  implements Callable<Double> {
    public static final String requestURL = "https://www.random.org/decimal-fractions/?num=1&dec=6&col=1&format=plain&rnd=new";

    @Override
    public Double call() {
        double value = 0.00;
        try{
            URL url = new URL(requestURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String reply = in.readLine();
            con.disconnect();
            value = Double.parseDouble(reply);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

}
