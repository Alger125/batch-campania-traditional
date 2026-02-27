package dao.impl;

import dao.ClienteDAO;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class ClienteDAOImpl extends BaseDAO implements ClienteDAO {

    @Override
    public List<Map<String, Object>> obtenerDatosMasivos() {
        String sql = "SELECT C.CLIENTE_ID, C.NOMBRE, C.APELLIDO, C.SEGMENTO, " +
                "CU.SALDO, M.FECHA AS MOV_FECHA " +
                "FROM CLIENTES_BBVA C " +
                "LEFT JOIN CUENTAS_BBVA CU ON C.CLIENTE_ID = CU.CLIENTE_ID " +
                "LEFT JOIN MOVIMIENTOS_BBVA M ON C.CLIENTE_ID = M.CLIENTE_ID";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> obtenerMetricasGlobales() {
        String sql = "SELECT C.CLIENTE_ID, " +
                "C.NOMBRE || ' ' || C.APELLIDO AS NOMBRE_COMPLETO, " +
                "(SELECT SUM(SALDO) FROM CUENTAS_BBVA WHERE CLIENTE_ID = C.CLIENTE_ID) AS SALDO_GLOBAL, " +
                "(SELECT COUNT(*) FROM TARJETAS_BBVA WHERE CLIENTE_ID = C.CLIENTE_ID) AS TOTAL_TARJETAS, " +
                "(SELECT AVG(MONTO) FROM MOVIMIENTOS_BBVA WHERE CLIENTE_ID = C.CLIENTE_ID AND TIPO = 'COMPRA') AS TICKET_PROMEDIO_COMPRA, " +
                "(SELECT MAX(FECHA) FROM MOVIMIENTOS_BBVA WHERE CLIENTE_ID = C.CLIENTE_ID) AS ULTIMA_ACTIVIDAD " +
                "FROM CLIENTES_BBVA C";
        return jdbcTemplate.queryForList(sql);
    }
}