package ch.heigvd.prr_labo03;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 * Cette classe se charge de tester périodiquement que l'élu est vivant et de
 * lancer une exception si ce n'est pas le cas.
 * 
 * Il faut appeler la méthode start pour commencer l'applicatif.
 * 
 * Cette classe commence par lancer une élection, puis attend le résultat. Une fois
 * le résultat connu, elle demande à l'élu s'il est en vie. 
 * 
 * Si ce n'est pas le cas, elle relance une élection. et ainsi de suite. 
 *
 * Elle effectue cette action toutes les 5 secondes.
 */
public class Applicatif implements Observer {
   
   // Election object to use
   private Election election;

   // Id of the current process
   private int idProcess;

   // Socket pour recevoir des demandes de vie
   private DatagramSocket receiveSocket;
   
   // Socket pour envoyer des demandes de vie
   private DatagramSocket sendSocket;
   
   // Thread pour recevoir des demandes de vie
   private Thread receiveThread;
   
   // Liste des process et de leurs adresses
   private List<Pair<InetAddress, Integer>> processes;

   public Applicatif(Election election, int idProcess, List<Pair<InetAddress, Integer>> processes) throws SocketException {

      this.election = election;
      election.addObserver(this);

      this.idProcess = idProcess;
      
      this.processes = processes;
      
      

      receiveSocket = new DatagramSocket(
              24000 + idProcess
      );
      
      sendSocket = new DatagramSocket();
      sendSocket.setSoTimeout(3000);

      receiveThread = new Thread(() -> {

         while (true) {
            byte[] buf = new byte[8];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
               // Receive messages
               receiveSocket.receive(packet);
               
               byte[] data = new byte[8];
               
               // Send echo
               receiveSocket.send(new DatagramPacket(
                       data,
                       data.length,
                       packet.getAddress(),
                       packet.getPort()
               ));

            } catch (IOException ex) {
               Logger.getLogger(Applicatif.class.getName()).log(Level.SEVERE, null, ex);
            }
         }

      });
      receiveThread.start();

   }

   public void start() {
      election.startElection();

      synchronized (this) {
         try {
            this.wait();
         } catch (InterruptedException ex) {
            Logger.getLogger(Applicatif.class.getName()).log(Level.SEVERE, null, ex);
         }
      }

      int elu = election.elected();
      
      // Envoyer périodiquement
      while (true) {
         try {
            Thread.sleep(5000);
         } catch (InterruptedException ex) {
            Logger.getLogger(Applicatif.class.getName()).log(Level.SEVERE, null, ex);
         }
         
         byte[] data = new byte[8];
               
         try {
            // Send echo
            sendSocket.send(new DatagramPacket(
                    data,
                    data.length,
                    processes.get(elu).getKey(),
                    24000 + elu
            ));
            
            byte[] buf = new byte[8];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            
            sendSocket.receive(packet);
            
         } catch (SocketTimeoutException ex) {
            
            // Le processus est mort
            election.startElection();
            
            synchronized(this){
               try {
                  this.wait();
               } catch (InterruptedException ex1) {
                  Logger.getLogger(Applicatif.class.getName()).log(Level.SEVERE, null, ex1);
               }
            }            
         } catch (IOException ex) {
            Logger.getLogger(Applicatif.class.getName()).log(Level.SEVERE, null, ex);
         }
         
      }

   }

   @Override
   public void update(Observable o, Object arg) {
      synchronized (this) {
         this.notify();
      }
   }
}
