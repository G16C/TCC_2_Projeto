package br.unb.poc.interfaces;

/** HTML/CSS/JS do dashboard de comparação (rede real vs. sintéticas), autocontido. */
final class ComparacaoHtml {

    private ComparacaoHtml() {}

    static final String PAGINA = """
<!DOCTYPE html>
<html lang="pt-br">
<head>
<meta charset="utf-8">
<title>Comparação: rede real x redes sintéticas</title>
<style>
  :root {
    --fundo: #14161c; --painel: #1d2029; --borda: #2c3040;
    --texto: #e7e9ee; --texto-fraco: #9aa0b4;
  }
  * { box-sizing: border-box; }
  body { margin:0; font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif; background:var(--fundo); color:var(--texto); }
  header { padding:16px 24px; border-bottom:1px solid var(--borda); display:flex; align-items:baseline; gap:16px; flex-wrap:wrap; }
  header h1 { font-size:18px; margin:0; }
  header .sub { color:var(--texto-fraco); font-size:13px; }
  header a { color:var(--texto-fraco); font-size:13px; text-decoration:underline; margin-left:auto; }
  main { max-width:1200px; margin:0 auto; padding:16px 24px; display:flex; flex-direction:column; gap:16px; }
  .painel { background:var(--painel); border:1px solid var(--borda); border-radius:10px; padding:14px; }
  .painel h2 { font-size:14px; margin:0 0 10px 0; color:var(--texto-fraco); font-weight:600; text-transform:uppercase; letter-spacing:.04em; }
  table { width:100%; border-collapse:collapse; font-size:13px; }
  th, td { text-align:left; padding:6px 10px; border-bottom:1px solid var(--borda); white-space:nowrap; }
  th { color:var(--texto-fraco); font-weight:600; }
  td.destaque { color:#e8b933; font-weight:600; }
  table.mono td, table.mono th { font-variant-numeric: tabular-nums; }
  canvas { width:100%; display:block; background:#10121a; border-radius:8px; }
  .legenda { display:flex; gap:14px; margin-top:8px; font-size:12px; color:var(--texto-fraco); flex-wrap:wrap; }
  .legenda span { display:inline-flex; align-items:center; gap:6px; }
  .ponto { width:10px; height:10px; border-radius:50%; display:inline-block; }
  #status { font-size:13px; color:var(--texto-fraco); }
  .nota { font-size:12px; color:var(--texto-fraco); margin-top:8px; line-height:1.5; }
  .scroll { overflow-x:auto; }
</style>
</head>
<body>
<header>
  <h1>Comparação: rede real do Twitter x redes sintéticas</h1>
  <span class="sub">mesmo tamanho de rede e mesma quantidade de sementes emocionais por emoção</span>
  <a href="/">&larr; voltar ao dashboard de simulação única</a>
</header>
<main>
  <div id="status" class="painel">Carregando comparação...</div>

  <section class="painel" id="painelTabela" style="display:none">
    <h2>Métricas estruturais</h2>
    <div class="scroll"><table class="mono" id="tabelaEstrutura"></table></div>
    <p class="nota">Coeficiente de agrupamento e distância média seguem as definições de Watts &amp; Strogatz (1998) e Newman (2010). Redes com mais de 300 nós usam amostragem para a distância média.</p>
  </section>

  <section class="painel" id="painelLinha" style="display:none">
    <h2>Evolução da emoção de referência ao longo dos ciclos</h2>
    <canvas id="graficoLinha" width="1100" height="320"></canvas>
    <div class="legenda" id="legendaLinha"></div>
    <p class="nota" id="notaEmocaoRef"></p>
  </section>

  <section class="painel" id="painelBarra" style="display:none">
    <h2>Alcance final da emoção de referência (% dos nós)</h2>
    <canvas id="graficoBarra" width="1100" height="220"></canvas>
  </section>

  <section class="painel" id="painelDistribuicao" style="display:none">
    <h2>Distribuição final por rótulo dominante</h2>
    <div class="scroll"><table class="mono" id="tabelaDistribuicao"></table></div>
  </section>
</main>

<script>
const PALETA = ['#e8b933', '#3f7ab0', '#e0483e', '#5a8f4f'];
const CORES_EMOCAO = { anger:'#e0483e', disgust:'#5a8f4f', joy:'#e8b933', sadness:'#3f7ab0' };
const ROTULOS_EMOCAO = { anger:'raiva', disgust:'nojo', joy:'alegria', sadness:'tristeza' };

async function carregar() {
  const status = document.getElementById('status');
  try {
    const resp = await fetch('/api/comparacao.json', { cache: 'no-store' });
    const dados = await resp.json();
    if (!dados.cenarios || dados.cenarios.length === 0) {
      status.textContent = 'Nenhum dado de comparação disponível ainda.';
      return;
    }
    status.style.display = 'none';
    montarTabelaEstrutura(dados.cenarios);
    const emocaoRef = escolherEmocaoReferencia(dados.cenarios[0]);
    montarGraficoLinha(dados.cenarios, emocaoRef);
    montarGraficoBarra(dados.cenarios, emocaoRef);
    montarTabelaDistribuicao(dados.cenarios);
  } catch (e) {
    status.textContent = 'Falha ao carregar a comparação: ' + e;
  }
}

function escolherEmocaoReferencia(cenarioReal) {
  const dist = cenarioReal.propagacao.distribuicaoInicial || {};
  let melhor = 'joy', maior = -1;
  for (const chave of ['ANGER', 'DISGUST', 'JOY', 'SADNESS']) {
    const valor = dist[chave] || 0;
    if (valor > maior) { maior = valor; melhor = chave.toLowerCase(); }
  }
  return melhor;
}

function montarTabelaEstrutura(cenarios) {
  const linhas = [
    ['Nós', c => c.estrutura.numNos],
    ['Arestas', c => c.estrutura.numArestas],
    ['Grau médio', c => c.estrutura.grauMedio.toFixed(2)],
    ['Grau máximo', c => c.estrutura.grauMaximo],
    ['Grau mínimo', c => c.estrutura.grauMinimo],
    ['Densidade', c => c.estrutura.densidade.toFixed(4)],
    ['Coef. de agrupamento médio', c => c.estrutura.coeficienteAgrupamentoMedio.toFixed(4)],
    ['Distância média (aprox.)', c => c.estrutura.distanciaMediaAprox.toFixed(2)],
    ['Componentes conexos', c => c.estrutura.numComponentesConexos],
    ['Fração no maior componente', c => (c.estrutura.fracaoMaiorComponente * 100).toFixed(1) + '%'],
    ['Ciclo de convergência', c => c.propagacao.cicloConvergencia < 0 ? 'não convergiu' : c.propagacao.cicloConvergencia],
  ];
  const tabela = document.getElementById('tabelaEstrutura');
  let html = '<tr><th>Métrica</th>' + cenarios.map(c => '<th>' + c.nome + '</th>').join('') + '</tr>';
  for (const [rotulo, fn] of linhas) {
    html += '<tr><td>' + rotulo + '</td>' + cenarios.map(c => '<td>' + fn(c) + '</td>').join('') + '</tr>';
  }
  tabela.innerHTML = html;
  document.getElementById('painelTabela').style.display = '';
}

function montarGraficoLinha(cenarios, emocaoRef) {
  const canvas = document.getElementById('graficoLinha');
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const margem = 46;
  const largura = canvas.width - margem * 2;
  const altura = canvas.height - margem * 2;

  const maxCiclos = Math.max(...cenarios.map(c => c.propagacao.serieMedia.length));

  // Escala automática: mesmo motivo do gráfico do dashboard principal —
  // a média geral costuma ser pequena (maioria dos nós neutra), então uma
  // escala fixa 0-1 esconderia a evolução.
  let valorMax = 0;
  cenarios.forEach(c => {
    c.propagacao.serieMedia.forEach(ponto => {
      valorMax = Math.max(valorMax, ponto[emocaoRef]);
    });
  });
  if (valorMax <= 0) valorMax = 1;
  valorMax *= 1.15;

  ctx.strokeStyle = 'rgba(255,255,255,0.15)';
  ctx.beginPath();
  ctx.moveTo(margem, margem);
  ctx.lineTo(margem, margem + altura);
  ctx.lineTo(margem + largura, margem + altura);
  ctx.stroke();

  ctx.fillStyle = 'rgba(231,233,238,0.6)';
  ctx.font = '11px sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText(valorMax.toFixed(3), margem - 6, margem + 4);
  ctx.fillText((valorMax / 2).toFixed(3), margem - 6, margem + altura / 2 + 4);
  ctx.fillText('0', margem - 6, margem + altura + 4);

  cenarios.forEach((cenario, idx) => {
    const serie = cenario.propagacao.serieMedia;
    ctx.beginPath();
    ctx.strokeStyle = PALETA[idx % PALETA.length];
    ctx.lineWidth = 2;
    serie.forEach((ponto, i) => {
      const x = margem + (maxCiclos <= 1 ? 0 : (largura * i) / (maxCiclos - 1));
      const y = margem + altura - (ponto[emocaoRef] / valorMax) * altura;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    });
    ctx.stroke();
  });

  const legenda = document.getElementById('legendaLinha');
  legenda.innerHTML = cenarios.map((c, idx) =>
    '<span><i class="ponto" style="background:' + PALETA[idx % PALETA.length] + '"></i>' + c.nome + '</span>'
  ).join('');

  document.getElementById('notaEmocaoRef').textContent =
    'Emoção de referência: ' + (ROTULOS_EMOCAO[emocaoRef] || emocaoRef) +
    ' (a que teve mais nós-semente na rede real). Eixo Y: intensidade média (0 a 1). Eixo X: ciclo.';
  document.getElementById('painelLinha').style.display = '';
}

function montarGraficoBarra(cenarios, emocaoRef) {
  const canvas = document.getElementById('graficoBarra');
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const margem = 40;
  const largura = canvas.width - margem * 2;
  const altura = canvas.height - margem * 2;
  const rotuloAlvo = emocaoRef.toUpperCase();

  const valores = cenarios.map(c => {
    const dist = c.propagacao.distribuicaoFinal || {};
    const alvo = dist[rotuloAlvo] || 0;
    return (alvo / c.estrutura.numNos) * 100;
  });

  ctx.strokeStyle = 'rgba(255,255,255,0.15)';
  ctx.beginPath();
  ctx.moveTo(margem, margem);
  ctx.lineTo(margem, margem + altura);
  ctx.lineTo(margem + largura, margem + altura);
  ctx.stroke();

  const larguraBarra = largura / cenarios.length * 0.5;
  const passo = largura / cenarios.length;

  cenarios.forEach((c, idx) => {
    const valor = valores[idx];
    const alturaBarra = (valor / 100) * altura;
    const x = margem + passo * idx + (passo - larguraBarra) / 2;
    const y = margem + altura - alturaBarra;
    ctx.fillStyle = PALETA[idx % PALETA.length];
    ctx.fillRect(x, y, larguraBarra, alturaBarra);
    ctx.fillStyle = '#e7e9ee';
    ctx.font = '12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(valor.toFixed(1) + '%', x + larguraBarra / 2, y - 6);
    ctx.fillText(c.nome, x + larguraBarra / 2, margem + altura + 16);
  });
}

function montarTabelaDistribuicao(cenarios) {
  const rotulos = new Set();
  cenarios.forEach(c => Object.keys(c.propagacao.distribuicaoFinal || {}).forEach(r => rotulos.add(r)));
  const listaRotulos = Array.from(rotulos).sort();

  const tabela = document.getElementById('tabelaDistribuicao');
  let html = '<tr><th>Cenário</th>' + listaRotulos.map(r => '<th>' + r + '</th>').join('') + '</tr>';
  cenarios.forEach(c => {
    const dist = c.propagacao.distribuicaoFinal || {};
    html += '<tr><td>' + c.nome + '</td>' +
      listaRotulos.map(r => '<td>' + (dist[r] || 0) + '</td>').join('') + '</tr>';
  });
  tabela.innerHTML = html;
  document.getElementById('painelDistribuicao').style.display = '';
}

carregar();
</script>
</body>
</html>
""";
}
