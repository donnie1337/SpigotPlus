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
```

## Arquitetura

O SpigotPlus **não é um fork independente do Minecraft**. O servidor final continua sendo um **Spigot 26.2 pré-compilado**. O BuildTools é utilizado somente no processo separado de preparação do `spigot.jar`; ele não é executado durante o startup. A documentação oficial do Spigot descreve o BuildTools como a ferramenta usada para compilar o JAR do Spigot. citeturn0search0turn0search3

```text
SpigotPlus.jar
      │
      ├── spigotplus.properties
      │
      ├── valida Java 26
      ├── valida spigot.jar
      ├── verifica componentes
      │     ├── Geyser-Spigot
      │     ├── ViaVersion
      │     └── ViaBackwards
      ├── atualiza somente se necessário
      ├── valida JAR + versão + SHA opcional
      ├── cria backup antes da substituição
      └── inicia spigot.jar
```

## Configuração

O arquivo `spigotplus.properties` fica na raiz do servidor e é criado automaticamente quando necessário:

```properties
spigot.version=26.2
java.version=26

memory.min=2048M
memory.max=4096M

geyser.version=2.11.2
viaversion.version=5.11.0
viabackwards.version=5.11.0
```

Também é possível configurar SHA-256 para os arquivos de compatibilidade:

```properties
sha256.Geyser-Spigot.jar=...
sha256.ViaVersion.jar=...
sha256.ViaBackwards.jar=...
```

Quando um SHA estiver configurado, o arquivo baixado só será aceito se o hash corresponder.

## Atualização segura dos componentes

Na primeira inicialização, o SpigotPlus instala os componentes que estiverem ausentes.

Nas próximas inicializações:

```text
arquivo existe + JAR válido + versão correta
                │
                └── não baixa novamente
```

Se a versão estiver diferente, o SpigotPlus:

1. baixa para um arquivo temporário;
2. valida se o arquivo é um JAR;
3. valida a versão;
4. valida SHA-256 quando configurado;
5. salva a versão anterior em `plugins/.backup/`;
6. substitui o arquivo somente depois das validações.

São feitas até três tentativas de download. Se uma atualização falhar e a versão anterior continuar funcional, ela é mantida e o servidor pode continuar usando o componente instalado.

### Estrutura de backup

```text
plugins/
├── Geyser-Spigot.jar
├── ViaVersion.jar
├── ViaBackwards.jar
└── .backup/
    ├── Geyser-Spigot.jar.2.10.x.jar
    ├── ViaVersion.jar.5.10.x.jar
    └── ViaBackwards.jar.5.10.x.jar
```

## Diagnóstico

O launcher possui modo de diagnóstico:

```bash
java -jar SpigotPlus.jar status
```

Ele verifica:

- `spigot.jar`;
- Java configurada;
- Geyser-Spigot;
- ViaVersion;
- ViaBackwards;
- EssentialsPlus;
- CargoPlus;
- UtilidadesPlus;
- ChatPlus;
- LoginPlus;
- ClanPlus.

Exemplo:

```text
[SpigotPlus] Status
Spigot 26.2: ✓
Java 26: ✓
Geyser-Spigot.jar: ✓
ViaVersion.jar: ✓
ViaBackwards.jar: ✓
EssentialsPlus.jar: ✓
CargoPlus.jar: ✓
UtilidadesPlus.jar: ✓
ChatPlus.jar: ✓
LoginPlus.jar: ✓
ClanPlus.jar: ✓
```

## Compatibilidade

### Servidor

- Minecraft 26.2
- Spigot 26.2
- JDK 26

### Clientes Java

O objetivo do projeto é permitir clientes Java de **1.19.x até 26.2.x**, utilizando ViaVersion/ViaBackwards quando necessário. A compatibilidade exata depende das versões suportadas pelas camadas Via instaladas; o SpigotPlus não modifica o protocolo do Spigot diretamente.

### Bedrock

O Geyser-Spigot fornece a ponte entre Bedrock e Java.

```text
Bedrock UDP: 19132
```

## Build do Spigot

O `spigot.jar` deve ser pré-compilado antes da execução/distribuição:

```bash
java -jar BuildTools.jar --rev 26.2
```

A documentação oficial recomenda evitar diretórios com espaços ou sincronizados durante o BuildTools, e também recomenda mover o JAR compilado para o diretório do servidor. citeturn0search0

Depois de compilar, renomeie o resultado para:

```text
spigot.jar
```

O SpigotPlus **não executa BuildTools no startup**.

## Inicialização

```bash
java -Xms2048M -Xmx4096M -jar SpigotPlus.jar nogui
```

A memória padrão também pode ser alterada em `spigotplus.properties`.

## Estrutura do servidor

```text
Servidor/
├── SpigotPlus.jar
├── spigot.jar
├── spigotplus.properties
├── plugins/
│   ├── Geyser-Spigot.jar
│   ├── ViaVersion.jar
│   ├── ViaBackwards.jar
│   ├── EssentialsPlus.jar
│   ├── CargoPlus.jar
│   ├── UtilidadesPlus.jar
│   ├── ChatPlus.jar
│   ├── LoginPlus.jar
│   └── ClanPlus.jar
├── server.properties
├── eula.txt
├── world/
├── world_nether/
└── world_the_end/
```

## Build do SpigotPlus

Requer:

- JDK 26
- Maven

```bash
mvn -B clean package
```

O resultado é:

```text
target/SpigotPlus.jar
```

## CI/CD

O GitHub Actions agora:

1. compila com JDK 26;
2. valida o `SpigotPlus.jar`;
3. cria o pacote de distribuição;
4. gera `SHA256SUMS.txt`;
5. publica os artefatos do workflow;
6. quando uma tag `v*` é criada, publica automaticamente uma GitHub Release.

O pacote de distribuição contém o launcher e sua configuração. O `spigot.jar` continua sendo um artefato separado, preparado com BuildTools.

## Segurança e recuperação

O runtime não substitui um plugin instalado diretamente pelo download. Ele sempre trabalha primeiro com um arquivo temporário e só troca o arquivo após as validações.

Em caso de falha de atualização, o componente funcional anterior é preservado. Backups das versões substituídas ficam em `plugins/.backup/`.

## Plugins do servidor

O SpigotPlus não baixa automaticamente os plugins próprios do servidor. Ele apenas os identifica no diagnóstico.

Plugins integrados ao ambiente do projeto:

- EssentialsPlus
- CargoPlus
- UtilidadesPlus
- ChatPlus
- LoginPlus
- ClanPlus

## Estado atual

### Funcionando

- [x] Bootstrap do SpigotPlus
- [x] Spigot 26.2 pré-compilado
- [x] Java 26
- [x] `spigotplus.properties`
- [x] Verificação do `spigot.jar`
- [x] Verificação de versão dos componentes
- [x] Downloads somente quando necessário
- [x] Download temporário
- [x] Validação de JAR
- [x] SHA-256 configurável
- [x] Backup antes de atualização
- [x] Tentativas de download e fallback para versão funcional
- [x] Diagnóstico via `status`
- [x] ViaVersion
- [x] ViaBackwards
- [x] Geyser-Spigot
- [x] CI/CD com artefato de distribuição
- [x] Release automática por tag

### Próximos testes

- [ ] Validar clientes Java 1.19.x → 26.2.x individualmente
- [ ] Validar Bedrock em rede externa
- [ ] Testar cenários de corrupção/rollback em máquina real
- [ ] Configurar SHA-256 oficial de cada componente quando os checksums oficiais estiverem disponíveis
- [ ] Documentar configurações de firewall e rede

## Objetivo

**SpigotPlus = Spigot como núcleo + launcher de distribuição + gerenciamento seguro dos componentes + compatibilidade Java/Bedrock.**
