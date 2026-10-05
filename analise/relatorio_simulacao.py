"""
Gera um relatório completo (gráficos + estatísticas) de uma simulação
registrada no PostgreSQL da PoC.

Uso:
    python relatorio_simulacao.py                  # simulação mais recente
    python relatorio_simulacao.py --id 57
    python relatorio_simulacao.py --id 57 --saida ./saida
"""
import argparse
import os

import db
import estatisticas
import graficos


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--id", type=int, default=None,
                         help="ID da simulação (padrão: a mais recente)")
    parser.add_argument("--saida", default=".",
                         help="Pasta onde salvar os gráficos e o CSV de resumo")
    parser.add_argument("--max-agentes-trajetoria", type=int, default=15,
                         help="Quantos agentes amostrar no gráfico de trajetórias individuais")
    args = parser.parse_args()

    os.makedirs(args.saida, exist_ok=True)

    simulacao_id = args.id or db.ultima_simulacao_id()
    meta = db.carregar_metadados(simulacao_id)
    ego = meta["twitter_ego_id"]
    print(
        f"Simulação #{simulacao_id}: {meta['num_agentes']} agentes, {meta['num_ciclos']} ciclos, "
        f"alpha={meta['alpha']}, delta={meta['delta']}, fonte={meta['fonte_dados']}"
        + (f", ego={ego}" if ego else "")
    )

    df = db.carregar_estados(simulacao_id)
    print(f"{df['id_agente'].nunique()} agentes, {df['ciclo'].nunique()} ciclos, {len(df)} registros carregados.")

    resumo = estatisticas.resumo_por_ciclo(df)
    caminho_resumo = os.path.join(args.saida, f"resumo_sim{simulacao_id}.csv")
    resumo.to_csv(caminho_resumo, index=False)
    print(f"Resumo estatístico por ciclo salvo em: {caminho_resumo}")

    graficos.grafico_evolucao_media(
        df, os.path.join(args.saida, f"evolucao_media_sim{simulacao_id}.png"),
        titulo=f"Evolução emocional média — simulação #{simulacao_id}")

    graficos.grafico_distribuicao_categoria(
        df, os.path.join(args.saida, f"distribuicao_categoria_sim{simulacao_id}.png"),
        titulo=f"Distribuição do rótulo dominante — simulação #{simulacao_id}")

    for emocao in ("anger", "joy"):
        graficos.grafico_trajetorias_agentes(
            df, os.path.join(args.saida, f"trajetorias_{emocao}_sim{simulacao_id}.png"),
            emocao=emocao, max_agentes=args.max_agentes_trajetoria,
            titulo=f"Trajetórias individuais ({emocao}) — simulação #{simulacao_id}")

    estatistica, p_valor, tabela = estatisticas.teste_mudanca_distribuicao(df)
    print("\n=== Teste qui-quadrado: a distribuição de rótulos mudou do primeiro para o último ciclo? ===")
    print(tabela.to_string())
    print(f"qui² = {estatistica:.3f}, p-valor = {p_valor:.5f}")
    if p_valor < 0.05:
        print("=> Diferença estatisticamente significativa (p < 0.05).")
    else:
        print("=> Sem evidência estatística de mudança (p >= 0.05) com os dados disponíveis.")

    print(f"\nGráficos e resumo salvos em: {args.saida}")


if __name__ == "__main__":
    main()
