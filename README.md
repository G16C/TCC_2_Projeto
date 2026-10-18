# PoC — Propagação de Emoções em Redes Sociais Digitais

Prova de conceito para simulação de propagação emocional em redes sociais
digitais utilizando agentes JADE, desenvolvida para o TCC II — Engenharia
de Software, UnB, 2026.

Autor: Gabriel Campello Marques.

Consulte `docs/ETAPAS_EVOLUCAO.md` para o histórico detalhado de cada
etapa do desenvolvimento.

## Visão geral

- **Modelo**: propagação multidimensional (raiva, nojo, alegria, tristeza)
  entre agentes, segundo o modelo de Fan (`FanEmotionModel`).
- **Topologia**: sintética (small-world/aleatória) ou importada do dataset
  real *Twitter Social Circles* (Stanford SNAP).
- **Execução**: agentes JADE trocando mensagens ACL, com persistência em
  PostgreSQL.
- **Interface**: dashboard web embutido (sem dependências externas) para
  visualizar a rede e a evolução emocional por ciclo.

## Pré-requisitos

- JDK 17 ou superior
- Maven
- PostgreSQL (apenas para a simulação completa via JADE — ver Caminho 3)

O `lib/jade.jar` já está incluído no repositório; não é necessário baixá-lo.

---

## Caminho 1 — Dashboard rápido, sem PostgreSQL nem JADE

Sobe o dashboard usando uma rede sintética de demonstração, gerada em
memória. Bom para conferir a interface rapidamente.

```bash
cd poc-emocoes
mvn compile
mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorInterface
```

Abra `http://localhost:8080/`.

Parâmetros opcionais (porta, agentes, ciclos, alpha, delta, semente):

```bash
mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorInterface \
  -Dexec.args="8080 30 20 0.6 0.02 42"
```

---

## Caminho 2 — Dashboard com o dataset real do Twitter

Mesma ideia do Caminho 1, mas carregando uma rede egocêntrica real do
*Twitter Social Circles* em vez da rede sintética. Não depende de banco
nem de JADE.

```bash
# extraia o dataset em algum lugar, ex.: tar -xzf twitter_tar.gz -C ~/datasets
mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorInterface \
  -Dexec.args="--twitter /caminho/para/o/dataset 11681802 x 8080 20 0.6 0.02"
```

Ordem dos argumentos:
`--twitter <caminho> <egoId> <seedNodeId ou vazio> <porta> <numCiclos> <alpha> <delta>`

---

## Caminho 2.5 — Comparar a rede real com redes sintéticas equivalentes

Roda a rede real do Twitter e três topologias sintéticas (Small-World, Scale-Free, Modular) do **mesmo tamanho**, com a **mesma quantidade de nós-semente por emoção**, e mostra lado a lado métricas estruturais (grau, agrupamento, distância média, componentes) e o resultado da propagação em cada uma. Não depende de PostgreSQL nem de JADE.

```bash
mvn exec:java -Dexec.mainClass=br.unb.poc.plataforma.LancadorComparacao \
  -Dexec.args="/caminho/para/o/dataset 11681802 8081 20 0.6 0.02 42"
```

Abra `http://localhost:8081/`. Argumentos: `<caminho> <egoId> [porta] [numCiclos] [alpha] [delta] [semente]`.

---

## Caminho 3 — Simulação completa (JADE + PostgreSQL)

Este é o pipeline "oficial" da PoC: agentes JADE reais, trocando mensagens
de forma assíncrona e persistindo cada ciclo no PostgreSQL.

### 3.1 Criar o banco e as tabelas

Com o PostgreSQL rodando:

```bash
psql -U postgres -c "CREATE DATABASE poc_emocoes;"
psql -U postgres -d poc_emocoes -f src/main/resources/init_db.sql
```

Credenciais padrão do projeto (ajuste em `ConfiguracaoPoc.java` se o seu
Postgres usa outras): usuário `postgres`, senha `postgres`, banco
`poc_emocoes`, `localhost:5432`.

### 3.2 Compilar o projeto

```bash
mvn clean compile
```

### 3.3 Rodar com a rede sintética padrão

```bash
mvn exec:java
```

Usa `SimulationConfiguration.padrao()` — a topologia sintética já
configurada no projeto, não o dataset do Twitter.

### 3.4 Ou rodar com o dataset real do Twitter

```bash
mvn exec:java -Dexec.args="--twitter /caminho/para/o/dataset 11681802"
```

Um quarto argumento opcional define o id do nó-semente. O caminho pode
ser a pasta extraída ou o próprio `.tar.gz`.

Por padrão, o console mostra apenas mensagens de marco (início, ID da
simulação, contagem de agentes, conclusão) — em datasets grandes, o log
detalhado (uma linha por mensagem trocada entre agentes) ocuparia o
terminal inteiro. Para ativar o log ciclo a ciclo, acrescente `--verbose`
em qualquer posição:

```bash
mvn exec:java -Dexec.args="--twitter /caminho/para/o/dataset 11681802 --verbose"
```

### 3.5 Cuidado com a GUI do JADE

`LancadorSimulacao` está configurado com `Profile.GUI = "true"`, ou seja,
abre a interface gráfica do JADE (RMA) e **exige um ambiente com
display**. Rodando em servidor/container sem tela, mude essa linha para
`"false"` em `LancadorSimulacao.java` antes de compilar, ou use algo como
o Xvfb.

### 3.6 Conferir os resultados

Os dados ficam nas tabelas `simulacao` e `estado_emocional` do Postgres.
Para visualizar essa execução real no dashboard (em vez de consultar o
banco diretamente), é possível montar o mesmo JSON usado pela interface a
partir da própria execução:

```java
ColetorResultados.Instantaneo instantaneo =
        ColetorResultados.obterInstantaneo(AgenteControle.ultimaSimulacaoId);
String json = RelatorioExport.paraJson(
        instantaneo.rede(), configuracao, instantaneo.historico(), indiceSemente);
```

Uma integração automática (subir o `DashboardServer` direto do
`LancadorSimulacao`, atualizando-o durante a execução) ainda não existe —
é um próximo passo em aberto, ver `docs/ETAPAS_EVOLUCAO.md`.

---

## Rodar os testes

```bash
mvn test
```

Cobre, entre outros, `TwitterDatasetLoaderTest`, `HeuristicaTwitterTest`,
`PocTest`, e os testes das Etapas 4.9/5.0/análise (`SimuladorDiretoTest`,
`RelatorioExportTest`, `MetricasEstruturaisTest`, `MetricasPropagacaoTest`,
`SementesComparaveisTest`, `EmotionStateNeutroTest`) — 38 testes ao todo,
nenhum deles depende de PostgreSQL/JADE em execução.

Para ver a cobertura de testes (linha a linha, quais classes/métodos
estão ou não cobertos):

```bash
mvn test jacoco:report
# abra target/site/jacoco/index.html no navegador
```

Um workflow de CI (`.github/workflows/ci.yml`) roda `mvn verify`
automaticamente a cada push/PR (se o repositório estiver no GitHub),
publicando os relatórios de teste e cobertura como artefatos do
workflow — não precisa rodar nada localmente para conferir.

## Analisar os dados (Python)

Além do dashboard web, há um toolkit em Python (`analise/`) para gerar
gráficos em qualidade de publicação e rodar testes estatísticos —
ver `analise/README.md` para o guia completo. Resumo:

```bash
cd analise
pip install -r requirements.txt

# relatório de uma simulação real (PostgreSQL)
python relatorio_simulacao.py --id 57 --saida ./saida

# comparação rede real x sintéticas (a partir do dashboard)
curl http://localhost:8080/api/comparacao.json -o comparacao.json
python comparar_json.py comparacao.json --saida ./saida_comparacao
```

## Solução de problemas

- **Não consigo acessar o dashboard (`localhost:8080`)**: verifique se o
  console mostrou a linha `[Interface] Dashboard disponível em
  http://localhost:8080/` — se não mostrou, o processo provavelmente
  falhou antes de chegar lá (leia a exceção impressa). Se a porta já
  estiver em uso por outro processo, a mensagem de erro agora informa
  isso claramente; tente outra porta nos argumentos.
- **O navegador não abre sozinho**: `LancadorInterface` tenta abrir via
  `java.awt.Desktop`, com fallback para `xdg-open`/`open`/`rundll32`.
  Isso depende do seu ambiente (sessão gráfica, variável `DISPLAY` no
  Linux, navegador padrão configurado). Se não abrir, a URL fica impressa
  em destaque no console — copie e cole manualmente.
- **`Erro: This connection has been closed` durante a simulação real**:
  já corrigido nesta versão (a conexão JDBC era compartilhada sem
  sincronização entre os agentes). Se ainda aparecer, confirme que está
  usando o código deste pacote, não uma versão anterior.
- **`psql: ERROR: column "id_externo" ... does not exist`** ao rodar
  `init_db.sql`: já corrigido nesta versão.
- **`Could not find artifact com.tilab.jade:jade:jar:4.6.0 in central`**
  (normalmente aparece rodando `mvn test`/`mvn verify` num ambiente
  "limpo", como o CI do GitHub, mesmo que `mvn compile` já funcionasse
  na sua máquina antes): já corrigido nesta versão — o `pom.xml` agora
  instala `lib/jade.jar` automaticamente no repositório Maven local no
  início de qualquer build (JADE não é publicado no Maven Central, só
  incluído localmente neste projeto). Se por algum motivo ainda
  aparecer, rode manualmente antes de qualquer outro comando Maven:
  `mvn install:install-file -Dfile=lib/jade.jar -DgroupId=com.tilab.jade -DartifactId=jade -Dversion=4.6.0 -Dpackaging=jar`.

## Estrutura do projeto

```
src/main/java/br/unb/poc/
  agentes/      Agentes JADE (AgenteControle, UsuarioAgent)
  analise/      Métricas estruturais e de propagação, comparação rede real x sintéticas
  dados/        Carregamento do dataset do Twitter
  demo/         Gerador de rede sintética para demonstração da interface
  interfaces/   Dashboard web embutido (Etapa 5.0)
  modelo/       Rede social, configuração, modelo emocional (Fan)
  plataforma/   Pontos de entrada (LancadorSimulacao, LancadorInterface, LancadorComparacao)
  relatorio/    Preparação de dados para a interface (Etapa 4.9)
  util/         Persistência em PostgreSQL
src/test/java/  Testes JUnit (38 testes, nenhum depende de PostgreSQL/JADE)
analise/        Toolkit Python para análise estatística e gráficos (ver analise/README.md)
docs/           Documentação das etapas do TCC
data/twitter/   Onde colocar o dataset real (não incluído no repositório)
.github/workflows/ci.yml   Pipeline de CI (compila + testa a cada push/PR)
```
