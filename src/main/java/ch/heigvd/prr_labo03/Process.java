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
   
   private static List<Pair<InetAddress, Integer>> readProcessesAddresses(String filename) 
   throws Exception {
      try (
              BufferedReader buffer = new BufferedReader(
                      new InputStreamReader(
                              Process.class.getResourceAsStream(filename)
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
         
         return processes;

         
      } catch (IOException e) {
         throw new Exception("Une erreur est survenue en lisant les fichiers.", e);
      }
   }

   public static void main(String[] args) throws InterruptedException {
      
      int id = -1;
      
      if (args.length != 1) {
         System.out.println("Le programme doit recevoir en paramètre son ID de site"
                 + " (0 à 3).");
         System.out.println("Lance le programme : java -jar <nom_application_jar> <id_du_site>");
         //System.exit(1);
         id = 0;
      }
      
      if (id < 0)
         id = Integer.parseInt(args[0]);
      
      if (id < 0 || id > 3) {
         System.out.println("Erreur: l'ID de site doit être dans l'intervalle doit être "
                 + "entre 0 et 3 compris.");
         System.exit(1);
      }

      String fileName = "/processes.txt";
      
      try {
         List<Pair<InetAddress, Integer>> processes = readProcessesAddresses(fileName);
         
         Election e1 = new Election(0, processes);
         Election e2 = new Election(1, processes);
         Election e3 = new Election(2, processes);
         Election e4 = new Election(3, processes);
         
         e1.startElection();

         Thread.sleep(1000);
         
      } catch (Exception ex) {
         
         Logger.getLogger(Process.class.getName())
                 .log(Level.SEVERE, null, ex);
      }
      
      System.exit(0);
   }
}
