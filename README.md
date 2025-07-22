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

2. **Copiar o JAR do middleware para a aplicação:**
```bash
cp target/plataforma-middleware-1.0-SNAPSHOT.jar ../aplicacao/lib/
```

3. **Construir a aplicação (que importa o middleware como JAR):**
```bash
cd ../aplicacao
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

## Testes JMeter

Foram criados 4 planos de teste JMeter:

### 1. basic-load-test.jmx
- Teste básico de carga
- 50 usuários, 100 iterações
- Testa método `add` simples

### 2. middleware-load-test.jmx
- Teste de carga do middleware
- 100 usuários, 200 iterações
- Inclui lookup e invocação

### 3. capacity-test.jmx
- Teste de capacidade
- 200 usuários, 500 iterações
- Mede limites do sistema

### 4. comprehensive-test.jmx
- Teste abrangente
- Múltiplos métodos (add, multiply, processComplexData)
- Cenário realista de uso
