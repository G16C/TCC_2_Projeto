package br.unb.poc.analise;

import br.unb.poc.modelo.RedeSocial;

import java.util.*;

/**
 * Métricas estruturais clássicas de análise de redes, calculadas sobre
 * uma {@link RedeSocial} — usadas para comparar objetivamente a
 * topologia de redes sintéticas (Small-World, Scale-Free, Modular) com a
 * topologia de uma rede real do Twitter.
 *
 * <p>Definições seguem a literatura padrão de ciência de redes: Newman,
 * M. E. J. (2010). <i>Networks: An Introduction</i>. Oxford University
 * Press — em particular o coeficiente de agrupamento local de Watts e
 * Strogatz (1998), <i>Collective dynamics of "small-world" networks</i>,
 * Nature.
 *
 * @param numNos                       número de nós (agentes) da rede
 * @param numArestas                   número de arestas não-dirigidas distintas
 * @param grauMedio                    grau médio (2 * numArestas / numNos)
 * @param grauMaximo                   maior grau observado
 * @param grauMinimo                   menor grau observado
 * @param densidade                    fração de todas as arestas possíveis que existem
 * @param coeficienteAgrupamentoMedio  média do coeficiente de agrupamento local de cada nó
 * @param distanciaMediaAprox          distância média entre pares de nós conectados (aproximada por amostragem em redes grandes)
 * @param numComponentesConexos        número de componentes conexos
 * @param fracaoMaiorComponente        fração dos nós que pertence ao maior componente conexo
 */
public record MetricasEstruturais(
        int numNos,
        int numArestas,
        double grauMedio,
        int grauMaximo,
        int grauMinimo,
        double densidade,
        double coeficienteAgrupamentoMedio,
        double distanciaMediaAprox,
        int numComponentesConexos,
        double fracaoMaiorComponente) {

    /** Acima deste número de nós, a distância média é aproximada por amostragem de origens de BFS. */
    private static final int LIMITE_AMOSTRAGEM_BFS = 300;

    public static MetricasEstruturais calcular(RedeSocial rede) {
        int n = rede.getNumAgentes();
        if (n == 0) {
            return new MetricasEstruturais(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        List<Set<Integer>> vizinhos = new ArrayList<>(n);
        long somaGraus = 0;
        int grauMax = 0, grauMin = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            Set<Integer> viz = rede.getVizinhos(i);
            vizinhos.add(viz);
            somaGraus += viz.size();
            grauMax = Math.max(grauMax, viz.size());
            grauMin = Math.min(grauMin, viz.size());
        }
        int numArestas = (int) (somaGraus / 2);
        double grauMedio = (double) somaGraus / n;
        double densidade = n < 2 ? 0.0 : (2.0 * numArestas) / ((double) n * (n - 1));

        double somaCoefAgrupamento = 0.0;
        for (int i = 0; i < n; i++) {
            somaCoefAgrupamento += coeficienteLocal(vizinhos.get(i), vizinhos);
        }
        double coefMedio = somaCoefAgrupamento / n;

        ResultadoComponentes componentes = calcularComponentes(vizinhos, n);
        double distanciaMedia = calcularDistanciaMediaAprox(vizinhos, n);

        return new MetricasEstruturais(
                n, numArestas, grauMedio, grauMax, grauMin == Integer.MAX_VALUE ? 0 : grauMin,
                densidade, coefMedio, distanciaMedia,
                componentes.numComponentes(), componentes.fracaoMaiorComponente());
    }

    /**
     * Coeficiente de agrupamento local de um nó: fração de pares dos seus
     * vizinhos que também são vizinhos entre si (proporção de triângulos
     * fechados em relação aos triângulos possíveis).
     */
    private static double coeficienteLocal(Set<Integer> vizinhosDoNo, List<Set<Integer>> todosOsVizinhos) {
        int k = vizinhosDoNo.size();
        if (k < 2) return 0.0;

        Integer[] lista = vizinhosDoNo.toArray(new Integer[0]);
        int ligacoesEntreVizinhos = 0;
        for (int a = 0; a < lista.length; a++) {
            Set<Integer> vizinhosDeA = todosOsVizinhos.get(lista[a]);
            for (int b = a + 1; b < lista.length; b++) {
                if (vizinhosDeA.contains(lista[b])) {
                    ligacoesEntreVizinhos++;
                }
            }
        }
        int paresPossiveis = k * (k - 1) / 2;
        return (double) ligacoesEntreVizinhos / paresPossiveis;
    }

    private record ResultadoComponentes(int numComponentes, double fracaoMaiorComponente) {}

    private static ResultadoComponentes calcularComponentes(List<Set<Integer>> vizinhos, int n) {
        int[] componenteDoNo = new int[n];
        Arrays.fill(componenteDoNo, -1);
        int numComponentes = 0;
        int maiorTamanho = 0;

        for (int inicio = 0; inicio < n; inicio++) {
            if (componenteDoNo[inicio] != -1) continue;
            int tamanho = 0;
            Deque<Integer> pilha = new ArrayDeque<>();
            pilha.push(inicio);
            componenteDoNo[inicio] = numComponentes;
            while (!pilha.isEmpty()) {
                int atual = pilha.pop();
                tamanho++;
                for (int viz : vizinhos.get(atual)) {
                    if (componenteDoNo[viz] == -1) {
                        componenteDoNo[viz] = numComponentes;
                        pilha.push(viz);
                    }
                }
            }
            maiorTamanho = Math.max(maiorTamanho, tamanho);
            numComponentes++;
        }
        return new ResultadoComponentes(numComponentes, n == 0 ? 0.0 : (double) maiorTamanho / n);
    }

    /**
     * Distância média (número de arestas do caminho mais curto) entre
     * pares de nós conectados. Ignora pares em componentes distintos
     * (não somam à média — grafos reais frequentemente são desconexos).
     * Em redes grandes, amostra até {@link #LIMITE_AMOSTRAGEM_BFS} nós de
     * origem em vez de rodar BFS a partir de todos os nós.
     */
    private static double calcularDistanciaMediaAprox(List<Set<Integer>> vizinhos, int n) {
        if (n < 2) return 0.0;

        List<Integer> origens = new ArrayList<>();
        for (int i = 0; i < n; i++) origens.add(i);
        if (n > LIMITE_AMOSTRAGEM_BFS) {
            Collections.shuffle(origens, new Random(7));
            origens = origens.subList(0, LIMITE_AMOSTRAGEM_BFS);
        }

        long somaDistancias = 0;
        long numPares = 0;
        int[] distancia = new int[n];

        for (int origem : origens) {
            Arrays.fill(distancia, -1);
            Deque<Integer> fila = new ArrayDeque<>();
            distancia[origem] = 0;
            fila.add(origem);
            while (!fila.isEmpty()) {
                int atual = fila.poll();
                for (int viz : vizinhos.get(atual)) {
                    if (distancia[viz] == -1) {
                        distancia[viz] = distancia[atual] + 1;
                        fila.add(viz);
                    }
                }
            }
            for (int destino = 0; destino < n; destino++) {
                if (destino != origem && distancia[destino] > 0) {
                    somaDistancias += distancia[destino];
                    numPares++;
                }
            }
        }
        return numPares == 0 ? 0.0 : (double) somaDistancias / numPares;
    }
}
