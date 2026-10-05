# Etapa 4A.1 — Inferência emocional por heurística

A etapa introduz uma camada de inferência entre as features do Twitter Social Circles e o estado emocional inicial dos agentes.

## Princípio

O dataset representa uma rede social real e fornece relações e atributos binários. Ele não fornece rótulos emocionais. Assim, as emoções iniciais são estimadas por uma heurística determinística e explicitamente configurada.

Fluxo:

TwitterDatasetLoader → TwitterNetworkData → HeuristicaTwitter → EmotionState → FanEmotionModel

## Heurística

Para cada feature ativa reconhecida pela heurística, são aplicados pesos a uma ou mais emoções. O score bruto de cada emoção é a soma dos pesos das features presentes. O resultado é normalizado para o intervalo [0,1].

Features sem regra permanecem neutras e não contribuem para o score.

O vocabulário inicial é conservador e deve ser considerado uma calibração inicial, não uma verdade anotada pelo dataset. Sua validação e eventual calibração com literatura ou dados rotulados fazem parte das etapas experimentais seguintes.

## Referências

- As quatro emoções mapeadas (raiva, nojo, alegria, tristeza) seguem o mesmo conjunto do modelo de propagação: **Fan, Xu e Zhao (2018)**.
- O uso de hashtags como indicador (proxy) de emoção — e não como rótulo emocional explícito do dataset — segue a abordagem de "distant supervision"/rotulação fraca da literatura de análise de emoção em redes sociais:
  - Mohammad, S. M. (2012). *#Emotional Tweets*. Proceedings of the First Joint Conference on Lexical and Computational Semantics (\*SEM 2012).
  - Wang, W., Chen, L., Thirunarayan, K., & Sheth, A. P. (2012). *Harnessing Twitter "Big Data" for Automatic Emotion Identification*. IEEE SocialCom/PASSAT.
  - Suttles, J., & Ide, N. (2013). *Distant Supervision for Emotion Classification with Discrete Binary Values*. CICLing.
- O peso de 0,70 usado por padrão em `HeuristicaTwitter.regrasPadrao()` é uma constante conservadora escolhida manualmente, não derivada estatisticamente de nenhuma das referências acima. Ela existe para que uma única hashtag reconhecida já produza um sinal claro sem saturar a escala [0,1]. A calibração desse peso com dados rotulados/literatura permanece em aberto (ver observação em `docs/ETAPAS_EVOLUCAO.md`).

## Rastreabilidade

A implementação mantém separadas:
- aquisição dos dados;
- representação da rede;
- inferência emocional;
- propagação emocional.

Isso permite substituir a heurística futuramente por outra estratégia sem modificar o modelo de propagação de Fan.
