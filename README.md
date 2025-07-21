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

## Estrutura do Projeto

```
├── plataforma-middleware/          # Módulo do middleware
│   ├── src/main/java/imd/ufrn/br/
│   │   ├── annotations/           # Anotações do sistema
│   │   ├── broker/               # Padrão Broker
│   │   ├── exceptions/           # Hierarquia de exceções
│   │   ├── identification/       # Padrões de identificação
│   │   ├── remoting/            # Padrões de remoting
│   │   └── monitoring/          # Monitoramento e heartbeat
├── aplicacao/                    # Aplicação exemplo
│   └── src/main/java/imd/ufrn/br/app/
│       ├── CalculatorServiceImpl.java    # Serviço exemplo
│       ├── ComplexData.java             # Classe de dados
│       └── Main.java                    # Aplicação principal
└── jmeter-tests/                 # Planos de teste JMeter
    ├── basic-load-test.jmx
    ├── middleware-load-test.jmx
    ├── capacity-test.jmx
    └── comprehensive-test.jmx
```

## Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.6+
- JMeter (para testes de carga)

### Construção e Execução

1. **Construir o projeto:**
```bash
mvn clean package
```

2. **Executar o servidor:**
```bash
cd aplicacao
java -jar target/aplicacao-1.0-SNAPSHOT.jar
```

O servidor inicia em:
- HTTP: porta 8080 (para JMeter)
- UDP: porta 9090 (para tolerância a falhas)

### Testando a Aplicação

#### Teste HTTP (via curl)
```bash
# Listar serviços
curl "http://localhost:8080/lookup?service=CalculatorService"

# Invocar método add
curl -X POST "http://localhost:8080/invoke" \
  -H "Content-Type: application/json" \
  -d '{"objectId":"CalculatorService","methodName":"add","args":[5,3]}'

# Invocar método processComplexData
curl -X POST "http://localhost:8080/invoke" \
  -H "Content-Type: application/json" \
  -d '{"objectId":"CalculatorService","methodName":"processComplexData","args":[{"name":"Test","value":42,"active":true}]}'
```

#### Testes com JMeter

Os testes oficiais são executados através do JMeter. Certifique-se de que a aplicação esteja executando antes de executar os testes:

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

### Executando Testes JMeter

```bash
# Teste básico
jmeter -n -t jmeter-tests/basic-load-test.jmx -l results-basic.jtl

# Teste de capacidade
jmeter -n -t jmeter-tests/capacity-test.jmx -l results-capacity.jtl

# Teste abrangente
jmeter -n -t jmeter-tests/comprehensive-test.jmx -l results-comprehensive.jtl
```

## Funcionalidades Implementadas

### ✅ Requisitos Atendidos

1. **Middleware Distribuído**: ✅ Implementado com padrões do livro
2. **Invocação Remota via HTTP**: ✅ Para testes JMeter
3. **Invocação Remota via UDP**: ✅ Para tolerância a falhas
4. **Remoção de Comentários**: ✅ Todos os comentários removidos
5. **Padrões Requeridos**: ✅ Todos os 9 padrões implementados
6. **Modelo de Componentes**: ✅ Baseado em anotações
7. **Testes JMeter**: ✅ 4 planos de teste criados
8. **Tolerância a Falhas**: ✅ Heartbeat e monitoramento
9. **Robustez**: ✅ Tratamento de exceções e recuperação

### Detalhes de Implementação

- **JSON Marshalling**: Utiliza Jackson para serialização eficiente
- **Reflexão Java**: Para invocação dinâmica de métodos
- **HTTP Server Nativo**: Usa `com.sun.net.httpserver`
- **UDP Assíncrono**: Para comunicação de baixa latência
- **Thread Safety**: Uso de `ConcurrentHashMap` para thread safety
- **Error Handling**: Hierarquia robusta de exceções

## Logs e Monitoramento

O sistema produz logs detalhados:
- Registro de serviços
- Invocações remotas (HTTP/UDP)
- Status de heartbeat
- Detecção de falhas
- Métricas de performance

## Considerações de Performance

- Suporte a múltiplos protocolos (HTTP/UDP)
- Pool de threads para processamento concorrente
- Serialização JSON otimizada
- Cache de metadados de reflexão
- Monitoramento não-bloqueante

## Conclusão

Esta implementação demonstra uma plataforma de middleware completa e robusta, seguindo os padrões estabelecidos na literatura de sistemas distribuídos. O sistema suporta tanto casos de uso de alta performance (UDP) quanto compatibilidade com ferramentas de teste padrão (HTTP/JMeter), fornecendo uma base sólida para aplicações distribuídas.