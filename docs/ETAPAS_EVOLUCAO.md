# Evolução da PoC para o TCC II

## Etapa 1 — Refatoração do núcleo

Status: **implementada neste pacote**.

Objetivo: preparar a PoC para receber novos modelos, dados externos e uma interface sem alterar a regra escalar atualmente validada.

### Alterações

- criada `SimulationConfiguration`, configuração imutável por cenário;
- `RedeSocial` passou a aceitar explicitamente a configuração;
- `GeradorTopologia` passou a receber a configuração do cenário;
- topologias Small-World e Scale-Free deixaram de depender diretamente dos parâmetros globais quando recebem uma configuração;
- criado `EmotionPropagationModel`;
- criado `FanEmotionModel`, encapsulando a equação escalar atualmente utilizada;
- `UsuarioAgent` passou a depender da abstração do modelo emocional;
- `AgenteControle` passou a criar e propagar a configuração do cenário;
- `DatabaseUtil` passou a aceitar configuração para registro da simulação/conexão;
- `LancadorSimulacao` deixou de assumir 10 agentes no Sniffer;
- adicionados testes unitários para configuração e modelo emocional;
- mantida `ConfiguracaoPoc` como camada de compatibilidade durante a migração.

### O que deliberadamente NÃO foi alterado nesta etapa

- representação das quatro emoções de Fan;
- competição emocional;
- limiar de influência;
- integração com banco externo;
- topologia Modular;
- calibração por dados empíricos;
- API/interface gráfica ou Web;
- esquema definitivo de persistência.

Esses itens serão tratados nas etapas seguintes para evitar alterar simultaneamente o núcleo já funcional e a semântica experimental.

## Etapa 2 — Modelo emocional multidimensional

Próxima etapa proposta:

- `Emotion` com `ANGER`, `DISGUST`, `JOY` e `SADNESS`;
- `EmotionState` vetorial;
- atualização multidimensional;
- limiar e competição emocional;
- testes matemáticos do modelo;
- comparação com o comportamento da Etapa 1.

## Etapa 3 — Topologias

- consolidar Small-World;
- validar Scale-Free;
- implementar Modular;
- parametrizar pesos uniformes, aleatórios e derivados do grau.

## Etapa 4 — Dados externos

- definir fonte;
- criar `ExternalDataSource`;
- adaptadores;
- normalização;
- construção da rede a partir de dados;
- preparação para calibração.

## Etapa 5 — Métricas e persistência

- distribuição emocional;
- velocidade de propagação;
- polarização;
- clusters;
- armazenamento por ciclo;
- experimentos e cenários.

## Etapa 6 — Interface

- configuração;
- execução;
- acompanhamento;
- visualização da rede;
- gráficos;
- comparação de experimentos.


## Etapa 2 — Estado emocional multidimensional

A Etapa 2 substitui o estado emocional operacional do `UsuarioAgent` por `EmotionState`,
com quatro dimensões:

- ANGER;
- DISGUST;
- JOY;
- SADNESS.

O `FanEmotionModel` passa a receber e devolver `EmotionState` e aplica a regra de
persistência, influência ponderada e decaimento separadamente para cada emoção.

### Observação metodológica

A proposta do TCC estabelece as quatro categorias emocionais, a influência ponderada,
a persistência e o decaimento. A representação vetorial implementada nesta etapa é uma
decisão de engenharia para permitir a evolução do modelo. A definição matemática
definitiva de competição/normalização entre as quatro emoções não foi inferida como
sendo parte do modelo original e deverá ser validada na etapa de revisão bibliográfica
e dos dados de Fan, Xu e Zhao (2018).

### Compatibilidade

A tabela `estado_emocional` mantém `valor` e `categoria` por compatibilidade com a PoC.
A partir desta etapa também são persistidos:

- `anger`;
- `disgust`;
- `joy`;
- `sadness`.

`valor` representa a maior intensidade e `categoria` representa a emoção dominante.

### Semente

A configuração passa a permitir selecionar a emoção da semente por meio de
`SimulationConfiguration.emocaoSemente`, com `ANGER` como padrão.

## Etapa 3 — Topologias de rede

A Etapa 3 amplia o gerador de redes para suportar três famílias de topologia:

- Small-World;
- Scale-Free;
- Modular.

A seleção é feita por `TipoTopologia` e `GeradorTopologiaFactory`, sem alteração da lógica dos agentes.

### Novos parâmetros de configuração

- `modularComunidades`: quantidade de comunidades;
- `modularProbabilidadeInterna`: probabilidade de conexão dentro da comunidade;
- `modularProbabilidadeExterna`: probabilidade de conexão entre comunidades.

A topologia modular distribui os agentes de forma balanceada entre as comunidades e utiliza probabilidades distintas para conexões intra e intercomunitárias.

### Validação

Foi realizada compilação isolada das classes de topologia e um teste de fumaça com 30 agentes. As três topologias foram geradas com a mesma semente e produziram redes sem nós isolados no cenário testado:

- Small-World: 60 arestas;
- Scale-Free: 57 arestas;
- Modular: 113 arestas.

A validação completa via Maven deverá ser executada no ambiente de desenvolvimento que possua Maven instalado.


## Etapa 3.5 — Integração estrutural com dados reais do Twitter

Foi adicionada uma fonte de dados real baseada no conjunto Twitter Social Circles. O sistema agora consegue carregar uma rede de ego a partir de uma pasta extraída ou diretamente do arquivo `.tar.gz`, preservar os identificadores originais dos usuários, carregar atributos `.feat`/`.featnames` e utilizar as arestas reais como topologia da simulação. Esta etapa não realiza inferência emocional a partir das features; essa decisão foi deliberadamente deixada para a etapa de dados/calibração.

Componentes principais:
- `FonteDados`;
- `TwitterDatasetLoader`;
- `TwitterNetworkData`;
- `TopologiaFonteExterna`;
- suporte a `twitterDatasetPath`, `twitterEgoId` e `twitterSeedNodeId` em `SimulationConfiguration`;
- persistência de `fonte_dados`, `twitter_ego_id` e `id_externo`.

Validação realizada com o arquivo fornecido:
- ego `11681802`;
- 134 nós identificados por `.edges`/`.feat`;
- 718 arestas únicas;
- 134 vetores de atributos;
- 596 nomes de atributos.

## Etapa 4.9 — Dados para a interface

Adicionado o pacote `br.unb.poc.relatorio`, responsável por converter a topologia da rede e o histórico de estados emocionais em um contrato de dados único e independente de JADE/PostgreSQL/HTML:

- `RelatorioExport`: monta um documento JSON com metadados do cenário, nós, arestas e a série de estados por ciclo (com médias agregadas por emoção);
- `ColetorResultados`: coletor em memória, thread-safe, plugado em `UsuarioAgent.persistirEstado` e `AgenteControle.setup` de forma aditiva (nunca lança exceção, nunca altera o comportamento existente). Existe porque o esquema atual do PostgreSQL (`simulacao`/`estado_emocional`) não armazena a topologia da rede — apenas o estado por ciclo — então reconstruir o grafo a partir de uma consulta posterior ao banco não é possível sem essa camada;
- `SimuladorDireto`: execução síncrona de referência do mesmo `FanEmotionModel`, útil para alimentar a interface sem depender da plataforma JADE nem do banco (por exemplo, em ambientes de desenvolvimento sem essa infraestrutura disponível).

### Observação metodológica

Ao inspecionar `AgenteControle.criarAgentesUsuario`, foi identificado que, quando a fonte de dados é o Twitter, o estado emocional inicial de **todos** os agentes — inclusive o nó-semente escolhido por `twitterSeedNodeId` — é substituído integralmente pela inferência da heurística (Etapa 4.5). Os parâmetros `estadoInicialSemente`/`estadoInicialPadrao` continuam vigentes apenas na topologia sintética. Esse comportamento foi preservado nesta etapa (inclusive no modo de demonstração da interface) por não caracterizar um defeito óbvio — pode ser uma decisão deliberada de dar prioridade aos dados observados sobre a semente configurada manualmente — mas fica registrado como ponto a validar com o orientador antes da calibração final.

## Etapa 5.0 — Interface gráfica

Adicionados os pacotes `br.unb.poc.interfaces` e `br.unb.poc.demo`:

- `DashboardServer`: servidor HTTP embutido (apenas classes do JDK, sem servlet container ou dependência nova), expõe `GET /` (dashboard), `GET /api/resultado.json` (dados da Etapa 4.9) e `GET /api/executar` (reexecuta um cenário com novos parâmetros, quando aplicável);
- `DashboardHtml`: página autocontida (HTML/CSS/JS sem CDN) com grafo da rede colorido pela emoção dominante por ciclo, gráfico de médias emocionais ao longo dos ciclos, controle de reprodução por ciclo e formulário para rodar novos cenários de demonstração;
- `DatasetDemo`: gera, em disco temporário, uma rede sintética no formato do Twitter Social Circles (comunidades com hashtags emocionalmente marcadas), reaproveitando o `TwitterDatasetLoader` real — não requer o dataset original para demonstrar a interface;
- `LancadorInterface`: ponto de entrada do modo de demonstração (`java ... br.unb.poc.plataforma.LancadorInterface [porta] [numAgentes] [numCiclos] [alpha] [delta] [semente]`), que gera a rede, infere os estados iniciais (Etapa 4.5), executa a propagação (`SimuladorDireto`) e sobe o `DashboardServer`.

### Validação realizada

O servidor foi executado localmente e testado via requisições HTTP diretas:

- `GET /` retornou 200 com o dashboard;
- `GET /api/resultado.json` retornou 200 com um documento JSON coerente (30 nós, 97 arestas, 16 ciclos no cenário padrão);
- `GET /api/executar?numAgentes=12&numCiclos=5&alpha=0.5&delta=0.1&semente=7` recalculou o cenário e retornou um novo JSON consistente com os parâmetros informados.

Também foi validado `LancadorInterface --twitter <caminho> <egoId> [seedNodeId]`, o mesmo caminho de código usado com o dataset real (apenas trocando o gerador sintético pelo `TwitterDatasetLoader` real): com um dataset de 5 nós contendo hashtags `#happy`/`#angry`/`#sad`, a inferência da Etapa 4.5 e a propagação retornaram exatamente os estados esperados (nó com `#happy` → alegria 0,70; nó com `#angry` → raiva 0,70; nó com ambas as features → empate resolvido pela ordem do enum; nós com `#sad` → tristeza 0,70), confirmando a cadeia completa dado real → inferência → propagação → JSON → interface.

Essa validação cobre a cadeia completa dado → propagação → JSON → interface, mas não substitui a execução real via JADE/PostgreSQL (Etapas 4.6–4.8), que depende de infraestrutura (banco de dados, ambiente com display para a GUI do JADE) não disponível neste ambiente de verificação.

### Como visualizar o resultado de uma execução real

Como `ColetorResultados` já é alimentado por `UsuarioAgent` durante uma execução real via `LancadorSimulacao`, é possível montar o mesmo relatório a partir de uma simulação JADE em andamento:

```java
ColetorResultados.Instantaneo instantaneo =
        ColetorResultados.obterInstantaneo(AgenteControle.ultimaSimulacaoId);
String json = RelatorioExport.paraJson(
        instantaneo.rede(), configuracao, instantaneo.historico(), indiceSemente);
```

Uma integração completa (subir o `DashboardServer` automaticamente a partir de `LancadorSimulacao`, atualizando-o periodicamente durante a execução) fica como próximo passo, para não alterar mais do que o necessário na pipeline já validada nesta etapa.

## Inspeção do dataset real (twitter_tar.gz)

O arquivo `twitter_tar.gz` fornecido foi inspecionado e confirmado como o dataset real do Stanford SNAP "Twitter Social Circles" (973 redes egocêntricas, ~277 MB descompactado), incluindo o ego `11681802` já referenciado nesta documentação.

Testes realizados com o loader real (`TwitterDatasetLoader`) sobre o ego `11681802`:

- **134 nós / 718 arestas** — confirma exatamente o resultado do smoke test já documentado;
- **Cobertura do vocabulário emocional da Etapa 4.5**: apenas **5 dos 134 nós (~3,7%)** possuem alguma feature reconhecida pela `HeuristicaTwitter` (`#love`, `#winning`, `#fail`). Isso é esperado e correto — as features do dataset são majoritariamente menções (`@usuario`), termos técnicos (`#Android`, `#App`) e hashtags não emocionais; a heurística é deliberadamente conservadora e não deve inferir emoção de termos ambíguos (ex.: `#FarCry3` corretamente não confundido com "cry"). O restante dos nós permanece neutro por falta de sinal, não por erro do loader ou da heurística.

### Bug encontrado e corrigido: rótulo de "emoção dominante" em estados neutros

`EmotionState.getDominantEmotion()` (código anterior a esta sessão) resolve empates pela ordem do enum, com `ANGER` declarado primeiro. Um estado neutro (todas as intensidades em zero — o caso de ~96% dos nós no dataset real) é tecnicamente um empate entre as quatro emoções, então o método retornava `ANGER` mesmo sem nenhum sinal emocional real. Esse valor já era persistido na coluna `categoria` de `estado_emocional` (Etapa 4.8) e seria usado pela interface (Etapa 4.9/5.0), o que faria a maioria dos usuários aparecer como "com raiva" apenas por falta de dado.

Importante: esse problema afeta somente o **rótulo derivado** para exibição/persistência — não afeta os valores numéricos de `anger/disgust/joy/sadness` nem a propagação do `FanEmotionModel`, que opera diretamente sobre as quatro intensidades contínuas, sem depender do rótulo dominante.

Correção aplicada (mínima, sem alterar a assinatura de `getDominantEmotion()` para não quebrar código existente):

- `EmotionState.isNeutro()`: verdadeiro quando todas as intensidades são zero;
- `EmotionState.getRotuloDominante()`: retorna `"NEUTRO"` nesse caso, ou o nome da emoção dominante caso contrário;
- `DatabaseUtil.salvarEstado` e `RelatorioExport` passaram a usar `getRotuloDominante()` em vez de `getDominantEmotion().name()`;
- o dashboard (Etapa 5.0) ganhou uma cor neutra (cinza) e uma entrada na legenda para esse caso.

Validado após a correção, no mesmo ego `11681802`: distribuição inicial `{NEUTRO≈128, ANGER≈3, JOY≈3}` (antes da correção, os ≈128 neutros apareciam como `ANGER`).

### Resultado da propagação (10 ciclos, alpha=0.6, delta=0.02) — achado substantivo, não um bug

Rodando `LancadorInterface --twitter <dataset> 11681802 x 8095 10 0.6 0.02` até o fim: a distribuição de rótulo dominante evolui de `{NEUTRO:128, ANGER:3, JOY:3}` (ciclo 0) para `{ANGER:103, JOY:21, NEUTRO:10}` (ciclo 10) — ou seja, a raiva se propaga por uma fração maior da rede do que a alegria, mesmo com o mesmo número de nós-semente (3 e 3) para cada emoção. Isso é um resultado da topologia real (os nós com `#fail`/`#angry` parecem ter maior centralidade/alcance nessa rede específica), não um artefato do código — mas é um ponto que vale a pena investigar/relatar no TCC (ex.: comparar grau e centralidade dos nós-semente de cada emoção).

## Validação do pipeline real (JADE + PostgreSQL) contra o dataset do Twitter

Rodado `LancadorSimulacao --twitter <dataset> 11681802` de ponta a ponta (134 agentes, 20 ciclos, PostgreSQL real) para validar as mudanças desta sessão. Dois problemas reais foram encontrados e corrigidos:

### Bug de concorrência no `DatabaseUtil`

Sintoma observado na primeira execução real: 58 de 285 linhas do log eram erros `This connection has been closed` / `An I/O error occurred while sending to the backend` durante a persistência de estados.

Causa: `DatabaseUtil` mantém uma única `Connection` JDBC estática, compartilhada por todos os `UsuarioAgent` — que rodam como threads concorrentes dentro da mesma JVM (agentes JADE). Uma `Connection` não é thread-safe; escritas simultâneas de dezenas de agentes corrompiam o protocolo da conexão.

Correção: os métodos de `DatabaseUtil` que tocam a conexão (`getConexao`, `inicializarEsquema`, `registrarSimulacao`, as três variantes de `salvarEstado`, `fecharConexao`) passaram a ser `synchronized`, serializando as escritas no banco. Isso é aceitável para o volume desta PoC e evita abrir uma conexão por agente (o que estouraria o limite padrão de conexões do PostgreSQL com datasets maiores).

Validado após a correção, na mesma execução (134 agentes × 21 ciclos = 0 a 20): **2.814 linhas gravadas em `estado_emocional`, 0 erros de persistência**. A distribuição em `ciclo=0` (`NEUTRO=129, JOY=3, ANGER=2`) bateu exatamente com a mesma execução via `SimuladorDireto`/`LancadorInterface`, confirmando que os dois caminhos de execução (agentes JADE reais e o modo de referência síncrono da interface) produzem resultados consistentes entre si.

### `init_db.sql` desatualizado

O script `src/main/resources/init_db.sql`, usado para configuração manual do banco (documentado no `README.md`), não incluía a coluna `id_externo` na tabela `estado_emocional`, embora tivesse um `COMMENT ON COLUMN` referenciando essa coluna — um erro de sintaxe que interrompia a execução do script (`ERROR: column "id_externo" of relation "estado_emocional" does not exist`). Esse bug já existia antes desta sessão (o método `DatabaseUtil.inicializarEsquema()`, chamado automaticamente pela aplicação, sempre esteve correto — só o script standalone estava desatualizado). Corrigido para incluir a coluna, mantendo os dois caminhos de inicialização do esquema consistentes.

### Redução do log da simulação real

Antes desta sessão, `UsuarioAgent` imprimia uma linha a cada mensagem ACL enviada/recebida entre vizinhos e a cada ciclo, além de uma linha por agente criado em `AgenteControle`. Em datasets pequenos (dezenas de agentes) isso já era bastante texto; no dataset real (134 agentes, 718 arestas) o volume tornava o terminal inutilizável — a mesma execução de 20 ciclos que hoje produz 285 linhas de log geraria, com o comportamento anterior, algo da ordem de dezenas de milhares de linhas (~718 arestas × 2 sentidos × 20 ciclos só nas mensagens `INFORM`, fora as linhas de ciclo por agente).

Correção: adicionado `SimulationConfiguration.isLogDetalhado()` (padrão `false`), usado para condicionar essas impressões por agente/por ciclo/por mensagem em `UsuarioAgent` e `AgenteControle`. Por padrão, o console agora mostra apenas as mensagens de marco (início, ID da simulação, contagem de agentes, conclusão) e eventuais erros. Para depuração ciclo a ciclo, `LancadorSimulacao` aceita a flag `--verbose` em qualquer posição dos argumentos (ex.: `--twitter <dataset> 11681802 --verbose`).

Validado: a mesma execução real (134 agentes, 20 ciclos) que antes geraria um log gigantesco agora produz 285 linhas totais, sendo 0 delas de spam `INFORM`/`Ciclo` — apenas mensagens de marco, avisos internos do JADE e (após a correção de concorrência) nenhum erro de persistência.

### Abertura automática do navegador e diagnóstico de porta

`LancadorInterface` agora tenta abrir automaticamente `http://localhost:<porta>/` no navegador padrão ao subir o dashboard, via `java.awt.Desktop` com fallback para `xdg-open`/`open`/`rundll32` conforme o sistema operacional; se nada funcionar, a URL é impressa de forma destacada no console para abertura manual. Além disso, `DashboardServer` agora detecta especificamente o caso de porta já em uso e informa isso com uma mensagem clara, em vez de um stack trace genérico de `BindException`.

## Causa raiz real de "o dashboard não abre": `-Dexec.mainClass` era ignorado pelo Maven

Analisando um log real enviado pelo usuário, ficou claro que o comando

```bash
mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorInterface -Dexec.args="--twitter ... 11681802 x 8080 20 0.6 0.02"
```

na verdade **nunca executou `LancadorInterface`** — o banner impresso, incluindo linhas adicionadas nesta própria sessão só em `LancadorSimulacao` ("Log: resumido...", "134 agentes criados..."), comprova que `LancadorSimulacao` rodou de qualquer forma, ignorando o `-Dexec.mainClass` da linha de comando.

Causa: o `pom.xml` configurava `<mainClass>br.unb.poc.plataforma.LancadorSimulacao</mainClass>` como um valor **fixo (texto literal)** dentro do `exec-maven-plugin`. Em Maven, um valor de configuração explicitamente definido no POM tem precedência sobre a propriedade `-D` equivalente passada na linha de comando — a propriedade só é usada quando o POM não define um valor fixo (ou referencia a própria propriedade via `${...}`).

Correção: adicionada a propriedade `exec.mainClass` em `<properties>`, com `LancadorSimulacao` como valor padrão, e o `exec-maven-plugin` passou a referenciar `${exec.mainClass}` em vez do texto fixo. Agora `mvn exec:java` sem argumentos continua rodando `LancadorSimulacao` (comportamento padrão preservado), e `-Dexec.mainClass=br.unb.poc.plataforma.LancadorInterface` na linha de comando passa a funcionar de verdade.

## Outros dois bugs encontrados no mesmo log real

Ainda a partir desse log (execução real, 134 agentes, dataset do Twitter):

- **`TickerBehaviour` sem `stop()`**: o agente de controle usa um `TickerBehaviour` para sinalizar o fim da simulação, mas nunca chama `stop()` dentro de `onTick()` — como `TickerBehaviour` é cíclico por padrão, a mensagem "Simulação concluída. Encerrando agentes..." (e o fechamento da conexão com o banco) repetia indefinidamente a cada `duracaoTotal` ms enquanto o container JADE seguisse de pé. No log enviado, apareceu 6 vezes. Corrigido adicionando `stop()` ao final de `onTick()`; validado que passou a aparecer exatamente 1 vez na mesma execução real.
- **Avisos `HTTPServer error: ... XMLCodec cannot access ... xerces ...`**: o MTP HTTP do JADE, usado para mensageria entre plataformas JADE distintas (não necessário nesta PoC, que roda tudo num único container local), falha em JDKs modernos por causa do sistema de módulos do Java bloquear acesso a uma classe interna do parser XML. Corrigido desabilitando o MTP HTTP (`Profile.NO_MTP = "true"`) em `LancadorSimulacao`, já que não é usado por esta PoC. Validado: 0 ocorrências desse aviso na mesma execução real (antes: dezenas).

Também corrigido, no mesmo log: o banner inicial mostrava "Agentes: 10" (valor padrão da topologia sintética) mesmo ao rodar com `--twitter`, antes do dataset real ser carregado — agora mostra "(definido pelo dataset do Twitter, carregado a seguir)" nesse caso.

Reexecutada a simulação completa após todas essas correções: **209 linhas de log** (contra 285 antes), **1** ocorrência de "Simulação concluída", **0** avisos `XMLCodec`, **0** erros de persistência, **2.814 linhas** em `estado_emocional` (134 agentes × 21 ciclos) — mesmo resultado numérico de antes, com log limpo.

## Análise comparativa: rede real x redes sintéticas

Adicionado o pacote `br.unb.poc.analise`, respondendo diretamente ao pedido de comparar redes sintéticas e a rede real do Twitter, não apenas visualizá-las isoladamente:

- `MetricasEstruturais`: grau médio/máximo/mínimo, densidade, coeficiente de agrupamento médio (Watts & Strogatz, 1998), distância média aproximada (com amostragem acima de 300 nós), número de componentes conexos e fração no maior componente;
- `MetricasPropagacao`: série de médias por emoção ciclo a ciclo, distribuição de rótulo dominante no início e no fim, e ciclo de convergência (primeira variação entre ciclos consecutivos abaixo de 0,001 em todas as emoções);
- `SementesComparaveis`: para tornar a comparação justa, conta quantos nós-semente por emoção existem na rede real (via `HeuristicaTwitter`) e distribui a **mesma quantidade** de sementes aleatoriamente nas redes sintéticas — mesmo tamanho de rede, mesma quantidade de sinal emocional injetado, variando apenas a topologia;
- `AnaliseComparativa`: orquestra a execução de 4 cenários — rede real do Twitter, Small-World, Scale-Free e Modular, todas com o mesmo número de nós da rede real — e calcula as métricas de cada um;
- `AnaliseExport`: serializa os 4 cenários em um único JSON para a interface.

Novo ponto de entrada `LancadorComparacao`, e uma segunda página no dashboard (`/comparacao`, com link a partir de `/`) mostrando: tabela de métricas estruturais lado a lado, gráfico de linhas com a evolução da "emoção de referência" (a que teve mais sementes na rede real) em cada cenário, gráfico de barras com o alcance final dessa emoção (% dos nós) por cenário, e uma tabela com a distribuição final completa por rótulo.

### Resultado observado (ego 11681802, 20 ciclos, alpha=0,6, delta=0,02, semente=42)

| Cenário | Nós | Arestas | Grau médio | Agrupamento | Dist. média | Sementes iniciais | Rótulo final |
|---|---|---|---|---|---|---|---|
| Twitter (real) | 134 | 718 | 10,72 | 0,358 | 2,63 | 2 ANGER / 3 JOY | 69 ANGER / 55 JOY / 10 NEUTRO |
| Small-World (sintética) | 134 | 268 | 4,00 | 0,251 | 4,43 | 2 ANGER / 3 JOY | 68 ANGER / 66 JOY |
| Scale-Free (sintética) | 134 | 265 | 3,96 | 0,117 | 3,09 | 2 ANGER / 3 JOY | 17 ANGER / 117 JOY |
| Modular (sintética) | 134 | 1674 | 24,99 | 0,568 | 2,04 | 2 ANGER / 3 JOY | 34 ANGER / 100 JOY |

Achado relevante para o TCC: com o **mesmo número de sementes** (menos sementes de raiva do que de alegria em todos os cenários), a raiva só ultrapassa a alegria no cenário da **rede real**. Nas três topologias sintéticas comparáveis, a alegria se espalha mais (ou empata, no caso Small-World). Isso indica que a propagação observada na rede real não decorre simplesmente da quantidade de sinal emocional inicial — é influenciada por alguma característica estrutural específica da rede real (possivelmente a posição/centralidade dos nós-semente de raiva nessa topologia específica, já que o coeficiente de agrupamento real, 0,358, é mais alto que o das redes Small-World e Scale-Free geradas com os parâmetros padrão). Fica como direção de investigação para a monografia: repetir a comparação com outras sementes aleatórias (o `semente` do `LancadorComparacao` controla isso) para verificar se o padrão se repete, e/ou medir a centralidade dos nós-semente especificamente.

Uso:

```bash
mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorComparacao \
  -Dexec.args="/caminho/para/o/dataset 11681802 8081 20 0.6 0.02 42"
```

## Correções de UX no dashboard, a partir de feedback com capturas de tela reais

- **Gráficos de linha "sumiam"**: tanto o gráfico principal ("Média de intensidade emocional") quanto o da página de comparação usavam uma escala fixa no eixo Y (0 a 1). Como a maioria dos nós fica neutra (poucas sementes emocionais espalhadas pela rede), a média geral costuma ficar na casa de 0,01–0,03 — ou seja, a linha ficava comprimida em poucos pixels perto do eixo X, parecendo "sem dados" mesmo quando a propagação estava acontecendo normalmente. Corrigido: os dois gráficos agora calculam o maior valor observado e escalam o eixo Y dinamicamente (com folga de 15%), exibindo o valor máximo no topo do eixo para dar noção de escala.
- **"Executar novo cenário" virou uma página própria** (`/executar`), em vez de ficar embutido no dashboard principal — a mesma lógica de separação já usada para `/comparacao`. A página detecta automaticamente se o cenário atual é de demonstração (mostra todos os campos: agentes, ciclos, alpha, delta, semente) ou o dataset real (mostra só ciclos/alpha/delta, já que a rede em si não muda); ao executar com sucesso, redireciona de volta para `/`.
- **"Reexecução não disponível neste modo"**: esse recurso só era ligado no modo de demonstração — no modo com dataset real (`--twitter`), `DashboardServer.definirReexecutor` nunca era chamado, então o botão sempre falhava. Corrigido: o modo real agora também tem um reexecutor, que roda a propagação de novo sobre a **mesma** rede e os **mesmos** estados iniciais já inferidos (só ciclos/alpha/delta mudam — a topologia e a inferência heurística não são refeitas a cada clique).

## Observabilidade de testes e análise de dados (finalização do sistema)

### Testes automatizados para as Etapas 4.9/5.0/análise

Até esta sessão, `EmotionState.isNeutro/getRotuloDominante` (a correção do bug de neutro exibido como raiva), `SimuladorDireto`, `RelatorioExport`, `MetricasEstruturais`, `MetricasPropagacao` e `SementesComparaveis` — ou seja, toda a lógica adicionada nas Etapas 4.9/5.0 e na análise comparativa — não tinham nenhum teste automatizado. Adicionados 17 novos testes JUnit (38 no total, contra 21 antes), todos independentes de PostgreSQL/JADE em execução:

- `EmotionStateNeutroTest`: fixa como regressão o bug do rótulo "ANGER" para estados neutros;
- `SimuladorDiretoTest`: rede totalmente neutra permanece neutra; uma semente se propaga para o vizinho conectado; nó isolado mantém seu estado;
- `RelatorioExportTest`: o JSON exportado tem os campos e contagens de nós/arestas/ciclos esperados;
- `MetricasEstruturaisTest`: valores exatos conhecidos analiticamente em grafos pequenos (triângulo: agrupamento=1, distância média=1; caminho de 3 nós: agrupamento=0, distância média=4/3; dois componentes: fração no maior componente=3/5);
- `MetricasPropagacaoTest`: média por ciclo, distribuição inicial/final e detecção do ciclo de convergência;
- `SementesComparaveisTest`: contagem por emoção dominante e distribuição sem repetir nó, inclusive quando a contagem pedida excede o tamanho da rede.

Validado rodando a suíte completa com JUnit 5 (console standalone) nesta sessão: **38/38 testes passando**.

### Cobertura de testes e CI

Adicionado o plugin `jacoco-maven-plugin` ao `pom.xml` — `mvn test jacoco:report` gera um relatório HTML (`target/site/jacoco/index.html`) mostrando linha a linha o que está coberto pelos testes, dando visibilidade real sobre a cobertura em vez de "confiar" que os testes existem.

Adicionado `.github/workflows/ci.yml`: roda `mvn verify` (compilação + testes + cobertura) a cada push/pull request, publicando os relatórios de teste (Surefire) e cobertura (JaCoCo) como artefatos do workflow. Não precisa de PostgreSQL no CI, porque nenhum teste atual depende de uma conexão de banco ativa.

### Toolkit de análise de dados em Python (`analise/`)

O script Python já existente no projeto (`analise_emocoes.py`) era da primeira versão da PoC: só lia a coluna `valor` do modelo emocional escalar antigo (a PoC hoje usa quatro dimensões — anger/disgust/joy/sadness) e quebrava com `IndexError` para redes com mais de 10 agentes, por usar paletas de cores/estilos/marcadores de tamanho fixo. Reescrito como um toolkit modular:

- `db.py` — conexão e consultas ao PostgreSQL, retornando `pandas.DataFrame`;
- `graficos.py` — evolução emocional média (± desvio-padrão), distribuição do rótulo dominante por ciclo (área empilhada), trajetórias individuais de uma amostra de agentes — todos funcionando para qualquer número de agentes;
- `estatisticas.py` — resumo descritivo por ciclo, e um teste qui-quadrado comparando a distribuição de rótulos entre o primeiro e o último ciclo;
- `comparar_json.py` — lê o `comparacao.json` do dashboard (`/api/comparacao.json`) ou do `LancadorComparacao` e gera gráficos comparativos + um teste qui-quadrado entre os 4 cenários (rede real e as três sintéticas);
- `relatorio_simulacao.py` — CLI principal, gera o relatório completo de uma simulação do banco;
- `analise_emocoes.py` — mantido como atalho de compatibilidade para `relatorio_simulacao.py`.

Validado nesta sessão rodando de ponta a ponta contra o banco populado por uma execução real (ego 11681802, simulação #1, 134 agentes, 2.814 registros) e contra um `comparacao.json` gerado pelo `LancadorComparacao`. Achados estatísticos obtidos:

- **Mudança de distribuição ao longo da simulação**: χ² ≈ 212,6, p < 0,001 — a propagação muda significativamente a composição emocional da rede do ciclo 0 para o ciclo 20 (não é ruído).
- **Diferença entre rede real e redes sintéticas equivalentes**: χ² ≈ 102,1, p < 0,001 — confirma estatisticamente, e não apenas visualmente, a observação já registrada na seção "Análise comparativa" desta página: a topologia da rede real leva a um resultado de propagação diferente das topologias sintéticas com o mesmo tamanho e a mesma quantidade de sementes.

## Bug real encontrado pelo próprio CI: JADE não resolvia em ambiente limpo

Assim que o pipeline de CI (`.github/workflows/ci.yml`) rodou pela primeira vez no GitHub, ele falhou — exatamente o tipo de problema que a observabilidade de testes deveria pegar:

```
Could not find artifact com.tilab.jade:jade:jar:4.6.0 in central (https://repo.maven.apache.org/maven2)
```

Causa: o `pom.xml` sempre declarou o JADE como uma dependência Maven normal (`<groupId>com.tilab.jade</groupId>`), mas o JADE **nunca foi publicado no Maven Central** — por isso o projeto inclui `lib/jade.jar` localmente desde o início. Isso nunca dava erro na máquina de quem já tinha, em algum momento, instalado esse jar manualmente no `~/.m2` local (ex.: via `mvn install:install-file`, possivelmente nos passos de setup originais do projeto) — mas falha em qualquer ambiente "limpo" sem esse cache prévio, como um runner de CI recém-criado. Esse bug é anterior a esta sessão; só ficou visível porque agora existe um pipeline rodando `mvn verify` do zero.

Corrigido adicionando o `maven-install-plugin`, configurado para instalar `lib/jade.jar` no repositório Maven local automaticamente na fase `validate` (a primeira do ciclo de vida padrão do Maven, antes de `compile` precisar resolver a dependência) — um padrão documentado pela própria Apache para "3rd party JARs" não publicados em nenhum repositório público. Com isso, `mvn compile`/`test`/`verify`/`package` funcionam em qualquer máquina sem nenhum passo manual de setup, incluindo o runner do CI.

## Próxima etapa

A integração emocional/calibração deverá ser definida separadamente, evitando atribuir significado emocional às features do dataset sem uma justificativa metodológica. Também está em aberto: (a) decidir se o comportamento descrito na observação metodológica da Etapa 4.9 deve mudar; (b) usar o dataset real (já disponível e validado nesta sessão) como fonte padrão dos experimentos, reservando o gerador sintético apenas para desenvolvimento da interface; (c) opcionalmente automatizar a atualização do dashboard durante uma execução real via JADE; (d) investigar a assimetria de alcance entre raiva e alegria observada na propagação sobre o ego `11681802` (possível efeito de centralidade dos nós-semente).
