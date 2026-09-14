# SpigotPlus — Arquitetura

## 1. Princípios

1. O Core deve ser independente da versão do protocolo.
2. Compatibilidade Bukkit/Spigot deve ficar isolada em uma camada de compatibilidade.
3. Operações de IO não devem bloquear a thread principal sem necessidade.
4. Trabalho assíncrono nunca deve modificar diretamente estado crítico do mundo sem passar pelos mecanismos seguros do Core.
5. Entrada de rede deve ser validada antes de alcançar a lógica do servidor.
6. Diferenças entre versões devem ser tratadas por adaptadores e capabilities, evitando condicionais de versão espalhadas pelo código.
7. Falhas de plugins, IO e rede devem ser observáveis e não derrubar silenciosamente o núcleo do servidor.

## 2. Camadas

```text
┌──────────────────────────────────────────────┐
│              Bukkit / Spigot API             │
├──────────────────────────────────────────────┤
│            Compatibility Layer               │
├──────────────────────────────────────────────┤
│                 Core API                     │
├───────────────┬──────────────┬───────────────┤
│    World      │   Players    │    Plugin     │
├───────────────┼──────────────┼───────────────┤
│    Storage    │  Scheduler   │ Performance   │
├───────────────┴──────────────┴───────────────┤
│             Network / Security              │
├──────────────────────────────────────────────┤
│          Version Protocol Adapters           │
└──────────────────────────────────────────────┘
```

## 3. Protocol adapters

Cada família de protocolo deve traduzir o tráfego externo para uma representação interna estável. O Core não deve precisar conhecer IDs ou estruturas específicas de uma versão para executar regras de negócio.

A compatibilidade deve ser baseada em capacidades quando possível:

- componentes de itens;
- chat moderno;
- registries;
- entidades disponíveis;
- fases de configuração de conexão;
- recursos de inventário;
- partículas e outros recursos específicos.

## 4. Network pipeline

```text
Socket
  → Decoder
  → Size / Structure Validation
  → Rate Limiter
  → Security Checks
  → Version Adapter
  → Internal Packet
  → Core
```

Pacotes inválidos ou excessivos devem ser rejeitados antes de consumir recursos caros do Core.

## 5. Scheduler

O scheduler terá separação explícita entre:

- trabalho de tick/main thread;
- tarefas assíncronas;
- IO;
- trabalhos de chunks.

Nenhuma operação assíncrona deve assumir que objetos do mundo podem ser modificados livremente fora da thread responsável por eles.

## 6. Chunks e IO

O carregamento, geração e salvamento serão organizados por prioridade. Chunks necessários imediatamente para jogadores devem ter prioridade sobre pré-carregamentos distantes.

IO de jogadores, mundos e plugins deve usar filas controladas, com flush e shutdown seguros.

## 7. Observabilidade

O servidor deverá expor métricas e diagnóstico para:

- TPS;
- MSPT;
- tempo de tarefas;
- filas de IO;
- chunks carregando/gerando/salvando;
- conexões e pacotes;
- memória;
- consumo por plugins.

## 8. Shutdown e recuperação

O shutdown deve impedir novas operações, finalizar tarefas críticas, salvar jogadores e mundos, fazer flush de IO e somente então fechar a rede e os plugins.

Falhas críticas devem produzir diagnóstico suficiente para identificar o subsistema responsável.

## 9. Ordem de implementação

1. Bootstrap e Core.
2. Configuração e logging.
3. Scheduler e lifecycle.
4. API interna e contratos.
5. Network pipeline.
6. Primeiros adaptadores de protocolo.
7. Plugin loader e compatibilidade Bukkit/Spigot.
8. World/chunks/storage.
9. Segurança avançada.
10. Performance e profiling.
