package br.unb.poc.interfaces;

/** HTML/CSS/JS da página "executar novo cenário", separada do dashboard principal. */
final class ExecutarHtml {

    private ExecutarHtml() {}

    static final String PAGINA = """
<!DOCTYPE html>
<html lang="pt-br">
<head>
<meta charset="utf-8">
<title>Executar novo cenário</title>
<style>
  :root {
    --fundo: #14161c; --painel: #1d2029; --borda: #2c3040;
    --texto: #e7e9ee; --texto-fraco: #9aa0b4;
  }
  * { box-sizing: border-box; }
  body { margin:0; font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif; background:var(--fundo); color:var(--texto); }
  header { padding:16px 24px; border-bottom:1px solid var(--borda); display:flex; align-items:baseline; gap:16px; flex-wrap:wrap; }
  header h1 { font-size:18px; margin:0; }
  header a { color:var(--texto-fraco); font-size:13px; text-decoration:underline; margin-left:auto; }
  main { max-width:560px; margin:0 auto; padding:24px; }
  .painel { background:var(--painel); border:1px solid var(--borda); border-radius:10px; padding:18px; }
  .painel p.intro { font-size:13px; color:var(--texto-fraco); margin:0 0 16px 0; line-height:1.5; }
  form { display:grid; grid-template-columns:repeat(2,1fr); gap:12px 14px; }
  label { display:flex; flex-direction:column; gap:4px; font-size:13px; color:var(--texto-fraco); }
  input { background:#10121a; border:1px solid var(--borda); border-radius:6px; color:var(--texto); padding:8px 10px; font-size:14px; }
  .acao { grid-column:1/-1; display:flex; justify-content:flex-end; align-items:center; gap:10px; margin-top:4px; }
  button { background:#2c3040; color:var(--texto); border:1px solid #3a3f52; border-radius:6px; padding:9px 16px; cursor:pointer; font-size:14px; }
  button:hover { background:#363c52; }
  button:disabled { opacity:0.6; cursor:default; }
  #status { font-size:13px; color:var(--texto-fraco); }
  #status.erro { color:#e0483e; }
  #status.ok { color:#5a8f4f; }
  .aviso { font-size:12px; color:var(--texto-fraco); margin-top:14px; line-height:1.5; }
</style>
</head>
<body>
<header>
  <h1>Executar novo cenário</h1>
  <a href="/">&larr; voltar ao dashboard</a>
</header>
<main>
  <section class="painel">
    <p class="intro" id="intro">Carregando cenário atual...</p>
    <form id="config">
      <label id="campoAgentes" style="display:none">Agentes
        <input name="numAgentes" type="number" min="4" max="300" value="30">
      </label>
      <label>Ciclos
        <input name="numCiclos" type="number" min="1" max="200" value="20">
      </label>
      <label>Alpha (persistência)
        <input name="alpha" type="number" min="0" max="1" step="0.05" value="0.6">
      </label>
      <label>Delta (decaimento)
        <input name="delta" type="number" min="0" max="1" step="0.01" value="0.02">
      </label>
      <label id="campoSemente" style="display:none">Semente
        <input name="semente" type="number" value="42">
      </label>
      <div class="acao">
        <span id="status"></span>
        <button type="submit" id="btnExecutar">Executar</button>
      </div>
    </form>
    <p class="aviso" id="avisoModo"></p>
  </section>
</main>

<script>
let modoDemo = true;

async function carregarMeta() {
  try {
    const resp = await fetch('/api/resultado.json', { cache: 'no-store' });
    const dados = await resp.json();
    const m = dados.meta || {};
    modoDemo = (m.twitterEgoId === 'demo' || !m.twitterEgoId);

    document.getElementById('campoAgentes').style.display = modoDemo ? '' : 'none';
    document.getElementById('campoSemente').style.display = modoDemo ? '' : 'none';

    const form = document.getElementById('config');
    if (m.numAgentes) form.numAgentes.value = m.numAgentes;
    if (m.numCiclos) form.numCiclos.value = m.numCiclos;
    if (m.alpha !== undefined) form.alpha.value = m.alpha;
    if (m.delta !== undefined) form.delta.value = m.delta;

    document.getElementById('intro').textContent = modoDemo
      ? 'Modo de demonstração: gera uma nova rede sintética (formato Twitter Social Circles) com os parâmetros abaixo.'
      : 'Dataset real (ego ' + (m.twitterEgoId || '?') + '): a rede não muda, mas você pode reexecutar a propagação com outros valores de ciclos, alpha e delta.';
  } catch (e) {
    document.getElementById('intro').textContent = 'Não foi possível carregar o cenário atual — o formulário ainda pode ser usado com os valores padrão.';
  }
}

document.getElementById('config').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const status = document.getElementById('status');
  const botao = document.getElementById('btnExecutar');
  status.className = '';
  status.textContent = 'Executando...';
  botao.disabled = true;

  const dadosForm = new FormData(ev.target);
  const params = new URLSearchParams();
  for (const [chave, valor] of dadosForm.entries()) {
    if (!modoDemo && (chave === 'numAgentes' || chave === 'semente')) continue;
    params.set(chave, valor);
  }

  try {
    const resp = await fetch('/api/executar?' + params.toString());
    if (!resp.ok) {
      const erro = await resp.json().catch(() => ({}));
      status.className = 'erro';
      status.textContent = 'Erro: ' + (erro.erro || resp.status);
      botao.disabled = false;
      return;
    }
    status.className = 'ok';
    status.textContent = 'Cenário executado — abrindo o dashboard...';
    setTimeout(() => { window.location.href = '/'; }, 600);
  } catch (e) {
    status.className = 'erro';
    status.textContent = 'Falha ao executar: ' + e;
    botao.disabled = false;
  }
});

carregarMeta();
</script>
</body>
</html>
""";
}
