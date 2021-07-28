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

import java.util.*;

/**
 *
 * @author hector
 */
public class ListeTaches implements SourceTaches {
    
    private LinkedList listeTaches;

    public ListeTaches() {
        listeTaches = new LinkedList();
    }

    @Override
    public synchronized Runnable getTache() throws InterruptedException {
        System.out.println("getTache avant wait");
        while(!existTaches())
            wait();
        return (Runnable)listeTaches.remove();
    }

    @Override
    public synchronized boolean existTaches() {
        return !listeTaches.isEmpty();
    }

    @Override
    public synchronized void recordTache(Runnable r) {
        listeTaches.addLast(r);
        System.out.println("ListeTaches : tache dans la file");
        notify();
    }
    
}
