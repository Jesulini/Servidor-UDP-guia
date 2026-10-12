
package ServidorIMC;

import javax.swing.*;
import java.awt.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Servidor extends JFrame {

    private JTextField txtHost;
    private JTextField txtPuerto;
    private JButton btnServidor;
    private JLabel lblEstado;
    private JTextArea txtRegistro;

    private DatagramSocket servidor;
    private Thread hiloServidor;
    private volatile boolean servidorActivo = false;

    public Servidor() {

        setTitle("Servidor UDP - Calculadora IMC");
        setSize(700, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelPrincipal =
                new JPanel(new BorderLayout(10, 10));

        panelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );

        JLabel titulo = new JLabel(
                "SERVIDOR UDP - CÁLCULO DEL IMC",
                SwingConstants.CENTER
        );

        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        panelPrincipal.add(titulo, BorderLayout.NORTH);

        JPanel panelConfiguracion =
                new JPanel(new GridLayout(4, 2, 10, 10));

        panelConfiguracion.setBorder(
                BorderFactory.createTitledBorder(
                        "Configuración del servidor"
                )
        );

        panelConfiguracion.add(new JLabel("Host / IP:"));

        txtHost = new JTextField("localhost");
        panelConfiguracion.add(txtHost);

        panelConfiguracion.add(new JLabel("Puerto UDP:"));

        txtPuerto = new JTextField("5000");
        panelConfiguracion.add(txtPuerto);

        panelConfiguracion.add(new JLabel("Estado:"));

        lblEstado = new JLabel("APAGADO");
        lblEstado.setFont(new Font("Arial", Font.BOLD, 14));

        panelConfiguracion.add(lblEstado);

        panelConfiguracion.add(new JLabel());

        btnServidor = new JButton("ENCENDER SERVIDOR");
        panelConfiguracion.add(btnServidor);

        panelPrincipal.add(
                panelConfiguracion,
                BorderLayout.CENTER
        );

        txtRegistro = new JTextArea();
        txtRegistro.setEditable(false);

        txtRegistro.setFont(
                new Font("Monospaced", Font.PLAIN, 13)
        );

        txtRegistro.setLineWrap(true);
        txtRegistro.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(txtRegistro);

        scroll.setBorder(
                BorderFactory.createTitledBorder(
                        "Registro del servidor"
                )
        );

        panelPrincipal.add(scroll, BorderLayout.SOUTH);

        add(panelPrincipal);

        btnServidor.addActionListener(
                e -> cambiarEstadoServidor()
        );

        agregarRegistro("==========================================");
        agregarRegistro("SERVIDOR UDP DE IMC");
        agregarRegistro("Servidor actualmente apagado.");
        agregarRegistro("Configure el Host/IP y el puerto.");
        agregarRegistro("Luego presione ENCENDER SERVIDOR.");
        agregarRegistro("==========================================");
    }

    // ENCENDER O APAGAR EL SERVIDOR

    private void cambiarEstadoServidor() {

        if (!servidorActivo) {
            iniciarServidor();
        } else {
            detenerServidor();
        }
    }

    // INICIAR SERVIDOR

    private void iniciarServidor() {

        String host = txtHost.getText().trim();
        String puertoTexto = txtPuerto.getText().trim();

        if (host.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Debe ingresar un Host o una dirección IP.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        int puerto;

        try {
            puerto = Integer.parseInt(puertoTexto);

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "El puerto debe ser un número.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        if (puerto < 1 || puerto > 65535) {

            JOptionPane.showMessageDialog(
                    this,
                    "El puerto debe estar entre 1 y 65535.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;
        }

        try {

            InetAddress direccion =
                    InetAddress.getByName(host);

            servidor = new DatagramSocket(puerto, direccion);

            servidorActivo = true;

            lblEstado.setText("ACTIVO");
            btnServidor.setText("APAGAR SERVIDOR");

            txtHost.setEnabled(false);
            txtPuerto.setEnabled(false);

            agregarRegistro("");
            agregarRegistro("==========================================");
            agregarRegistro("SERVIDOR UDP ENCENDIDO");
            agregarRegistro("Host/IP: " + host);
            agregarRegistro("Puerto: " + puerto);
            agregarRegistro("Esperando clientes...");
            agregarRegistro("==========================================");

            hiloServidor = new Thread(this::escucharClientes);
            hiloServidor.start();

        } catch (Exception e) {

            servidorActivo = false;

            JOptionPane.showMessageDialog(
                    this,
                    "No fue posible iniciar el servidor.\n\n"
                            + "Detalle: " + e.getMessage(),
                    "Error del servidor",
                    JOptionPane.ERROR_MESSAGE
            );

            agregarRegistro(
                    "ERROR AL INICIAR: " + e.getMessage()
            );
        }
    }

    // DETENER SERVIDOR

    private void detenerServidor() {

        servidorActivo = false;

        if (servidor != null && !servidor.isClosed()) {
            servidor.close();
        }

        lblEstado.setText("APAGADO");
        btnServidor.setText("ENCENDER SERVIDOR");

        txtHost.setEnabled(true);
        txtPuerto.setEnabled(true);

        agregarRegistro("");
        agregarRegistro("==========================================");
        agregarRegistro("SERVIDOR UDP APAGADO");
        agregarRegistro("El puerto fue liberado.");
        agregarRegistro("==========================================");
    }

    // ESCUCHAR CLIENTES

    private void escucharClientes() {

        while (servidorActivo) {

            try {

                byte[] buffer = new byte[1024];

                DatagramPacket paqueteRecibido =
                        new DatagramPacket(
                                buffer,
                                buffer.length
                        );

                servidor.receive(paqueteRecibido);

                if (!servidorActivo) {
                    break;
                }

                String direccionCliente =
                        paqueteRecibido.getAddress()
                                .getHostAddress();

                int puertoCliente =
                        paqueteRecibido.getPort();

                String mensaje = new String(
                        paqueteRecibido.getData(),
                        0,
                        paqueteRecibido.getLength(),
                        StandardCharsets.UTF_8
                );

                agregarRegistro("");
                agregarRegistro("------------------------------------------");
                agregarRegistro("SOLICITUD RECIBIDA");

                agregarRegistro(
                        "Cliente: " + direccionCliente
                                + ":" + puertoCliente
                );

                agregarRegistro("Datos: " + mensaje);

                procesarSolicitud(
                        mensaje,
                        paqueteRecibido
                );

            } catch (Exception e) {

                if (servidorActivo) {

                    agregarRegistro(
                            "Error al recibir datos: "
                                    + e.getMessage()
                    );
                }
            }
        }
    }

    // PROCESAR SOLICITUD DE IMC

    private void procesarSolicitud(
            String mensaje,
            DatagramPacket paqueteRecibido) {

        String respuesta;

        /*
         * Formato recibido:
         *
         * peso|altura|unidadPeso|unidadAltura
         *
         * Ejemplo:
         * 70|1.75|kg|m
         */

        String[] datos = mensaje.split("\\|", -1);

        if (datos.length != 4) {

            respuesta =
                    "false|Error: formato incorrecto. "
                    + "Se esperaba peso|altura|unidadPeso|unidadAltura.";

        } else {

            try {

                double peso = Double.parseDouble(
                        datos[0].trim()
                );

                double altura = Double.parseDouble(
                        datos[1].trim()
                );

                String unidadPeso = datos[2].trim();
                String unidadAltura = datos[3].trim();

                agregarRegistro("Peso recibido: " + peso);
                agregarRegistro("Altura recibida: " + altura);
                agregarRegistro("Unidad de peso: " + unidadPeso);
                agregarRegistro("Unidad de altura: " + unidadAltura);

                if (!Double.isFinite(peso)
                        || !Double.isFinite(altura)
                        || peso <= 0
                        || altura <= 0) {

                    throw new IllegalArgumentException(
                            "El peso y la altura deben ser mayores que cero."
                    );
                }

                // Convertir peso a kilogramos.

                if (unidadPeso.equals("lb")) {

                    peso = peso * 0.45359237;

                } else if (!unidadPeso.equals("kg")) {

                    throw new IllegalArgumentException(
                            "Unidad de peso no válida."
                    );
                }

                // Convertir altura a metros.

                if (unidadAltura.equals("cm")) {

                    altura = altura / 100.0;

                } else if (!unidadAltura.equals("m")) {

                    throw new IllegalArgumentException(
                            "Unidad de altura no válida."
                    );
                }

                if (!Double.isFinite(peso)
                        || !Double.isFinite(altura)
                        || peso <= 0
                        || altura <= 0) {

                    throw new IllegalArgumentException(
                            "Los valores convertidos no son válidos."
                    );
                }

                agregarRegistro(
                        String.format(
                                Locale.US,
                                "Peso convertido: %.2f kg",
                                peso
                        )
                );

                agregarRegistro(
                        String.format(
                                Locale.US,
                                "Altura convertida: %.2f m",
                                altura
                        )
                );

                // Calcular IMC.

                double imc = peso / (altura * altura);

                String clasificacion;

                if (imc < 18.5) {

                    clasificacion = "Bajo peso";

                } else if (imc < 25) {

                    clasificacion = "Peso normal";

                } else if (imc < 30) {

                    clasificacion = "Sobrepeso";

                } else {

                    clasificacion = "Obesidad";
                }

                String resultado = String.format(
                        Locale.US,
                        "Peso: %.2f kg\n"
                                + "Altura: %.2f m\n"
                                + "IMC: %.2f\n"
                                + "Clasificación: %s",
                        peso,
                        altura,
                        imc,
                        clasificacion
                );

                agregarRegistro("Cálculo del IMC:");

                agregarRegistro(
                        String.format(
                                Locale.US,
                                "%.2f / (%.2f * %.2f) = %.2f",
                                peso,
                                altura,
                                altura,
                                imc
                        )
                );

                agregarRegistro("Resultado: " + resultado);

                respuesta = "true|" + resultado;

            } catch (NumberFormatException e) {

                respuesta =
                        "false|Error: el peso y la altura "
                                + "deben ser valores numéricos válidos.";

                agregarRegistro("Error: valores no numéricos.");

            } catch (IllegalArgumentException e) {

                respuesta = "false|Error: " + e.getMessage();

                agregarRegistro(
                        "Error: " + e.getMessage()
                );
            }
        }

        // ENVIAR RESPUESTA AL CLIENTE

        try {

            byte[] datosRespuesta =
                    respuesta.getBytes(
                            StandardCharsets.UTF_8
                    );

            DatagramPacket paqueteRespuesta =
                    new DatagramPacket(
                            datosRespuesta,
                            datosRespuesta.length,
                            paqueteRecibido.getAddress(),
                            paqueteRecibido.getPort()
                    );

            servidor.send(paqueteRespuesta);

            agregarRegistro("Respuesta enviada:");
            agregarRegistro(respuesta);
            agregarRegistro("Esperando siguiente solicitud...");

        } catch (Exception e) {

            if (servidorActivo) {

                agregarRegistro(
                        "Error al enviar respuesta: "
                                + e.getMessage()
                );
            }
        }
    }

    // MOSTRAR REGISTRO EN LA VENTANA

    private void agregarRegistro(String mensaje) {

        SwingUtilities.invokeLater(() -> {

            txtRegistro.append(mensaje + "\n");

            txtRegistro.setCaretPosition(
                    txtRegistro.getDocument().getLength()
            );
        });
    }

    // CERRAR SOCKET AL CERRAR VENTANA

    @Override
    public void dispose() {

        if (servidorActivo) {
            detenerServidor();
        }

        super.dispose();
    }

    // MÉTODO PRINCIPAL

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            Servidor ventana = new Servidor();
            ventana.setVisible(true);

        });
    }
}
