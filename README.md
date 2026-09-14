# SpigotPlus

Distribuição e bootstrap de servidor **Spigot 26.2**, mantendo o ecossistema Bukkit/Spigot como base e adicionando compatibilidade Java/Bedrock.

## Visão geral

```text
                         SPIGOTPLUS
                             │
                    Bootstrap / Launcher
                             │
                    Spigot 26.2 pré-compilado
                             │
              ┌──────────────┼──────────────┐
              │              │              │
         ViaVersion     ViaBackwards      Geyser
              │              │              │
              └──────────────┼──────────────┘
                             │
                          Spigot
                             │
                    Bukkit / Spigot API
                             │
          ┌──────────┬───────┼───────┬──────────┐
          │          │       │       │          │
     Essentials   Cargo   ChatPlus LoginPlus Utilidades
        Plus       Plus                         Plus
```

### Componentes

- **Spigot 26.2** — motor principal do servidor.
- **ViaVersion** — compatibilidade com clientes Java de versões diferentes.
- **ViaBackwards** — compatibilidade com versões Java anteriores suportadas pelo conjunto Via.
- **Geyser-Spigot** — conexão de jogadores Bedrock ao servidor Java.
- **Plugins Bukkit/Spigot** — continuam utilizando a API padrão do Spigot.

## Compatibilidade

### Servidor

- **Minecraft 26.2**
- **Spigot 26.2**
- **JDK 26**

### Clientes Java

O objetivo é permitir clientes Java de **1.19.x até 26.2.x** no mesmo servidor, utilizando ViaVersion/ViaBackwards quando necessário.

A compatibilidade exata depende das versões suportadas pelas camadas Via instaladas. O SpigotPlus não substitui nem modifica o motor do Spigot para realizar essa compatibilidade.

### Clientes Bedrock

O Geyser-Spigot fornece a ponte entre Bedrock e Java.

```text
Bedrock UDP: 19132
```

## Arquitetura atual

O SpigotPlus **não é um fork independente do Minecraft**. O servidor final continua sendo um **Spigot 26.2 oficial compilado**.

O fluxo de execução é simples:

```text
SpigotPlus.jar
      │
      ▼
verifica spigot.jar
      │
      ├── existe e é válido ──► inicia Spigot
      │
      └── ausente/inválido ───► informa o erro e encerra
```

### Importante

O SpigotPlus **não executa o BuildTools durante a inicialização**.

Também não:

- compila o Spigot a cada startup;
- baixa o BuildTools durante o startup;
- cria workspace `.spigot-build` para produção;
- baixa automaticamente os plugins de compatibilidade durante o startup.

Isso deixa a execução do servidor rápida, previsível e independente de downloads ou caches de compilação.

## Build do Spigot

O `spigot.jar` deve ser **pré-compilado antes da execução/distribuição**.

O BuildTools oficial continua sendo utilizado para gerar o Spigot, porém como parte do processo de **build/release**, e não como parte do runtime do servidor.

Depois de compilar o Spigot 26.2, coloque o JAR final no diretório do servidor com o nome:

```text
spigot.jar
```

É recomendado utilizar um workspace limpo e fora de diretórios sincronizados, como OneDrive, durante a compilação.

## Inicialização

Com o `spigot.jar` já preparado:

```bash
java -Xms2048M -Xmx4096M -jar SpigotPlus.jar nogui
```

O launcher valida os arquivos básicos e executa:

```text
spigot.jar
```

Se o `spigot.jar` não existir ou for inválido, o SpigotPlus encerra a inicialização e informa que o JAR pré-compilado precisa ser colocado no diretório do servidor.

## Estrutura do servidor

```text
Servidor/
├── SpigotPlus.jar
├── spigot.jar
├── plugins/
│   ├── Geyser-Spigot.jar
│   ├── ViaVersion.jar
│   ├── ViaBackwards.jar
│   └── ...
├── server.properties
├── eula.txt
├── world/
├── world_nether/
└── world_the_end/
```

O `spigot.jar` é o servidor Spigot 26.2 pré-compilado.

O `SpigotPlus.jar` é apenas o launcher responsável por iniciar esse servidor.

Os plugins de compatibilidade devem estar previamente presentes em `plugins/` quando forem utilizados.

## Compatibilidade com plugins

Como o servidor final continua sendo **Spigot**, plugins Bukkit/Spigot podem ser utilizados normalmente, respeitando as APIs e limitações da versão do servidor.

Plugins integrados ao ambiente do projeto incluem:

- EssentialsPlus
- CargoPlus
- UtilidadesPlus
- ChatPlus
- LoginPlus
- ClanPlus
- ViaVersion
- ViaBackwards
- Geyser-Spigot

## Rede e compatibilidade

### Java

```text
Cliente Java 1.19.x → 26.2.x
              │
              ▼
        ViaVersion / ViaBackwards
              │
              ▼
           Spigot 26.2
              │
              ▼
       Bukkit / Spigot API
              │
              ▼
            Plugins
```

### Bedrock

```text
Cliente Bedrock
      │
      ▼
 Geyser-Spigot
      │
      ▼
  Spigot 26.2
      │
      ▼
    Plugins
```

## Build do projeto

Requer:

- **JDK 26**
- **Maven**

```bash
mvn -B clean package
```

O artefato do projeto é gerado em `target/`.

A compilação do SpigotPlus e a compilação do `spigot.jar` são processos separados: o Maven gera o launcher e o BuildTools é utilizado separadamente para preparar o Spigot.

## Estado atual

### Funcionando

- [x] Bootstrap do SpigotPlus
- [x] Inicialização do Spigot 26.2 através de `spigot.jar` pré-compilado
- [x] Inicialização sem BuildTools
- [x] Inicialização sem compilação automática do Spigot
- [x] Inicialização sem download automático de plugins
- [x] Java 26
- [x] ViaVersion
- [x] ViaBackwards
- [x] Geyser-Spigot
- [x] Execução dos plugins Bukkit/Spigot
- [x] Mundos Java do servidor

### Próximos testes / evolução

- [ ] Validar clientes Java 1.19.x → 26.2.x individualmente
- [ ] Validar conexão Bedrock em rede externa
- [ ] Ajustar configurações de ping/legacy do Geyser quando necessário
- [ ] Documentar configurações recomendadas de rede e firewall
- [ ] Expandir ferramentas de diagnóstico e manutenção
- [ ] Automatizar o processo de release para gerar e empacotar o `spigot.jar` previamente

## Objetivo do projeto

O objetivo do SpigotPlus é fornecer uma instalação de servidor **Spigot moderna, simples e compatível**, mantendo o ecossistema Bukkit/Spigot como base e adicionando:

- múltiplas versões de clientes Java;
- acesso de jogadores Bedrock;
- inicialização rápida através de um `spigot.jar` pré-compilado;
- distribuição organizada dos componentes de compatibilidade;
- manutenção centralizada da distribuição.

> **SpigotPlus = Spigot como núcleo + launcher de distribuição + compatibilidade Java/Bedrock.**
