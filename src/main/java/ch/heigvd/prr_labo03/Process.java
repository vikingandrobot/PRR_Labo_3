package ch.heigvd.prr_labo03;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;

/**
 *
 */
public class Process {

   public static void main(String[] args) throws InterruptedException {

      String fileName = "/processes.txt";

      // Lecture du fichier
      try (
              BufferedReader buffer = new BufferedReader(
                      new InputStreamReader(
                              Process.class.getResourceAsStream(fileName)
                      )
              )) {

         // Liste des sites par adresse IP et port
         List<Pair<InetAddress, Integer>> processes = new ArrayList<>();

         // Lecture du fichier ligne par ligne
         buffer.lines().forEach((t) -> {
            try {
               String[] address = t.split(" ");
               processes.add(
                       new Pair<>(
                               InetAddress.getByName(address[0]),
                               Integer.parseInt(address[1])
                       )
               );
            } catch (UnknownHostException e) {
               Logger.getLogger(Process.class.getName())
                       .log(Level.SEVERE, null, e);
            }
         });
         
         processes.forEach(System.out::println);
         
         
         Election e1 = new Election(0, processes);
         Election e2 = new Election(1, processes);
         Election e3 = new Election(2, processes);
         Election e4 = new Election(3, processes);
         
         new Thread(e1).start();
         new Thread(e2).start();
         new Thread(e3).start();
         new Thread(e4).start();

         e1.startElection();

         Thread.sleep(1000);




         
      } catch (IOException e) {
         Logger.getLogger(Process.class.getName())
                 .log(Level.SEVERE, null, e);
      }
      
      System.exit(0);
   }
}
