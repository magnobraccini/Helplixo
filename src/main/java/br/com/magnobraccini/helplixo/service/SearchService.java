package br.com.magnobraccini.helplixo.service;

import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.model.PhoneContact;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.model.Tool;
import br.com.magnobraccini.helplixo.repository.FolderRepository;
import br.com.magnobraccini.helplixo.repository.NoticeRepository;
import br.com.magnobraccini.helplixo.repository.PhoneContactRepository;
import br.com.magnobraccini.helplixo.repository.ProcessRepository;
import br.com.magnobraccini.helplixo.repository.ToolRepository;
import br.com.magnobraccini.helplixo.util.TextNormalizer;

import java.util.ArrayList;
import java.util.List;

public final class SearchService {
    private final ProcessRepository processRepository;
    private final FolderRepository folderRepository;
    private final PhoneContactRepository phoneContactRepository;
    private final NoticeRepository noticeRepository;
    private final ToolRepository toolRepository;

    public SearchService(ProcessRepository processRepository, FolderRepository folderRepository,
                          PhoneContactRepository phoneContactRepository, NoticeRepository noticeRepository,
                          ToolRepository toolRepository) {
        this.processRepository = processRepository;
        this.folderRepository = folderRepository;
        this.phoneContactRepository = phoneContactRepository;
        this.noticeRepository = noticeRepository;
        this.toolRepository = toolRepository;
    }

    public SearchService(ProcessRepository processRepository, FolderRepository folderRepository,
                          PhoneContactRepository phoneContactRepository) {
        this(processRepository, folderRepository, phoneContactRepository, null, null);
    }

    public SearchService(ProcessRepository processRepository, FolderRepository folderRepository) {
        this(processRepository, folderRepository, null, null, null);
    }

    public List<SearchResult> search(String query) {
        String normalized = TextNormalizer.normalize(query);
        if (normalized.length() < 2) {
            return List.of();
        }
        List<SearchResult> results = new ArrayList<>();
        processRepository.search(query, 30).forEach(process -> results.add(SearchResult.forProcess(process)));
        folderRepository.findAll().stream()
                .filter(folder -> TextNormalizer.normalize(folder.getName()).contains(normalized))
                .limit(15)
                .map(SearchResult::forFolder)
                .forEach(results::add);
        if (phoneContactRepository != null) {
            phoneContactRepository.search(query, 15).forEach(contact -> results.add(SearchResult.forPhoneContact(contact)));
        }
        if (noticeRepository != null) {
            noticeRepository.search(query, 15).forEach(notice -> results.add(SearchResult.forNotice(notice)));
        }
        if (toolRepository != null) {
            toolRepository.search(query, 15).forEach(tool -> results.add(SearchResult.forTool(tool)));
        }
        return results;
    }

    public record SearchResult(String type, String title, String subtitle, ProcessDocument process, Folder folder,
                                PhoneContact phoneContact, Notice notice, Tool tool) {
        public static SearchResult forProcess(ProcessDocument process) {
            return new SearchResult("Processo", process.getTitle(), process.getDescription(), process, null,
                    null, null, null);
        }

        public static SearchResult forFolder(Folder folder) {
            return new SearchResult("Pasta", folder.getName(), folder.getDescription(), null, folder,
                    null, null, null);
        }

        public static SearchResult forPhoneContact(PhoneContact contact) {
            String subtitle = String.join(" · ", nonBlank(contact.getSector()), nonBlank(contact.getFloor()),
                    nonBlank(contact.getExtension()));
            return new SearchResult("Ramal", contact.getName(), subtitle, null, null, contact, null, null);
        }

        public static SearchResult forNotice(Notice notice) {
            return new SearchResult("Aviso", notice.getTitle(), notice.getCategory().getDisplayName(),
                    null, null, null, notice, null);
        }

        public static SearchResult forTool(Tool tool) {
            return new SearchResult("Ferramenta", tool.getName(), nonBlank(tool.getDescription()),
                    null, null, null, null, tool);
        }

        private static String nonBlank(String value) {
            return value == null ? "" : value;
        }

        @Override
        public String toString() {
            return type + " — " + title;
        }
    }
}
