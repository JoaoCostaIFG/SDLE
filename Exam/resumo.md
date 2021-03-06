# SDLE 2021/2022

- **Distributed System** - collection of distinct processes which are spatially
  separated and which communicate with one another by exchanging messages;
- **Message** - an atomic bit string.

## Message-based communication

### TCP Reliability

- Network can be abstracted as a communication channel;

| Property                          | UDP     | TCP    |
| --------------------------------- | ------- | ------ |
| Abstraction                       | Message | Stream |
| Connection-based                  | N       | Y      |
| Reliadbility (loss & duplication) | N       | Y      |
| Order                             | N       | Y      |
| Flow control                      | N       | Y      |
| Number of recipients              | `1..n`  | 1      |

- **TCP guarantee** - the application will be notified if the local end is
  unable to communicate with the remote end;
- **TCP cannot guarantee that there won't be data loss**;
- **TCP** does not re-transmit data that was lost in other connections.

### Message duplication

- Can't always retransmit a message that may have not been delivered => the
  message might have arrived;
- **TCP** is not able to filter data duplicated by the application (only
  duplicated TCP segments);
- This may be an issue: duplicated data is a request for a **non-idempotent**
  operation;
- Re-sync might be necessary.

### RPC

- RPC is typically implemented on top of the transport layer (TCP/IP);

#### Client Stub

- **Request:**
  1. Assembles message: parameter marshalling;
  2. Sends message (`write()`/`sendto()`);
  3. Blocks waiting for response (`read()`/`recvfrom()`) (differente in async
     RPC).
- **Response:**
  1. Receives reponses;
  2. Extracts the results (unmarshalling);
  3. Returns to client (assuming synchronous RPC).

#### Server Stub

- **Request:**
  1. Receives message with request (`read()`/`Recfrom()`);
  2. Parses message to determine arguments (unmarshalling);
  3. Calls function.
- **Response:**
  1. Assembles message with the return value of the function;
  2. Sends message (`write()`/`sendto()`);
  3. Blocks waiting for a new request.

#### Async. vs Sync.

![RPC](img/rpc.png)

---

- (non-async) RPC is a useful paradigm - programming almost as simple as
  non-distributed application (discarding failures);
- Only great for request-reply communication;

### Asynchronous Communication

- **Problem:** Communicating parties may not always be simultaneously available;
- **Solution:** Async communication (parties don't need to be active
  simultaneously).

## Message Oriented Middleware (MOM)

- Approprieta when sender and receiver are loosely coupled: Email/SMS;
- Async message-based communication:
  - Sender and receiver need not synchronize with one another to exchange
    messages;
  - Middleware stores the messages as long as needed to deliver them;
  - At the lowest communication level, there is sync between client and
    middleware.
- **publishers** - send messages;
- **subscribers** - receive messages.

### Basic Patters

- **Point-to-Point** (**queue**):
  - Several senders can put messages in a queue;
  - Several receivers can get messages from a queue;
  - Each message is **delivered to at most one receiver**;
- **Publish-subscriber** (topics):
  - Several **publishers** can put messages in a topic;
  - Several **subscribers** can get messages from a topic;
  - Each message can be **delivered to more than one subcriber**;

### Differences with UDP

- **UDP** supports Unicast and Multicast communication;
- **MOM**:
  - **Asynchrony** - senders/publishers need not synchronize with
    receivers/publishers;
  - **Anonymity** - senders/publishers need not know receivers/subscribers (and
    vice-versa). Queues and topics generaly use high-level naming (not transport
    level addresses).

### JMS Queues

| --        | Blocking | Non-Blocking | Asynchronous |
| --------- | -------- | ------------ | ------------ |
| send()    | Y        | --           | via callback |
| receive() | Y        | via timeout  | via callback |

## Threads

- Threads in a process share most resources, except the stack and the processor
  state;
- Thread-specific info: state (ready, running, waiting), process state (SP, PC),
  and stack;
- **Kernel-level**:
  - kernel's scheduler allocates cores to threads;
  - OS keeps threads table with information on every thread;
  - Management operation incur a system call.
- **User-level**:
  - kernel is not aware of the existence of threads;
  - OS doesn't need to support it;
  - Problems like thread page-fault (all have to wait), non-blocking system
    calls, thread never yielding;
  - Cannot be used to exploit parallelism in multicore architectures.

## Multi-threaded server

![Multi-threaded server](img/multi_thread_server.png)

- Common pattern:
  - 1 dispatcher thread which accepts connection requests;
  - Several worker threads, each processing a single request.

### Bounding threads resource usage

- **Thread-pools**:
  - allow to bound the number of threads;
  - avoid thread creation/destruction overhead (sys call).
- **Excessive thread-switch overhead**:
  - arises more often when using multiple-thread pools;
  - bound number of active thread (e.g. using semaphore).

## Sync vs. Async I/O

### Sync I/O

- **Blocking**:
  - thread blocks until the operation is completed;
  - `write()`/`send()` sys calls may return immediately after copying the data
    to kernel space and enqueueuing the output request;
- **Non-blocking**:
  - thread does not block (not even in input operations => call returns
    immediately with whatever data is available at the kernel);
  - in Unix, all I/O to block devices (and regular files or directories) is
    blocking.

### Async I/O

- Sys call enqueues the I/O request and return immediately;
- Thread may execute while the requested I/O operation is being executed;
- Thread learns about the termination of the I/O operation either by polling or
  via event notification (signal or function call);

### poll()/epoll() and Blocking I/O

- use fewer threads than data sockets in TCP server by polling for events;
- `poll()` blocks until one of the requested events (e.g. data input) occurs;
- Can set timeout aswell;
- **Problem:** doesn't work with regular file (poll always true for read and
  write) => use helper threads for disk I/O.

## Event-driven Server

![Event-driven server](img/event_driven_server.png)

- Server executes a loop:
  - wait for events (usually IO);
  - process the events.
- Blocking is avoided by using non-blocking IO;
- Scalability issues:
  - Data copying => user buffer descriptors or scatter/gather IO;
  - Memory alloc (default allocator is general purpose) => design own;
  - Concurrency control => avoid sharing + locking granularity + minimize
    critical sections.

## Data Replication

- Replicate data at many nodes;
- Performance => user has more local reads;
- Reliability => no data-loss unless data is lost in all replicas;
- Availability => data available unless all replicas fail or become unreachable;
- Scalability => balance load across nodes for reads;
- **Update** => push data to all replicas => Problem of ensuring **data
  consistency**.

### Strong consistency

- All replicas execute updates in the same order. Same initial state leads to
  same result.

#### Sequential Consistency

An execution is **sequential consistent** iff it is identical to a sequential
execution of all the operations in that execution such that all operation
executed by any thread appear in the order in which they were executed by the
corresponding thread.

**A ordem em que as opera????es s??o ordenadas (no global), t??m de ser consistentes
com a ordem em que cada cliente v?? opera????es.**

- This is the model provided by a multi-threaded system on a uniprocessor;
- Protocol:
  - Read(a) - reads value at index a from one replica;
  - Write(a, v) - writes value v to index a to all replicas;
  - Snapshot - reads all from one replica.
- **Not Composable** - Assume 2 sub-arrays of 2 elements. We can't interleave
  operations on the two arrys and still remains sequential consistent.

![Seq Consis](img/seq_consis.png)

#### Linearizability

Execution is **linearizable** if it is **sequential consistent** and if _op1_
occurs before _op2_ according to an **omniscient observer**, then _op1_ appears
before _op2_.

- **Assumption:** operation have start and finish time (measured on some global
  clock):
  - if _op1_ occurs before _op2_, _op1_ finish time is smaller than _op2_ start
    time;
  - if _op1_ and _op2_ overlap in time, their relative order may be any.
- **Protocol:** Igual mas o **Write** tem de esperar por um **ACK**.

#### One-copy Serializability (Transaction-based systems)

Executions of a set of transactions is **one-copy serializable** iff its outcome
is similar to the execution of those transactions in a single copy.

- Serializability used to be the most common consistency model used in
  transaction-based systems:
  - nowadays there are weaker consistency models to acieve higher performance.
- This is essentially the **sequential consistency** model when the operations
  executed by all processors are transactions;

## Scalable Distributed Topologies

- **Simple Graph** - undirected, no loops, no more than onde edge between any
  two vertices;
- **Connected Graph** - there is a path between any two nodes. Strongly
  connected if path existe nas duas dire????es;
- **Star** - central vertice and many leaf nodes connected to the central one;
- **Tree** - connected graph with no cycles;
- **Planar Graph** - vertices and edges can be drawn in a plane and no two edges
  intersect (E.g. rings and trees);
- **Ring** - anelzinho de n??s. **Periphery** e **center** s??o todos os n??s.
- **Connected Component** - maximal connected subgraph of G;
- **Distance** $d(v_i, v_j)$ - length of the shortest path connecting those
  nodes;
- **Eccentricity of $v_i$** - $ecc(v_i) = max(d(v_i, v_j))$
- **Diameter** - $D = max(ecc(v_i))$
- **Radius** - $R = min(ecc_(v_i))$
- **Center** - $ecc(v_i) == R$
- **Periphery** - $ecc(v_i) == D$
- In networks cycles allow **multi-path routing**. This can be more robust but
  data handling can become more complex;

### Complex topologies

- **Random geometric** - vertices dropped randomly uniformly into a unit square,
  adding edges to connect any two points within a given euclidean distance;
- **Random Erdos-Rebyi** - n nodes connected randomly with independent
  probability p => low diameter com support para small paths ($O(log n)$);
- **Watts-Strogatz model** - nos estabelecem k contactos locais (metrica de
  distancia) e alguns de longa distancia (uniformemente at random) => low
  diameter e high clustering;
- **Barabasi-Albert model** - Preferential attachment. The more connected a node
  is, the more likely it is to receive new links. Degree Distribution follows a
  power law.

### Synchronous SyncBFS Algorithm

Processes communicate over directed edges. Unique UIDs are available, but
network diameter and size is unknown.

- A directed spanning tree with root node _i_ is **breadth first** provided that
  each node at distance _d_ from _i_ in the graph appears at depth _d_ in the
  tree;
- Every **strongly connected graph** has a breadth-first directed spanning tree;
- **Applications:**
  - Aggregation of values - input values in each process can be aggregated
    towards a sync node (cada valor s?? contribui 1 vez);
  - Leader election - largest UID wins. Todos os processos tornam-se root da sua
    pr??pria ??rvore e agregam Max(UID). Cada um decide comparam o seu UID com o
    Max(UID);
  - Broadcast - message payload pode ir junto com a SyncBFS construction ou ser
    broadcasted depois de formar a ??rvore;
  - Computing diameter - cada processo constroi uma SyncBFS. Em seguida
    determinan _maxdist_ (longest tree path). Depois, todos os processos usam as
    suas ??rvores para agregar $Max(maxdist)$ desde todos os roots/nodes - Time
    $O(diam)$ and messages $O(diax * |E|)$.

#### Initial state

- parent = null
- marked = False (True in root node $i_0$)

#### Algorithm

- Process $i_0$ sends a search message in round 1;
- Unmarked processes receiving a search message from `x` set `marked = True` and
  `parent = x`. In the next round search messages are sent from these processes.

#### Complexity

- Time - at most _diameter_ rounds (depending on $i_0$ eccentricity);
- Message - $|E|$. Messages are sent across all edges $E$;
- Child pointers - if parents need to know their offspring, processes must reply
  to search messages with either _parent_ or _nonparent_. Only easy if graph
  undirected, but is achievable in general strongly connected graphs.
- Termination: making $i_0$ know that the tree is constructed - all procs
  respond with _parent_ or _nonparent_. Parent terminates when all children
  terminate. Responses are collected from leaves to tree root.

### AsynchSpanningTree

![AsynchSpanningTree](img/asynchspanningtree.png)

#### Reliable FIFO send/receive channels

`cause()` function maps an event to the preceding event that caused it.

- cause(receive(x)) = send(y) => x = y;
- For every send there is a mapped receive => Messages are not lost;
- For every receive, there is a distinct send => Messages are not duplicated;
- receive < receive' => cause(receive) < cause(receive') => Order is preserved.

**Channel automaton** - consumes `send("search")` and produces
`receive("search")` in reliable FIFO order.

- Assume no faults, and reliable FIFO send/receive channels;

---

- Doesn't necessarily produce a breadth first spanning tree;
- Faster longer paths will win over slower direct path when setting up parent;
- However, a spanning tree is constructed.
- **Invariant:** In any reachable state, the edges defined by all parent
  variables form a spanning tree containing $i_0$, moreover, if there is a
  message in any channel $C_{i, j}$, then $i$ is in this spanning tree.
  - Tudo se forma a partir de $i_0$ e s?? h?? mensagens em n??s j?? integrados na
    ??rvore.
- **Invariant:** All nodes are searched.
- **Theorem:** algoritmo constroi a spanning tree no undirected graph G.

#### Properties

- Apesar de n??o existir time limits, vamos assumir umas upper bounds:
  - tempo para processar um effect => l;
  - tempo para entregar uma mensagem => d.
- **Complexidade:**
  - N?? mensagens ?? $O(|E|)$;
  - Tempo ?? $O(diam * (l + d))$ => `[l] -d-> [l] -d-> ...`
- Uma ??rvore com height, h, maior que diam pode occorer apenas se n??o demorar
  mais tempo que uma ??rvore com h = diam => n??o excede os bounds que assumimos.

#### Applications

- **Child pointers e broadcast** - se nos reportarem _parent_ e _nonparent_,
  conseguimos fazer broadcast;
  - A time complexity passa a ser $O(h * (l + d))$ ($O(n * (l + d))$ no pior
    caso). **Um caminho longo s?? se forma se for mais r??pido, mas eventualmente
    pode deixar de ser mais r??pido que o caminho curto => temos de
    reverificar**.
- **Broadcast with Acks** - coleta Acks enquanto a ??rvore vai sendo construida.
  Quando recebe um broadcast, n?? d?? Ack se j?? conhecer e d?? Ack ao parent quando
  todos os neighbors que derem Ack;
- **Leader Election** - Se quando se iniciar termination, todos os n??s
  reportared o seu UID, podemos fazer leader election com unkown diameter e
  n??mero de n??s.

### Epidemic Broadcast Trees

- **Gossip broadcast**:
  - highly scalable and resilient;
  - excessive message overhead.
- **Tree-based broadcast**:
  - small message complexity;
  - fragile in the presence of failures.
- **gossip strategies**:
  - eager push - immediately forward new messages;
  - pull - nodes periodically query for new messages;
  - lazy push - nodes push new message ids and accept pulls.

#### Gossiping into tree

- N??s escolhem conjunto pequeno (3 ou 4) de n??s aleat??rios como vizinhos;
- Links s??o bidirecionais (tentar que sejam est??veis);
- O canal em que recebemos uma mensagem primeiro fica **eager push**;
- Canais em que mensagem chega repetida ficam a **lazy push**;
- **Eager push** de payload e **lazy push** de metadata;
- Quando ??rvore parte, vamos receber metadata sem payload. Timer de controlo
  expira e promove **lazyPush** a **eagerPush**. Se ficarem caminhos redundantes
  => algoritmo limpa.

#### Small Worlds

- "Six degrees of separation";
- Watts Strogatz graph => low diameter e high clusting;
- Se dermos flood a um grafo destes, um observador global consegue encontrar
  caminhos $O(log N)$ entre 2 pontos arbitrarios;
- Os caminhos n??o t??m locallity => ?? dificil encontrar esses caminhos curtos s??
  com local knowledge.
- **Solu????o: Kleinberg** - em vez de usar uma probabilidade uniforme, dar mais
  prioridade a liga????es pr??ximas (exponencial) => **tipo os fingers no Chord**;

## System design for large scale

### Napster

- Centralized catalog of music descriptions and references to online users that
  hosted copies of it;
- Download are done among peers => sometimes I am the provider, other times I am
  the recepient;
- Due to presence of firewalls, ability to communicate with a server does not
  imply capacity to accept connection (**double firewall problem**);
- Relience on a server => tech weakness for legal attacks.

### Gnutella

- Fully distributed solution for P2P file sharing;
- Partially randomized overlay network. Cada n?? conecta-se a k vizinhos;
- O n??mero de vizinhos varia entre n??s;
- **Bootstrapping** - HTTP hosted host cache + local host cache de sess??es
  pr??vias;
- **High churn** => local host caches ficavam desatualizadas rapidamente;
- **Routing** => flooding + reverse path routing;
- **PING** + **PONG** - ping era flooded e ent??o eu ficava a conhecer vizinhos a
  2, 3, 4, etc... hops;
- **QUERY** + **QUERY RESPONSES** - para fazer queries. Tamb??m ?? flooded e back
  propagated. O answer set aumenta com o tempo e o diameter ou os max hops serem
  alcan??ados;
- **GET** + **PUSH** - push ?? usado para dar circunvent a uma single firewall
  (double firewall n??o tem solu????o).

#### Improvements

- **Super-peers** - m??quinas que t??m maior capacidade e estavam sempre online.
  N??s preferiam conectar a n??s com maior uptime;
- Super peers estavam conectados entre si;
- Peers comuns estavam conectados a um ou mais super peer (**two-tier
  architecture**);
- Ping/Pongs s?? se propagavam na camada dos super peers => super peers protegem
  outros de tr??fego;
- Agora super-peers usam **bloom filter** para saber o conte??do dos seus peers
  => peers s?? s??o contactados se tiverem uma alta probabilidade de ter o
  conte??do.

### DHT

#### Chord

- UID no range [0, $2^m - 1$] - SHA1 do IP e das keys;
- O n?? que guarda uma key ?? o n?? com ID >= que a key (nodeId >= keyId);
- Tem de ser possivel contactar um n?? arbitrario e pedir para encontrar o
  sucessor de um key;
- Cada n?? tem m fingers (clockwise) e conhece r vicinity nodes (both
  directions);
- Routing ?? $O(log n)$.

#### Kademlia

- ID = SHA1 do IP => 160 bits;
- Dist??ncia de IDs ?? XOR. Esta m??trica ?? sim??trica e respeita a triangle
  property;
  - dist(10001, 11100) = 01101.
- Routing ?? sim??trico. Alternative next hops can be chosen for low latency or
  parallel routing;
- Routing tables consist of a list for each bit of node ID em, que temos 1 bit
  em comum, depois 2, depois 3, etc... Quanto mais deep na tabela, fica
  exponencialmente mais dificil encontrar matching nodes. E.g. com ID =
  100110...

| Bits | Addr    |
| ---- | ------- |
| 0    | 0...    |
| 1    | 11...   |
| 2    | 101...  |
| 3    | 1000... |

![Kademlia](img/kademlia.png)

- Para termos em conta falhas de n??s, guardamos at?? k n??s em cada posi????o
  (usualmente k);
- O uptime do n?? ?? usado para desempatar limited positions.

## Physical and Logical Time

- **Clock drift** - drift between measured time and reference time for the same
  measurement unit;
- **External sync** - precision em rela????o a uma referencia authoritative. Para
  uma banda $D > 0$ e UTC source _S_, temos $|S(t) - C_i(t)| < D$;
- **Internal sync** - precision entre 2 n??s. Para uma banda $D > 0$, temos
  $|C_j(t) - C_i(t)| < D$;
- Se 2 pessoas estao a 1D de uma source, podem estar at?? 2D entre si;
- **Monotonicity** - $t' > t => C(t') > C(t)$ - Many uses assume it => time
  can't go backwards. Correcting advanced clocks can be obtained by reducing
  time rate until aimed synchrony is reached;

### Synchronization

#### Synchronous system

- Sabendo o tempo de tr??nsito de uma mensagem que cont??m o tempo, podemos dar
  set a $t' = t + trans$;
- _trans_ pode variar entre _tmin_ e _tmax_. Usar um ou outro d?? uma incerteza
  de $u = tmax - tmin$;
- Usando $t + \frac{tmin + tmax}{2}$, a incerteza torna-se $\frac{u}{2}$;

#### Asynchronous system - Cristian's algorithm

- **Problema:** tmax pode ser infinito;
- Faz um request, _mr_, que despoleta uma resposta _mt_ que contem o tempo _t_;
- Medimos o round-trip-time do request-reply => _tr_;
- Assum??mos RTT balanceado (igual para cada lado) => $t + \frac{t_r}{2}$;
- Podemos aumentar a precis??o repetindo o protocolo at?? ocorrer um _tr_ baixo;
- **Berkeley algorithm** - Um coordenador mede RTT para v??rios outros n??s e d??
  set ao target time com a average dos tempos. O tempo que d??-mos set s??o
  deltas, e.g. avan??a 1 sec (em vez de mete x).

#### Causality - Happens-before

- D?? mais info que timeline (tempo ?? limitado);
- S?? indica potencial influ??ncia;
- Tem a ver com memoria de eventos relevantes;
- Causal histories s??o encodings simples de causalidade (sistemas abaixo s??o
  mecanismos de encoding).

##### Causal histories

- Memorias s??o sets de eventos ??nicos;
- **Causality check** - inclus??o num set explica causalidade:
  - iff {a1, b1} C {a1, a2, b1}
  - **Otimiza????o:** testar apenas se o elemento mais recente da esquerda
    pertence.
- Tu est??s no meu past se eu conhe??o a tua hist??ria;
- Se n??o conhecemos as nossas historias mutuamente => somos concurrents;
- Se as nossas historias s??o iguais => somos os mesmos;

![Causal histories](img/causal_histories.png)

- **Nota:** {en} C Cx => {e1...en} C Cx. Isto pode ser comprimido:
  - {a1, a2, b1, b2, b3, c1, c2, c3};
  - {a -> 2, b -> 3, c -> 3};
  - Se tivermos um n?? fixo de processos totalmente ordenados => [2, 3, 3]. A
    uni??o de sets torna-se num point-wise max (m??ximo em cada casa) (n??o
    esquecer que depois de unir ?? preciso incrementar evento on ocorreu).

![Causal histories optmization](img/causal_histories_optimized.png)

#### Vector clocks

- Comprar gr??ficamente os vectors das causal histories;
- Se eu conhe??o uma coisa que tu n??o **e** vice-versa => concurrentes;
- Se eu conhe??o uma coisa que tu n??o => sou maior que tu;

![Graphical vector clock](img/vector_clock_1.png)

- **Dots** - Em vez de ter [2, 0, 0], temos [1, 0, 0]a2;
- Assim temos o last event ?? parte => basta checkar esse para saber se ta num
  history;
- [1, 0, 0]a2 -> [2, 1, 0]b2 iff dot (a2) <= 2.

- **Relevant events** - S?? eventos que s??o relevantes s??o adicionados ao
  hist??rico;

![Merge de causality](img/vector_clock_2.png)

#### Version vectors

- **Version vector** != **Vector clock**;

![Version vector](img/version_vector.png)

#### Scaling causality

- Scaling at the edges (**DVVs**):
  - 8 m??quinas no data center a gerir escritas;
  - 200 m??quinas proxy com o cliente;
  - evitar 200 m??quinas a criar updates no controlo de vers??o.
- Dynamic concurrency degree (**ITCs**):
  - creation and retirement of active entities;
  - passar de 8 m??quinas para 10 (ou reduzir sem manter lixo).
- **Dynamo like**, get/put interface:
  - **Conditional writes** - rejeitamos escritas em sitios que t??m conflito;
  - **Overwrite first value** (last writter wins) - rescrevemos ??ltimo valor;
  - **Multi-Value** - mantemos escritas concurrentes (vetor de vers??o).

##### Dotted version vectors (DVV)

- Os eventos ficam ambos no server, separadamente: {s1}, {s2} => hist??rias
  diferentes => concurrentes;
- Para representar compactamente, pomos caixinha no meio do ar;

![Dotted version vector](img/dotted_version_vector.png)

- `get()` vai retornar os 2 juntos => user tem depois de dar override na
  escrita;

##### Dynamic Causality (ITC)

- Tracking de causality requer acesso exclusivo a identidades;
- Ids podem ser dividos por parte de qualquer identidade (divis??o infinita);
- Come??amos com um ID original que se divide;

![ID split](img/id_split.png)

- Entities can register new events e tornar-se concurrentes;

![ID concurrency](img/id_concurr.png)

- IDs na imagem s??o todos superiores aos de cima mas concurrentes entre si;
- IDs podem dar join (**mesmo que n??o sejam partes de um todo originalmente**);

![ID space collection](img/id_space_collection.png)

- Podemos juntar os IDs num s??;
- **Important** cada replica ativa tem de controlar um espa??o de IDs distinto;

## High Availability under Eventual Consistency

- **local** - l = 50ms
- **inter-continental** - L = [100ms, 300ms]
- **Planet-wide geo-replication**:
  - Consensus/Paxos => [L, 2L] (sem diverg??ncia);
  - Primary-Backup => [l, L] (asynchronous/lazy);
  - Multi-Master => l (allowing divergence).
- ?? imposs??vel ter sempre linearizability (CAP theorem);
- **Eventually consistent** - Para sistema ser reliable ?? preciso trade-offs
  entre consist??ncia e disponibilidade. Caso especial de **weak consistency**.
  Quando h?? update, se n??o houver mais updates eventualmente todos os read v??o
  ver esse update.

### Session guarantees

- Read Your Writes;
- Monotonic Reads - reads sucessivos v??m as mesmas ou cada vez mais escritas;
- Write Follow Reads - escritas s??o propagadas depois dos reads em que dependem.
  Writes numa sess??o s?? podem ser feitos ap??s writes cujo efeito foi visto por
  reads passados na sess??o;
- Monotonic Writes - Escrita s?? em incorporada numa copia do servidor se a c??pia
  j?? tiver incorporado as escritas anteriores da sess??o.

### Conflict-Free Replicated Data Types (CRDTs)

- Convergir depois de updates concurrentes => favorece availability e
  partition-tolerance (CAP);
- E.g. counters, sets, mv-registers, maps, graphs;
- **operation-based**:
  - todas as opera????es s??o comutativas (inc/dec);
- **state-based**:
  - Estados em conseguimos definir a opera????o de `join`;
  - `join` ?? indepotente;
  - `join` ?? associativo;
  - `<=` reflete monotonia na evolu????o do estado;
  - Updates t??m de respeitar `<=`.
  - **Eventual Consistency, non stop** - `upds(a) C= upds(b) => a <= b`

### Principle of permutation equivalence

Se as opera????es numa execu????o sequencial podem comutar (preservando um
resultado), as mesmas numa execu????o concurrente podem comutar (preservando **o
mesmo** resultado).

![Permutation equivalence](img/permutation_equivalence.png)

- E.g. para um contador, guardamos pares (Incremento, Decremento);
- Join faz point-wise max nas entries;
- Valor do counter ?? soma de todos os Incs - soma de todos os Decs.

```
B(10, 0) --> {A(35, 0), B(10, 0)} -|
         |                         v
         --> {B(10, 0), C(0, 5)} ----> {A(35, 0), B(12, 0), C(0, 5)}
```

### Registers

- ?? um ordered set de write operations;
- **Simple approach**: Last Writer Wins. Usamos timestamps para discartar older
  writes.
  - **Problema:** se um peer tiver o rel??gio atrasado em rela????o a outro vai ser
sempre rejeitado.
  - O `apply()` ?? comparar timestamps e manter aquele que tem a maior;
  - O `join()` ?? escolher o que tem o maior timestamp.
- Um register mostra valor _v_ na replica _i_ se:
  - `wr(v)` for uma das opera????es que ocorreu em _i_ (_Oi_);
  - N??o existir nenhum _v'_ em _Oi_, tal que `wr(v) < wr(v')`.
- **Preservation of sequential semantics** - Semanticas concurrentes devem
  preservar as semanticas sequenciais. Isto tamb??m assegura execu????o sequencial
  correta em sistemas distribuidos;

#### Multi-value Registers

- N??o existe wr(v'), tal que $wr(v) \in O_i$ e wr(v) <(ordem parcial) wr(v');

![Multi-value registers](img/multi_value_register.png)

- Podemos implementar com **version vectors**;

![Multi-value register with version vector](img/multi_value_reg_vv.png)

### Sets

- **Synch:** Temos todos os _e_ que foram `add` e n??o t??m `remove` ?? frente;
- **Asynch:** Usamos **Add-Wins**.
  - Temos todos os _e_ que foram `add` e n??o t??m `remove` ?? frente (ordem
    parcial);
  - ?? o mesmo para ordem parcial.

![Sets concurrent execution](img/sets_concurr.png)

- No exemplo, ficamos com o _x_, porque existe um `add(x)` sem `remove(x)` ??
  frente (no ramo do B).

- set ?? um par (payload, tombstones);
- `apply(add)` - cria tag de add;
- `apply(rmv)` - cria uma tag nas tombstones por cada add do elemento;
- `eval` - todos os adds que nao est??o nas tombstones;
- `merge` - par (uni??o de payloads, uni??o de tombstones);

- Concurrent executions can have **richer outcomes**.
  - Com as opera????es abaixo, acabamos com {x, y};
  - N??o ?? poss??vel fazer uma execu????o sequencial que resulte nisso: fica sempre
    um `rmv` em ??ltimo.

![Equivalence to sequential exec](img/set_not_seq.png)

## Quorum Consensus

- Client comunica com server/replicas diretamente;
- Todos as r??plicas s??o iguais entre si;
- Cada opera????o (read/write) requer um **quorum**;
- Se uma opera????o depende de outra, **quorums** t??m de ter servers em comum;

### Read/Write quorums must overlap

- Replicas s?? fazem `read` e `write` => aplicadas a objetos num todo;
- Como output de `read` depende de `write` pr??vio, o read quorum tem de dar
  overlap ao write quorum:
  - $N_R + N_W > N$
  - Tamanho de read quorum + write quorum tem de ser maior que o n??mero de
    replicas.

### Implementation

- Cada objeto tem um **version number**;
- **Read**:
  - Poll a read quorum para encontrar o current version number:
    - servers respondem com a vers??o atual;
    - pode haver um com uma ver??o maior.
  - Ler o objeto de uma replica atualizada (version number maior);
- **Write**:
  - Poll a write quorum para descobrir vers??o atual;
  - Escreve novo valor com nova vers??o num write quorum (assumimos que
    alternamos o objeto completo);
- **`write` depende de `write` pr??vio** (atrav??s de vers??es). Write quorums t??m
  de dar overlap $N_W + N_W > N$ => isto previne incoer??ncia.

#### Naive implementation with faults

- Se hover parti????o na rede durante write, replica que foi atualizada pode ser
  isolada da rede;
- Version bump n??o ?? propagado a tempo para o quorum todo;
- Algu??m faz write num novo quorum de escrita (d?? overlap com o antigo) mas o
  version bump vai ser o mesmo (antigo n??o foi visto);
- Parti????o da rede resolve-se => 2 objetos diferentes com a mesma vers??o;
- **Solu????o:** transa????es.

#### Naive implementation with concurrent writes

- Fomos buscar a vers??o ao mesmo tempo (2 clientes) => temos as mesmas vers??es;
- Ao dar o bump, ficamos com a mesma version;
- Os writes v??o possivelmente dar 2 objetos diferentes em r??plicas diferentes,
  mas com a mesma vers??o;
- **Solu????o:** transa????es.

### Ensuring Consistency with Transactions

- Usar um algoritmo de atomic commitment, e.g. **two phase commit**;
- O acesso ?? mesmo, mas ?? feito no contexto de uma transa????o;
- Cliente age como coordenador e as r??plicas no quorum agem como participantes;
- Quando **faults**:
  - Na parti????o, s?? a r??plica que ficou de fora respondia com commit;
  - As replicas que n??o viram update => n??o respondem ou respondem abort;
  - Client cancela transa????o;
  - O outro client d?? commit na boa.
- Quando **concurrent writes**:

  - Quando se inicia o write, obtem-se um **lock**;
  - Aquele que chega primeiro fico com um lock para a sua transa????o => outra
    transa????o tem de esperar;
  - Ap??s o primeiro two-phase commit (assumir sucesso), o segundo d?? unlock;
  - Ao tentar escrever, pelo menos 1 das r??plicas (sobreposi????o de write
    quorums) vai ver que est?? a invalidar um n??mero de vers??o e aborta =>
    abortando assim a transa????o toda.

- **Problemas:**
  - **Deadlocks** s??o poss??veis se transa????es usarem locks;
  - Se o coordenador falahar num tempo mau h?? blocking desnecess??rio;
  - Pode ser util user **coordinator proxy servers** em vez dos clientes
    coordenarem;
  - Transa????es trazem availability problems.

### Playing with Quorums

- Manipulando $N_R$ e $N_W$, podemos brincar com os trade-offs de performance e
  availability;
- **read-one/write-all protocol**:
  - est??o todos no write quorum;
  - 1 deles tamb??m (sobreposi????o) est?? no read quorum (sozinho);
  - Leitura bu?? acess??vel e fast, mas write bu?? pesado => basta 1 server estar
    inacess??vel que j?? n??o conseguimos um write quorum. O write em todos, faz
    com que tenhamos de ligar a todas as r??plicas.
- Quando baixamos $N_R$ temos de subir $N_W$ => trade-off => n??o d?? para melhor
  leitura e escrita simultaneamente.

### Quorum consensus fault tolerance

- Tolera unavailability de r??plicas:
  - inclui unavailability causada pelo processo ou pelo meio de comunica????o
    (incluindo parti????es);
  - quorum n??o destingue entre os 2 tipos de falhas.
- No m??nimo, s?? precisamos de (metade + 1) (N/2 + 1) das r??plicas a funcionar,
  sendo que uma delas tem de fazer parte do quorum de escrita anterior. E.g.:
  - 12 r??plicas;
  - 12/2 + 1 = 7;
  - 7 + 6 = 12 => 7 de escrita + 6 leitura em que algumas r??plica est??o nos 2
    quorums.

### Dynamo quorums

- Usa quorums para aumentar a disponibilidade;
- N??o usa transa????es;
- Usa **version vector** em vez de n??mero de vers??o;
- Cada key est?? associada a um set de server => **preference list**. Os
  primeiros N nas lista s??o as main r??plicas e os outros s??o backup r??plicas
  (para certos cen??rios de falhas);
- Cada opera????o tem um coordenador => uma das main r??plicas da lista. Este
  desempenha as fun????es do cliente no quorum;

#### put(key, value, context)

- **context** - set de version vectors;
- Gera novo n??mero de vers??o (determinada pelo context) e escreve valor
  localmente;
- Envia (key, value) e seu version vector para as main r??plicas (da preference
  list) => success se pelo menos W-1 replicas responderem

#### get(key)

- Coordenador pede todas as vers??es do par (key, value) e seus version vectors
  das main r??plicas restantes da preference list;
- Ap??s receber pelo menos R-1 respostas, retorna todas aquelas que tenham o
  **version vector** maximal:
  - Se forem retornadas m??ltiplas vers??es, a aplica????o que fez `get` ??
    respons??vel por fazer `put` da reconsilia????o da informa????o.

---

- Se n??o acontecerem falhas => dynamo d?? **strong consistency**.
- Quando h?? falhas pode n??o ser poss??vel obter um quorum => **sloppy quorum**:
  - Usamos os backup servers: tenham metadata que identifica quem ?? suposto ter
    a c??pia certa (**votos para quem tem a info boa**);
  - Em writes, podem ficar a segurar dados enquanto esperam que as r??plicas que
    est??o a substituir voltem a vida => entregam-lhes os objetos quando elas
    voltarem;
  - **Problema:** n??o assegura consistency pk o write quorum pode n??o dar
    overlap com o anterior.

## Quorum-Consensus Replicated (Abstract Data Types) ADT

- Abstrair abstract data types em quorums;
- Quando executamos uma opera????o:
  - read from **initial quorum**;
  - write to **final quorum**.
- `read` operation => inicial ?? quorum de leitura e final ?? vazio;
- **quorum** de uma opera????o ?? um set de r??plicas que inclui um initial e um
  final quorum;
- Assumindo que todas as r??plicas s??o consideradas iguais, um **quorum** de uma
  opera????o ?? um par `(m, n)` cujos elementos s??o os tamos do quorum inicial (m)
  e final (n).

```plain
 ->write -----------> read
|   |
 ---
```

Quorum intersection graph.

- Todas as opera????es (read/write) passam a ter um initial (ler) e final
  (escrever) quorum.

### Constraints

- Quorum final da opera????o de escrita tem de intersetar cada quorum inicial da
  opera????o de leitura => **leitura v?? sempre o meis recente**;
- Quorum final da opera????o de `write` tem de intersetar cada quorum inicial de
  `write` => **versions s??o updated corretamente**.
- E.g. minimal (size) quorum for an object with 5 replicas:

| Operation | quorum choices | ...    | ...    |
| --------- | -------------- | ------ | ------ |
| read      | (1, 0)         | (2, 0) | (3, 0) |
| write     | (1, 5)         | (2, 4) | (3, 3) |

### Replicated queue

- **Enq** - add item to queue;
- **Deq** - remove elemento mais recente da queue (exception se empty):
  1. Ler um initial quorum de read para obter vers??o da queue;
  2. Ler state numa updated replica;
  3. Se a fila n tiver vazia => **normal deq**. Abnormal deq acontece otherwise:
  - Remover item da head da queue;
  - Write novo queue state para o final **write quorum**;
  - Returnar item removido.
- Minimal quorum:
  - ?? s?? juntar a tabela anterior;
  - Enq (1 read e 1 write) - (1,5); (2,4); (3,3);
  - Normal Deq (1 read e 1 write) - (1,5); (2,4); (3,3);
  - Abnormal Deq (1 read) - (1,0); (2,0); (3,0);
  - S?? a ??ltima hip??tese ?? que faz sentido pk s?? precisa de 3 r??plicas.

## Heligh's Replication Method

- Usa timestamps em vez de version number;
- Em vez de manter vers??es do estado, replicas mant??m logs => mais flex??vel;
- **Assumption:** Clients conseguem gerar timestamps que podem ser totally
  ordered:
  - order consistent with linearizability (omniscient observer);
  - hierarchical timestamp: 1 field para ordenar transactions e outro para
    ordenar dentro de uma transaction.

### Read

Similar ao version-based mas comparamos timestamps instead;

### Write

- N??o h?? necessidade de ler vers??o de um initial quorum => n??o ?? necess??ria a
  initial message round;
- Cliente s?? precisa de escrever o novo estado para o final quorum (**suitable
  only for whole state changes**).

```
write -----> read
```

Quorum intersection graph.

- Minimal quorum choices (para 5 replicas):
  - Read - (1,0), (2,0), (3,0), (4,0), (5,0)
  - Write - (0,5), (0,4), (0,3), (0,2), (0,1)

### Event Logs

- **Event** - mudan??a de estado ?? um par (Operation, Outcome):
  - **Operation** - Read() ou Write(x);
  - **Outcome** - Ok() or Ok(x);
- **Log** - timestamped events.

### Implementation de uma replicated Queue

Exemplo de Deq;

- Pede logs a um initial quorum e cria uma view;
  - **view** ?? o merge dos logs por ordem das timestamps;
  - Discarta dups (same log).
- Reconstroi queue a partir da view e encontra item para returnar;
- Se a queue n??o estava vazia:
  - regista event => append ?? view;
  - envia a view modificada para um final quorum.

**Nota:** No Enq o cliente n??o envia a vista modificada no fim, s?? a opera????o.

#### Constraints

![rep queue](img/herlihy_rep_queue.png)

- Terceira coluna favorece abnormal deq que n??o muda state => n??o faz sentido;
- Balanced approach ?? cool.

#### Optimizations

Logs podem crescer indefinidamente.

- **Garbage coollection**:
  - Se um item foi deq, todos os items com earlier timestamp tmb tiveram de ser;
  - Podem ser readded com log merge;
  - **Horizon timestamp** - timestamp of the most recent deq;
  - Log vem s?? com timestamps maiores que **horizon timestamp**.
- **Cache logs at clients**.

### Issues

- Damos rely em timestamps gerados pelo client:
  - hierarchical timestamps podem resolver;
  - precisam de transa????es.
- Logs t??m de ser garbage collected:
  - garbage collection dependem da ADT a ser implementada;
  - queue's ?? OK mas em outros ADT pode ser mais dif??cil.

## Consensus with Byzantine Failures

- **Byzantine Generals Problem (BGP)**:
  - **Agreement** - todos os processos OK entregam a mesma mensagem;
  - **Validity** - se o broadcaster for OK, todos os OK entregam a mensagem do
    broadcaster.
- **General rule** - os processos byzontinos t??m de ser menos de 1/3 dos
  processos => BGP n??o tem solu????o em sistemas com 3 (ou menos) processos (a n??o
  ser que messages sejam **signed**).

![Byzantine comm with 3 peers](img/byz_signed_3.png)

- U - set dos servers;
- Quorum system - $Q C= 2^U$ - Todo Q E $Q$ ?? um quorum;
- $B$ - subsets de U que n??o est??o contidos um no outro. Alguns B E $B$ cont??m
  todos os faulty servers.

## Quorum consensus with Byzantine failures

- Cada opera????o precisa de um quorum;
- Se o resultado de uma opera????o depende do resultado de outra => quorums t??m de
  dar overlap;

### Access Protocol: Asynchrony

- Get replies from all servers in a quorum. Byzonte can fail by not responding;
- At any time, there must be a quorum of non-faulty servers => might need to
  attempt operation multiple times on different quorus => eventually make
  progress.

### Access Protocol: Beginning and End Events

- **write**:
  - **begin** - when the client initiates the operation;
  - **end** - when all corrent servers in some quorum have processed the update.
- **read**:
  - **begin** - when the client initiates the operation;
  - **end** - when the `Result()` function return, thus determining the read
    result.
- _op1_ precedes _op2_ if _op1_ ends before _op2_;
- _op1_ and _op2_ are concurrent if neither precedes the other.

## Size-based Byzantine Masking Quorums

![Masking Quorum](img/masking_quorum.png)

- **M-consistency** - ensures that a client always obtains an up-to-date-value
  => need to find it;
- Every pair of quorums must intersect in at least $2 * f + 1$ servers:
  - let f be the bound on faulty servers;
  - We need at least $f + 1$ up-to-date non-faulty servers => outnumber the
    faulty ones;

![Byzonte upper bound](img/byzonte_upper_bound.png)

- **M-availability** - required for liveness;
- $n - f >= q$
  - f upper bound byzonte;
  - q size of a quorum;
  - n number of servers;

Combining the inequalities => $q = 3 * f + 1$

## Non-byzantine Read-Write Quorums based on size

- $w >= f + 1$ - ensures writes survive failures;
- $w + r > n$ - ensures that reads see most recent write;
- $n - f >= r$ - ensures read availability;
- $n - f >= w$ - ensures write availability.
- => $n > 2 * f$
  - Let $n = 2 * f + 1$;
  - All conditions are valid;
  - Apparently increasing _n_ only worsens performance, increases fault
    tolerance (f can rise).

### Read operation

- Query servers at?? ter reply de $3 * f + 1$ different servers;
  - _A_ ?? o set de value/timestamp pairs recebidos de pelo menos $f + 1$
    servers;
  - O _A_ ?? os pares que pelo menos $f + 1$ servers reportaram;
  - _A_ pode ser vazio se houver replicas que ainda n atualizaram valor.
- Fazer `Result(A)` => retorna o valor com maior timestamp (ou vazio se A for
  vazio).

### Naive Implementation under faults/concurrent writes

- Alguem tem de chegar ?? maioria primeiro => ganha;
- Se ninguem chegar a maioria, valor ?? considerada faulty nos reads e o Result
  n??o d?? nada => escreve-se por cima depois.

## State machine replication

### Impossibility of consensus with faulty process

- **Consensus problem** - cada processo come??a com um input value de um set _V_
  e t??m de decidir num valor de _V_;
- **Safety:**
  - **Agreement** - todos os proc tomam a mesma decis??o;
  - **Validity** - o valor est?? no set de valores poss??veis.
- **Liveness** - todas as execu????es do protocolo decidem num valor;
- **FLP's impossibility result:** num sistema asynch em que pelo menos 1 proc
  possa falhar, n??o h?? nenhum algoritmo determinista de consensus que seja
  **live** e **safe** (mesmo que a rede seja fi??vel).
  - N??o d?? para distinguir crash de slow;
  - Se proc n??o decide => ficamos stuck => viola liveness;
  - Se proc decide independentemente da decision rule => possivelmente viola
    safety.

### Views and Leaders

- **View** - configura????o de sistema numerada:
  - replicas passam por uma sucess??o de **views**;
  - cada **view** tem um **leader**: $p = v mod n$
  - v - view number; $n = 3 * f + 1$ - numero de replicas;
  - A **view** muda quando o leader atual ?? suspeito.

### Algoritmo (SMR)

- Client envia um pedido para executar uma opera????o ao **leader**;
- Leader **atomically bradcasts** the request para todas as replicas;
  - garante um ordem total na entreda de mensagens de non-faulty replicas;
- Replicas executam o pedido e enviam a resposta ao client;
- O client espera por respostas com o mesmo resultado que $f + 1$ replicas.

### Client

- O pedido do client (enviado ao leader) tem uma timestamp, t => garante
  **exactly once** semantics;
  - Monotonically increasing;
  - Todas as mensagens das replicas para o cliente inclu??m o current view
    number, v => client track o leader atual;
- O cliente espera por $f + 1$ replies com **assinaturas v??lidas** (com o mesmo
  t e r).
  - r - resultado;
- Se o cliente n??o obtiver respostas suficientes num intervalo de tempo:
  - Broadcast para todas as replicas;
  - Se pedido j?? foi processado, enviam resposta again;
  - Se n??o, replicas enviam o pedido para o **leader** => se este n??o fizer o
    multicast do pedido para o grupo, vai ser suspeito de ser faulty pelas
    replicas.

## PBFT - Quorums and Certificates

- PBFT uses quorums to implement atomic multicast;
- **Intersection** - any two quorums have at least a correct replica in common
  (they intersect in $f + 1$ replicas);
- **Availability** - there is always a quorum with no faulty replicas;
- Messages are sent to replicas;
- Replicas collect **quorum certificates**:
  - **Quorum certificate** - ?? um set com uma mensagem por cada elemento num
    quorum => assegura que a informa????o relevante foi guardada;
  - **Weak certificate** - set com pelo menos $f + 1$ mensagens de diferentes
    replicas => o set que um cliente tem de receber antes de returnar um
    resultado ?? um **weak certificate** (**Reply certificate**).

### Replicas

- Estado de uma replica:
  - Estado do servi??o;
  - Message log - mensagens que a replica aceitam;
  - View id - current view id da replica.
- Quando leader, l, recebe um client request, m => come??a three-phase protocol
  de multicast atomico (para todas as replicas):
  - **Pre-prepare**;
  - **Prepare** - garante total order dos requests numa view em conjunto com o
    **pre-prepare**;
  - **Commit** - garante total order de requests entre views.

### Pre-Prepare Phase

- Leader:
  - D?? um sequence number, _n_, ao request (monotonically increasing);
  - Multicast da mensagem de PRE-PREPARE para as outras replicas. Leva um digest
    da mensagem, _n_, e _v_.
- Replica aceita a PRE-PREPARE se:
  - estiver na view _v_;
  - As assinaturas no request m e na PRE-PREPARE message forem v??lidos (e d for
    digest de m);
  - Ainda n??o tiver aceite uma PRE-PREPARE para _v_ com _n_ e digest diferente;
  - _n_ tem de estar entre low e high watermark => previne faulty leader de
    esgotar o sequence number space selecionando um muito grande.

### Prepare Phase

- Ao receber PRE-PREPARE message, a replica entra em **prepare phase**:
  - multicast para as outras replicas de PREPARE.
- Ao receber PREPARE, a replica/leader aceita se:
  - A view for a mesma que a sua atual;
  - A assinatura estiver correta;
  - O sequence number tiver dentro das water marks.
- **Prepared Certificate** - cada replica coleta para o request _m_, em _v_, com
  _n_:
  - 1 PRE-PREPARE;
  - $2 * f$ PREPARES de outras replicas;
- Ap??s certificado obtido, a replica sabe a ordem do request na view atual.

**Total order within a view** - com estas 2 phases, uma replica n??o pode obter
um certificado de requests diferentes com a mesma view e sequence number.

### Commit Phase

2 phases anteriores n??o garantem ordem entre view changes.

- Apos obter um prepared certificate:
  - Replica entrea commit phase;
  - Multicast de COMMIT para todas as replicas.
- Aceita COMMIT com os mesmos crit??rios que PREPARE;
  - Replica pode receber COMMIT antes de estar na commit-phase.
- **Commit certificate** - $2 * f + 1$ COMMIT messages (same _v_, _n_, _d_) de
  diferentes replicas;
- **Committed request** - se replica tiver **prepared e committed
  certificates**.

Guarante que se uma r??plica se comprometer com um pedido, o pedido foi preparado
com pelo menos $f + 1$ non-faulty replicas.

### Request delivery and execution

- Executa pedidos committed quando executar todos aqueles com _n_ inferior =>
  non-faulty replicas execute requests in the same order.
- Replicas reply to the client after executing the reqeusted operation:
  - Discard requests com timestamp menor que a ??ltima timestamp que enviaram ao
    client => **exactly-once semantics**.

### Garbage collection + Checkpoints

- N??o podemos apagar logs de pedidos j?? committed pk podemos usar em view
  changes;
- Replica repair/replace => need state synch;
- Replica periodicamente cria **checkpoint** do seu estado (prova de corre????o);
  - Depois de provado => **stable checkpoint**.
  - Prova requer troca de mensagens;
- Replica mant??m v??rias c??pias do seu estado:
  - ??ltima c??pia est??vel;
  - 1 ou mais checkpoints ainda n??o est??veis;
  - estado atual.

### Checkpoint Proof Generation

- Replica faz multicast de CHECKPOINT message. Cont??m:
  - n - sequence number do last request executado no estado;
  - d - digest do state.
- CHECKPOINT messages recebidas s??o guardadas at?? **stable certificate** ser
  obtido;
- **stable certificate** - weak certificate ($f + 1$) de CHECKPOINT messages
  assinadas por diferentes replicas (**incluindo a si mesma**). isto para um _n_
  e _d_.
- Quando ?? coletado **stable certificate** a replica discarta:
  - PRE-PREPARE, PREPARE, COMMIT messages com _n_ menos;
  - Checkpoints mais antigos (e respetivas CHECKPOINT messages).
- Low e High watermarks s??o avan??adas quando se come??ar um novo checkpoint.

## Blockchain - BitCoin

- Cadeia de blocos => set de eventos (tamb??m pode guardar estado);
- Cada bloco cont??m um **header** com metadata;
  - Inclui refer??ncia para o bloco anterior na chain;
- Primeiro bloco na chain ?? o **genesis block**;
- Blocos s??o appended ?? **blockchain head** (most recently added block);
- O tamanho m??ximo de um bloco ?? 1MByte.

### Network

- P2P em que cada n?? se liga 8 n??s;
- Usa um mecanismo de boostrapping;
- N??o existe limite de n??s ent??o um n?? (caso aceite conec????es) pode ligar-se a
  muitos mais n??s;
- Peers mant??m uma c??pia da blockchain inteira.

### Consensus

- ?? preciso concordar no conte??do de blocos e sua ordem;
- Algorimos tradicionais que suportam falhas bizantines d??o rely em quorums:
  - ?? dificil saber quantos n??s existem na rede;
  - ?? facil criar m??ltiplas identidades (Sybil attack).
- **Solution:** **proof-of-work**.

#### Proof-of-Work (PoW)

- Resolver um puzzle cryptographico que leva um tempo aleat??rio (mas grande) a
  resolver;
  - Encontrar um **nonce** para por no header de um bloco de forma a que o seu
    SHA-256 seja menor que um **target** conhecido a priori.
- SHA-256 ?? non-invertible => temos de fazer brute force.
- O **target** ?? ajustado de modo a que seja gerado 1 bloco a cada 10 minutos:
  - Block rate ?? independente do hash-power da network;
  - S??o espectados $2^{256}/target$ hashes para resolver o puzzle;
  - Bitcoin ajusta o **target** a cada 2016 blocos (expectado equivaler a 14
    dias).

#### Block Broadcasting

- Ap??s resolver PoW, n?? d?? broadcast do novo bloco;
- Ap??s receber um bloco, um n??:
  - Verifica se ?? v??lido: verifica PoW e transactions;
  - Se bloco ?? valido, n?? para de procurar PoW e adiciona o novo bloco ?? cabe??a
    da blockchain;
  - Propaga novo bloco.
- Em ambos os casos, o n?? come??a a trabalhar no pr??ximo PoW;
- Quando um n?? recebe um bloco, podem faltar antecessores ?? chain:
  - N?? procura missing blocks.

#### Anti-Entropy

- Ap??s validar novo bloco, n?? envia aos seus vizinhos mensagens contendo um set
  de hashes de blocos que ele tem => inv message;
- Se n?? recebe mensagem com hash de blocos que ele n??o tem, pede os blocos a
  quem enviou a mensagem => getdata message;
- Quando n?? recebe `getdata`, envia os blocos pedidos => block message;
- Quando bloco ?? gerado, ?? inserido na rede usando um `block` message n??o
  solicitada.

#### Block propagation delay

- Valida????o de um bloco adiciona delay;
- Valida????o ?? repetida em cada hop;
- Block propagation delay has a **long tail distribution**.

#### Forks

- ?? poss??vel que mais do que 1 peer possa resolver um PoW;
- O propagation delay grande o que pode contribuir para isto;
- Este evento chama-se **fork**;
- Se hourve conflitos, a blockchain mais comprida (com mais PoW) ?? que ganha;
  - Troca para a nova quando souber da sua existencia.
- N??o h?? 100% garantia que um bloco v?? persistir na chain:
  - Chance de ser removido diminui com cada bloco adicionado em cima;
  - 6 confirmations s??o consideradas final (chance);
  - **code-based checkpointing** - a hash de um bloco que n??o pode ser replaced
    (nem os que o precedem) foi hardcoded.
- **Eventual consistency** with high probability:
  - Assumindo que o hash power do adversario ?? limitado;
  - Se for maior que 50%, ele eventualmente pode mudar tudo.
- Selfish mining strategies podem n??o anunciar logo o seu PoW e come??ar a
  trabalhar numnovo at?? algu??m anunciar o seu => come??ar a minar o seguinte mais
  cedo ent??o t??m maior chance de o substituir.

### Scalability

- Blockchain cresce 60GB por ano;
- Fazemos broadcast em rede muito grande;
- S?? temos 1MB por cada 10 minutos;
- S?? podemos fazer pouca transactions por segundo;
