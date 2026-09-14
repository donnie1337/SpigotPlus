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

## Status

Projeto em fase inicial de arquitetura. A implementação será construída por etapas, começando pelo núcleo, bootstrap e contratos internos antes da integração completa de mundos e protocolos.
