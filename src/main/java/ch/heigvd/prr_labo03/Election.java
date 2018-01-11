package ch.heigvd.prr_labo03;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 * Le gestionnaire d'élection
 */
public class Election extends Observable {
   
   public enum Protocol {
      ANNOUNCEMENT(0),
      RECEIPT(1);
      
      private final int code;
      
      private Protocol(int code) {
         this.code = code;
      }
      
      public int code() {
         return code;
      }
   };

   private static final int RECEIPT_TIMEOUT = 8000;

   // Le numéro du site
   private final int idProcess;

   // Les sites de l'environnement réparti par adresse IP et port
   private final List<Pair<InetAddress, Integer>> processes;
   

   public Election(int idProcess, List<Pair<InetAddress, Integer>> processes) {
      this.idProcess = idProcess;
      this.processes = new ArrayList<>(processes);

      /**
       * Ce thread reçoit les annonces et les traites. Pour le premier passage
       * de l'anneau, on s'ajoute à la liste et on s'annonce au voisin suivant.
       * 
       * Puis au deuxième passage, on détermine l'élu et on passe à la dernière
       * étape qui consiste à communiquer le résultat à tout le monde.
       */
      new Thread(() -> {

         try (DatagramSocket socket = new DatagramSocket(
                 processes.get(idProcess).getValue()
         )) {

            while (true) {

               // Adresse et port de l'émetteur
               InetAddress address;
               int port;

               // Réception de la liste

               byte[] buf = new byte[256];
               DatagramPacket packet = new DatagramPacket(buf, buf.length);
               socket.receive(packet);

               // Récupère l'adresse et port de l'émetteur
               port = packet.getPort();
               address = packet.getAddress();
               
               System.out.println("Annonce recu par le site " + idProcess);
               
               // Envoie de la quittance
               String message = "RECEIPT";
               byte[] data = message.getBytes();

               socket.send(new DatagramPacket(
                       data,
                       data.length,
                       address,
                       port
               ));

               ByteBuffer buffer = ByteBuffer.wrap(packet.getData());

               int protocol = buffer.getInt(0);

               // Si c'est une annonce
               if(protocol == Protocol.ANNOUNCEMENT.code()) {
                  List<Pair<Integer, Integer>> aptitudePerProcess = new ArrayList<>();

                  boolean inList = false;

                  // Parcours la liste
                  int nbProcesses = buffer.getInt(1 * 4);
                  for (int i = 0; i < nbProcesses; i++) {
                     int no = buffer.getInt((i + 2) * 4);
                     int apt = buffer.getInt((i + 2 + 1) * 4);
                     
                     System.out.println(no + " " + apt);

                     aptitudePerProcess.add(new Pair<>(no, apt));

                     // Vérifie que le site courant est dans la liste
                     if(no == idProcess) {
                        inList = true;
                     }
                  }

                  if(inList) {
                     // TODO : Si c'est le deuxième passage, déterminer l'identité de l'élu ... Plus grande aptitude et en cas d'égalité, plus petite IP
                     // Si c'est moi, arrêter d'envoyer la liste et envoyer le résultat (Donc il faut mettre en place un protocol ...)

                     // TODO : Si c'est le toisième passage, supprimer le site en panne (Celui qui est élu normalement)
                     // Trouver un moyen de savoir si c'est le troisième cycle. Boolean interne ?
                  }
                  else {
                     // Si le site courant n'est pas dans la liste, ajouter
                     aptitudePerProcess.add(new Pair<>(idProcess, aptitude()));

                     // Et announcement au prochain voisin
                     announcement(aptitudePerProcess);
                  }
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

   /**
    * Démarre une nouvelle élection
    */
   public void startElection() {
      // Implémentation pour lancer l'élection
      new Thread(() -> {
         System.out.println("Démarre l'élection");

         // S'annonce avec son numéro et son aptitude
         announcement(Arrays.asList(new Pair<>(idProcess, aptitude())));
      }).start();
   }

   
   /**
    * Effectue une annonce auprès d'un voisin. Si le voisin est inatteignable,
    * les voisins suivants sont sollicités.
    *
    * @param aptitudePerProcess Liste des sites participant à l'élection. Chaque
    * site est couplé de son aptitude. Pair<No, Aptitude>
    */
   private void announcement(List<Pair<Integer, Integer>> aptitudePerProcess) {
      
      System.out.println("Le site " + idProcess + " s'annonce");
      
      // Récupère le voisin suivant
      int neighbour = (idProcess + 1) % processes.size();

      try (DatagramSocket socket = new DatagramSocket()) {

         // Configure l'attente pour la quittance
         socket.setSoTimeout(RECEIPT_TIMEOUT);

         // Envoie de la liste complétée au voisin
         {
            ByteBuffer buffer = ByteBuffer.allocate(40);
            buffer.putInt(Protocol.ANNOUNCEMENT.code());
            buffer.putInt(aptitudePerProcess.size());

            aptitudePerProcess.forEach((s) -> {
               buffer.putInt(s.getKey());
               buffer.putInt(s.getValue());
            });

            byte[] data = buffer.array();
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
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
         // TODO : Essayer avec le prochain voisin
      } catch (SocketException ex) {
         Logger.getLogger(Election.class.getName())
                 .log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
         Logger.getLogger(Election.class.getName())
                 .log(Level.SEVERE, null, ex);
      }
   }

   
   /**
    * Retourne l'aptitude du site. Ce dernier est calculé à l'aide du dernier
    * octet de l'adresse IP + le numéro de port.
    *
    * @return Retourne l'aptitude du site.
    */
   private int aptitude() {
      
      Pair<InetAddress, Integer> address = processes.get(idProcess);
      byte[] addressBytes = address.getKey().getAddress();
      
      
      return (int) addressBytes[3] + (int) address.getValue();
   }

   
   /**
    * Retourne l'élu
    */
   public synchronized int elected() {
      return 0;
   }
}
