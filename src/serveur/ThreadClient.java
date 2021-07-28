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

/**
 *
 * @author hector
 */
public class ThreadClient extends Thread {
    
    private SourceTaches tachesAExecuter;
    private String nom;
    private Runnable tacheEnCours;
    
    public ThreadClient(SourceTaches st, String n) {
        tachesAExecuter = st;
        nom = n;
    }
    
    public void run() {
        while (!isInterrupted()) {
            try {
                System.out.println("Tread client avant get");
                tacheEnCours = tachesAExecuter.getTache();
            }
            catch (InterruptedException e) {
                System.out.println("Interruption : " + e.getMessage());
            }

            System.out.println("run de tacheencours");
            tacheEnCours.run();
            System.out.println("fin run de tacheencours");
            
        }
    }
}
