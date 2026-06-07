package com.sstt.academics;

import jakarta.validation.constraints.NotBlank;

public class SubjectForm {
    @NotBlank
    private String name;
    private String description;
    private String semester;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}
