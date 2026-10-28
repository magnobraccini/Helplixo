package br.com.magnobraccini.helplixo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessStep {
    private UUID id;
    private int number;
    private String title;
    private String description;
    private String expectedResult;
    private List<String> checklist = new ArrayList<>();

    public ProcessStep() {
    }

    public static ProcessStep create(int number, String title, String description) {
        ProcessStep step = new ProcessStep();
        step.id = UUID.randomUUID();
        step.number = number;
        step.title = title;
        step.description = description;
        return step;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
    public List<String> getChecklist() { return checklist; }
    public void setChecklist(List<String> checklist) { this.checklist = checklist; }
}
