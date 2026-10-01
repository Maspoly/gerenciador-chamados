# ESPECIFICACAO.md

## 1. Objetivo da implementação

Esta evolução adiciona ao sistema existente a funcionalidade de reservas de áreas comuns, preservando o comportamento já existente dos chamados.

A solução foi implementada sobre a stack já fornecida pelo projeto:

- Java com Spring Boot;
- JSP no frontend;
- PostgreSQL;
- Flyway para versionamento do banco.

A funcionalidade permite:

- manutenção de áreas comuns por administrador;
- consulta de disponibilidade por morador;
- solicitação de reservas;
- aprovação e negação por administrador;
- cancelamento por morador proprietário ou administrador;
- acompanhamento das reservas em lista e calendário;
- preservação de histórico;
- proteção contra conflitos, inclusive em aprovações concorrentes.

---

## 2. Interpretações e decisões adotadas

### 2.1. Estado inicial e disponibilidade

Toda nova reserva é criada com estado `SOLICITADA`.

Uma reserva `SOLICITADA` representa um pedido pendente e não bloqueia o horário. Somente reservas `APROVADA` ocupam efetivamente o intervalo para efeito de conflito.

Reservas `NEGADA` e `CANCELADA` permanecem consultáveis como histórico, mas não ocupam disponibilidade.

### 2.2. Regra de conflito

O conflito é avaliado apenas entre reservas `APROVADA` da mesma área comum.

Assim:

- reservas sobrepostas da mesma área não podem permanecer simultaneamente aprovadas;
- reservas sobrepostas em áreas diferentes são permitidas;
- uma reserva apenas `SOLICITADA` não bloqueia outra aprovação.

A verificação considera sobreposição real de intervalos, isto é, um intervalo conflita quando começa antes do término do outro e termina depois do início do outro.

### 2.3. Concorrência em aprovações

Para evitar que duas decisões administrativas simultâneas aprovem reservas conflitantes da mesma área, foi adotado lock pessimista sobre a área comum durante a aprovação.

A escolha foi mantida porque serializa a decisão crítica por área e foi validada com teste de integração usando PostgreSQL real via Testcontainers.

O teste concorrente executa duas aprovações simultâneas de reservas conflitantes e comprova que, ao final, no máximo uma delas permanece `APROVADA`.

### 2.4. Cancelamento

O proprietário da reserva e o administrador podem cancelar reservas nos estados `SOLICITADA` ou `APROVADA`, desde que o horário inicial ainda não tenha sido atingido.

Reservas `NEGADA` e `CANCELADA` são tratadas como estados terminais para este fluxo.

Para o morador, a consulta e o cancelamento são sempre restringidos à própria reserva.

Na interface administrativa foi adotada, por decisão de produto, a exigência de um motivo de cancelamento. O desafio exige motivo obrigatório para a negativa, mas não torna o motivo de cancelamento um pré-requisito de domínio. A exigência na interface administrativa foi mantida por rastreabilidade e auditabilidade.

### 2.5. Negativa

A negativa administrativa exige motivo não vazio.

Quando concluída, a reserva passa para `NEGADA`, o motivo é preservado e a alteração é registrada no histórico.

### 2.6. Desativação de área comum

A desativação impede novas solicitações para a área, mas não remove nem altera automaticamente reservas já existentes.

As reservas anteriores continuam preservadas e seguem seu ciclo de vida normal.

### 2.7. Referência temporal

A funcionalidade utiliza a mesma referência temporal global já adotada pela aplicação.

O timezone padrão da JVM é configurado pela aplicação a partir de `APP_TIMEZONE` e, na ausência deste, `TZ`. Na configuração de referência fornecida pelo projeto, `APP_TIMEZONE` utiliza `America/Sao_Paulo`.

O fluxo de reservas utiliza essa referência para comparações de início, fim, aprovação, cancelamento e registros de histórico, evitando manter uma configuração de timezone exclusiva da feature.

### 2.8. Controle de acesso

Foram mantidas as responsabilidades definidas para os perfis:

- `MORADOR`: consulta disponibilidade, solicita, acompanha e cancela somente as próprias reservas;
- `ADMINISTRADOR`: mantém áreas comuns, consulta todas as reservas, aprova, nega e cancela;
- `COLABORADOR`: não participa do fluxo de reservas.

Tentativas de acesso a dados de reserva de outro morador são recusadas sem exposição do conteúdo protegido.

### 2.9. Preservação do sistema existente

Durante o desenvolvimento foram evitadas alterações em regras e fluxos antigos de chamados que não fossem necessárias para a funcionalidade de reservas.

Sugestões de refatoração ou alteração em componentes legados foram descartadas quando não havia relação direta com os requisitos do desafio.

---

## 3. Impacto no modelo de dados

Foi adicionada uma nova migration Flyway, preservando as migrations anteriores.

A evolução inclui as estruturas principais:

### `areas_comuns`

Representa as áreas disponíveis para reserva.

Principais informações:

- identificador;
- nome;
- descrição;
- indicador de área ativa.

### `reservas`

Representa a solicitação e seu ciclo de vida.

Relacionamentos principais:

- uma reserva pertence a uma área comum;
- uma reserva pertence a um morador.

Principais informações:

- data e hora de início;
- data e hora de fim;
- estado da reserva;
- motivo de negativa, quando aplicável;
- data de criação.

Estados utilizados:

- `SOLICITADA`;
- `APROVADA`;
- `NEGADA`;
- `CANCELADA`.

### `historico_reservas`

Registra mudanças relevantes no ciclo de vida da reserva.

Relacionamentos principais:

- pertence a uma reserva;
- referencia o usuário responsável pela ação.

Registra, entre outros dados:

- estado anterior;
- novo estado;
- motivo associado à alteração;
- data e hora da alteração.

### 3.1. Relações adicionadas e justificativas

O diagrama relacional foi atualizado para representar somente relações que existem de fato no modelo de dados, ou seja, relações sustentadas por chaves estrangeiras.

As novas relações são:

#### `areas_comuns` 1:N `reservas`

A tabela `reservas` possui a chave estrangeira `area_comum_id`, que referencia `areas_comuns.id`.

Isso representa que cada reserva pertence a uma única área comum, enquanto uma mesma área comum pode possuir várias reservas ao longo do tempo.

Essa relação também é necessária para que a regra de conflito seja aplicada por área: duas reservas só entram em conflito quando pertencem à mesma área comum e possuem intervalos sobrepostos nas condições definidas pela funcionalidade.

#### `moradores` 1:N `reservas`

A tabela `reservas` possui a chave estrangeira `morador_id`, que referencia `moradores.id`.

Cada reserva pertence ao morador que realizou a solicitação, enquanto um mesmo morador pode realizar várias solicitações de reserva.

Essa relação é importante também para a regra de visibilidade e autorização, pois o morador deve consultar e cancelar somente as próprias reservas.

#### `reservas` 1:N `historico_reservas`

A tabela `historico_reservas` possui a chave estrangeira `reserva_id`, que referencia `reservas.id`.

Cada registro de histórico pertence a uma única reserva, enquanto uma reserva pode possuir vários registros ao longo do seu ciclo de vida, por exemplo na criação, aprovação, negativa ou cancelamento.

O histórico foi mantido separado da própria reserva porque ele representa uma sequência de mudanças, enquanto a tabela `reservas` guarda o estado atual da solicitação.

#### `usuarios` 1:N `historico_reservas`

A tabela `historico_reservas` possui a chave estrangeira `usuario_id`, que referencia `usuarios.id`.

Essa relação identifica qual usuário foi responsável pela ação que provocou a alteração registrada no histórico.

O relacionamento é feito com `usuarios`, e não diretamente com `administradores` ou `moradores`, porque ações que geram histórico podem ser realizadas por perfis diferentes. Um administrador pode aprovar, negar ou cancelar uma reserva, enquanto o próprio morador também pode realizar um cancelamento permitido.

Isso não significa que o usuário cria diretamente o registro de histórico. O registro é criado pelo sistema como consequência da ação realizada pelo usuário. Por esse motivo, no diagrama a relação deve ser entendida como **usuário responsável pela ação registrada**, e não como uma relação genérica de criação do histórico.

### 3.2. Relações que não foram adicionadas ao diagrama

Não foi criada uma relação direta entre `administradores` e `areas_comuns`.

Embora somente o administrador possa cadastrar, alterar ou desativar áreas comuns, essa é uma regra de autorização da aplicação. A tabela `areas_comuns` não possui uma chave estrangeira para `administradores`, portanto não existe uma relação relacional entre essas tabelas no banco de dados.

Pelo mesmo motivo, também não foram adicionadas relações diretas como:

- administrador aprova reserva;
- administrador nega reserva;
- administrador cancela reserva;
- colaborador participa de reserva.

Esses comportamentos pertencem às regras da aplicação e ao controle de acesso, não ao modelo relacional. Quando uma ação administrativa precisa ser registrada, o responsável fica identificado por `historico_reservas.usuario_id`.

Dessa forma, o diagrama permanece fiel ao banco de dados: ele mostra somente as relações persistidas por chaves estrangeiras e evita representar regras funcionais como se fossem vínculos físicos entre tabelas.

O banco continua podendo ser criado do zero pela sequência completa de migrations Flyway.

---

## 4. Testes e evidências

### 4.1. Suíte completa

Data da evidência: **01/10/2026**

Comando utilizado:

```bash
mvn clean verify
```

Resultado final:

```text
Tests run: 87
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A suíte inclui testes unitários, testes de autorização, testes web já existentes e testes de integração com PostgreSQL.

### 4.2. Cobertura unitária da funcionalidade

O percentual foi calculado especificamente sobre os principais serviços novos/alterados da funcionalidade:

- `ReservaService`;
- `AreaComumService`.

Comando utilizado:

```bash
mvn clean test \
  -Dtest=ReservaServiceTest,AreaComumServiceTest,ReservaAuthorizationTest \
  jacoco:report
```

Consulta da evidência:

```bash
grep -E 'ReservaService|AreaComumService' \
  target/site/jacoco/jacoco.csv
```

Critério utilizado: **cobertura de linhas**.

Resultado:

- linhas cobertas: **152**;
- linhas totais: **193**;
- cobertura: **78,76%**.

Suíte utilizada nessa medição:

```text
Tests run: 27
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A medição é restrita ao escopo informado e não usa a cobertura global do projeto como substituto.

### 4.2.1. Escopo da medição e exclusões

A medição unitária foi concentrada em `ReservaService` e `AreaComumService`, onde estão centralizadas as principais regras de negócio da funcionalidade.

Interfaces de repository não foram utilizadas no cálculo unitário; comportamentos de persistência, consultas de conflito e concorrência foram exercitados nos testes de integração com PostgreSQL.

Entidades JPA e a migration representam principalmente estrutura e mapeamento de dados, enquanto JSP, CSS e JavaScript não fazem parte da instrumentação de cobertura de linhas do JaCoCo.

Os controllers também não foram utilizados como base do percentual informado. As regras críticas de autorização e negócio foram exercitadas diretamente nos serviços e nos testes específicos da funcionalidade.

Essas exclusões se referem apenas ao cálculo percentual de cobertura unitária e não à exclusão das regras críticas dos testes.

### 4.3. Cenários críticos cobertos

Foram testados explicitamente, entre outros:

- criação válida de reserva;
- intervalo inválido;
- data/hora ausente;
- horário inicial já ocorrido;
- área inativa;
- aprovação válida;
- conflito com reserva aprovada;
- solicitação pendente sem bloquear outra aprovação;
- reservas sobrepostas em áreas diferentes;
- aprovação simultânea concorrente;
- negativa com motivo;
- rejeição de negativa sem motivo;
- cancelamento pelo próprio morador;
- cancelamento administrativo;
- cancelamento após o início;
- tentativa de cancelamento por outro morador;
- estados terminais;
- consulta protegida de reserva de outro morador;
- autorização de `MORADOR`, `ADMINISTRADOR` e `COLABORADOR`;
- preservação de reserva existente após desativação da área.

### 4.4. Teste de concorrência

A concorrência foi validada com PostgreSQL real executado por Testcontainers.

O teste cria reservas conflitantes da mesma área e executa aprovações concorrentes. O resultado esperado e validado é que somente uma delas possa terminar aprovada.

Essa evidência também valida a estratégia de lock pessimista utilizada na aprovação.

---

## 5. Limitações conhecidas e melhorias futuras

### 5.1. Organização por feature

O projeto atual é organizado principalmente por camadas técnicas.

Uma melhoria futura que considero válida seria avaliar uma organização por feature/domínio para funcionalidades novas, agrupando os componentes relacionados a reservas de forma mais próxima. A proposta não implica que a arquitetura atual esteja incorreta, mas pode facilitar navegação e manutenção conforme o sistema cresce.

### 5.2. Acentuação nas JSPs

Durante o desenvolvimento foram percebidas limitações relacionadas à exibição ou manutenção de textos com acentuação em algumas JSPs.

Uma melhoria futura seria revisar de forma global o tratamento de encoding dessas páginas, sem limitar a correção apenas à funcionalidade de reservas.

### 5.3. Reservas que atravessam dias

O fluxo atual trabalha com data e intervalo de início e fim.

Uma regra que eu definiria de forma explícita em uma evolução futura é se uma reserva pode atravessar a meia-noite e terminar em outro dia.

Considero válido avaliar a restrição para que início e fim pertençam ao mesmo dia, ou, caso reservas entre dias sejam permitidas, definir claramente como devem aparecer no calendário e como a disponibilidade deve ser apresentada.

Essa definição não foi adicionada como nova regra nesta entrega porque não estava especificada no desafio.

---

## 6. Recorte de escopo da implementação

A implementação foi mantida concentrada na funcionalidade de áreas comuns e reservas. Não foram realizadas refatorações amplas no módulo existente de chamados ou na arquitetura geral do projeto, porque essas alterações não eram necessárias para atender aos requisitos do desafio e aumentariam o impacto sobre código já existente.

Também não foram adicionadas regras de negócio que não estavam definidas no enunciado, como restringir obrigatoriamente uma reserva ao mesmo dia ou impor duração mínima ou máxima. Esses pontos foram mantidos como possíveis alinhamentos futuros em vez de serem assumidos durante a implementação.

---

## 7. Pontos que eu teria alinhado antes da implementação

Durante o desenvolvimento, alguns pontos de negócio se mostraram pouco detalhados no enunciado. Em um cenário real, eu teria buscado esclarecer esses pontos antes da implementação para evitar assumir regras que não estavam explicitamente definidas.

1. Uma reserva pode começar em um dia e terminar no dia seguinte, ou início e fim devem obrigatoriamente pertencer à mesma data?
2. O cancelamento realizado pelo administrador também deve exigir um motivo, ou essa obrigatoriedade se aplica somente à negativa da solicitação?
3. Quando o requisito fala em retirar ou desativar uma área comum, a retirada deve ser apenas lógica, preservando o cadastro, ou existe algum cenário em que a exclusão física seja permitida?
4. Na consulta de disponibilidade, uma solicitação `SOLICITADA` de outro morador deve ser apresentada apenas como um intervalo pendente, sem expor informações sobre quem realizou a solicitação?

---

## 8. Uso de inteligência artificial

Foram utilizadas ferramentas de IA como apoio ao desenvolvimento, principalmente ChatGPT e GitHub Copilot.

A IA foi usada tanto como assistente no processo quanto como recurso de aprendizado. Como eu possuía menos familiaridade com partes da stack Java/Spring Boot/JSP e com alguns tipos de teste utilizados no projeto, em vários momentos pedi explicações sobre conceitos, funcionamento do código existente e formas de implementar ou validar determinado comportamento.

O objetivo não foi apenas obter código pronto. Em diferentes etapas, a IA foi usada para:

- explicar partes do projeto existente;
- ajudar a entender conceitos de Spring Boot, JPA, JSP e testes;
- auxiliar na interpretação dos requisitos do desafio;
- sugerir formas de testar cenários críticos;
- revisar alterações e possíveis impactos fora do escopo;
- ajudar na leitura dos resultados de Maven e JaCoCo;
- apoiar a organização da documentação final.

As sugestões recebidas eram analisadas antes de serem mantidas no projeto.

### 8.1. Sugestões aceitas

Entre as sugestões aceitas, após revisão, estão:

- uso de PostgreSQL real com Testcontainers para validar a concorrência;
- uso de lock pessimista para proteger a aprovação simultânea de reservas conflitantes;
- criação de testes específicos para autorização dos perfis envolvidos;
- inclusão de testes para áreas diferentes com horários sobrepostos, estados terminais e desativação de área com reservas existentes.

Essas sugestões foram mantidas porque estavam diretamente relacionadas aos requisitos e puderam ser validadas pelos testes.

### 8.2. Sugestões modificadas

Algumas sugestões foram adaptadas antes de serem utilizadas.

Um exemplo foi o teste de concorrência. Uma abordagem inicial permitia que o teste pudesse ser ignorado dependendo do ambiente. A solução final foi ajustada para que o cenário de concorrência fosse executado de forma reproduzível com Testcontainers, já que esse comportamento é exigido pelo desafio.

Outro exemplo foi a referência temporal. Em vez de manter uma configuração de timezone específica da funcionalidade de reservas, a implementação foi ajustada para reutilizar a referência temporal global já existente na aplicação.

### 8.3. Sugestões rejeitadas

Sugestões que alteravam partes antigas do sistema sem necessidade direta para a funcionalidade de reservas foram rejeitadas ou revertidas.

Também foram evitadas refatorações mais amplas apenas por preferência arquitetural, porque a intenção foi manter o menor impacto possível sobre o sistema existente e trabalhar somente no que era necessário para o desafio.

### 8.4. Como as sugestões da IA foram avaliadas e validadas

As sugestões fornecidas pelas ferramentas de IA não foram aplicadas automaticamente.

Antes de aceitar uma sugestão, eu comparava o que estava sendo proposto com o código existente e com os requisitos do desafio, buscando entender por que aquela alteração seria necessária e qual impacto ela poderia causar.

Quando a sugestão envolvia código, eu revisava a alteração e o `git diff` para verificar se ela estava limitada ao escopo esperado. Depois disso, o comportamento era validado com o tipo de teste adequado ao caso.

Em situações específicas, também foram usados recursos como PostgreSQL com Testcontainers para validar concorrência e JaCoCo para conferir a cobertura exigida.

Sugestões que eu não entendia suficientemente, que alteravam comportamento antigo sem necessidade ou que extrapolavam o escopo eram questionadas, modificadas ou rejeitadas.

### 8.5. Decisões não delegadas à IA

As decisões finais de negócio, segurança, escopo e aceite permaneceram sob minha responsabilidade.

Entre elas:

- manter `SOLICITADA` como estado pendente que não ocupa o horário;
- considerar conflito apenas entre reservas aprovadas da mesma área;
- preservar reservas existentes ao desativar uma área;
- restringir o morador às próprias reservas;
- exigir motivo de cancelamento na interface administrativa por uma decisão de rastreabilidade;
- evitar alterações desnecessárias no módulo de chamados;
- decidir quais sugestões de IA seriam aceitas, modificadas ou rejeitadas;
- validar os resultados dos testes antes de considerar uma etapa concluída.

### 8.6. Interações relevantes com IA

Abaixo estão exemplos resumidos e sanitizados de interações relevantes. Não representam o histórico completo de prompts.

**Interação 1 — entendimento da stack e do código existente**

Foi solicitado auxílio para entender como a funcionalidade deveria se encaixar em um projeto Spring Boot com JSP, incluindo a responsabilidade de controllers, services, repositories e páginas JSP.

A explicação foi usada como apoio para compreender melhor a estrutura existente antes de realizar alterações.

**Interação 2 — concorrência de aprovações**

Foi discutido como impedir duas aprovações simultâneas para reservas conflitantes da mesma área.

Entre as alternativas analisadas, foi adotado o uso de lock pessimista por área. A solução foi posteriormente validada com um teste concorrente em PostgreSQL real.

**Interação 3 — cobertura e cenários obrigatórios**

Foi solicitado auxílio para interpretar a exigência de cobertura mínima e identificar quais cenários deveriam ser testados independentemente do percentual.

A partir dessa análise, foi feita uma separação entre a medição de cobertura unitária e os testes obrigatórios de autorização, conflito, tempo, estados terminais e concorrência.

**Interação 4 — revisão de escopo**

Durante revisões de código, surgiram sugestões de alterações em partes antigas do sistema que não eram necessárias para a funcionalidade de reservas.

Essas sugestões foram rejeitadas ou revertidas para evitar impacto desnecessário no comportamento existente.

**Interação 5 — referência temporal**

Foi analisado o uso de data e hora no fluxo de reservas.

Ao revisar o projeto, foi identificado que a aplicação já possuía uma configuração global de timezone. A solução foi então ajustada para reutilizar essa referência em vez de manter uma configuração duplicada na funcionalidade de reservas.

---

## 9. Observação sobre o histórico de commits

Alguns commits aparecem concentrados em um intervalo curto porque adiei o registro das alterações no Git enquanto concluía e validava blocos do desenvolvimento localmente.

Parte relevante do código já estava implementada antes desses commits; portanto, as datas refletem o momento em que as alterações foram organizadas e registradas no repositório, e não todo o período em que foram desenvolvidas.

---

## 10. Considerações finais

A implementação buscou manter o menor impacto possível sobre o sistema existente, adicionando a funcionalidade de reservas sem modificar o comportamento anterior dos chamados.

As principais preocupações da solução foram:

- controle de acesso;
- preservação de histórico;
- consistência temporal;
- proteção contra conflitos;
- concorrência em decisões administrativas;
- rastreabilidade por testes;
- manutenção do escopo do desafio.

As melhorias futuras registradas neste documento representam pontos observados durante o desenvolvimento, mas não foram adicionadas à entrega por não fazerem parte dos requisitos atuais.
