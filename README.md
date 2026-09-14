# SpigotPlus

Distribuição e bootstrap de servidor **Spigot**, preparada para executar o servidor Minecraft em **26.2** com compatibilidade de clientes de versões anteriores através da camada de protocolo.

O projeto foi criado para manter o servidor baseado no ecossistema **Bukkit/Spigot**, sem substituir o motor do Spigot por um engine próprio.

## Visão geral

```text
                         SPIGOTPLUS
                             │
                    Bootstrap / Launcher
                             │
                    Official Spigot 26.2
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
- **ViaVersion** — permite a entrada de clientes de versões diferentes da versão do servidor.
- **ViaBackwards** — amplia a compatibilidade com versões anteriores suportadas pelo conjunto Via.
- **Geyser-Spigot** — permite a conexão de jogadores Bedrock ao servidor Java.
- **Plugins Bukkit/Spigot** — continuam funcionando através da API padrão do Spigot.

## Compatibilidade

### Servidor

A versão de servidor atualmente utilizada pelo SpigotPlus é:

- **Minecraft 26.2**
- **Spigot 26.2**
- **JDK 26**

### Clientes Java

O objetivo do projeto é permitir que clientes Java de **1.19.x até 26.2.x** entrem no mesmo servidor, utilizando ViaVersion/ViaBackwards quando necessário.

A compatibilidade exata de cada versão depende das versões suportadas pelas camadas Via instaladas.

### Clientes Bedrock

O Geyser-Spigot fornece a ponte entre Bedrock e Java.

Configuração padrão utilizada no ambiente atual:

```text
Bedrock UDP: 19132
```

O jogador Bedrock entra pelo endereço do servidor usando a porta UDP configurada no Geyser.

## Arquitetura atual

O SpigotPlus **não é um fork independente do Minecraft** e não substitui o funcionamento interno do Spigot.

O projeto atua como uma camada de distribuição/bootstrap que:

1. prepara o ambiente do servidor;
2. obtém o BuildTools oficial do Spigot;
3. compila o Spigot 26.2 quando necessário;
4. localiza o JAR compilado do Spigot;
5. copia o resultado para `spigot.jar` no diretório do servidor;
6. prepara os plugins de compatibilidade;
7. inicia o Spigot normalmente.

Isso mantém a compatibilidade com o ecossistema Bukkit/Spigot e permite que os plugins do servidor continuem utilizando a API convencional.

## Build do Spigot

O SpigotPlus utiliza o **Spigot BuildTools oficial** para gerar o servidor.

A compilação é feita em um workspace isolado, fora do diretório principal do servidor. No Windows, o workspace preferencial fica em:

```text
%LOCALAPPDATA%\SpigotPlus\BuildTools\26.2\
```

Caso `LOCALAPPDATA` não esteja disponível, o sistema utiliza um diretório temporário.

Antes de uma nova compilação, o workspace é limpo, mantendo apenas o `BuildTools.jar`. Isso evita que arquivos temporários, caches ou artefatos de uma compilação anterior contaminem uma nova execução.

O isolamento também evita problemas quando o servidor está dentro de diretórios sincronizados, como OneDrive.

## Inicialização

O launcher do SpigotPlus prepara o servidor e, depois, inicia o Spigot.

Exemplo:

```bash
java -Xms2048M -Xmx4096M -jar SpigotPlus.jar nogui
```

Depois da preparação, o servidor é executado através de:

```text
spigot.jar
```

## Estrutura do servidor

Uma instalação típica possui:

```text
Servidor/
├── SpigotPlus.jar
├── spigot.jar
├── plugins/
│   ├── Geyser-Spigot.jar
│   ├── ViaVersion.jar
│   ├── ViaBackwards.jar
│   └── ...
├── world/
├── world_nether/
└── world_the_end/
```

O `spigot.jar` é o servidor compilado pelo BuildTools. O `SpigotPlus.jar` é responsável pelo processo de preparação e inicialização da distribuição.

## Compatibilidade com plugins

Como o servidor final continua sendo **Spigot**, plugins Bukkit/Spigot podem ser utilizados normalmente, respeitando as APIs e limitações da versão do servidor.

Plugins atualmente integrados ao ambiente do projeto incluem:

- EssentialsPlus
- CargoPlus
- UtilidadesPlus
- ChatPlus
- LoginPlus
- ClanPlus
- ViaVersion
- ViaBackwards
- Geyser-Spigot

## Rede e segurança

A arquitetura de compatibilidade utiliza a seguinte sequência conceitual:

```text
Cliente Java / Bedrock
          ↓
       Geyser / Via
          ↓
        Spigot
          ↓
    Bukkit / Spigot API
          ↓
         Plugins
```

O objetivo é manter a camada de compatibilidade separada dos plugins do servidor, reduzindo a necessidade de alterações específicas em cada plugin para suportar diferentes clientes.

## Build do projeto

Requer:

- **JDK 26**
- **Maven**

Para compilar o projeto:

```bash
mvn -B clean package
```

O artefato do projeto é gerado em `target/`.

## Estado atual

### Funcionando

- [x] Bootstrap do SpigotPlus
- [x] Preparação automática do Spigot 26.2
- [x] BuildTools isolado fora do diretório do servidor
- [x] Limpeza do workspace antes da compilação
- [x] Geração e utilização do `spigot.jar`
- [x] Inicialização do Spigot 26.2
- [x] Java 26
- [x] ViaVersion
- [x] ViaBackwards
- [x] Geyser-Spigot
- [x] Execução dos plugins Bukkit/Spigot do servidor
- [x] Inicialização completa do ambiente com mundos Java

### Próximos testes / evolução

- [ ] Validar clientes Java 1.19.x → 26.2.x individualmente
- [ ] Validar conexão Bedrock em rede externa
- [ ] Ajustar configurações de ping/legacy do Geyser quando necessário
- [ ] Documentar configurações recomendadas de rede e firewall
- [ ] Expandir ferramentas de diagnóstico e manutenção da distribuição

## Objetivo do projeto

O objetivo do SpigotPlus é fornecer uma instalação de servidor **Spigot moderna, automatizada e compatível**, mantendo o ecossistema Bukkit/Spigot como base e adicionando uma camada prática para:

- múltiplas versões de clientes Java;
- acesso de jogadores Bedrock;
- preparação automática do servidor;
- isolamento seguro do BuildTools;
- inicialização simplificada;
- manutenção centralizada da distribuição.

> **SpigotPlus = Spigot como núcleo + automação de distribuição + compatibilidade Java/Bedrock.**
