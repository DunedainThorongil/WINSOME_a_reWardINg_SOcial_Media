import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * classe che modella il task che si occuperà di attendere dal gruppo multicast le notifiche dell'avvenuto calcolo del guadagno
 */
public class TaskClientMulticast implements Runnable {
    int portaMulticast;
    String gruppoMulticast;

    public TaskClientMulticast(int portaMulticast, String gruppoMulticast){
        this.portaMulticast = portaMulticast;
        this.gruppoMulticast = gruppoMulticast;

    }

    @Override
    public void run() {

        try(MulticastSocket multicastSocket = new MulticastSocket(portaMulticast) ){
            InetAddress inetAddress = InetAddress.getByName(gruppoMulticast);

            multicastSocket.joinGroup(inetAddress);
            multicastSocket.setSoTimeout(400000); //circa 7 minuti

            while (true){
                byte[] bytes = new byte[1024];
                DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
                multicastSocket.receive(datagramPacket);
                String s = new String(datagramPacket.getData(), StandardCharsets.UTF_8);

                System.out.print(s);
            }

        } catch (SocketTimeoutException e ){
            System.out.print("[ Notifica: Nessuna richiesta arrivata nel tempo di timeout! ]\r\n> ");


        } catch (IOException | SecurityException e){
            e.printStackTrace();
        }

    }


}
