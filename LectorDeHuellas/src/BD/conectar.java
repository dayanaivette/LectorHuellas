/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BD;

//import com.sun.jdi.connect.spi.Connection;
import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author PC
 */
public class conectar {
    Connection con = null;
    public Connection conexion(){
        try {
            String url = "jdbc:mysql://localhost:3306/unab";
            String user = "root";
            String pass = "root";
            
            con = DriverManager.getConnection(url, user, pass);
            
        }
    
    catch(SQLException e){
    System.out.println("error de conexion");
    JOptionPane.showMessageDialog(null, "error de conexion" + e);
    
    }
    return con;
    }
    
    public Connection CerrarConexion(){
        try{
            con.close();
            System.out.println("cerrar conexion a");
            
        }
        catch(SQLException ex){
            System.out.println(ex);
        }
        return null;
    }
}
