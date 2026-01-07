package com.pos.tech;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Feedback {

    private Long id;
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}