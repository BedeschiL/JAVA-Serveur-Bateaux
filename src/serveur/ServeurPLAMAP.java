/*
 * Copyright (C) 2020 hector
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package serveur;

import ProtocoleBISAMAP.RequeteBISAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import ProtocolePLAMAP.RequetePLAMAP;
import ProtocoleTRAMAP.RequeteTRAMAP;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class ServeurPLAMAP  extends Thread {
    
    private final int NB_THREADS = 3;
    
    private int port;
    private Socket socketClient;
    private SourceTaches tachesAExecuter;
    private ConsoleServeur guiApplication;
    private ServerSocket SSocket = null;
    
    public ServeurPLAMAP(int port, Socket socketClient, SourceTaches tachesAExecuter, ConsoleServeur guiApplication) {
        this.port = port;
        this.socketClient = socketClient;
        this.tachesAExecuter = tachesAExecuter;
        this.guiApplication = guiApplication;
    }
    
    public void run() {
        try {
            SSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            System.err.println("Erreur de port d'écoute ! ? [" + e + "]"); System.exit(1);
        }
        
// Démarrage du pool de threads
        for (int i = 0; i < NB_THREADS; i++) {
            ThreadClient thr = new ThreadClient(tachesAExecuter, "Thread du pool n°" + String.valueOf(i));
            thr.start();
        }
        
// Mise en attente du serveur
        Socket CSocket = null;
        
        while(!isInterrupted()) {
            try {
                System.out.println("************ Serveur en attente");
                CSocket = SSocket.accept();
                guiApplication.TraceEvenements(CSocket.getRemoteSocketAddress().toString() + "#accept#thread serveur");
                
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(CSocket.getInputStream()));
                String chu = inFromServer.readLine();
                System.out.println(chu);
                RequetePLAMAP req = new RequetePLAMAP(chu.split("\r\n")[0]);
                req.setIn(inFromServer);
                
                Runnable travail = req.createRunnable(CSocket, guiApplication);
                if (travail != null) {
                    tachesAExecuter.recordTache(travail);
                    System.out.println("Travail mis dans la file");
                }
                else
                    System.out.println("Pas de mise en file");
            }
            catch(IOException e) {
                System.err.println("Erreur d'accept ! ? [" + e.getMessage() + "]"); System.exit(1);
            }
        }
    }
}
