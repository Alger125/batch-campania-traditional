package service.impl;

import dao.ClienteDAO;
import dto.MétricasClienteDTO;
import service.CampaniaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementación del servicio de Campañas para el procesamiento Batch.
 * Estilo tradicional Java 1.8 sin lambdas.
 * REGLA APLICADA: Los datos internos de fecha ahora muestran T-1 (día anterior).
 */
@Service
public class CampaniaServiceImpl implements CampaniaService {

    @Autowired
    private ClienteDAO clienteDAO;

    @Value("${ruta.archivo.salida}")
    private String rutaArchivo;

    /**
     * Orquestador del proceso.
     */
    @Override
    public void ejecutarProcesoCampania() {
        // Mantenemos la fecha actual para el nombre del archivo si así se desea
        String fechaHoy = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String rutaFinal = construirRutaDinamica(fechaHoy);

        List rawCandidatos = clienteDAO.obtenerDatosMasivos();
        List rawMetricas = clienteDAO.obtenerMetricasGlobales();

        Map mapaMetricas = indexarMetricasPorId(rawMetricas);

        escribirReporteHorizontal(rawCandidatos, mapaMetricas, rutaFinal);
    }

    /**
     * Toma la fecha de actividad y la transforma al día anterior (T-1).
     * Además, elimina el componente de tiempo (00:00:00.0).
     * Ejemplo: "2026-02-18 00:00:00.0" -> "2026-02-17"
     * * @param fechaSucia Fecha original de la base de datos (Timestamp).
     * @return String con la fecha del día anterior en formato YYYY-MM-DD.
     */
    private String obtenerFechaT1Interna(String fechaSucia) {
        if (fechaSucia == null || fechaSucia.trim().isEmpty() || "N/A".equals(fechaSucia)) {
            return "N/A";
        }

        try {
            // 1. Limpiar el string para quedarnos solo con yyyy-MM-dd
            String soloFecha = fechaSucia;
            int indiceEspacio = fechaSucia.indexOf(" ");
            if (indiceEspacio != -1) {
                soloFecha = fechaSucia.substring(0, indiceEspacio);
            }

            // 2. Convertir a objeto Date para restar el día
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fechaDato = sdf.parse(soloFecha);

            // 3. Usar Calendar para restar 1 día (Lógica T-1)
            Calendar cal = Calendar.getInstance();
            cal.setTime(fechaDato);
            cal.add(Calendar.DAY_OF_MONTH, -1);

            // 4. Retornar la fecha limpia y restada
            return sdf.format(cal.getTime());

        } catch (Exception e) {
            // Si el formato falla, devolvemos el dato original truncado por seguridad
            return fechaSucia.length() > 10 ? fechaSucia.substring(0, 10) : fechaSucia;
        }
    }

    /**
     * Formatea montos eliminando .00 para mostrar el dato limpio.
     */
    private String formatearMontoOBlanco(Double valor, int ancho) {
        if (valor == null || valor == 0.0) {
            return generarEspacios(ancho);
        }
        String texto = String.format("%.2f", valor);
        if (texto.endsWith(".00")) {
            texto = texto.substring(0, texto.length() - 3);
        }
        return ajustarAlAncho(texto, ancho);
    }

    /**
     * Escribe el reporte aplicando el cambio T-1 en cada fila de datos.
     */
    private void escribirReporteHorizontal(List rawCandidatos, Map mapaMetricas, String rutaDestino) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(rutaDestino, false));
            escribirEncabezado(bw);

            for (int i = 0; i < rawCandidatos.size(); i++) {
                Map fila = (Map) rawCandidatos.get(i);
                String id = fila.get("CLIENTE_ID").toString();
                Double saldo = fila.get("SALDO") != null ? ((Number) fila.get("SALDO")).doubleValue() : 0.0;
                String segmento = (fila.get("SEGMENTO") != null) ? fila.get("SEGMENTO").toString() : "N/A";

                if (saldo > 50000 && !"PLATINO".equalsIgnoreCase(segmento)) {
                    MétricasClienteDTO mExtra = (MétricasClienteDTO) mapaMetricas.get(id);

                    String nombre = (fila.get("NOMBRE") != null ? fila.get("NOMBRE").toString() : "") + " " +
                            (fila.get("APELLIDO") != null ? fila.get("APELLIDO").toString() : "");

                    Double saldoG = (mExtra != null) ? mExtra.getSaldoGlobal() : 0.0;
                    Integer tjs = (mExtra != null) ? mExtra.getTotalTarjetas() : 0;

                    // --- APLICACIÓN DE T-1 EN EL DATO INTERNO ---
                    String fechaRaw = (mExtra != null) ? mExtra.getUltimaActividad() : "N/A";
                    String ultimaActT1 = obtenerFechaT1Interna(fechaRaw);

                    String saldoStr = formatearMontoOBlanco(saldo, 12);
                    String saldoGStr = formatearMontoOBlanco(saldoG, 12);

                    String tjsStr = (tjs == null || tjs == 0) ? generarEspacios(5) : ajustarAlAncho(tjs.toString(), 5);

                    // Pinta la línea con la fecha ya restada (Ej: 2026-02-17)
                    String linea = String.format("%-30s | %-10s | %s | %s | %s | %-12s",
                            nombre, segmento, saldoStr, saldoGStr, tjsStr, ultimaActT1);

                    bw.write(linea);
                    bw.newLine();
                }
            }
            System.out.println(">>> Éxito: Datos internos generados con lógica T-1.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cerrarRecurso(bw);
        }
    }

    /**
     * Métodos auxiliares de formato y estructura tradicional.
     */
    private String ajustarAlAncho(String texto, int ancho) {
        StringBuilder sb = new StringBuilder();
        int espacios = ancho - texto.length();
        for (int i = 0; i < espacios; i++) sb.append(" ");
        sb.append(texto);
        return sb.toString();
    }

    private String generarEspacios(int ancho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ancho; i++) sb.append(" ");
        return sb.toString();
    }

    private Map indexarMetricasPorId(List filasMetricas) {
        Map mapa = new HashMap();
        for (int i = 0; i < filasMetricas.size(); i++) {
            Map fila = (Map) filasMetricas.get(i);
            if (fila.get("CLIENTE_ID") != null) {
                String id = fila.get("CLIENTE_ID").toString();
                MétricasClienteDTO m = new MétricasClienteDTO();
                m.setSaldoGlobal(fila.get("SALDO_GLOBAL") != null ? ((Number) fila.get("SALDO_GLOBAL")).doubleValue() : 0.0);
                m.setTotalTarjetas(fila.get("TOTAL_TARJETAS") != null ? ((Number) fila.get("TOTAL_TARJETAS")).intValue() : 0);
                m.setUltimaActividad(fila.get("ULTIMA_ACTIVIDAD") != null ? fila.get("ULTIMA_ACTIVIDAD").toString() : "N/A");
                mapa.put(id, m);
            }
        }
        return mapa;
    }

    private void escribirEncabezado(BufferedWriter bw) throws IOException {
        String header = String.format("%-30s | %-10s | %-12s | %-12s | %-5s | %-12s",
                "NOMBRE COMPLETO", "SEGMENTO", "SALDO", "SALDO GLOB", "Total de Tarjetas", "Ultima_Actividad_Fecha");
        bw.write(header);
        bw.newLine();
        bw.write("--------------------------------------------------------------------------------------------------");
        bw.newLine();
    }

    private String construirRutaDinamica(String fecha) {
        if (rutaArchivo.contains(".txt")) {
            return rutaArchivo.replace(".txt", "_" + fecha + ".txt");
        }
        return rutaArchivo + "_" + fecha + ".txt";
    }

    private void cerrarRecurso(BufferedWriter bw) {
        if (bw != null) { try { bw.close(); } catch (IOException e) { e.printStackTrace(); } }
    }
}