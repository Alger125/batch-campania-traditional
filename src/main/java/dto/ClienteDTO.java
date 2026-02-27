package dto;

public class ClienteDTO {
    private String nombreCompleto;
    private String segmento;
    private Double saldoTotal;

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String n) { this.nombreCompleto = n; }
    public String getSegmento() { return segmento; }
    public void setSegmento(String s) { this.segmento = s; }
    public Double getSaldoTotal() { return saldoTotal; }
    public void setSaldoTotal(Double s) { this.saldoTotal = s; }
}