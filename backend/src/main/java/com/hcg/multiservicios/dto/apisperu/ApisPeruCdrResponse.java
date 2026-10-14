package com.hcg.multiservicios.dto.apisperu;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApisPeruCdrResponse {
    private String id;
    private String code;
    private String description;
    private List<String> notes = new ArrayList<>();

    public ApisPeruCdrResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
}
