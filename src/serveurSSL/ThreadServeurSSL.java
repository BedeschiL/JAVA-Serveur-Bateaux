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
package serveurSSL;

import serveur.*;
import ProtocoleBISAMAP.RequeteBISAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import ProtocoleIOBREP.RequeteIOBREP;
import ProtocoleSAMOP.RequeteSAMOP;
import ProtocoleTRAMAP.RequeteTRAMAP;
import RequeteSSL.RequeteSSL;
import protocole.ConsoleServeur;
import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class ThreadServeurSSL extends Thread {
    
    private final int NB_THREADS = 3;
    
    private int port;
    private SourceTaches tachesAExecuter;
    private ConsoleServeur guiApplication;
    SSLServerSocket SslSSocket = null; 
    SSLSocket SslSocket = null; 

    public ThreadServeurSSL(int port, SourceTaches tachesAExecuter, ConsoleServeur guiApplication) {
        this.port = port;
        this.tachesAExecuter = tachesAExecuter;
        this.guiApplication = guiApplication;
    }
    
    public void run() {
        try {
            KeyStore ServerKs = KeyStore.getInstance("JKS"); 
           String FICHIER_KEYSTORE = "c:\\test\\test_keystore2.jks"; 
           char[] PASSWD_KEYSTORE = "123456".toCharArray(); 
           FileInputStream ServerFK = new FileInputStream (FICHIER_KEYSTORE); 
           ServerKs.load(ServerFK, PASSWD_KEYSTORE); 
           // 2. Contexte
           SSLContext SslC = SSLContext.getInstance("SSLv3"); 
           KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509"); 
           char[] PASSWD_KEY = "123456".toCharArray(); 
           kmf.init(ServerKs, PASSWD_KEY); 
           TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509"); 
           tmf.init(ServerKs); 
           SslC.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null); 
           // 3. Factory
           SSLServerSocketFactory SslSFac= SslC.getServerSocketFactory(); 
           // 4. Socket
           SslSSocket = (SSLServerSocket) SslSFac.createServerSocket(port); 
        }
        catch (IOException e) {
            System.err.println("Erreur de port d'écoute ! ? [" + e + "]"); System.exit(1);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyManagementException ex) {
            Logger.getLogger(ThreadServeurSSL.class.getName()).log(Level.SEVERE, null, ex);
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
                SslSocket = (SSLSocket)SslSSocket.accept(); 
                guiApplication.TraceEvenements(SslSocket.getRemoteSocketAddress().toString() + "#accept#thread serveur");
                  System.out.println("************ Serveur en accept");
            }
            catch(IOException e) {
                System.err.println("Erreur d'accept ! ? [" + e.getMessage() + "]"); System.exit(1);
            }

            ObjectInputStream ois = null;
            RequeteSSL req = null;
            
            try {
                ois = new ObjectInputStream(SslSocket.getInputStream());
                req = (RequeteSSL)ois.readObject();
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
            }
            catch (ClassNotFoundException e) {
                System.err.println("Erreur de def de classe [" + e.getMessage() + "]");
            }
            catch (IOException e) {
                System.err.println("Erreur ? [" + e.getMessage() + "]");
            }
            
            if(req instanceof RequeteTRAMAP)
                ((RequeteTRAMAP)req).setOis(ois);
            if(req instanceof RequeteCHAMAP)
                ((RequeteCHAMAP)req).setOis(ois);
            if(req instanceof RequeteBISAMAP)
                ((RequeteBISAMAP)req).setOis(ois);
              if(req instanceof RequeteIOBREP)
                ((RequeteIOBREP)req).setOis(ois);
              if(req instanceof RequeteSAMOP)
                ((RequeteSAMOP)req).setOis(ois);
            Runnable travail = req.createRunnable(SslSocket, guiApplication);
            if (travail != null) {
                tachesAExecuter.recordTache(travail);
                System.out.println("Travail mis dans la file");
            }
            else
                System.out.println("Pas de mise en file");
        }
    }
}
