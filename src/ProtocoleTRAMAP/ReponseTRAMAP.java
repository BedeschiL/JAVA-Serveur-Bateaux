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
package ProtocoleTRAMAP;

import protocole.Reponse;
import java.io.Serializable;

/**
 *
 * @author hector
 */
public class ReponseTRAMAP implements Reponse, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGIN_OK = 100;
    public static int WRONG_LOGIN = 101;
    public static int ALREADY_LOGGED_IN = 102;
    public static int INPUT_LORRY_OK = 200;
    public static int RESERVATION_NOT_FOUND = 201;
    public static int CONTAINER_ALREADY_PRESENT = 202;
    public static int INPUT_LORRY_WITHOUT_RESERVATION_OK = 300;
    public static int NO_SPACE_LEFT = 301;
    public static int OUTPUT_CONTAINER_OK = 400;
    public static int CONTAINER_NOT_FOUND = 401;
    public static int LIST_OPERATIONS_OK = 500;
    public static int NO_OPS_FOUND = 501;
    public static int LOGOUT_OK = 600;
    public static int NOT_LOGGED_IN = 601;
    public static int ADD_TO_DB_OK = 700;
    public static int SQL_ERROR = 701;
    public static int INVALID_FORMAT = 888;
    public static int UNKNOWN_TYPE = 999;
    public static int SERVER_FAIL = -1;
    
    private int codeRetour;
    private String chargeUtile;
    
    public ReponseTRAMAP(int c, String chu) {
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
