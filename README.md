# bytecode-ai-agent

Agente de IA para **análise de bytecode / desmontagem**. Recebe binários Java
(`.class`/`.jar`) ou Android (`.apk`), desmonta/decompila o código e envia os
trechos para a API da **Anthropic (Claude)**, que retorna uma explicação
simplificada da lógica e aponta possíveis vulnerabilidades. Documentado via
**OpenAPI/Swagger**.

## Stack

Java 21 · Spring Boot 3.3.5 · springdoc-openapi (Swagger UI) · ASM (desmontagem
de bytecode JVM) · JADX (decompilação de APK, ferramenta externa) · API de
Messages da Anthropic.

## Arquitetura

```
POST /api/v1/analysis/bytecode (.class/.jar)
POST /api/v1/analysis/apk      (.apk)
                │
                ▼
      CodeExtractor (ASM | JADX)  ──► desmonta/decompila cada classe
                │
                ▼
      ClaudeAnalysisClient  ──► Anthropic Messages API
                │
                ▼
      AnalyzeResponse { explicação + vulnerabilidades por classe }
```

- `service/extract/JarBytecodeExtractor`: lê `.class`/`.jar` e desmonta cada
  classe com ASM (`Textifier`, estilo `javap -c`). Protegido contra zip bomb
  (limite configurável de bytes descomprimidos).
- `service/extract/ApkJadxExtractor`: chama o executável **JADX** externo para
  decompilar um `.apk` em Java legível. Requer JADX instalado (ver abaixo);
  se não estiver disponível, o endpoint `/apk` responde `503` com instruções.
- `service/ai/ClaudeAnalysisClient`: monta o prompt (`PromptBuilder`) e chama
  `POST /v1/messages` da Anthropic, pedindo uma resposta JSON estrita com
  `explanation` e `vulnerabilities`.
- `service/AnalysisService`: orquestra extração + chamadas à IA em paralelo
  (um `ExecutorService` de virtual threads do Java 21, uma por classe).

## Como rodar

```bash
export ANTHROPIC_API_KEY=sk-ant-...
mvn spring-boot:run
```

Servidor em `http://localhost:8080`. Swagger UI em
`http://localhost:8080/swagger-ui.html` (JSON em `/v3/api-docs`).

### Abrir no IntelliJ

File → Open → selecione a pasta `bytecode-ai-agent` (ou o `pom.xml`). O
IntelliJ detecta o projeto Maven automaticamente. Garanta um JDK 21
configurado em *Project Structure*.

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `ANTHROPIC_API_KEY` | _(vazio)_ | Obrigatória para os endpoints de análise funcionarem |
| `ANTHROPIC_MODEL` | `claude-sonnet-4-5` | Modelo usado nas chamadas |
| `ANTHROPIC_MAX_TOKENS` | `1024` | Tokens máximos de saída por classe analisada |
| `MAX_CLASSES_PER_REQUEST` | `15` | Limite de classes analisadas por upload |
| `MAX_SNIPPET_CHARS` | `6000` | Trunca o código enviado à IA por classe |
| `MAX_ZIP_BYTES` | `52428800` (50MB) | Limite de bytes descomprimidos de um `.jar` (anti zip-bomb) |
| `JADX_EXECUTABLE` | `jadx` | Caminho/nome do executável do JADX |
| `JADX_TIMEOUT_SECONDS` | `60` | Timeout da decompilação de APK |

### Instalar o JADX (necessário só para `/apk`)

```bash
# via releases do projeto: https://github.com/skylot/jadx/releases
unzip jadx-*.zip -d ~/jadx
export JADX_EXECUTABLE=~/jadx/bin/jadx
```

Sem o JADX instalado, `/api/v1/analysis/apk` responde `503` e
`/api/v1/analysis/health` mostra `jadxAvailable: false`.

## Endpoints

- `POST /api/v1/analysis/bytecode` — multipart `file` (`.class` ou `.jar`).
- `POST /api/v1/analysis/apk` — multipart `file` (`.apk`).
- `GET /api/v1/analysis/health` — indica se a chave da Anthropic e o JADX
  estão disponíveis.

Exemplo:

```bash
curl -X POST localhost:8080/api/v1/analysis/bytecode \
  -F "file=@MinhaClasse.class"
```

Resposta (resumida):

```json
{
  "artifact": "MinhaClasse.class",
  "engine": "ASM",
  "classesAnalyzed": 1,
  "overallSummary": "Analisadas 1 unidade(s); 1 possivel(is) vulnerabilidade(s) identificada(s) pela IA.",
  "classes": [
    {
      "unitName": "MinhaClasse",
      "engine": "ASM",
      "code": "// bytecode desmontado (ASM) ...",
      "aiExplanation": "Este metodo concatena ...",
      "vulnerabilities": [
        {"severity": "HIGH", "category": "hardcoded-secret", "description": "...", "evidence": "..."}
      ]
    }
  ]
}
```

## Testes

```bash
mvn test
```

Cobertura: desmontagem via ASM (`BytecodeDisassemblerTest`), extração de
`.class`/`.jar` com guarda anti zip-bomb (`JarBytecodeExtractorTest`),
montagem do prompt (`PromptBuilderTest`) e orquestração completa com o
cliente de IA mockado (`AnalysisServiceTest`). Nenhum teste chama a API real
da Anthropic nem requer o JADX instalado.

> Nota: o `pom.xml` define `-Dnet.bytebuddy.experimental=true` no Surefire
> porque o Mockito/Byte Buddy usado pelo Spring Boot 3.3.5 ainda não suporta
> oficialmente versões de JDK muito recentes (ex.: Java 25) como *runtime* de
> teste — a flag libera a instrumentação de mocks nesse cenário.

## Limitações e próximos passos

- Decompilação de `.jar`/`.class` é feita por **desmontagem de bytecode**
  (ASM/`javap`-like), não por um decompilador de fonte Java completo. Um
  motor adicional (ex.: CFR/Vineflower) pode ser plugado implementando
  `CodeExtractor`.
- Binários não são persistidos em disco além dos arquivos temporários usados
  pelo JADX (removidos ao final da requisição).
- Upload limitado a 25MB (`spring.servlet.multipart.max-*`).
