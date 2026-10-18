# Dataset Twitter

Coloque aqui o dataset do Twitter Social Circles ou utilize diretamente o arquivo `.tar.gz` por meio do parâmetro `--twitter`.

Exemplo:

```bash
mvn exec:java -Dexec.args="--twitter /caminho/twitter.tar.gz 11681802"
```

O dataset não é embutido no código-fonte. O loader aceita tanto o `.tar.gz` original quanto uma pasta extraída.
