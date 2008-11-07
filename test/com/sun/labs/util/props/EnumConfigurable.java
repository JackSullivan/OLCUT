/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.util.props;

/**
 *
 */
public class EnumConfigurable implements Configurable {
    
    public enum Type { A, B, C, D, E, F};
    
    Type one;
    
    Type two;

    public void newProperties(PropertySheet ps) throws PropertyException {
        one = (Type) ps.getEnum(PROP_ENUM1);
        two = (Type) ps.getEnum(PROP_ENUM2);
    }
    
    @ConfigEnum(type=EnumConfigurable.Type.class)
    public static final String PROP_ENUM1 = "enum1";
    
    @ConfigEnum(type=EnumConfigurable.Type.class,defaultValue="A")
    public static final String PROP_ENUM2 = "enum2";
}