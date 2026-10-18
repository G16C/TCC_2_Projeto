package br.unb.poc.interfaces;

/** Mantém o HTML/CSS/JS do dashboard fora de {@link DashboardServer} por legibilidade. */
final class DashboardHtml {

    private DashboardHtml() {}

    static final String PAGINA = """
<!DOCTYPE html>
<html lang="pt-br">
<head>
<meta charset="utf-8">
<title>PoC - Propagacao de Emocoes em Redes Sociais</title>
<style>
  :root {
    --cor-anger: #e0483e;
    --cor-disgust: #5a8f4f;
    --cor-joy: #e8b933;
    --cor-sadness: #3f7ab0;
    --fundo: #14161c;
    --painel: #1d2029;
    --borda: #2c3040;
    --texto: #e7e9ee;
    --texto-fraco: #9aa0b4;
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; font-family: -apple-system, Segoe UI, Roboto, Arial, sans-serif;
    background: var(--fundo); color: var(--texto);
  }
  header {
    padding: 16px 24px; border-bottom: 1px solid var(--borda);
    display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px 24px;
  }
  header h1 { font-size: 18px; margin: 0; }
  header .sub { color: var(--texto-fraco); font-size: 13px; }
  #meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; color: var(--texto-fraco); }
  #meta b { color: var(--texto); }
  main {
    display: grid; grid-template-columns: 1.3fr 1fr; gap: 16px;
    padding: 16px 24px; max-width: 1400px; margin: 0 auto;
  }
  .painel {
    background: var(--painel); border: 1px solid var(--borda); border-radius: 10px; padding: 14px;
  }
  .painel h2 { font-size: 14px; margin: 0 0 10px 0; color: var(--texto-fraco); font-weight: 600; text-transform: uppercase; letter-spacing: .04em; }
  canvas { width: 100%; display: block; background: #10121a; border-radius: 8px; }
  .controles { display: flex; align-items: center; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
  .controles input[type=range] { flex: 1; min-width: 160px; }
  button {
    background: #2c3040; color: var(--texto); border: 1px solid #3a3f52; border-radius: 6px;
    padding: 7px 12px; cursor: pointer; font-size: 13px;
  }
  button:hover { background: #363c52; }
  .legenda { display: flex; gap: 14px; margin-top: 8px; font-size: 12px; color: var(--texto-fraco); flex-wrap: wrap; }
  .legenda span { display: inline-flex; align-items: center; gap: 6px; }
  .ponto { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
  .links { display: flex; gap: 16px; margin-left: auto; }
  .links a { color: var(--texto-fraco); font-size: 13px; text-decoration: underline; }
  #status { font-size: 12px; color: var(--texto-fraco); min-height: 16px; }
  @media (max-width: 900px) { main { grid-template-columns: 1fr; } }
</style>
</head>
<body>
<header>
  <h1>Propagacao de emocoes em redes sociais</h1>
  <span class="sub">TCC II &mdash; visualizacao do resultado da simulacao</span>
  <div class="links">
    <a href="/executar">Executar novo cenário &rarr;</a>
    <a href="/comparacao">Comparar com redes sintéticas &rarr;</a>
  </div>
  <div id="meta"></div>
</header>
<main>
  <section class="painel">
    <h2>Rede social e estado emocional por ciclo</h2>
    <canvas id="grafo" width="760" height="520"></canvas>
    <div class="controles">
      <button id="btnPlay">Reproduzir</button>
      <input id="slider" type="range" min="0" max="0" value="0" step="1">
      <span id="rotuloCiclo">ciclo 0</span>
    </div>
    <div class="legenda">
      <span><i class="ponto" style="background:var(--cor-anger)"></i>raiva</span>
      <span><i class="ponto" style="background:var(--cor-disgust)"></i>nojo</span>
      <span><i class="ponto" style="background:var(--cor-joy)"></i>alegria</span>
      <span><i class="ponto" style="background:var(--cor-sadness)"></i>tristeza</span>
      <span><i class="ponto" style="background:#5b6070"></i>neutro (sem feature reconhecida)</span>
      <span>&#9679; contorno mais claro = no-semente</span>
    </div>
  </section>

  <section class="painel">
    <h2>Media de intensidade emocional ao longo dos ciclos</h2>
    <canvas id="grafico" width="560" height="300"></canvas>
    <div class="legenda">
      <span><i class="ponto" style="background:var(--cor-anger)"></i>raiva</span>
      <span><i class="ponto" style="background:var(--cor-disgust)"></i>nojo</span>
      <span><i class="ponto" style="background:var(--cor-joy)"></i>alegria</span>
      <span><i class="ponto" style="background:var(--cor-sadness)"></i>tristeza</span>
    </div>
    <p style="font-size:12px;color:var(--texto-fraco);margin-top:8px;line-height:1.5">
      Escala do eixo Y ajustada ao maior valor observado (mostrado no topo do gráfico) — a
      maioria dos nós fica neutra, então a média geral costuma ser pequena mesmo quando a
      propagação está acontecendo. Veja o grafo da rede à esquerda para o estado por nó.
    </p>
  </section>
</main>

<script>
const CORES = { ANGER: '#e0483e', DISGUST: '#5a8f4f', JOY: '#e8b933', SADNESS: '#3f7ab0', NEUTRO: '#5b6070' };
let dados = null;
let cicloAtual = 0;
let reproduzindo = false;
let timerReproducao = null;

async function carregar() {
  const resp = await fetch('/api/resultado.json', { cache: 'no-store' });
  dados = await resp.json();
  cicloAtual = 0;
  aplicarDados();
}

function aplicarDados() {
  if (!dados || !dados.meta) return;
  const m = dados.meta;
  document.getElementById('meta').innerHTML =
    '<span><b>' + m.numAgentes + '</b> agentes</span>' +
    '<span><b>' + m.numCiclos + '</b> ciclos</span>' +
    '<span>alpha=<b>' + m.alpha + '</b></span>' +
    '<span>delta=<b>' + m.delta + '</b></span>' +
    '<span>fonte=<b>' + m.fonteDados + '</b></span>' +
    (m.twitterEgoId ? '<span>ego=<b>' + m.twitterEgoId + '</b></span>' : '');

  const slider = document.getElementById('slider');
  slider.max = Math.max(0, dados.ciclos.length - 1);
  slider.value = 0;
  cicloAtual = 0;
  posicionarNos();
  desenharGrafo();
  desenharGrafico();
  atualizarRotulo();
}

// --- layout circular determinístico, agrupando por comunidade aproximada (grau + vizinhanca) ---
let posicoes = {};
function posicionarNos() {
  posicoes = {};
  const n = dados.nos.length;
  const raio = 220;
  const cx = 380, cy = 260;
  for (let i = 0; i < n; i++) {
    const ang = (2 * Math.PI * i) / n;
    posicoes[dados.nos[i].id] = {
      x: cx + raio * Math.cos(ang),
      y: cy + raio * Math.sin(ang)
    };
  }
}

function estadoDoNo(id, ciclo) {
  const c = dados.ciclos[ciclo];
  if (!c) return null;
  for (const e of c.estados) if (e.id === id) return e;
  return null;
}

function desenharGrafo() {
  const canvas = document.getElementById('grafo');
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  ctx.strokeStyle = 'rgba(255,255,255,0.10)';
  ctx.lineWidth = 1;
  for (const a of dados.arestas) {
    const p1 = posicoes[a.origem], p2 = posicoes[a.destino];
    if (!p1 || !p2) continue;
    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
  }

  for (const no of dados.nos) {
    const p = posicoes[no.id];
    const estado = estadoDoNo(no.id, cicloAtual);
    const cor = estado ? CORES[estado.dominante] : '#666';
    const raioNo = 4 + Math.min(8, no.grau * 0.6);
    ctx.beginPath();
    ctx.arc(p.x, p.y, raioNo, 0, 2 * Math.PI);
    ctx.fillStyle = cor;
    ctx.fill();
    if (dados.meta.indiceSemente === no.id) {
      ctx.lineWidth = 2;
      ctx.strokeStyle = '#ffffff';
      ctx.stroke();
    }
  }
}

function desenharGrafico() {
  const canvas = document.getElementById('grafico');
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const margem = 36;
  const largura = canvas.width - margem * 2;
  const altura = canvas.height - margem * 2;
  const n = dados.ciclos.length;

  const chaves = ['anger', 'disgust', 'joy', 'sadness'];
  const coresChave = { anger: CORES.ANGER, disgust: CORES.DISGUST, joy: CORES.JOY, sadness: CORES.SADNESS };

  // Escala automática: o eixo Y vai de 0 até o maior valor observado em
  // qualquer emoção/ciclo (com uma folga de 15%), em vez de assumir 0-1
  // fixo — como a maioria dos nós costuma ficar neutra, a média geral é
  // tipicamente pequena, e uma escala fixa 0-1 deixaria a linha colada
  // no eixo X, escondendo a evolução real.
  let valorMax = 0;
  for (let i = 0; i < n; i++) {
    for (const chave of chaves) {
      valorMax = Math.max(valorMax, dados.ciclos[i].media[chave]);
    }
  }
  if (valorMax <= 0) valorMax = 1;
  valorMax *= 1.15;

  ctx.strokeStyle = 'rgba(255,255,255,0.15)';
  ctx.beginPath();
  ctx.moveTo(margem, margem);
  ctx.lineTo(margem, margem + altura);
  ctx.lineTo(margem + largura, margem + altura);
  ctx.stroke();

  // rótulos do eixo Y (0, metade, máximo)
  ctx.fillStyle = 'rgba(231,233,238,0.6)';
  ctx.font = '11px sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText(valorMax.toFixed(3), margem - 6, margem + 4);
  ctx.fillText((valorMax / 2).toFixed(3), margem - 6, margem + altura / 2 + 4);
  ctx.fillText('0', margem - 6, margem + altura + 4);

  for (const chave of chaves) {
    ctx.beginPath();
    ctx.strokeStyle = coresChave[chave];
    ctx.lineWidth = 2;
    for (let i = 0; i < n; i++) {
      const valor = dados.ciclos[i].media[chave];
      const x = margem + (n <= 1 ? 0 : (largura * i) / (n - 1));
      const y = margem + altura - (valor / valorMax) * altura;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.stroke();
  }

  // marcador do ciclo selecionado
  if (n > 0) {
    const x = margem + (n <= 1 ? 0 : (largura * cicloAtual) / (n - 1));
    ctx.strokeStyle = 'rgba(255,255,255,0.5)';
    ctx.beginPath();
    ctx.moveTo(x, margem);
    ctx.lineTo(x, margem + altura);
    ctx.stroke();
  }
}

function atualizarRotulo() {
  document.getElementById('rotuloCiclo').textContent = 'ciclo ' + cicloAtual;
}

document.getElementById('slider').addEventListener('input', (ev) => {
  cicloAtual = parseInt(ev.target.value, 10);
  desenharGrafo();
  desenharGrafico();
  atualizarRotulo();
});

document.getElementById('btnPlay').addEventListener('click', () => {
  reproduzindo = !reproduzindo;
  document.getElementById('btnPlay').textContent = reproduzindo ? 'Pausar' : 'Reproduzir';
  if (reproduzindo) {
    timerReproducao = setInterval(() => {
      const slider = document.getElementById('slider');
      let proximo = cicloAtual + 1;
      if (proximo > parseInt(slider.max, 10)) proximo = 0;
      slider.value = proximo;
      cicloAtual = proximo;
      desenharGrafo();
      desenharGrafico();
      atualizarRotulo();
    }, 450);
  } else {
    clearInterval(timerReproducao);
  }
});

carregar();
</script>
</body>
</html>
""";
}
