/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Reconocimiento;

import BD.conectar;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPErrorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPErrorEvent;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.mysql.jdbc.CallableStatement;
import com.mysql.jdbc.Connection;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.sql.*;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author PC
 */
public class Registro extends javax.swing.JFrame {

    private DPFPCapture Lector = DPFPGlobal.getCaptureFactory().createCapture();
    
    private DPFPEnrollment Reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();

    private DPFPVerification Verificador = DPFPGlobal.getVerificationFactory().createVerification();
            
    private DPFPTemplate template;
    
    public  static String TEMPLATE_PROPERTY = "template";
    public DPFPFeatureSet featuresinscripcion;     
    public DPFPFeatureSet featuresverificacion;
    
    Connection conectarbd;
    Connection desconectarbd;
    CallableStatement cst;
    ResultSet resultado;
    
    conectar con = new conectar();
    
    public Registro() {
        initComponent();
    }
    
    public void EstadoHuellas(){
        EnviarTexto("Muestra de huella necesaria para guardar template " + Reclutador.getFeaturesNeeded());
    }
    public void EnviarTexto(String string){
        txtArea.append(string + "\n");
    }
    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose){
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try{
            return extractor.createFeatureSet(sample, purpose);
        }catch(DPFPImageQualityException e){
            return null;
        }
        }
    public Image CrearImagenHuella(DPFPSample sample){
            return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }
    
    //Dibujando
    public void DibujarHuellas(Image image){
        lblImagenHuella.setIcon(new ImageIcon(
                image.getScaledInstance(lblImagenHuella.getWidth(), lblImagenHuella.getHeight(), image.SCALE_FAST)));
        repaint();
    }
    //Creando plantilla con las caracteristicas
    public void setTemplate(DPFPTemplate template){
        DPFPTemplate old = this.template;
        this.template=template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }
    //Estado actual del lecto de huellas
    public void stop(){
        Lector.stopCapture();
        EnviarTexto("No se está usando el lector de Huella Dactilar");
    }
    
    public void start(){
        Lector.startCapture();
        EnviarTexto("El lector de Huellas Dactilares ha sido iniciado");
    }
    
    protected void Iniciar(){
        Lector.addDataListener(new DPFPDataAdapter(){
            @Override
            public void dataAcquired(final DPFPDataEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run (){
                        EnviarTexto("La Huella Digital ha sido capturada");
                        ProcessCapture(e.getSample());
                    }
                });
            }
        });
        
        Lector.addReaderStatusListener(new DPFPReaderStatusAdapter(){
            @Override
            public void readerConnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run(){
                        EnviarTexto("El sensor de Huella Digital esta Activado o Conectado");
                    }
                });
            };
            
            @Override
            public void readerDisconnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run(){
                        EnviarTexto("El sensor de Huella Digital esta Desactivado o no Conectado");
                    }
                });
            }
        });
        
        Lector.addSensorListener(new DPFPSensorAdapter(){
            @Override
            public void fingerTouched(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run(){
                        EnviarTexto("El dedo ha sido colocado sobre el lector de Huella");
                    }
                });
            }
            @Override
            public void fingerGone(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run(){
                        EnviarTexto("El dedo ha sido removido del lector de Huella");
                    }
                });
            }
        });
        
        Lector.addErrorListener(new DPFPErrorAdapter(){
            public void errorReader(final DPFPErrorEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override
                    public void run(){
                        EnviarTexto("Error: " + e.getError());
                    }
                });
            }
        }); 
    }
    
    public void ProcessCapture(DPFPSample sample){
        //Procesar la muestra de la huella y crear un conjunto de caracteristicas con el porposito de  inscripcin
        featuresinscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        
        //PRocesar la muestra de la huella y crear un consjunto de caracteristicas econ el proposito de veridicacion
        featuresverificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        
        //Comprobar la calidad de la muestra de la huella y lo añade a su reclutador si es bueno
        if (featuresinscripcion != null){
            try {
                System.out.println("Las caracteristicas de la Huella han sido creadas");
                Reclutador.addFeatures(featuresinscripcion); //Agregar las caracteristicas de la huella a la plantilla
                
                //Dibujar la huella dactilar capturada
                Image image = CrearImagenHuella(sample);
                DibujarHuellas(image);
                
            } catch (DPFPImageQualityException e) {
                System.err.print("Error: " + e.getMessage());
            } finally{
                EstadoHuellas();
                // Comprueba si la plantilla se ha creado
                switch (Reclutador.getTemplateStatus()) {
                    case TEMPLATE_STATUS_READY: // informe de exito y detine la captura
                        stop();
                        setTemplate(Reclutador.getTemplate());
                        EnviarTexto("La plantilla de la huella ha sido creada, ya puede Verificarla o identificarla");
                        break;
                    
                    case TEMPLATE_STATUS_FAILED: // informe de  fallas y reiniciar la captura de huellas
                        Reclutador.clear();
                        stop();
                        EstadoHuellas();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(Registro.this, "La plantilla de la huella no pudo ser creada","Inscripcion de huellas dactilares",JOptionPane.ERROR_MESSAGE);
                        start();
                        break;
                    default:
                throw new AssertionError();
            }
        }
    }
}
    public boolean guardarIHuella(){
    conectarbd= con.conexion();
    ByteArrayInputStream datosHuella= new ByteArrayInputStream(template.serialize());
    Integer tamañoHuella = template.serialize().length;
    String nombre = JOptionPane.showInputDialog("Nombre: ");
    try{
        Connection c=con.conexion();
        PreparedStatement guardarStnt = c.prepareStatement("INSERTAR INTO admin (NombrePersona, huella)values(?,?)");
        guardarStnt.setString(1,nombre);
        guardarStnt.setBinaryStream(2,datosHuella, tamañoHuella);
        
        JOptionPane.showMessageDialog(null,"Huella gardada con exito");
        
        conectarbd.setAutoCommit(false);
        guardarStnt.executeUpdate();
        conectarbd.commit();
        
        return true;
    }catch(SQLException ex){
        JOptionPane.showMessageDialog(null, ex.toString());
    }
    return false;    
}
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     *
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        guardarbt = new javax.swing.JButton();
        lblImagenHuella = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jLabel1.setText("Huella");

        guardarbt.setText("jButton1");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 615, Short.MAX_VALUE)
                .addComponent(guardarbt)
                .addGap(319, 319, 319))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblImagenHuella, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(guardarbt))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblImagenHuella, javax.swing.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
                .addContainerGap())
        );

        txtArea.setColumns(20);
        txtArea.setRows(5);
        jScrollPane1.setViewportView(txtArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 106, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
/**
* 
* @param evt 
*/
    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:
        Iniciar();
        start();
        EstadoHuellas();
        guardarbt.setEnabled(false);
    }//GEN-LAST:event_formWindowOpened

    /**
     * @param args the command line arguments
     */
    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Registro.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Registro.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Registro.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Registro.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Registro().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton guardarbt;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblImagenHuella;
    private javax.swing.JTextArea txtArea;
    // End of variables declaration//GEN-END:variables
}
