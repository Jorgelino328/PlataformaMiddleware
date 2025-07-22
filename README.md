# Middleware Platform - Trabalho de Programação Distribuída (2ª Unidade)

## Visão Geral

Este projeto implementa uma plataforma de middleware distribuído em Java baseada nos padrões do livro "Remoting Patterns: Foundations of Enterprise, Internet and Realtime Distributed Object Middleware". A implementação suporta invocação de objetos remotos via HTTP (para testes JMeter) e UDP (para tolerância a falhas).

## Arquitetura e Padrões Implementados

### Configuração "Individual"
A implementação inclui todos os padrões requeridos para a configuração Individual:

#### Broker Pattern
- **Localização**: `plataforma-middleware/src/main/java/imd/ufrn/br/broker/Broker.java`
- **Funcionalidade**: Gerencia o registro e descoberta de objetos remotos, atua como intermediário entre clientes e serviços

#### Basic Remoting Patterns
1. **Server Request Handler**: 
   - HTTP: `SimpleHttpServerRequestHandler.java`
   - UDP: `UDPServerRequestHandler.java`
   - Processam requisições de clientes remotos

2. **Invoker**: `Invoker.java`
   - Executa métodos em objetos remotos usando reflexão

3. **Marshaller**: `JsonMarshaller.java`
   - Serializa/deserializa objetos para comunicação remota usando JSON

4. **Remote Object**: Implementado via anotação `@RemoteObject`
   - Marca classes como disponíveis para invocação remota

5. **Remoting Error**: Hierarquia de exceções personalizada
   - `RemotingException` (base)
   - `InvocationException`
   - `MarshallingException`
   - `ObjectNotFoundException`

#### Identification Patterns
1. **Lookup**: `LookupService.java`
   - Permite localizar objetos remotos por nome

2. **Object Id**: `ObjectId.java`
   - Identificador único para objetos remotos

3. **Absolute Object Reference**: `AbsoluteObjectReference.java`
   - Referência completa incluindo localização do objeto

### Modelo de Componentes Baseado em Anotações

O sistema utiliza a anotação `@RemoteObject` para marcar serviços:

```java
@RemoteObject(name = "CalculatorService")
public class CalculatorServiceImpl {
    // métodos públicos são automaticamente expostos
}
```

### Tolerância a Falhas

- **Heartbeat Monitor**: `HeartbeatMonitor.java`
- Monitora a saúde dos serviços via UDP
- Detecta falhas e permite recuperação automática
- Integrado com o servidor UDP para comunicação de heartbeat


## Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.6+
- JMeter (para testes de carga)

### Construção e Execução

1. **Construir o middleware e gerar o JAR:**
```bash
cd plataforma-middleware
mvn clean package -DskipTests
```

2. **Instalar o JAR do middleware no repositório Maven local:**
```bash
cd ../aplicacao
mvn install:install-file -Dfile=../plataforma-middleware/target/plataforma-middleware-1.0-SNAPSHOT.jar -DgroupId=imd.ufrn.br -DartifactId=plataforma-middleware -Dversion=1.0-SNAPSHOT -Dpackaging=jar
```
3. **Construir a aplicação (que importa o middleware como JAR):**
```bash
mvn clean package -DskipTests
```

4. **Executar o sistema:**
```bash
java -jar target/aplicacao-1.0-SNAPSHOT.jar
```

O sistema inicia com:
- HTTP Gateway: porta 8082 (para JMeter)
- TCP Server: porta 8085 (comunicação interna)
- UDP Server: porta 8086 (monitoramento)

### Estrutura do Projeto

O projeto está organizado em dois módulos Maven independentes:

- **plataforma-middleware/**: Contém a implementação completa do middleware
- **aplicacao/**: Aplicação que usa o middleware como dependência JAR via `lib/`

A aplicação importa o middleware como uma dependência Maven normal, e o Maven Shade Plugin cria um JAR executável único com todas as dependências incluídas.

**Por que usar `mvn install:install-file`?**

Você estava certo ao questionar por que não simplesmente copiar o JAR para `lib/`. A razão é que:

1. **System scope dependencies** têm limitações no Maven Shade Plugin
2. O comando `mvn install:install-file` instala o JAR no repositório Maven local (`~/.m2/repository`)
3. Isso permite usar uma **dependência Maven normal** em vez de system scope
4. O Maven Shade Plugin funciona perfeitamente com dependências normais
5. É mais limpo e confiável que tentar forçar system scope dependencies

## Testes JMeter

O sistema está configurado para ser testado exclusivamente via JMeter no endpoint HTTP.

### Endpoints Disponíveis

- **URL base**: `http://localhost:8082/invoke/{objectName}/{methodName}`
- **Método HTTP**: POST
- **Content-Type**: application/json
- **Body**: Array JSON com parâmetros do método

### Métodos de Teste Disponíveis

O serviço `CalculatorService` expõe os seguintes métodos para teste:

1. **add(int, int)**: Soma dois inteiros
   - URL: `POST http://localhost:8082/invoke/CalculatorService/add`
   - Body: `[5, 10]`

2. **echo(String)**: Retorna uma mensagem
   - URL: `POST http://localhost:8082/invoke/CalculatorService/echo`
   - Body: `["Hello World"]`

3. **processData(ComplexData)**: Processa objeto complexo
   - URL: `POST http://localhost:8082/invoke/CalculatorService/processData`
   - Body: `[{"name":"test","value":42,"active":true}]`

4. **getStatus()**: Retorna status do serviço
   - URL: `POST http://localhost:8082/invoke/CalculatorService/getStatus`
   - Body: `[]`

5. **greetAll(List<String>)**: Cumprimenta lista de nomes
   - URL: `POST http://localhost:8082/invoke/CalculatorService/greetAll`
   - Body: `[["Alice", "Bob", "Charlie"]]`

### Configuração de Planos de Teste JMeter

Foram criados planos de teste JMeter que devem usar os endpoints HTTP acima:

- **Teste básico de carga**: 50 usuários, método `add`
- **Teste de middleware**: 100 usuários, múltiplos métodos
- **Teste de capacidade**: 200 usuários, cenário intensivo
- **Teste abrangente**: Combinação de todos os métodos disponíveis
