# Integração com o Twitter Social Circles

## Objetivo

A partir desta etapa, a simulação pode utilizar uma rede real do conjunto **Twitter Social Circles** como estrutura topológica, em substituição às redes sintéticas Small-World, Scale-Free e Modular.

A integração foi implementada antes da etapa de calibração emocional porque o dataset disponibilizado contém principalmente:

- identificadores de usuários;
- relações entre usuários;
- atributos binários (`.feat`);
- nomes dos atributos (`.featnames`);
- atributos do ego (`.egofeat`);
- círculos sociais (`.circles`).

O dataset **não fornece diretamente rótulos emocionais por usuário**. Portanto, esta etapa não atribui emoções com base nos atributos. A extração emocional e a calibração serão tratadas posteriormente.

---

## Estrutura do dataset

O arquivo `twitter.tar.gz` contém 973 redes de ego, cada uma identificada por um ID.

Para um ego `11681802`, por exemplo:

```text
11681802.edges
11681802.feat
11681802.featnames
11681802.egofeat
11681802.circles
```

O arquivo `.edges` é utilizado para construir a rede social.

Os arquivos `.feat` e `.featnames` são carregados e preservados para uso futuro na etapa de análise/calibração.

---

## Execução

A aplicação aceita diretamente o arquivo `.tar.gz`.

Exemplo:

```bash
mvn exec:java \
  -Dexec.args="--twitter /caminho/twitter.tar.gz 11681802"
```

O terceiro argumento é o ID do ego:

```text
11681802
```

Opcionalmente, pode ser informado o identificador real do usuário que deverá receber o estado emocional inicial:

```bash
mvn exec:java \
  -Dexec.args="--twitter /caminho/twitter.tar.gz 11681802 180505807"
```

Se o quarto argumento não for informado, o primeiro nó do grafo carregado será utilizado como semente.

---

## Exemplo com o arquivo utilizado no desenvolvimento

Para o arquivo fornecido neste projeto:

```bash
mvn exec:java \
  -Dexec.args="--twitter /caminho/twitter.tar.gz 11681802"
```

A rede `11681802` possui, no carregamento atual:

```text
134 nós identificados pelos arquivos .edges/.feat
718 arestas únicas
134 vetores de atributos
596 nomes de atributos
```

Os números acima correspondem ao carregamento realizado nesta versão do sistema e devem ser tratados como uma validação da integração, não como parâmetros fixos da implementação.

---

## Mapeamento de identificadores

O JADE utiliza índices locais:

```text
Usuario0
Usuario1
Usuario2
...
```

O dataset utiliza IDs reais do Twitter, por exemplo:

```text
100794531
15649735
180505807
...
```

O sistema mantém os dois identificadores:

```text
ID local JADE  →  ID original Twitter
       0       →  100794531
       1       →  15649735
       2       →  180505807
```

Essa separação é necessária para que o motor JADE continue trabalhando com índices compactos sem perder a rastreabilidade dos dados reais.

---

## Estado emocional

Nesta etapa, os atributos Twitter **não são convertidos automaticamente em emoções**.

Isso é intencional.

A estrutura:

```text
Twitter
   ↓
rede social
   ↓
JADE
```

já está implementada.

A etapa seguinte deverá definir:

```text
Twitter features
       ↓
interpretação semântica
       ↓
estado emocional
       ↓
calibração do modelo
```

Não deve ser assumido que uma feature binária representa diretamente `anger`, `disgust`, `joy` ou `sadness`.

---

## Arquitetura

O fluxo atual passa a ser:

```text
twitter.tar.gz
       ↓
TwitterDatasetLoader
       ↓
TwitterNetworkData
       ↓
TopologiaFonteExterna
       ↓
RedeSocial
       ↓
AgenteControle
       ↓
UsuarioAgent
       ↓
JADE
```

O `TwitterDatasetLoader` aceita:

```text
pasta extraída
ou
arquivo .tar.gz
```

Não é necessário adicionar uma biblioteca externa apenas para leitura do TAR.

---

## Limitações atuais

### 1. Pesos

O `.edges` não fornece intensidade de influência.

Nesta etapa, todas as conexões recebem:

```text
w_ij = 1.0
```

Isso é uma escolha de adaptação e deverá ser revista quando houver dados suficientes para estimar a força dos laços.

### 2. Direcionalidade

As conexões do arquivo `.edges` são tratadas como não direcionadas:

```text
A — B
```

portanto:

```text
w_AB = w_BA
```

A possibilidade de influência assimétrica deverá ser avaliada posteriormente.

### 3. Emoções

O dataset não fornece diretamente o estado emocional dos usuários.

A atribuição/calibração emocional ainda não foi implementada.

### 4. Ego

O ID do ego é utilizado para selecionar a rede correspondente.

O ego não é automaticamente inserido como um novo nó conectado a todos os usuários. A implementação preserva as relações presentes no arquivo `.edges`, evitando alterar artificialmente a estrutura original.

---

## Próxima etapa

Depois desta integração, a próxima evolução deverá investigar:

1. como os atributos `.feat` representam comportamento;
2. quais atributos podem ser relacionados a emoções;
3. como extrair sinais emocionais;
4. como estimar parâmetros do modelo;
5. como relacionar os dados observados ao modelo de Fan, Xu e Zhao;
6. como validar os resultados da simulação contra os dados reais.

A integração estrutural com o Twitter deve ser considerada concluída apenas no sentido de **carregamento e utilização da rede real**. A integração emocional ainda é uma etapa posterior.
