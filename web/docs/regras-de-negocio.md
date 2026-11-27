# Regras de negócio do Helplixo Web

> Cópia de referência do documento original: https://claude.ai/artifact/5rud8uZ6ugMsvGSfwDCoaz (versão de 25/11/2026, revisão 11). Se as duas divergirem, vale o documento original.

Estas são as regras decididas na maquete do Helplixo Web, a versão web do sistema de suporte da TI da prefeitura. Só a equipe de TI usa o sistema por enquanto, com login individual.

## Perfis e permissões

Há três perfis: Triagem, Técnico e Chefe de TI. A Triagem registra e acompanha; o Técnico atende; o Chefe faz tudo.

| Ação | Triagem | Técnico | Chefe de TI |
| --- | --- | --- | --- |
| Registrar chamado | sim | não | sim |
| Atualizar, fechar ou reabrir chamado | sim | sim | sim |
| Assumir chamado | não | sim | sim |
| Escolher ou trocar o técnico | não | sim | sim |
| Movimentar equipamento | não | sim | sim |
| Virar chamado em processo | não | sim | sim |
| Criar aviso no calendário | sim | sim | sim |

- A Triagem não escolhe técnico, nem no cadastro nem na lista.
- Cada cargo tem nível e setor. Quem tem nível maior no mesmo setor tem poder sobre o de baixo (hoje, só para remover avisos).
- A TI tem subdivisões com chefes próprios; os funcionários não respondem para todos os chefes.

## Chamados

Um chamado tem três status: Aberto, Em andamento e Fechado. Ele é atualizado, fechado e reaberto pelo mesmo formulário, na aba Resultado.

**Registro**

- O Nº Semin (código da máquina) é obrigatório, a não ser que se marque “Sem máquina”. A máquina precisa existir no inventário e não pode estar baixada.
- Setor, contato, problema e descrição são obrigatórios. O setor e o contato vêm do registro e não mudam no fechamento.
- Campos: Nº, data, hora, Nº Semin, equipamento, contato, problema, descrição, observação, técnico, área, urgência, ocorrência e “Não computável”.

**Atualizar, fechar e reabrir**

- O formulário tem Status, anotação, data, hora, técnico e, se houver, a movimentação de equipamento.
- A anotação é obrigatória só para fechar (pelo menos 10 letras). Para só mudar o status ela pode ficar em branco.
- Sem anotação e sem mudar o status, não há o que salvar.
- Escolher Aberto ou Em andamento num chamado fechado o reabre. O fechamento anterior fica guardado no andamento.
- A hora não pode estar no futuro nem antes da abertura. Fora do expediente (seg. a sex., 8h às 14h) gera só um aviso.

**Lista**

- Filtros por status, período (carrega só o período escolhido), secretaria, técnico e busca.
- Clicar na linha mostra a descrição e a última atualização. Cada linha tem “Ver detalhes” e “Atualizar”.
- O Nº Semin é clicável e abre a ficha da máquina. A marca ↺ mostra outros chamados da mesma máquina nos últimos 7 dias.
- O PDF sai de um chamado ou de vários selecionados, com o andamento.
- Ao sair para outra aba e voltar, o chamado e a aba abertos continuam onde estavam.
- Processos parecidos são sugeridos por palavras em comum (mínimo 2) entre o chamado e o processo.

## Tempos, cores e estatísticas

O tempo de espera só conta atraso para chamados Abertos. Em andamento já está com alguém e não conta.

| Situação | Cor | Rótulo |
| --- | --- | --- |
| Aberto há menos de 2 h | verde | no prazo |
| Aberto entre 2 h e 3 h | âmbar | atenção |
| Aberto há mais de 3 h | vermelho | atrasado |
| Em andamento | azul | em andamento |
| Fechado ou não computável | cinza | — |

- “Não computável” tira o chamado das estatísticas (médias, “Precisa de atenção”, “Esperando há mais tempo”). Ele aparece na lista com a etiqueta NC.
- O Início mostra 4 números clicáveis: Abertos, Em andamento, Esperando há mais tempo (só abertos) e Fechados hoje, com a média de atendimento de 30 dias.

## Inventário

O Nº Semin e o patrimônio são a identidade da máquina e nunca mudam. Chamados e movimentações apontam para eles.

- A busca vem primeiro: digite pelo menos 2 letras (Nº Semin, patrimônio, usuário, setor ou software). Sem busca, mostra só as vistas recentemente.
- A ficha tem 4 abas: Identificação e configuração, Informações de compra, Softwares instalados (comerciais e PMSL) e Histórico. Ela mostra se há pacote Office.
- Adicionar máquina, editar máquina, adicionar software e dar baixa exigem o modo edição.
- O Nº Semin tem 2 letras e até 4 números (ex.: IC21). Patrimônio não se repete. A compra não pode estar no futuro, e a garantia não termina antes da compra.
- Toda edição entra no histórico com o que mudou (antes → depois) e quem fez.
- Movimentação registra máquina, destino, motivo, data, hora e técnico; o local da máquina muda junto. O destino não pode ser o mesmo lugar de onde ela saiu.
- Baixa: motivo (descarte, doação, leilão, furto ou perda, ou outro, que pede explicação). Antes, os chamados abertos da máquina precisam ser fechados.
- A máquina baixada continua nas buscas, marcada “Baixada”, e não recebe chamado nem movimentação. A baixa pode ser desfeita.

## Processos

Os processos ficam numa árvore de pastas e abrem sempre ao lado da árvore, na mesma tela. Criar e editar processos exige o modo edição.

- O painel tem rolagem própria e um índice vertical (“Neste processo”) que marca a seção na tela.
- Um processo pode ter texto, passos numerados, notas, imagens, anexos e links.
- Links levam a outros processos (abrem ao lado) ou a pastas do computador e da rede (o sistema copia o caminho, porque o navegador não abre pastas locais).
- No sistema real, o arquivo anexo fica numa pasta do servidor e o banco guarda só a ficha: nome, tipo, tamanho, hash, quem enviou e de qual processo.

| Anexo | Limite |
| --- | --- |
| Imagens (png, jpg, gif, webp) | 10 MB |
| Documentos (pdf, Office, LibreOffice, txt, csv) | 25 MB |
| Pacotes (.zip, .7z, .rar) | 100 MB |
| Programas (.exe, .msi, .bat, .cmd, .ps1, .vbs, .js) | só dentro de .zip |
| Total por processo | 200 MB |

## Contatos

Contatos têm duas listas que se trocam com um clique: Internos (ramais) e Externos (fornecedores e contratos). Cadastrar contato exige o modo edição.

- Internos: filtro por secretaria e andar, e ordem por nome, setor ou ramal.
- Externos: filtro por serviço e situação do contrato (em dia, vence em até 60 dias, vencido), em ordem de vencimento.
- O ramal tem 4 números e pode se repetir, porque a maioria é compartilhada. O sistema só avisa com quem ele é dividido.
- O ramal não aparece no chamado, pelo mesmo motivo.
- Contato novo entra na lista na hora, e as opções dos filtros (andar, categoria) se atualizam sozinhas.
- O vencimento de cada contrato vira uma marcação automática no calendário.

## Calendário e avisos

Avisos são marcações do tipo Aviso no calendário; não há mais aba de avisos. Qualquer cargo cria aviso; as outras marcações exigem o modo edição.

- Feriados, vencimentos de contrato e fins de garantia entram sozinhos e não podem ser removidos.
- Remover: o próprio autor, ou um cargo de nível maior no mesmo setor.
- Visões: Dia, Semana, Mês e Agenda (próximos 30 dias). Agendas por tipo podem ser ligadas e desligadas; fins de semana são opcionais.
- Na Semana e no Dia, o expediente (8h às 14h) fica destacado. Marcar fora dele só gera um aviso.
- Marcação pode ter horário ou ser de dia inteiro, descrição, repetição (semanal, mensal, anual) com data para terminar, e lembrete.
- A hora de fim precisa ser depois da de início. Repetição vale só para marcação de um dia.
- Lembretes aparecem só no sino: no começo do dia (7h30), 30 minutos, 1 hora ou 1 dia antes.
- Avisos ficam no dia em que foram marcados. O aviso urgente aparece no topo do Início.

## Modo edição e aparência

O modo edição fica em Configurações e só liga com a senha de edição. No sistema real, a senha é conferida no servidor e nunca vai para o Git.

- Com o modo ligado, aparece no topo “Editando · salva para todos”.
- Aparência: Claro (creme, estilo Notion), Escuro (estilo Notion) ou Igual ao sistema. A escolha fica salva no computador de cada um.
- No tema escuro, a capa ilustrada usa a versão noturna.
- O menu lateral usa emojis: Início, Chamados, Inventário, Processos, Contatos, Calendário e Ferramentas.

## Fora do escopo agora

Estes pontos foram adiados de propósito e voltam depois.

- Login para outros setores: cada setor terá seu login e sua versão do sistema. Por agora, só a TI.
- Sistema de cargos completo: hierarquia por subdivisão da TI. Hoje só vale para remover avisos.
- Calendário ligado a calendários externos.
- Ferramentas: sem mudanças por enquanto; novas ferramentas serão pedidas depois.
- Sem alerta de “avaliar troca” por máquina com muitos chamados, sem resumo por técnico para o chefe e sem som nos lembretes.
- O que o cargo de cima pode fazer no de baixo, além de remover avisos: ainda não definido.
