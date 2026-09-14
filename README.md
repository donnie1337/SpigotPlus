# SpigotPlus

Servidor Minecraft compatível com clientes **1.19.x até 26.2.x**, com foco em segurança, desempenho, estabilidade e compatibilidade com Bukkit/Spigot.

## Objetivos

- Suporte multi-versão de Minecraft: 1.19.x → 26.2.x.
- Compatibilidade com APIs e plugins Bukkit/Spigot sempre que possível.
- Núcleo desacoplado da versão do protocolo.
- Processamento assíncrono controlado para IO e operações de chunks.
- Validação e rate limiting de pacotes antes de chegarem ao núcleo.
- Scheduler e sistema de eventos com baixo overhead.
- Salvamento seguro e shutdown controlado para reduzir perda de dados.
- Diagnóstico de TPS/MSPT, tarefas, chunks, memória e rede.

## Arquitetura

```text
SpigotPlus
├── Bootstrap
├── Core
├── API
│   ├── Bukkit
│   └── Spigot
├── Protocol
│   ├── 1.19+
│   └── 26.2
├── World
├── Network
├── Security
├── Plugin
├── Storage
├── Scheduler
├── Performance
└── Compatibility
```

O Core não deve depender diretamente de detalhes específicos de uma versão de Minecraft. Diferenças de protocolo, registries, pacotes e capacidades do cliente ficam isoladas na camada `Protocol`.

## Compatibilidade de clientes

O objetivo é permitir que jogadores de versões entre **1.19.x e 26.2.x** se conectem ao mesmo servidor e ao mesmo mundo, usando adaptadores de protocolo quando necessário.

## Segurança

O pipeline de rede seguirá o modelo:

```text
Cliente
  ↓
Network Decoder
  ↓
Packet Validation
  ↓
Security / Rate Limit
  ↓
Protocol Adapter
  ↓
Internal API
  ↓
Core
```

## Bootstrap e Core

A implementação inicial possui um entrypoint Maven executável, ciclo de vida explícito e uma thread autoritativa de 20 TPS. O bootstrap cria o `ServerRuntime`, instala o shutdown hook e aguarda a terminação do servidor.

Estados do ciclo de vida:

```text
NEW → STARTING → RUNNING → STOPPING → STOPPED
                       └──────────────→ FAILED
```

O loop de tick não conhece versões de Minecraft. World, Network, Plugin e os adaptadores de protocolo serão conectados ao Core em etapas posteriores.

## Build

Requer **JDK 26** e Maven.

```bash
mvn -B clean package
java -jar target/SpigotPlus-1.0.0-SNAPSHOT.jar
```

## Status

Projeto em implementação. Bootstrap, Core lifecycle e tick engine já estão estabelecidos; as próximas etapas são configuração persistente, scheduler de tarefas, API interna, pipeline de rede e os primeiros adaptadores de protocolo.
