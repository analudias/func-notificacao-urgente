package com.pos.tech;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class Feedback {
    private String descricao;
    @Min(value = 0, message = "A nota deve ser um valor de 0 a 10")
    @Max(value = 10, message = "A nota deve ser um valor de 0 a 10")
    private int nota;
    private String dataEnvio;

    public String getDescricao() { return descricao; }
    public void setDescricao(String d) { this.descricao = d; }
    public int getNota() { return nota; }
    public void setNota(int n) { this.nota = n; }
    public String getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(String dt) { this.dataEnvio = dt; }
}