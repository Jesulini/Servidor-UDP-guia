
package ClienteIMC;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class Cliente extends JFrame {

    private JTextField txtPeso;
    private JTextField txtAltura;

    private JComboBox<String> cmbUnidadPeso;
    private JComboBox<String> cmbUnidadAltura;

    private JButton btnCalcular;
    private JTextArea txtResultado;

    private final String HOST = "localhost";
    private final int PUERTO = 5000;

    public Cliente() {

        setTitle("Calculadora de IMC - UDP");
        setSize(500, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));

        JLabel titulo = new JLabel(
                "CÁLCULO DEL ÍNDICE DE MASA CORPORAL",
                SwingConstants.CENTER
        );

        titulo.setFont(new Font("Arial", Font.BOLD, 17));

        panel.add(titulo, BorderLayout.NORTH);

        JPanel formulario =
                new JPanel(new GridLayout(5, 2, 10, 10));

        formulario.setBorder(
                BorderFactory.createEmptyBorder(
                        20, 30, 20, 30
                )
        );

        // Peso

        formulario.add(new JLabel("Peso:"));

        txtPeso = new JTextField();
        formulario.add(txtPeso);

        // Unidad de peso

        formulario.add(new JLabel("Unidad de peso:"));

        cmbUnidadPeso = new JComboBox<>(
                new String[]{
                        "Kilogramos (kg)",
                        "Libras (lb)"
                }
        );

        formulario.add(cmbUnidadPeso);

        // Altura

        formulario.add(new JLabel("Altura:"));

        txtAltura = new JTextField();
        formulario.add(txtAltura);

        // Unidad de altura

        formulario.add(new JLabel("Unidad de altura:"));

        cmbUnidadAltura = new JComboBox<>(
                new String[]{
                        "Metros (m)",
                        "Centímetros (cm)"
                }
        );

        formulario.add(cmbUnidadAltura);

        // Botón

        formulario.add(new JLabel());

        btnCalcular = new JButton("CALCULAR IMC");
        formulario.add(btnCalcular);

        panel.add(formulario, BorderLayout.CENTER);

        // Resultado

        txtResultado = new JTextArea();
        txtResultado.setEditable(false);

        txtResultado.setFont(
                new Font("Arial", Font.BOLD, 14)
        );

        txtResultado.setLineWrap(true);
        txtResultado.setWrapStyleWord(true);

        txtResultado.setBorder(
                BorderFactory.createTitledBorder(
                        "Resultado"
                )
        );

        panel.add(txtResultado, BorderLayout.SOUTH);

        add(panel);

        btnCalcular.addActionListener(
                e -> enviarDatos()
        );

        getRootPane().setDefaultButton(btnCalcular);
    }

    // ENVIAR DATOS AL SERVIDOR

    private void enviarDatos() {

        String pesoTexto = txtPeso.getText().trim();
        String alturaTexto = txtAltura.getText().trim();

        // Validar campos vacíos.

        if (pesoTexto.isEmpty() || alturaTexto.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Debe ingresar el peso y la altura.",
                    "Error de validación",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        double peso;
        double altura;

        // Convertir los datos a números.

        try {

            peso = Double.parseDouble(
                    pesoTexto.replace(',', '.')
            );

            altura = Double.parseDouble(
                    alturaTexto.replace(',', '.')
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "El peso y la altura deben ser valores numéricos.",
                    "Error de validación",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // Validar valores positivos y finitos.

        if (!Double.isFinite(peso)
                || !Double.isFinite(altura)
                || peso <= 0
                || altura <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "El peso y la altura deben ser mayores que cero.",
                    "Error de validación",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        // Obtener unidades seleccionadas.

        String unidadPeso =
                cmbUnidadPeso.getSelectedIndex() == 0
                        ? "kg"
                        : "lb";

        String unidadAltura =
                cmbUnidadAltura.getSelectedIndex() == 0
                        ? "m"
                        : "cm";

        try (DatagramSocket socket = new DatagramSocket()) {

            // Tiempo máximo de espera de la respuesta.

            socket.setSoTimeout(3000);

            /*
             * Formato del mensaje:
             *
             * peso|altura|unidadPeso|unidadAltura
             *
             * Ejemplo:
             * 70.0|1.75|kg|m
             */

            String mensaje =
                    peso + "|"
                            + altura + "|"
                            + unidadPeso + "|"
                            + unidadAltura;

            byte[] datos = mensaje.getBytes(
                    StandardCharsets.UTF_8
            );

            // Dirección del servidor.

            InetAddress direccionServidor =
                    InetAddress.getByName(HOST);

            // Crear paquete UDP.

            DatagramPacket paqueteEnvio =
                    new DatagramPacket(
                            datos,
                            datos.length,
                            direccionServidor,
                            PUERTO
                    );

            // Enviar solicitud.

            socket.send(paqueteEnvio);

            // Preparar recepción.

            byte[] buffer = new byte[1024];

            DatagramPacket paqueteRespuesta =
                    new DatagramPacket(
                            buffer,
                            buffer.length
                    );

            // Esperar respuesta.

            socket.receive(paqueteRespuesta);

            // Convertir respuesta a texto.

            String respuesta = new String(
                    paqueteRespuesta.getData(),
                    0,
                    paqueteRespuesta.getLength(),
                    StandardCharsets.UTF_8
            );

            /*
             * Respuesta esperada:
             *
             * true|mensaje
             *
             * o
             *
             * false|mensaje
             */

            String[] partes = respuesta.split("\\|", 2);

            if (partes.length != 2) {

                txtResultado.setText(
                        "El servidor envió una respuesta no válida."
                );

                return;
            }

            boolean exito =
                    Boolean.parseBoolean(partes[0]);

            String mensajeRespuesta = partes[1];

            if (exito) {

                txtResultado.setText(
                        "Resultado recibido del servidor UDP:\n\n"
                                + mensajeRespuesta
                );

            } else {

                txtResultado.setText(
                        "El servidor informó un error:\n\n"
                                + mensajeRespuesta
                );
            }

        } catch (SocketTimeoutException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "El servidor no respondió a tiempo.",
                    "Error UDP",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "No fue posible comunicarse con el servidor UDP.\n\n"
                            + "Verifique que el servidor esté ejecutándose.\n"
                            + "Detalle: " + e.getMessage(),
                    "Error de conexión",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // MÉTODO PRINCIPAL

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            Cliente ventana = new Cliente();
            ventana.setVisible(true);

        });
    }
}
