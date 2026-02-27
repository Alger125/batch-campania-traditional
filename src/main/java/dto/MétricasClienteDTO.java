package dto;

public class MétricasClienteDTO {
    private String nombreCompleto;
    private Double saldoGlobal;
    private Integer totalTarjetas;
    private Double ticketPromedio;
    private String ultimaActividad;

    // Getters y Setters...
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String n) { this.nombreCompleto = n; }
    public Double getSaldoGlobal() { return saldoGlobal; }
    public void setSaldoGlobal(Double s) { this.saldoGlobal = s; }
    public Integer getTotalTarjetas() { return totalTarjetas; }
    public void setTotalTarjetas(Integer t) { this.totalTarjetas = t; }
    public Double getTicketPromedio() { return ticketPromedio; }
    public void setTicketPromedio(Double tp) { this.ticketPromedio = tp; }
    public String getUltimaActividad() { return ultimaActividad; }
    public void setUltimaActividad(String u) { this.ultimaActividad = u; }
}