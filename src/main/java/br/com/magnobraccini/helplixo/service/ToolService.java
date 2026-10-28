package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.logging.LogService;
import br.com.magnobraccini.helplixo.model.Tool;
import br.com.magnobraccini.helplixo.repository.ToolRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ToolService {
    private final ToolRepository repository;
    private final LogService logService;

    public ToolService(ToolRepository repository, LogService logService) {
        this.repository = repository;
        this.logService = logService;
    }

    public ToolService(ToolRepository repository) {
        this(repository, LogService.disabled());
    }

    public List<Tool> findAll() {
        return repository.findAll();
    }

    public List<Tool> search(String query) {
        return (query == null || query.isBlank()) ? findAll() : repository.search(query, 200);
    }

    public List<Tool> findFavorites(int limit) {
        return repository.findFavorites(limit);
    }

    public long count() {
        return repository.count();
    }

    public long countFavorites() {
        return repository.findFavorites(Integer.MAX_VALUE).size();
    }

    public Tool create(String name, String description, String category, String executablePath, String command,
                       String icon) {
        validateName(name);
        Tool tool = Tool.create(name.trim(), safe(description), safe(category), safe(executablePath),
                safe(command), safe(icon));
        repository.save(tool);
        logService.logCreate("Ferramenta criada: " + tool.getName());
        return tool;
    }

    public Tool update(UUID id, String name, String description, String category, String executablePath,
                       String command, String icon) {
        validateName(name);
        Tool tool = requireTool(id);
        tool.setName(name.trim());
        tool.setDescription(safe(description));
        tool.setCategory(safe(category));
        tool.setExecutablePath(safe(executablePath));
        tool.setCommand(safe(command));
        tool.setIcon(safe(icon).isBlank() ? "🛠" : safe(icon));
        tool.setUpdatedAt(LocalDateTime.now());
        Tool saved = repository.save(tool);
        logService.logEdit("Ferramenta editada: " + saved.getName());
        return saved;
    }

    public Tool toggleFavorite(UUID id) {
        Tool tool = requireTool(id);
        tool.setFavorite(!tool.isFavorite());
        tool.setUpdatedAt(LocalDateTime.now());
        return repository.save(tool);
    }

    public void delete(UUID id) {
        Tool tool = requireTool(id);
        repository.delete(id);
        logService.logDelete("Ferramenta excluída: " + tool.getName());
    }

    /** Executa o comando (ou o executável) configurado na ferramenta. */
    public void execute(Tool tool) {
        String command = resolveCommand(tool);
        try {
            new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", command).start();
            logService.logEdit("Ferramenta executada: " + tool.getName());
        } catch (IOException exception) {
            logService.logError("Execução da ferramenta \"" + tool.getName() + "\"", exception);
            throw new IllegalStateException("Não foi possível executar \"" + tool.getName() + "\".\n"
                    + exception.getMessage(), exception);
        }
    }

    private String resolveCommand(Tool tool) {
        if (tool.getCommand() != null && !tool.getCommand().isBlank()) {
            return tool.getCommand();
        }
        if (tool.getExecutablePath() != null && !tool.getExecutablePath().isBlank()) {
            return tool.getExecutablePath();
        }
        throw new IllegalArgumentException("Nenhum comando ou executável configurado para esta ferramenta.");
    }

    private Tool requireTool(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Ferramenta não encontrada."));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
