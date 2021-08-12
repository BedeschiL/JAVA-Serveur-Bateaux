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
package ProtocoleSAMOP;

import ProtocoleIOBREP.*;
import ProtocoleTRAMAP.*;
import protocole.Reponse;
import java.io.Serializable;

/**
 *
 * @author hector
 */
public class ReponseSAMOP implements Reponse, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGIN_OK = 100;
     public static int NOT_LOGGED_IN=-2;
    public static int WRONG_LOGIN = 101;
    public static int ALREADY_LOGGED_IN = 102;
    public static int INVALID_FORMAT = 888;
    public static int UNKNOWN_TYPE = 999;
    public static int SERVER_FAIL = -1;
 
    public static int LOGIN = 1;
    public static int  COMPUTE_SAL = 2;
    public static int  VALIDATE_SAL = 3;
    public static int LAUNCH_PAYMENT =4;
    public static int LAUNCH_PAYMENTS = 5;
    public static int ASK_PAYMENTS = 6;
       public static int CREATE_SAL = 7;
         public static int GET_SAL = 8;
    private int codeRetour;
    private String chargeUtile;
    
    public ReponseSAMOP(int c, String chu) {
        codeRetour = c;
        setChargeUtile(chu);
    }

    @Override
    public int getCode() {
        return codeRetour;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }

    public void setChargeUtile(String chargeUtile) {
        this.chargeUtile = chargeUtile;
    }
}
