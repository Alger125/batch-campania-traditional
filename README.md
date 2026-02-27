# 📦 Batch_Viejito

Proceso batch desarrollado con **Spring Boot** para la generación de reportes de candidatos a segmento Platino, basado en datos de clientes de Banco de Prueba. El sistema consulta la base de datos Oracle, aplica reglas de negocio y escribe un archivo de texto plano con los candidatos elegibles.

---

## 🏗️ Arquitectura del Proyecto

```
Batch_Viejito/
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── dao/
│   │       │   ├── ClienteDAO.java               # Interfaz de acceso a datos
│   │       │   └── impl/
│   │       │       ├── BaseDAO.java              # DAO base con JdbcTemplate
│   │       │       └── ClienteDAOImpl.java       # Implementación de consultas SQL
│   │       ├── dto/
│   │       │   ├── ClienteDTO.java               # DTO básico de cliente
│   │       │   └── MétricasClienteDTO.java       # DTO con métricas extendidas
│   │       ├── service/
│   │       │   ├── CampaniaService.java          # Interfaz del servicio
│   │       │   └── impl/
│   │       │       └── CampaniaServiceImpl.java  # Lógica de negocio y generación del reporte
│   │       └── org/example/batch_viejito/
│   │           └── BatchViejitoApplication.java  # Entry point (CommandLineRunner)
│   └── test/
│       └── ...
├── pom.xml
└── README.md
```

---

## ⚙️ Tecnologías y Dependencias

| Tecnología            | Versión   | Uso                                  |
|-----------------------|-----------|--------------------------------------|
| Java                  | 17        | Lenguaje base del proyecto           |
| Spring Boot           | 3.2.0     | Framework principal                  |
| Spring Data JDBC      | (managed) | Acceso a base de datos vía JdbcTemplate |
| Oracle JDBC (ojdbc11) | (managed) | Driver de conexión a Oracle DB       |
| Maven                 | -         | Gestión de dependencias y build      |

> **Nota:** El `pom.xml` tiene configurado el compilador con `source/target 25` y `--enable-preview`. Asegúrate de que tu JDK local sea compatible o ajusta estos valores a `17` si usas la versión estándar.

---

## 🗄️ Modelo de Base de Datos (Tablas Oracle Requeridas)

El sistema espera las siguientes tablas en el esquema Oracle configurado:

| Tabla                | Descripción                                      |
|----------------------|--------------------------------------------------|
| `CLIENTES`      | Datos maestros de clientes (ID, nombre, segmento)|
| `CUENTAS`       | Saldos asociados por cliente                     |
| `TARJETAS`      | Tarjetas activas por cliente                     |
| `MOVIMIENTOS`   | Historial de movimientos (compras, fechas)        |

---

## 🔄 Flujo del Proceso

```
[Inicio]
    ↓
Consultar CLIENTES + CUENTAS + MOVIMIENTOS
    ↓
Consultar métricas globales (saldo total, tarjetas, ticket promedio, última actividad)
    ↓
Indexar métricas por CLIENTE_ID en memoria
    ↓
Filtrar candidatos: SALDO > 50,000 AND SEGMENTO ≠ 'PLATINO'
    ↓
Aplicar lógica T-1 sobre fecha de última actividad
    ↓
Escribir reporte en archivo .txt con formato horizontal
    ↓
[Fin]
```

---

## 📋 Reglas de Negocio

### Criterio de Selección de Candidatos
Un cliente es considerado candidato a Platino si cumple **ambas** condiciones:
- `SALDO > 50,000`
- `SEGMENTO ≠ 'PLATINO'` (no es ya cliente Platino)

### Lógica T-1 (Fecha Interna)
La fecha de última actividad mostrada en el reporte corresponde al **día anterior** (`T-1`) respecto a la fecha registrada en la base de datos.

**Ejemplo:**
```
Fecha en BD:       2026-02-18 00:00:00.0
Fecha en reporte:  2026-02-17
```

Esto permite alinear el reporte con el cierre de operaciones del día anterior.

---

## 📄 Formato del Reporte de Salida

El archivo generado es un `.txt` con columnas de ancho fijo:

```
NOMBRE COMPLETO                | SEGMENTO   | SALDO        | SALDO GLOB   | Total de Tarjetas | Ultima_Actividad_Fecha
--------------------------------------------------------------------------------------------------
NOMBRE_1 APELLIDO_1            | ESTANDAR   |        75000 |        76000 |                   | 2026-02-15
NOMBRE_3 APELLIDO_3            | ESTANDAR   |       999000 |      1002000 |                   | 2026-02-15
```

| Columna                 | Descripción                                      |
|-------------------------|--------------------------------------------------|
| `NOMBRE COMPLETO`       | Nombre + Apellido del cliente (30 caracteres)    |
| `SEGMENTO`              | Segmento actual del cliente (10 caracteres)      |
| `SALDO`                 | Saldo individual (12 caracteres, sin decimales si son .00) |
| `SALDO GLOB`            | Suma de todos los saldos del cliente (12 caracteres) |
| `Total de Tarjetas`     | Cantidad de tarjetas activas (5 caracteres)      |
| `Ultima_Actividad_Fecha`| Fecha T-1 de última actividad (formato YYYY-MM-DD) |

---

## 🚀 Configuración y Ejecución

### 1. Prerrequisitos
- JDK 17+
- Maven 3.8+
- Acceso a base de datos Oracle con las tablas requeridas

### 2. Configuración (`application.properties`)

```properties
# Conexión a Oracle
spring.datasource.url=jdbc:oracle:thin:@//HOST:PORT/SERVICE
spring.datasource.username=USUARIO
spring.datasource.password=CONTRASEÑA
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# Ruta del archivo de salida (se añadirá la fecha automáticamente)
ruta.archivo.salida=/ruta/destino/candidatos_platino.txt
```

> El nombre del archivo generado tendrá el formato: `candidatos_platino_YYYYMMDD.txt`

### 3. Compilar y Ejecutar

```bash
# Compilar el proyecto
mvn clean package

# Ejecutar el JAR generado
java -jar target/Batch_Viejito-0.0.1-SNAPSHOT.jar
```

### 4. Salida Esperada en Consola

```
>>> Éxito: Datos internos generados con lógica T-1.
```

---

## 🧩 Descripción de Componentes

### `BatchViejitoApplication`
Punto de entrada de la aplicación. Implementa `CommandLineRunner` para ejecutar el proceso al iniciar. Usa `@ComponentScan` para detectar los beans en los paquetes `dao`, `service` y `dto`.

### `ClienteDAOImpl`
Contiene dos consultas principales:
- **`obtenerDatosMasivos()`** — JOIN entre clientes, cuentas y movimientos para obtener los datos base.
- **`obtenerMetricasGlobales()`** — Subqueries por cliente para calcular saldo global, total de tarjetas, ticket promedio y fecha de última actividad.

### `CampaniaServiceImpl`
Orquesta el proceso completo:
1. Obtiene los dos datasets del DAO.
2. Indexa las métricas en un `Map<String, MétricasClienteDTO>` por `CLIENTE_ID`.
3. Itera sobre los candidatos aplicando el filtro de negocio.
4. Aplica la transformación T-1 sobre la fecha de actividad.
5. Escribe el reporte con formato de ancho fijo.

### Método `obtenerFechaT1Interna`
Recibe un timestamp de Oracle (ej: `"2026-02-18 00:00:00.0"`), extrae solo la parte de fecha y le resta un día usando `Calendar`. Maneja errores de formato devolviendo el dato original truncado.

---

## ⚠️ Consideraciones Importantes

- El proceso **sobreescribe** el archivo de salida en cada ejecución (`FileWriter` con `append = false`).
- Los montos con decimales `.00` se muestran sin ellos para mayor limpieza visual.
- Si un cliente no tiene métricas globales, los valores de saldo global y tarjetas se muestran como `0` / espacios en blanco.
- El proyecto usa colecciones sin genéricos (`List`, `Map` raw types) por compatibilidad con el estilo Java tradicional del equipo.
- El `pom.xml` configura el compilador con Java 25 y `--enable-preview`. **Ajusta a `17`** si tu entorno no soporta versiones preview.

---

## 📁 Ejemplo de Archivo Generado

**Nombre:** `candidatos_platino_20260226.txt`

```
NOMBRE COMPLETO                | SEGMENTO   | SALDO        | SALDO GLOB   | Total de Tarjetas | Ultima_Actividad_Fecha
--------------------------------------------------------------------------------------------------
NOMBRE_1 APELLIDO_1            | ESTANDAR   |        75000 |        76000 |                   | 2026-02-15  
NOMBRE_3 APELLIDO_3            | ESTANDAR   |       999000 |      1002000 |                   | 2026-02-15  
...
```

---

## 👥 Equipo

Proyecto interno — Estilo Java tradicional (sin lambdas, colecciones tipadas mínimas).  
Para dudas sobre el modelo de datos o reglas de negocio, consultar al área de **Campañas y Segmentación**.
