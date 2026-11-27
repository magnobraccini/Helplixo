# Referências do Helplixo Web

Cópias fixas da maquete e das regras de negócio, para o desenvolvimento das telas (`web/telas/`) não depender dos links.

| Arquivo | Origem | O que é |
| --- | --- | --- |
| `maquete-v32.dc.html` | https://claude.ai/artifact/YKUfPne25G3Kt8vu67gsUd (`project/Main.dc.html`, v32) | Código da maquete aprovada. Fonte dos tokens de cor (`:root{--c-...}` e `[data-tema="escuro"]{...}`, linhas 14 e 15), dos textos e do comportamento das telas. |
| `regras-de-negocio.md` | https://claude.ai/artifact/5rud8uZ6ugMsvGSfwDCoaz (revisão 11, 25/11/2026) | Regras de negócio aprovadas. |

- A senha de teste da maquete foi **removida desta cópia** (nenhuma senha no Git). A senha real vem só de `HELPLIXO_SENHA_EDICAO`, no servidor.
- A maquete é um "Design Component" (`.dc.html`): não abre sozinha no navegador. Serve para ler e copiar estilos e regras.
- Se a cópia e o original divergirem, vale o original. Ao atualizar a maquete, atualize também esta cópia.
