"""
COMPATIBILIDADE: este script foi substituído por `relatorio_simulacao.py`.

O script original (a) só lia a coluna `valor` do modelo emocional
escalar antigo — a PoC hoje usa quatro dimensões (anger/disgust/joy/
sadness) — e (b) quebrava com `IndexError` para redes com mais de 10
agentes, por usar paletas de cores/estilos/marcadores de tamanho fixo
para desenhar uma curva por agente. Mantido apenas para não quebrar
quem já rodava `python analise_emocoes.py`.
"""
from relatorio_simulacao import main

if __name__ == "__main__":
    main()
