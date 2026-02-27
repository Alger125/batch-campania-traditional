package dao;

import java.util.List;
import java.util.Map;

public interface ClienteDAO {
    List<Map<String, Object>> obtenerDatosMasivos();
    List<Map<String, Object>> obtenerMetricasGlobales();
}