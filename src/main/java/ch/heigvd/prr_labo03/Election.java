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
public class Election {

   private static final int RECEIPT_TIMEOUT = 8000;

   // Le numéro du site
   private final int idProcess;

   // Les sites de l'environnement réparti par adresse IP et port
   private final List<Pair<InetAddress, Integer>> processes;


   public Election(int idProcess, List<Pair<InetAddress, Integer>> processes) {
      this.idProcess = idProcess;
      this.processes = new ArrayList<>(processes);

      // Implémentation pour recevoir les messages
      new Thread(() -> {

         try (DatagramSocket socket = new DatagramSocket(
                 processes.get(idProcess).getValue()
         )) {

            while (true) {
               
               // Adresse et port de l'émetteur
               InetAddress address = null;
               int port = 0;

               // Réception de la liste
               {
                  byte[] buf = new byte[256];
                  DatagramPacket packet = new DatagramPacket(buf, buf.length);
                  socket.receive(packet);
                  port = packet.getPort();
                  address = packet.getAddress();

                  System.out.println(new String(packet.getData()));
               }

               // Envoie de la quittance
               {
                  String message = "Salut";
                  byte[] buf = message.getBytes();
                  DatagramPacket packet = new DatagramPacket(
                          buf,
                          buf.length,
                          address,
                          port
                  );
                  socket.send(packet);
               }
            }
         } catch (SocketException ex) {
            Logger.getLogger(Election.class.getName())
                    .log(Level.SEVERE, null, ex);
         } catch (IOException ex) {
            Logger.getLogger(Election.class.getName())
                    .log(Level.SEVERE, null, ex);
         }
      }).start();
   }

   public void startElection() {

      // Implémentation pour lancer l'élection
      new Thread(() -> {
         System.out.println("Election en cours");

         // Récupère le voisin suivant
         int neighbour = idProcess + 1 % processes.size();

         try (DatagramSocket socket = new DatagramSocket()) {

            // Configure l'attente pour la quittance
            socket.setSoTimeout(RECEIPT_TIMEOUT);

            // Envoie de la liste complétée au voisin
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
               System.out.println("Quittance reçue");
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
      }).start();
   }

   private void aptitude() {

   }

   public void elected() {

   }
}
