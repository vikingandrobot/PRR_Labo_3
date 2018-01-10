package ch.heigvd.prr_labo03;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 * Le gestionnaire d'élection
 */
public class Election implements Runnable {

   private static final int REACHABLE_TIMEOUT = 8000;

   // Le numéro du site
   private final int idProcess;

   // Les sites de l'environnement réparti par adresse IP et port
   private final List<Pair<InetAddress, Integer>> processes;

   private final Thread receive;

   public Election(int idProcess, List<Pair<InetAddress, Integer>> processes) {
      this.idProcess = idProcess;
      this.processes = new ArrayList<>(processes);

      // TODO : Commentaire
      this.receive = new Thread(new Runnable() {

         private byte[] buf = new byte[256];

         @Override
         public void run() {

            try (DatagramSocket socket = new DatagramSocket(
                    processes.get(idProcess).getValue()
            )) {

               while (true) {
                  DatagramPacket packet = new DatagramPacket(buf, buf.length);
                  socket.receive(packet);

                  System.out.println(new String(packet.getData()));
               }
            } catch (SocketException ex) {
               Logger.getLogger(Election.class.getName())
                       .log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
               Logger.getLogger(Election.class.getName())
                       .log(Level.SEVERE, null, ex);
            }
         }
      });
   }

   public synchronized void startElection() {
      this.notify();
   }

   private synchronized void stopElection() {
      try {
         this.wait();
      } catch (InterruptedException ex) {
         Logger.getLogger(Election.class.getName())
                 .log(Level.SEVERE, null, ex);
      }
   }

   private void aptitude() {

   }

   public void elected() {

   }

   @Override
   public void run() {

      this.receive.start();

      while (true) {

         // Attente d'une nouvelle élection
         stopElection();

         System.out.println("Election en cours");

         // Récupère le voisin suivant
         int neighbour = idProcess + 1 % processes.size();

         try (DatagramSocket socket = new DatagramSocket()) {

            socket.setSoTimeout(10000);

            // Envoie de la liste complété au voisin
            {
               String message = "Salut";
               byte[] buf = message.getBytes();
               DatagramPacket packet = new DatagramPacket(
                       buf,
                       buf.length,
                       processes.get(neighbour).getKey(),
                       processes.get(neighbour).getValue()
               );
               socket.send(packet);
            }

            // Attente de la quittance
            {
               byte[] buf = new byte[256];
               DatagramPacket packet = new DatagramPacket(buf, buf.length);
               socket.receive(packet);
            }

         } catch (SocketTimeoutException ex) {
            // Erreur

         } catch (SocketException ex) {
            Logger.getLogger(Election.class.getName())
                    .log(Level.SEVERE, null, ex);
         } catch (IOException ex) {
            Logger.getLogger(Election.class.getName())
                    .log(Level.SEVERE, null, ex);
         }

      }
   }
}
