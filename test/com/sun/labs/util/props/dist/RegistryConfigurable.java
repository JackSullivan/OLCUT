/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.util.props.dist;

import com.sun.labs.util.props.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An interface that can be used remotely.
 */
public interface RegistryConfigurable extends Remote, Component {
    
    public String stringOp(String s) throws RemoteException;
    
    public int intOp(int x) throws RemoteException;

}
